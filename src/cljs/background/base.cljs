(ns background.base
  (:require [base.core :as base-app]))

(def last-error-time-atom (atom (base-app/get-time)))

(def audio-elem (goog.dom.getElement "audio-player"))
(def audio-listen (partial goog.events.listen audio-elem))
(def audio-unlisten (partial goog.events.unlisten audio-elem))

(def background-page (.. base-app/browser -extension getBackgroundPage))
(def background-page-url (.. background-page -location -href))

(defn get-src []
  (let [src (.-src audio-elem)]
    (if (or (= src "") (= src background-page-url)) nil src)))

(defn audio-paused? []
  (or (not (get-src)) (.-paused audio-elem)))

(defn get-buffered-value []
  (if (not= audio-elem.buffered.length 0)
    (.end audio-elem.buffered (- audio-elem.buffered.length 1))
    0))
