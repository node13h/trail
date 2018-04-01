(ns trail.config
  (:require [mount.core :as m]
            [environ.core :refer [env]]))

(m/defstate settings
  :start {:port (Integer/parseInt (:port env "3000"))
          :database-url (:database-url env)
          :max-date-range (Integer/parseInt (:max-date-range env "3600"))})
