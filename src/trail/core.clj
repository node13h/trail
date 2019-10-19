(ns trail.core
  (:require [trail.handler :as handler]
            [mount.core :as mount]
            [trail.cli :as tc]
            [migratus.core :as m]
            [trail.config :refer [settings]]
            [taoensso.timbre :as timbre])
  (:gen-class))

;; Mute messages like:
;; DEBUG [org.eclipse.jetty.util.log:181] - Logging to com.github.fzakaria.slf4j.timbre.TimbreLoggerAdapter@55f05a73 via org.eclipse.jetty.util.log.Slf4jLog
;; INFO [org.eclipse.jetty.util.log:193] - Logging initialized @18604ms to org.eclipse.jetty.util.log.Slf4jLog
(timbre/set-level! :warn)
(require '[ring.adapter.jetty :as jetty])

(defn run-jetty
  "Start Jetty"
  []
  (let [config {:port (:port settings)
                :join? false}]
    (jetty/run-jetty handler/app config)))

(mount/defstate http-server
  :start (run-jetty)
  :stop (.stop http-server))

(defn start-app
  "Start application only"
  []
  (mount/start-without #'http-server))

(defn stop-app
  "Stop application only"
  []
  (mount/stop-except #'trail.core/http-server))

(defn start-server
  "Start application server"
  []
  (mount/start))

(defn stop-server
  "Stop application server"
  []
  (mount/stop))

(defn exit
  [status msg]
  (println msg)
  (System/exit status))

(defn migrate!
  [options]
  (let [config {:store :database
                :migration-dir "migrations"
                :db (:database-url settings)}]
    (cond
      (:backwards options) (m/rollback config)
      :else (m/migrate config))))

(def log-levels [:error :warn :info :debug])

(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (tc/validate-args args)]
    (timbre/set-level! (get log-levels (:verbosity options) :debug))
    (mount/start #'settings)
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
        "start" (start-server)
        "migrate" (migrate! options)))))
