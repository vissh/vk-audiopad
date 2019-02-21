(ns background.migration
  (:require-macros [cljs.core.async.macros :refer (go)])
  (:require [cljs.core.async :refer (<!)]
            [base.core :as base-app]
            [base.constants :as constants]))

(defn purge-cache []
  (.. base-app/browser -storage -local (remove (clj->js ["track-cache"]))))

(defn show-notification []
  (.. base-app/browser -notifications
      (create (clj->js {:type "basic"
                        :iconUrl (.. base-app/browser -extension
                                     (getURL (:128 (js->clj (clj->js (.-icons (.. base-app/browser -runtime getManifest)))
                                                            :keywordize-keys true))))
                        :title constants/information
                        :message constants/fixed})
              (fn [notification-id]
                (js/setTimeout #(.. base-app/browser -notifications (clear notification-id)) 5000)))))

(defn on-update-new-version []
  (go
    (let [migration-name "migration-2.0.33"]
      (when-not (<! (base-app/contains-flag? migration-name))
        (base-app/set-flag! migration-name)
        (purge-cache)
        (show-notification)))))

(.. base-app/browser -runtime -onInstalled
    (addListener (fn [details]
                   (let [details (js->clj details :keywordize-keys true)
                         reason (:reason details)
                         prev-version (:previousVersion details)]
                     (when (and (= reason "update")
                                (not= base-app/manifest-data.version prev-version))
                       (on-update-new-version))))))
