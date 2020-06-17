(ns trail.config
  (:require [mount.core :as m]
            [environ.core :refer [env]]))

(m/defstate settings
  :start {:port (Integer/parseInt (:port env "3000"))
          :database-url (:database-url env)
          :max-http-results (Integer/parseInt (:max-http-results env "10000"))})
