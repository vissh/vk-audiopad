(ns popup.base
  (:require [clojure.string :refer (join)]
            [reagent.core :as r]))

(def menu-items ["active-playlist"
                 "my-music"
                 "recoms"
                 "recently-listened"
                 "friend-list"
                 "subscriptions"])

(def first-open-page? (r/atom true))

(def mouse-x-atom (r/atom 0))
(def mouse-y-atom (r/atom 0))

(def scroll-up? (atom false))

(def menu-position-index (zipmap menu-items (range (count menu-items))))

(defn jcls [lst] (join " " lst))

(defn audio? [content-type]
  (contains? #{"search" "album" "recoms" "recently-listened" "wall-playlist" "playlist"} content-type))

(defn user-list? [content-type]
  (= content-type "users"))

(defn wall? [content-type]
  (= content-type "wall"))

(defn empty-block [text]
  [:div {:class "audio_empty_placeholder _audio_empty_placeholder no_rows"
         :style {:display "block" :padding-bottom "350px"}} text])

(defn apply-scroll-position [value]
  (aset (goog.dom.getElement "audio_layer_rows") "scrollTop" value))

(defn scroll-upper []
  [:div {:style {:display :none}}
   (when @scroll-up?
     (reset! scroll-up? false)
     (apply-scroll-position 0))])
