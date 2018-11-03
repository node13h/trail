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
          first-lease (first sorted-all)
          last-lease (last sorted-all)
          merged-lease {:ip (:ip first-lease)
                        :mac (:mac first-lease)
                        :start-date (:start-date first-lease)
                        :duration (tl/duration-span first-lease last-lease)
                        :data (:data first-lease)}
          inserted-id (:id (tss/add! merged-lease))
          redundant-leases (remove #(= inserted-id (:id %1)) adjacent)
          offset-from-merged (tl/seconds-between merged-lease lease)]
      (when-not (zero? offset-from-merged)
        (tss/add-slice! {:lease-id inserted-id
                         :offset offset-from-merged}))
      (when (seq redundant-leases)
        (doseq [redundant-lease redundant-leases
                :let [offset-from-merged (tl/seconds-between
                                          merged-lease redundant-lease)
                      redundant-id (:id redundant-lease)]]
          (tss/add-slice! {:lease-id inserted-id
                           :offset offset-from-merged})
          (tss/move-slices! {:lease-id redundant-id
                             :to-lease-id inserted-id
                             :delta offset-from-merged}))
        (tss/delete! {:ids (map :id redundant-leases)}))
      {:id inserted-id})))
