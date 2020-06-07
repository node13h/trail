(ns trail.state.sql
  (:require [mount.core :as m]
            [conman.core :as c]
            [clj-time.jdbc]
            [clojure.java.jdbc :as jdbc]
            [environ.core :refer [env]]
            [cheshire.core :refer [generate-string parse-string]]
            [trail.config :refer [settings]]
            [trail.leases :as tl])
  (:import org.postgresql.util.PGobject
           clojure.lang.IPersistentMap))

(m/defstate ^:dynamic *db*
  :start (c/connect! {:jdbc-url (:database-url settings)})
  :stop (c/disconnect! *db*))

(c/bind-connection *db* "sql/queries.sql")

(defmacro with-transaction
  [& forms]
  `(c/with-transaction [*db*]
    ~@forms))

(extend-protocol jdbc/IResultSetReadColumn
  PGobject
  (result-set-read-column [pgobj _metadata _index]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "inet" (java.net.InetAddress/getByName value)
        "jsonb" (parse-string value true)
        value))))

(defn to-pg-json [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (generate-string value))))

(extend-protocol jdbc/ISQLValue
  IPersistentMap
  (sql-value [value] (to-pg-json value))
  java.net.InetAddress
  (sql-value [value] (.getHostAddress value)))

(defn release!
  "Release a lease reconstructing the tail if necessary"
  [{:keys [ip end-date]}]
  (with-transaction
    (-> {:ip ip
         :end-date end-date}
        add-release!)
    (when-first [lease (-> {:ip ip
                            :from-date end-date
                            :to-date end-date}
                           selection)]
      (let [lease-id (:id lease)]
        (-> {:id lease-id
             :end-date end-date}
            truncate-lease!)
        (-> {:lease-id lease-id
             :at-date end-date}
            delete-renewal!)
        (when-let [tail-start-date (-> {:lease-id lease-id
                                        :after-date end-date}
                                       first-renewal-after
                                       :at-date)]
          (let [inserted-id (-> lease
                                (tl/adjust-start-date tail-start-date)
                                add-or-update-lease!
                                :id)]

            (-> {:lease-id lease-id
                 :at-date tail-start-date}
                delete-renewal!)

            (-> {:lease-id lease-id
                 :to-lease-id inserted-id
                 :from-date tail-start-date}
                move-renewals!)))
        {:id lease-id}))))

(defn add!
  "Add the lease to the store merging with existing ones if necessary"
  [lease]
  (with-transaction
    (let [lease (->> lease
                     get-released
                     :end-date
                     (tl/truncated lease))
          adjacent (-> {:ip (:ip lease)
                        :mac (:mac lease)
                        :from-date (:start-date lease)
                        :to-date (tl/end-date lease)
                        :lock true}
                       selection)
          sorted-all (tl/sorted (conj adjacent lease))
          merged-lease (peek (tl/fused sorted-all))  ;; All leases in the source coll overlap, so there should be only one result
          inserted-id (-> merged-lease
                          add-or-update-lease!
                          :id)
          redundant-leases (remove #(= inserted-id (:id %1)) adjacent)]
      (when-not (tl/same-lease? merged-lease lease)
        (-> {:lease-id inserted-id
             :at-date (:start-date lease)}
            add-renewal!))
      (when (seq redundant-leases)
        (doseq [redundant-lease redundant-leases
                :let [redundant-id (:id redundant-lease)]]
          (-> {:lease-id inserted-id
               :at-date (:start-date redundant-lease)}
              add-renewal!)
          (-> {:lease-id redundant-id
               :to-lease-id inserted-id}
              move-renewals!))
        (-> {:ids (map :id redundant-leases)}
            delete!))
      {:id inserted-id})))

(defn trim!
  "Delete objects ending before the end-date"
  [params]
  (with-transaction
    ((juxt trim-leases! trim-releases!) params)))
