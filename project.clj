(defproject trail "2.1.1-SNAPSHOT"
  :description "IP history management API"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [metosin/compojure-api "1.1.12"]
                 [metosin/ring-swagger "0.26.2"]
                 [clj-time "0.15.2"]
                 [ring "1.7.1"]
                 [mount "0.1.16"]
                 [migratus "1.2.6"]
                 [com.taoensso/timbre "4.10.0"]
                 [org.slf4j/log4j-over-slf4j "1.7.28"]
                 [org.slf4j/jul-to-slf4j "1.7.28"]
                 [org.slf4j/jcl-over-slf4j "1.7.28"]
                 [com.fzakaria/slf4j-timbre "0.3.14"]
                 [org.postgresql/postgresql "42.2.8"]
                 [conman "0.8.4"]
                 [environ "1.1.0"]
                 [org.clojure/tools.cli "0.4.2"]]
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
                                  [cheshire "5.9.0"]
                                  [ring/ring-mock "0.4.0"]
                                  [midje "1.9.9"]]
                   :plugins [[lein-ring "0.12.5"]
                             [lein-midje "3.2.1"]]}})
