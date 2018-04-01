(ns trail.state.sql
  (:require [mount.core :as m]
            [conman.core :as c]
            [clj-time.jdbc]
            [clojure.java.jdbc :as jdbc]
            [environ.core :refer [env]]
            [cheshire.core :refer [generate-string parse-string]]
            [trail.config :refer [settings]])
  (:import org.postgresql.util.PGobject
           clojure.lang.IPersistentMap))

(m/defstate db
  :start (c/connect! {:jdbc-url (:database-url settings)})
  :stop (c/disconnect! db))

(c/bind-connection db "sql/queries.sql")

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
