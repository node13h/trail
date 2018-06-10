(ns trail.state.core
  (:require [trail.state.sql :as tss]
            [trail.leases :as tl]))

(def selection tss/selection)
(def release! tss/release!)
(def trim! tss/trim!)
(defn add!
  [lease]
  (tss/with-transaction
    (let [adjacent (-> {:ip (:ip lease)
                        :mac (:mac lease)
                        :from-date (:start-date lease)
                        :to-date (tl/end-date lease)
                        :lock true}
                       tss/selection)
          sorted-all (tl/sorted (conj adjacent lease))
          earliest (first sorted-all)
          latest (last sorted-all)
          inserted-id (-> {:ip (:ip earliest)
                           :mac (:mac earliest)
                           :start-date (:start-date earliest)
                           :duration (tl/duration-span earliest latest)
                           :data (:data earliest)}
                          tss/add!
                          :id)
          redundant-ids (remove (partial = inserted-id) (map :id adjacent))]
      (when (seq redundant-ids)
        (tss/delete! {:ids redundant-ids}))
      {:id inserted-id})))
