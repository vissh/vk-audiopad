(ns base.core
  (:require-macros [cljs.core.async.macros :refer (go)])
  (:require [clojure.string :refer (join split includes?)]
            [goog.string :as gstring]
            [cljs.core.async :refer (chan >! <!)]
            [cljs-http.client :as http]
            [base.constants :as constants]))

(enable-console-print!)

(def debug? false)

(def browser (or js/chrome js/browser))

(def opera? (includes? js/navigator.userAgent "OPR"))

(def firefox? (includes? js/navigator.userAgent "Firefox"))

(extend-type js/NodeList
  ISeqable
  (-seq [array] (array-seq array 0)))

(def storage (.. browser -storage -local))

(def manifest-data (.. browser -runtime getManifest))

(defn get-logger [ns-name]
  (if debug? (partial println (+ ns-name ":")) #()))

(defn get-time []
  (.getTime (js/Date.)))

(defn defer [f ms]
  (let [timeout-id (atom 0)]
    (fn [& args]
      (js/clearTimeout @timeout-id)
      (reset! timeout-id (js/setTimeout #(apply f args) ms)))))

(defn vec-remove [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn show-notification [title message callback]
  (let [icon-url (.. browser -extension
                     (getURL (:128 (js->clj (clj->js (.-icons manifest-data)) :keywordize-keys true))))]

    (.. browser -notifications
        (create (clj->js {:type "basic"
                          :iconUrl icon-url
                          :title title
                          :message message})
                (fn [notification-id]
                  (when callback
                    (callback))
                  (.. browser -notifications -onClicked
                      (addListener (fn [clicked-notification-id]
                                     (when (= clicked-notification-id notification-id)
                                       (.. browser -notifications (clear notification-id)))))))))))

(defn claim? [track]
  (includes? (or (nth track constants/AUDIO_ITEM_INDEX_EXTRA) "") "claim"))

(defn get-audio-id [track]
  (let [[hashes-exist [add-hash edit-hash action-hash delete-hash replace-hash url-hash]] (get-hashes track)]
    (if (or (not track) (empty? track))
      ""
      (str
       (nth track constants/AUDIO_ITEM_INDEX_OWNER_ID)
       "_"
       (nth track constants/AUDIO_ITEM_INDEX_ID)
       "_"
       action-hash
       "_"
       url-hash))))

(defn set-to-storage! [items]
  (let [c (chan)]
    (go (. storage set (clj->js items) #(go (>! c true))))
    (go (<! c))))

(defn get-from-storage! [items & {:keys [keywordize-keys] :or {keywordize-keys true}}]
  (let [c (chan)]
    (go (. storage get
           (clj->js items)
           #(go (>! c (js->clj % :keywordize-keys keywordize-keys)))))
    (go (<! c))))

(defn add-storage-watch [key-name callback-fn]
  (.. browser -storage -onChanged
      (addListener
       (fn [changes namespace]
         (let [changes (js->clj changes :keywordize-keys true)]
           (doseq [[changed-key-name change] (seq changes)]
             (when
              (= key-name changed-key-name)
               (callback-fn (:oldValue change) (:newValue change)))))))))

(defn reset-atoms-on-change-storage [atoms-map]
  (.. browser -storage -onChanged
      (addListener
       (fn [changes namespace]
         (let [changes (js->clj changes :keywordize-keys true)]
           (doseq [[changed-atom-name change] (seq changes)]
             (when
              (contains? atoms-map changed-atom-name)
               (let [reference (changed-atom-name atoms-map)
                     old-atom-value @reference
                     new-atom-value (:newValue change)]
                 (when (not= old-atom-value new-atom-value)
                   (reset! reference new-atom-value))))))))))

(defn get-flags! []
  (go (set (:flags (<! (get-from-storage! [:flags]))))))

(defn contains-flag? [flag]
  (go (contains? (<! (get-flags!)) flag)))

(defn set-flag! [flag]
  (go (set-to-storage! {:flags (conj (<! (get-flags!)) flag)})))

(defn format-duration [total-seconds]
  (let [total-seconds (int total-seconds)
        hours (quot total-seconds 3600)
        minutes (mod (quot total-seconds 60) 60)
        seconds (mod total-seconds 60)]
    (join ":" (remove zero? [hours
                             (gstring/format (if (zero? hours) "%01d" "%02d") minutes)
                             (gstring/format "%02d" seconds)]))))

(defn extra-format-duration [countdown? duration current-time]
  (if countdown?
    (+ "-" (format-duration (- duration current-time)))
    (format-duration current-time)))

(defn send-message [params]
  (.. browser -runtime (sendMessage (clj->js params))))

(defn set-user-id! []
  (go
    (let [response (<! (http/get "https://vk.com/"))
          user-id (second (re-find #"id:\s(\d+)," (:body response)))]
      (set-to-storage! {:user-id user-id}))))

(defn next-track! []
  (go
    (let [items (<! (get-from-storage! ["active-track-index"
                                        "active-playlist"
                                        "shuffle?"
                                        "shuffle-queue"
                                        "shuffle-index"]))
          active-playlist (:list (:active-playlist items))
          active-index (:active-track-index items)
          shuffle? (:shuffle? items false)
          shuffle-queue (:shuffle-queue items [])
          shuffle-index (:shuffle-index items -1)]

      (-next-track! active-playlist active-index shuffle? shuffle-queue shuffle-index 7))))

(defn -next-track! [active-playlist active-index shuffle? shuffle-queue shuffle-index depth]
  (when-not (zero? depth)
    (go
      (if shuffle?
        (let [next-shuffle-index (+ shuffle-index 1)
              next-index (nth shuffle-queue next-shuffle-index nil)
              add-new-index? (nil? next-index)
              length (count active-playlist)
              next-index (if add-new-index? (rand-int length) next-index)
              next-track (nth active-playlist next-index)]
          (if (claim? next-track)
            (-next-track! active-playlist active-index shuffle? shuffle-queue shuffle-index (- depth 1))
            (set-to-storage! {:shuffle-index next-shuffle-index
                              :shuffle-queue (if add-new-index? (conj shuffle-queue next-index) shuffle-queue)
                              :active-track next-track
                              :active-track-index next-index
                              :plays? true})))

        (let [length (count active-playlist)
              next-index (if (= (+ active-index 1) length) 0 (+ active-index 1))
              next-track (nth active-playlist next-index)]
          (if (claim? next-track)
            (-next-track! active-playlist next-index shuffle? shuffle-queue shuffle-index (- depth 1))
            (set-to-storage! {:active-track next-track
                              :active-track-index next-index
                              :plays? true})))))))

(defn prev-track! []
  (go
    (let [items (<! (get-from-storage! ["active-track-index"
                                        "active-playlist"
                                        "shuffle?"
                                        "shuffle-queue"
                                        "shuffle-index"]))
          active-playlist (:list (:active-playlist items))
          active-index (:active-track-index items)
          shuffle? (:shuffle? items false)
          shuffle-queue (:shuffle-queue items [])
          shuffle-index (:shuffle-index items -1)]

      (-prev-track! active-playlist active-index shuffle? shuffle-queue shuffle-index 7))))

(defn -prev-track! [active-playlist active-index shuffle? shuffle-queue shuffle-index depth]
  (when-not (zero? depth)
    (go
      (if shuffle?
        (let [next-shuffle-index (- shuffle-index 1)
              next-index (nth shuffle-queue next-shuffle-index nil)
              next-track (nth active-playlist next-index)]
          (when-not (nil? next-index)
            (set-to-storage! {:shuffle-index next-shuffle-index
                              :active-track next-track
                              :active-track-index next-index
                              :plays? true})))
        (let [length (count active-playlist)
              next-index (if (= (- active-index 1) -1) (- length 1) (- active-index 1))
              next-track (nth active-playlist next-index)]
          (if (claim? next-track)
            (-prev-track! active-playlist next-index shuffle? shuffle-queue shuffle-index (- depth 1))
            (set-to-storage! {:active-track next-track
                              :active-track-index next-index
                              :plays? true})))))))

(defn get-hashes [item]
  (let [track-hashes (nth item constants/AUDIO_ITEM_INDEX_HASHES nil)
        hashes (split (or track-hashes "//") #"/")]
    [(boolean track-hashes) hashes]))
