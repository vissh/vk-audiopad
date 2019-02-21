(ns popup.state
  (:require-macros [cljs.core.async.macros :refer (go)])
  (:require [cljs.core.async :refer (chan >! <!)]
            [reagent.core :as r]
            [base.core :as base-app]))

(def active-track (r/atom []))
(def search (r/atom {}))
(def content (r/atom {}))
(def recently-listened (r/atom {:type "recently-listened"}))
(def active-playlist (r/atom {}))
(def plays? (r/atom false))
(def volume (r/atom 100))
(def duration (r/atom 0))
(def current-time (r/atom 0))
(def buffered (r/atom 0))
(def countdown? (r/atom false))
(def menu-position (r/atom ""))
(def user-id (r/atom ""))
(def repeat? (r/atom false))
(def shuffle? (r/atom false))
(def data-loading? (r/atom false))
(def scroll-position (r/atom 0))

(def atoms-map {:active-track active-track
                :search search
                :content content
                :recently-listened recently-listened
                :active-playlist active-playlist
                :plays? plays?
                :volume volume
                :duration duration
                :current-time current-time
                :buffered buffered
                :countdown? countdown?
                :menu-position menu-position
                :user-id user-id
                :repeat? repeat?
                :shuffle? shuffle?
                :data-loading? data-loading?
                :scroll-position scroll-position})

(base-app/reset-atoms-on-change-storage atoms-map)

(go
  (let [items (<! (base-app/get-from-storage! (keys atoms-map)))]
    (doseq [[atom-name atom-state] (seq items)]
      (reset! (atom-name atoms-map) atom-state))))
