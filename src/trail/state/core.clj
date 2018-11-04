(ns trail.state.core
  (:require [trail.state.sql :as tss]
            [trail.leases :as tl]))

(def selection tss/selection)
(defn release!
  [{:keys [ip end-date]}]
  (tss/with-transaction
    (when-first [lease (-> {:ip ip
                            :from-date end-date
                            :to-date end-date}
                           tss/selection)]
      (let [lease-id (:id lease)
            cut-offset (tl/interval-seconds (:start-date lease) end-date)]
        (-> {:id lease-id
             :end-date end-date}
            tss/release!)
        (-> {:lease-id lease-id
             :offset cut-offset}
            tss/delete-slice!)
        (when-let [tail-offset (-> {:lease-id lease-id
                                    :offset cut-offset}
                                   tss/first-slice-after
                                   :offset)]
          (let [inserted-id (-> lease
                                (tl/offset-begining tail-offset)
                                tss/add!
                                :id)]

            (-> {:lease-id lease-id
                 :offset tail-offset}
                tss/delete-slice!)

            (-> {:lease-id lease-id
                 :to-lease-id inserted-id
                 :delta (- tail-offset)
                 :from tail-offset}
                tss/move-slices!)))
        {:id lease-id}))))

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
          inserted-id (-> merged-lease
                          tss/add!
                          :id)
          redundant-leases (remove #(= inserted-id (:id %1)) adjacent)
          offset-from-merged (tl/seconds-between merged-lease lease)]
      (when-not (zero? offset-from-merged)
        (-> {:lease-id inserted-id
             :offset offset-from-merged}
            tss/add-slice!))
      (when (seq redundant-leases)
        (doseq [redundant-lease redundant-leases
                :let [offset-from-merged (tl/seconds-between
                                          merged-lease redundant-lease)
                      redundant-id (:id redundant-lease)]]
          (-> {:lease-id inserted-id
               :offset offset-from-merged}
              tss/add-slice!)
          (-> {:lease-id redundant-id
               :to-lease-id inserted-id
               :delta offset-from-merged}
              tss/move-slices!))
        (-> {:ids (map :id redundant-leases)}
            tss/delete!))
      {:id inserted-id})))
