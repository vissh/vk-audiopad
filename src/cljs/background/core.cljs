(ns background.core
  (:require-macros [cljs.core.async.macros :refer (go)])
  (:require [clojure.string :refer (join includes?)]
            [cljs.core.async :refer (chan >! <!)]
            [cljs-http.client :as http]
            [base.constants :as constants]
            [base.core :as base-app]
            [base.vk :as vk]
            [background.base :as bg-base]
            [hls_lib]
            [unmask]))

(def log (base-app/get-logger "ns-background-core"))
(def clear-src? (atom false))

(defn get-url! [track]
  (go
    (let [audio-id (base-app/get-audio-id track)
          items (<! (base-app/get-from-storage! [:user-id]))]
      (js/unmaskUrl (<! (reload! audio-id))
                    (:user-id items)))))

(defn reload! [audio-id]
  (go
    (let [resp (<! (vk/reload-audio! [audio-id]))]
      (nth (first resp) constants/AUDIO_ITEM_INDEX_URL))))

(defn clear-src! []
  (go
    (let [c (chan)]
      (reset! clear-src? true)
      (.pause bg-base/audio-elem)
      (.removeAttribute bg-base/audio-elem "src")
      (js/setTimeout #(go (reset! clear-src? false) (>! c true)) 13)
      (<! c))))

(defn load-track! [track]
  (go
    (when (<! (clear-src!))
      (let [url (<! (get-url! track))
            props (clj->js {:src url})
            c (chan)]
        (base-app/set-to-storage! {:duration 0
                                   :current-time 0
                                   :buffered 0})
        (if-not url
          (do
            (base-app/set-to-storage! {:plays? false})
            (>! c false))
          (if (includes? url "index.m3u8")
            (js/loadHlsSource url bg-base/audio-elem #(go (>! c true)))
            (do
              (goog.dom.setProperties bg-base/audio-elem props)
              (.load bg-base/audio-elem)
              (js/setTimeout #(go (>! c true)) 150))))
        (<! c)))))

(defn play-track! [track]
  (go
    (when (<! (load-track! track))
      (.play bg-base/audio-elem))))

(defn play-pause! []
  (if (.-paused bg-base/audio-elem)
    (if (or (not (bg-base/get-src))
            (zero? bg-base/audio-elem.currentTime))
      (go (play-track! (:active-track (<! (base-app/get-from-storage! [:active-track])))))
      (.play bg-base/audio-elem))
    (.pause bg-base/audio-elem)))

(base-app/add-storage-watch :active-track (fn [old-track new-track]
                                            (play-track! new-track)))

(.. base-app/browser -runtime -onMessage
    (addListener
     (fn [request sender]
       (let [params (js->clj request :keywordize-keys true)]
         (condp = (:type params)
           "play_pause" (play-pause!)
           "current-time" (aset bg-base/audio-elem "currentTime" (:value params))
           "volume" (aset bg-base/audio-elem "volume" (/ (:value params) 100)))))))

(bg-base/audio-listen
 "pause"
 (fn [e]
   (when-not @clear-src?
     (base-app/set-to-storage! {:plays? false}))
   (.. base-app/browser -browserAction (setBadgeText (clj->js {:text ""})))))

(bg-base/audio-listen
 "playing"
 (fn [e]
   (base-app/set-to-storage! {:plays? true})))

(bg-base/audio-listen
 "timeupdate"
 (fn [e]
   (base-app/set-to-storage!
    (let [t e.target]
      {:duration t.duration
       :current-time t.currentTime
       :buffered (bg-base/get-buffered-value)
       :plays? true}))))

(bg-base/audio-listen
 "ended"
 (fn [e]
   (go
     (let [items (<! (base-app/get-from-storage! [:repeat? :active-track :recently-listened]))
           active-track (:active-track items)
           recently-list (:list (:recently-listened items) [])
           last-recently-track (first recently-list)]
       (if (:repeat? items)
         (play-track! active-track)
         (base-app/next-track!))
       (when-not (= (base-app/get-audio-id last-recently-track) (base-app/get-audio-id active-track))
         (let [recently-list (if (> (count recently-list) 350) (subvec recently-list 0 300) recently-list)]
           (base-app/set-to-storage! {:recently-listened {:type "recently-listened"
                                                          :list (into [active-track] recently-list)}})))))))

(bg-base/audio-listen
 "volumechange"
 (fn [e]
   (base-app/set-to-storage! {:volume (* e.target.volume 100)})))

(bg-base/audio-listen
 "loadstart"
 (fn [e]
   (base-app/set-to-storage! {:data-loading? true})))

(bg-base/audio-listen
 "loadeddata"
 (fn [e]
   (base-app/set-to-storage! {:data-loading? false})))

(bg-base/audio-listen
 "error"
 (base-app/defer
   (fn [e]
     (base-app/set-to-storage! {:data-loading? false})
     (when (and (< @bg-base/last-error-time-atom (+ (base-app/get-time) 2000))
                (.. js/window -navigator -onLine))
       (log "error code:" e.target.error.code)
       (go
         (let [items (<! (base-app/get-from-storage! [:active-track :current-time]))]
           (when (<! (load-track! (:active-track items)))
             (aset bg-base/audio-elem "currentTime" (:current-time items))
             (.play bg-base/audio-elem)
             (log "track reloaded")))))
     (reset! bg-base/last-error-time-atom (base-app/get-time)))
   constants/error-timeout))

(.. base-app/browser -browserAction (setBadgeBackgroundColor (clj->js {:color "#789ABF"})))

(go
  (let [items (<! (base-app/get-from-storage! [:volume]))
        volume (:volume items 100)]
    (when (bg-base/audio-paused?)
      (base-app/set-to-storage! {:plays? false}))
    (aset bg-base/audio-elem "volume" (/ volume 100))))

(js/setInterval
 (fn []
   (when-not (bg-base/audio-paused?)
     (go
       (let [items (<! (base-app/get-from-storage! [:countdown?]))
             countdown? (:countdown? items)
             duration (if-not (bg-base/audio-paused?)
                        (base-app/extra-format-duration countdown?
                                                        bg-base/audio-elem.duration
                                                        bg-base/audio-elem.currentTime)
                        "")]
         (.. base-app/browser -browserAction (setBadgeText (clj->js {:text duration}))))))
   (base-app/set-to-storage! {:buffered (bg-base/get-buffered-value)}))
 1000)
