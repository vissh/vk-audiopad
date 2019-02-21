(ns background.command
  (:require-macros [cljs.core.async.macros :refer (go)])
  (:require [cljs.core.async :refer (>! <!)]
            [base.constants :as constants]
            [base.core :as base-app]
            [background.core :as bg-core]))

(.. base-app/browser -commands -onCommand
    (addListener
     (fn [command]
       (condp = command
         "001_play" (bg-core/play-pause!)
         "002_prev" (base-app/prev-track!)
         "003_next" (base-app/next-track!)
         "010_loop" (go
                      (let [items (<! (base-app/get-from-storage! ["repeat?"]))
                            repeat? (:repeat? items)
                            manifest-data (.. base-app/browser -runtime getManifest)
                            icon-url (.. base-app/browser -extension
                                         (getURL (:128 (js->clj (clj->js (.-icons manifest-data)) :keywordize-keys true))))]
                        (<! (base-app/set-to-storage! {:repeat? (not repeat?)}))
                        (.. base-app/browser -notifications
                            (create (clj->js {:type "basic"
                                              :iconUrl icon-url
                                              :title constants/repeat
                                              :message (if repeat? constants/off constants/on)})
                                    (fn [notification-id]
                                      (js/setTimeout #(.. base-app/browser -notifications (clear notification-id)) 1500))))))
         "020_shuffle" (go
                         (let [items (<! (base-app/get-from-storage! ["shuffle?"]))
                               shuffle? (:shuffle? items)
                               manifest-data (.. base-app/browser -runtime getManifest)
                               icon-url (.. base-app/browser -extension
                                            (getURL (:128 (js->clj (clj->js (.-icons manifest-data)) :keywordize-keys true))))]
                           (<! (base-app/set-to-storage! {:shuffle? (not shuffle?)}))
                           (.. base-app/browser -notifications
                               (create (clj->js {:type "basic"
                                                 :iconUrl icon-url
                                                 :title constants/shuffle
                                                 :message (if shuffle? constants/off constants/on)})
                                       (fn [notification-id]
                                         (js/setTimeout #(.. base-app/browser -notifications (clear notification-id)) 1500))))))))))
