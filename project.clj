(defproject trail "1.2.3-SNAPSHOT"
  :description "IP history management API"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [metosin/compojure-api "1.1.11"]
                 [metosin/ring-swagger "0.26.0"]
                 [clj-time "0.14.2"]
                 [ring "1.6.3"]
                 [mount "0.1.11"]
                 [migratus "1.0.6"]
                 [com.taoensso/timbre "4.10.0"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [org.slf4j/jcl-over-slf4j "1.7.25"]
                 [com.fzakaria/slf4j-timbre "0.3.8"]
                 [org.postgresql/postgresql "42.2.1"]
                 [conman "0.7.5"]
                 [environ "1.1.0"]
                 [org.clojure/tools.cli "0.3.5"]]
  :ring {:handler trail.handler/app}
  :uberjar-name "server.jar"
  :main trail.core
  :migratus {:store :database :db ~(get (System/getenv) "DATABASE_URL")}
  :plugins [[migratus-lein "0.5.7"]]
  :profiles {
             :uberjar {:aot :all
                       :omit-source true}
             :repl {:main user}
             :dev {:source-paths ["dev"]
                   :dependencies [[javax.servlet/javax.servlet-api "3.1.0"]
                                  [cheshire "5.5.0"]
                                  [ring/ring-mock "0.3.2"]
                                  [midje "1.9.1"]]
                   :plugins [[lein-ring "0.12.0"]
                             [lein-midje "3.2"]]}})
