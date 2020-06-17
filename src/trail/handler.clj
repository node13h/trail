(ns trail.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [trail.api.public :as tap]
            [schema.core :as s]
            [trail.util :refer [version]]
            [clojure.string :refer [join]]
            [trail.api.core :as tac]
            [trail.leases :as tl]
            [trail.config :refer [settings]]))

(defn leases
  [ip mac from-date to-date tz]
  (let [results (tap/leases {:ip ip
                             :mac mac
                             :from-date from-date
                             :to-date to-date
                             :limit (inc (:max-http-results settings))} tz)]
    (if (> (count results) (:max-http-results settings))
      {:result (butlast results)
       :truncated true}
      {:result results
       :truncated false})))

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
       :return {:result [tap/Lease] :truncated s/Bool}
       :query-params [{ip :- s/Str nil}
                      {mac :- s/Str nil}
                      {from-date :- s/Str nil}
                      {to-date :- s/Str nil}]
       :summary "Lease aggregates"
       (ok
        (do
          (leases ip mac from-date to-date tz))))

     (POST "/leases" []
       :return {:result [s/Any]}
       :body [leases [tap/InputLease]]
       :summary "Add leases"
       (ok
        (do
          {:result (tap/add! leases tz)})))

     (POST "/leases/released" []
       :return {:result s/Any}
       :body-params [ip :- s/Str
                     end-date :- s/Str]
       :summary "Release leases"
       (ok
        (do
          {:result (tap/release! ip end-date tz)})))

     (DELETE "/leases/renewals" []
       :return {:result s/Any}
       :body-params [to-date :- s/Str]
       :summary "Trim lease renewals"
       (ok
        (do
          {:result (tap/trim-renewals! to-date tz)})))
     (DELETE "/leases" []
       :return {:result [s/Any]}
       :body-params [to-date :- s/Str]
       :summary "Trim leases"
       (ok
        (do
          {:result (tap/trim! to-date tz)})))
     (DELETE "/releases" []
       :return {:result s/Any}
       :body-params [to-date :- s/Str]
       :summary "Trim lease release records"
       (ok
        (do
          {:result (tap/trim-releases! to-date tz)}))))))
