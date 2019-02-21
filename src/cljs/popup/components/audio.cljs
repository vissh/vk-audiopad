(ns popup.components.audio
  (:require-macros [cljs.core.async.macros :refer (go)])
  (:require [clojure.string :refer (join split)]
            [cljs.core.async :refer (<!)]
            [goog.string :as gstring]
            [goog.string.format]
            [reagent.core :as r]
            [base.constants :as constants]
            [base.core :as base-app]
            [popup.base :as base-popup]
            [popup.handlers :as handlers]
            [popup.state :as app-state]))

(def just-added-atom (r/atom {}))

(defn jcls [lst]
  (join " " lst))

(defn audios []
  (let [content @app-state/content]
    (when (base-popup/audio? (:type content))
      (if (zero? (count (:list content)))
        (base-popup/empty-block constants/tracks-not-found)
        [:div {:class "audio_rows _audio_rows"}
         [:div {:class "audio_playlist_wrap _audio_playlist audio_current_rows"
                :style {:position "relative"}}
          (doall
           (for [[idx item] (map-indexed vector (:list content))]
             (audio-row idx item app-state/content)))]]))))

(defn audio-row [idx item content-atom]
  (let [lyrics-id (nth item constants/AUDIO_ITEM_INDEX_LYRICS)
        lyrics? (not (or (zero? lyrics-id) (not lyrics-id)))
        duration (nth item constants/AUDIO_ITEM_INDEX_DURATION)
        artist (gstring/unescapeEntities (nth item constants/AUDIO_ITEM_INDEX_PERFORMER))
        title (gstring/unescapeEntities (nth item constants/AUDIO_ITEM_INDEX_TITLE))
        active-track @app-state/active-track
        active? (= (base-app/get-audio-id item) (base-app/get-audio-id active-track))
        [hashes-exist [add-hash remove-hash edit-hash]] (base-app/get-hashes item)
        claim? (base-app/claim? item)]
    [:div {:class (jcls ["audio_row"
                         "_audio_row"
                         (when lyrics? "lyrics")
                         "hq"
                         "clear_fix"
                         (when active? "audio_row_current")
                         (when (and active? @app-state/plays?) "audio_row_playing")
                         (cond (or (contains? @just-added-atom item)
                                   (and hashes-exist (not= remove-hash ""))) "added"
                               (or (not hashes-exist)
                                   (not= add-hash "")) "canadd")
                         (when claim? "claimed")])
           :style {:display "block"}
           :on-click (fn [event] (when-not claim?
                                   (handlers/click-on-track! idx item content-atom)))
           :key (str idx (base-app/get-audio-id item))}
     (when (zero? idx)
       (base-popup/scroll-upper))
     [:div {:class "audio_play_wrap"}
      [:button {:class "audio_play _audio_play"
                :aria-label constants/play}]]
     [:div {:class "audio_info"}
      [:div {:class (jcls ["audio_duration_wrap"
                           "_audio_duration_wrap"])}
       [:div {:class "audio_hq_label"}]
       [:div {:class (jcls ["audio_duration"
                            "_audio_duration"])}
        (when duration (base-app/format-duration duration))]
       [:div {:class "audio_acts"}
        [:div {:id "recom"
               :class "audio_act"
               :on-click (fn [event]
                           (.stopPropagation event)
                           (handlers/click-on-track-recoms! item))}
         [:div {}]]
        [:div {:id "next"
               :class "audio_act"
               :style {:display :none}}
         [:div {}]]
        [:div {:id "edit"
               :class "audio_act"}
         [:div {}]]
        [:div {:id "delete"
               :class (jcls ["audio_act"
                             "_audio_act_delete"])}
         [:div {}]]
        [:div {:id "add"
               :class "audio_act"
               :on-click (fn [event]
                           (.stopPropagation event)
                           (go
                             (if (or (contains? @just-added-atom item)
                                     (and hashes-exist (not= remove-hash "")))
                               (do
                                 (let [remove-item (if (contains? @just-added-atom item) (get @just-added-atom item) item)]
                                   (<! (handlers/delete-track! remove-item))
                                   (if (and hashes-exist (not= remove-hash ""))
                                     (base-app/set-to-storage! {:content (assoc @content-atom :list (base-app/vec-remove (:list @content-atom) idx))})
                                     (swap! just-added-atom dissoc item))))
                               (let [added-item (<! (handlers/add-track! item))]
                                 (when added-item
                                   (swap! just-added-atom assoc item added-item))))))}
         [:div {}]]]]
      [:div {:class "audio_title_wrap"}
       [:a {:class "audio_performer"
            :on-click (fn [event]
                        (.stopPropagation event)
                        (let [value event.target.text
                              new-serach-state (assoc @app-state/search :value value :performer 1)]
                          (go
                            (<! (base-app/set-to-storage! {:search new-serach-state}))
                            (handlers/audio-search!))))}
        artist]
       [:span {:class "audio_info_divider"} "–"]
       [:span {:class "audio_title _audio_title"
               :on-click (fn [event]
                           (when lyrics?
                             (.stopPropagation event)
                             (go
                               (let [el (goog.dom.getElement (str "lyrics" idx lyrics-id))
                                     display (goog.style.getStyle el "display")]
                                 (if (not= display "")
                                   (if (= display "block")
                                     (goog.style.setStyle el "display" "none")
                                     (goog.style.setStyle el "display" "block"))
                                   (do
                                     (aset el "innerHTML" (<! (handlers/get-lyrics! item)))
                                     (goog.style.setStyle el "display" "block")))))))}
        [:span {:class "audio_title_inner"
                :tabIndex "0"
                :aria-label title}
         title]]
       [:span {:class "audio_author"}]]]
     [:div {:class "_audio_player_wrap"}]
     [:div {:id (str "lyrics" idx lyrics-id)
            :class "_audio_lyrics_wrap audio_lyrics"}]]))
