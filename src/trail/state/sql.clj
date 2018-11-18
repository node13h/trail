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
        "inet" (str value)
        "macaddr" (str value)
        "jsonb" (parse-string value true)
        value))))

(defn to-pg-json [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (generate-string value))))

(extend-protocol jdbc/ISQLValue
  IPersistentMap
  (sql-value [value] (to-pg-json value)))

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
      (let [lease-id (:id lease)
            cut-offset (tl/interval-seconds (:start-date lease) end-date)]
        (-> {:id lease-id
             :end-date end-date}
            truncate-lease!)
        (-> {:lease-id lease-id
             :offset cut-offset}
            delete-slice!)
        (when-let [tail-offset (-> {:lease-id lease-id
                                    :offset cut-offset}
                                   first-slice-after
                                   :offset)]
          (let [inserted-id (-> lease
                                (tl/offset-begining tail-offset)
                                add-or-update-lease!
                                :id)]

            (-> {:lease-id lease-id
                 :offset tail-offset}
                delete-slice!)

            (-> {:lease-id lease-id
                 :to-lease-id inserted-id
                 :delta (- tail-offset)
                 :from tail-offset}
                move-slices!)))
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
          first-lease (first sorted-all)
          last-lease (last sorted-all)
          merged-lease {:ip (:ip first-lease)
                        :mac (:mac first-lease)
                        :start-date (:start-date first-lease)
                        :duration (tl/duration-span first-lease last-lease)
                        :data (:data first-lease)}
          inserted-id (-> merged-lease
                          add-or-update-lease!
                          :id)
          redundant-leases (remove #(= inserted-id (:id %1)) adjacent)
          offset-from-merged (tl/seconds-between merged-lease lease)]
      (when-not (zero? offset-from-merged)
        (-> {:lease-id inserted-id
             :offset offset-from-merged}
            add-slice!))
      (when (seq redundant-leases)
        (doseq [redundant-lease redundant-leases
                :let [offset-from-merged (tl/seconds-between
                                          merged-lease redundant-lease)
                      redundant-id (:id redundant-lease)]]
          (-> {:lease-id inserted-id
               :offset offset-from-merged}
              add-slice!)
          (-> {:lease-id redundant-id
               :to-lease-id inserted-id
               :delta offset-from-merged}
              move-slices!))
        (-> {:ids (map :id redundant-leases)}
            delete!))
      {:id inserted-id})))

(defn trim!
  "Delete objects ending before the end-date"
  [params]
  (with-transaction
    ((juxt trim-releases! trim-leases!) params)))
