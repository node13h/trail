(ns trail.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [trail.api.public :as tap]
            [schema.core :as s]
            [trail.util :refer [version]]
            [taoensso.timbre :refer [info]]
            [clojure.string :refer [join]]
            [trail.api.core :as tac]
            [trail.leases :as tl]
            [trail.config :refer [settings]]))

(defn date-range-allowed?
  [max-delta from-date to-date tz]
  (let [from-date (tac/parsed-dt from-date tz)
        to-date (tac/parsed-dt to-date tz)]
    (<= (tl/interval-seconds from-date to-date) max-delta)))

(def app
  (api
   {:swagger
    {:ui "/"
     :spec "/swagger.json"
     :data {:info {:version version
                   :title "Trail"
                   :description "TRAIL API"}
            :tags [{:name "apiv3", :description "V3 API"}]}
     :options {:ui {:validatorUrl ""}}}}

   (context "/api/v3" []
            :tags ["apiv3"]
            :header-params [{tz :- s/Str nil}]

            (GET "/leases" []
                 :return {:result [tap/Lease]}
                 :query-params [{ip :- s/Str nil}
                                {mac :- s/Str nil}
                                from-date :- s/Str
                                to-date :- s/Str]
                 :summary "Lease aggregates"
                 (if (date-range-allowed? (:max-date-range settings) from-date to-date tz)
                   (ok
                    (do
                      (info (format "QUERY %s %s %s %s %s" tz ip mac from-date to-date))
                      {:result (tap/leases {:ip ip
                                            :mac mac
                                            :from-date from-date
                                            :to-date to-date} tz)}))
                   (bad-request
                    {:error (->> (:max-date-range settings)
                                 (format "Maximum allowed date range is %s seconds"))})))

            (POST "/leases" []
                  :return {:result [s/Any]}
                  :body [leases [tap/InputLease]]
                  :summary "Add leases"
                  (ok
                   (do
                     (->> leases
                          (map (comp (partial join " ") (juxt :ip :start-date)))
                          (join ", ")
                          (format "ADD %s %s" tz)
                          (info))
                     {:result (tap/add! leases tz)})))

            (POST "/leases/released" []
                  :return {:result s/Any}
                  :body-params [ip :- s/Str
                                end-date :- s/Str]
                  :summary "Release leases"
                  (ok
                   (do
                     (info (format "RELEASE %s %s %s" tz ip end-date))
                     {:result (tap/release! ip end-date tz)})))

            (DELETE "/leases/renewals" []
                    :return {:result s/Any}
                    :body-params [to-date :- s/Str]
                    :summary "Trim lease renewals"
                    (ok
                     (do
                       (info (format "TRIM-RENEWALS %s %s" tz to-date))
                       {:result (tap/trim-renewals! to-date tz)})))
            (DELETE "/leases" []
                    :return {:result [s/Any]}
                    :body-params [to-date :- s/Str]
                    :summary "Trim leases"
                    (ok
                     (do
                       (info (format "TRIM %s %s" tz to-date))
                       {:result (tap/trim! to-date tz)}))))))
