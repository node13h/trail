(ns trail.handler-test
  (:require [cheshire.core :as cheshire]
            [midje.sweet :refer :all]
            [trail.handler :as th]
            [trail.core :as t]
            [trail.api.core :as tac]
            [trail.leases :as tl]
            [ring.mock.request :as rmr]))

(defn parse-body [body]
  (cheshire/parse-string (slurp body) true))

(defn no-ids [coll]
  (map #(dissoc %1 :id) coll))

;; These tests will be replaced by the proper integration tests eventually

(defmacro req
  [req app tz & checkables]
  `(let [~'post #(rmr/json-body (rmr/request :post %1) %2)
         ~'put #(rmr/json-body (rmr/request :put %1) %2)
         ~'delete #(rmr/json-body (rmr/request :delete %1) %2)
         ~'get #(rmr/request :get %)
         ~'formatted #(tac/formatted-maps % ~tz)
         ~'response (-> ~req
                        (rmr/header "tz" ~tz)
                        ~app)
         ~'body (parse-body (:body ~'response))
         ~'status (:status ~'response)
         ~'result (:result ~'body)]
     ~@checkables))

(facts "about API"
       (against-background
        [(before :contents (t/start-app))
         (after :contents (t/stop-app))]

        (fact "initially the store is empty"
              (req (get "/api/v3/leases?from-date=2000-01-01%2000:00:00&to-date=2000-01-01%2001:00:00") th/app "UTC"
                   status => 200
                   result => []))

        (fact "can adds new leases"
              (let [leases [{:data {}
                             :duration 100
                             :ip "192.168.0.2"
                             :mac "aa:aa:aa:aa:aa:aa"
                             :start-date "2000-01-01 00:00:00"}
                            {:data {}
                             :duration 100
                             :ip "192.168.0.2"
                             :mac "aa:aa:aa:aa:aa:aa"
                             :start-date "2000-01-01 00:01:00"}]]
                (req (post "/api/v3/leases" leases) th/app "UTC"
                     status => 200)))

        (fact "can adds more leases"
              (let [leases [{:data {}
                             :duration 20
                             :ip "192.168.0.2"
                             :mac "aa:aa:aa:aa:aa:aa"
                             :start-date "2000-01-01 00:02:40"}
                            {:data {}
                             :duration 100
                             :ip "192.168.0.3"
                             :mac "bb:bb:bb:bb:bb:bb"
                             :start-date "2000-01-01 00:00:00"}]]
                (req (post "/api/v3/leases" leases) th/app "UTC"
                     status => 200)))

        (fact "can create new leases using custom time zone"
              (let [leases [{:data {}
                             :duration 100
                             :ip "192.168.0.4"
                             :mac "cc:cc:cc:cc:cc:cc"
                             :start-date "2000-01-01 02:00:00"}]]
                (req (post "/api/v3/leases" leases) th/app "Europe/Vilnius"
                     status => 200))

              (req (get "/api/v3/leases?ip=192.168.0.4&from-date=2000-01-01%2000:00:00&to-date=2000-01-01%2001:00:00") th/app "UTC"
                   status => 200
                   (no-ids result) => [{:data {}
                                        :duration 100
                                        :ip "192.168.0.4"
                                        :mac "cc:cc:cc:cc:cc:cc"
                                        :start-date "2000-01-01 00:00:00"}]))

        (fact "can get aggregated leases"
              (req (get "/api/v3/leases?ip=192.168.0.2&from-date=2000-01-01%2000:00:00&to-date=2000-01-01%2001:00:00") th/app "UTC"
                   status => 200
                   (no-ids result) => [{:data {}
                                        :duration 180
                                        :ip "192.168.0.2"
                                        :mac "aa:aa:aa:aa:aa:aa"
                                        :start-date "2000-01-01 00:00:00"}]))

        (fact "can get leases using custom time zone"
              (req (get "/api/v3/leases?ip=192.168.0.2&from-date=2000-01-01%2002:00:00&to-date=2000-01-01%2003:00:00") th/app "Europe/Vilnius"
                   status => 200
                   (no-ids result) => [{:data {}
                                        :duration 180
                                        :ip "192.168.0.2"
                                        :mac "aa:aa:aa:aa:aa:aa"
                                        :start-date "2000-01-01 02:00:00"}]))

        (fact "out of range does not return any results"
              (req (get "/api/v3/leases?ip=192.168.0.3&from-date=1999-01-01%2000:00:00&to-date=1999-01-01%2000:00:01") th/app "UTC"
                   status => 200
                   result => []))

        (fact "in range does return results"
              (req (get "/api/v3/leases?ip=192.168.0.3&from-date=2000-01-01%2000:00:00&to-date=2000-01-01%2000:01:00") th/app "UTC"
                   status => 200
                   (no-ids result) => [{:data {}
                                        :duration 100
                                        :ip "192.168.0.3"
                                        :mac "bb:bb:bb:bb:bb:bb"
                                        :start-date "2000-01-01 00:00:00"}]))

        (fact "can get lease range using custom time zone"
              (req (get "/api/v3/leases?ip=192.168.0.3&from-date=2000-01-01%2002:00:00&to-date=2000-01-01%2002:01:00") th/app "Europe/Vilnius"
                   status => 200
                   (no-ids result) => [{:data {}
                                        :duration 100
                                        :ip "192.168.0.3"
                                        :mac "bb:bb:bb:bb:bb:bb"
                                        :start-date "2000-01-01 02:00:00"}]))

        (fact "releasing last lease in continuous series will result in shorter aggregated lease"
              (req (post "/api/v3/leases/released" {:ip "192.168.0.2" :end-date "2000-01-01 00:02:41"}) th/app "UTC"
                   status => 200)
              (req (get "/api/v3/leases?ip=192.168.0.2&from-date=2000-01-01%2000:00:00&to-date=2000-01-01%2001:00:00") th/app "UTC"
                   status => 200
                   (no-ids result) => [{:data {}
                                        :duration 161
                                        :ip "192.168.0.2"
                                        :mac "aa:aa:aa:aa:aa:aa"
                                        :start-date "2000-01-01 00:00:00"}]))
        (fact "releasing a lease in the middle of aggregated series will break them into multipe leases"
              (req (post "/api/v3/leases/released" {:ip "192.168.0.2" :end-date "2000-01-01 00:00:59"}) th/app "UTC"
                   status => 200)
              (req (get "/api/v3/leases?ip=192.168.0.2&from-date=2000-01-01%2000:00:00&to-date=2000-01-01%2001:00:00") th/app "UTC"
                   status => 200
                   (no-ids result) => [{:data {}
                                        :duration 101
                                        :ip "192.168.0.2"
                                        :mac "aa:aa:aa:aa:aa:aa"
                                        :start-date "2000-01-01 00:01:00"}
                                       {:data {}
                                        :duration 59
                                        :ip "192.168.0.2"
                                        :mac "aa:aa:aa:aa:aa:aa"
                                        :start-date "2000-01-01 00:00:00"}
                                       ]))
        (fact "can release lease using custom time zone"
              (req (post "/api/v3/leases/released" {:ip "192.168.0.3" :end-date "2000-01-01 02:00:01"}) th/app "Europe/Vilnius"
                   status => 200)
              (req (get "/api/v3/leases?ip=192.168.0.3&from-date=2000-01-01%2000:00:00&to-date=2000-01-01%2001:00:00") th/app "UTC"
                   status => 200
                   (no-ids result) => [{:data {}
                                        :duration 1
                                        :ip "192.168.0.3"
                                        :mac "bb:bb:bb:bb:bb:bb"
                                        :start-date "2000-01-01 00:00:00"}]))
        (fact "release will truncate all matching leases"
              (let [leases [{:data {}
                             :duration 100
                             :ip "192.168.0.5"
                             :mac "dd:dd:dd:dd:dd:dd"
                             :start-date "2000-01-01 00:10:00"}
                            {:data {}
                             :duration 100
                             :ip "192.168.0.5"
                             :mac "dd:dd:dd:dd:dd:dd"
                             :start-date "2000-01-01 00:00:00"}
                            {:data {}
                             :duration 100
                             :ip "192.168.0.5"
                             :mac "dd:dd:dd:dd:dd:dd"
                             :start-date "2000-01-01 00:00:50"}]]
                (req (post "/api/v3/leases" leases) th/app "UTC"
                     status => 200))

              (req (post "/api/v3/leases/released" {:ip "192.168.0.5" :end-date "2000-01-01 00:01:00"}) th/app "UTC"
                   status => 200)

              (req (get "/api/v3/leases?ip=192.168.0.5&from-date=2000-01-01%2000:00:00&to-date=2000-01-01%2001:00:00") th/app "UTC"
                   status => 200
                   (-> result
                       no-ids
                       tl/sorted) => [{:data {}
                                       :duration 60
                                       :ip "192.168.0.5"
                                       :mac "dd:dd:dd:dd:dd:dd"
                                       :start-date "2000-01-01 00:00:00"}
                                      {:data {}
                                       :duration 100
                                       :ip "192.168.0.5"
                                       :mac "dd:dd:dd:dd:dd:dd"
                                       :start-date "2000-01-01 00:10:00"}]))

        (fact "can trim leases"
              (req (delete "/api/v3/leases" {:to-date "2000-01-01 00:02:40"}) th/app "UTC"
                   status => 200)
              (req (get "/api/v3/leases?from-date=2000-01-01%2000:00:00&to-date=2000-01-01%2001:00:00") th/app "UTC"
                   status => 200
                   (-> result
                       no-ids
                       tl/sorted) => [{:data {}
                                       :duration 101
                                       :ip "192.168.0.2"
                                       :mac "aa:aa:aa:aa:aa:aa"
                                       :start-date "2000-01-01 00:01:00"}
                                      {:data {}
                                       :duration 100
                                       :ip "192.168.0.5"
                                       :mac "dd:dd:dd:dd:dd:dd"
                                       :start-date "2000-01-01 00:10:00"}
                                      ]))

        (fact "can trim leases using custom time zone"
              (req (delete "/api/v3/leases" {:to-date "2000-01-01 02:02:42"}) th/app "Europe/Vilnius"
                   status => 200)
              (req (get "/api/v3/leases?from-date=2000-01-01%2000:00:00&to-date=2000-01-01%2001:00:00") th/app "UTC"
                   status => 200
                   (no-ids result) => [{:data {}
                                        :duration 100
                                        :ip "192.168.0.5"
                                        :mac "dd:dd:dd:dd:dd:dd"
                                        :start-date "2000-01-01 00:10:00"}]))
        (fact "can trim all leases"
              (req (delete "/api/v3/leases" {:to-date "2020-01-01 00:02:40"}) th/app "UTC"
                   status => 200)
              (req (get "/api/v3/leases?from-date=2000-01-01%2000:00:00&to-date=2000-01-01%2001:00:00") th/app "UTC"
                   status => 200
                   result => []))))
