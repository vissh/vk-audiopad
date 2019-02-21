(ns popup.core
  (:require-macros [cljs.core.async.macros :refer (go)])
  (:require [clojure.string :refer (join)]
            [cljs.core.async :refer (<!)]
            [goog.string :as gstring]
            [goog.string.format]
            [reagent.core :as r]
            [base.constants :as constants]
            [base.core :as base-app]
            [popup.base :as base-popup :refer (jcls)]
            [popup.handlers :as handlers]
            [popup.state :as app-state]
            [popup.components.audio :as cmp-audio]
            [popup.components.search :as cmp-search]
            [popup.components.users :as cmp-users]
            [popup.components.wall :as cmp-wall]
            [popup.components.menu :as cmp-menu]
            [popup.components.settings :as cmp-settings]
            [popup.components.info :as cmp-info]))

(def first-open-page? (r/atom true))
(def track-slider-down? (r/atom false))
(def track-slider-time (r/atom 0))
(def hint-track-slider-time (r/atom 0))
(def volume-slider-down? (r/atom false))
(def hint-volume-slider-percent (r/atom 100))
(def mouse-on-track-slider? (r/atom false))
(def mouse-on-volume-slider? (r/atom false))

(def save-scroll-position
  (base-app/defer
    (fn [position]
      (base-app/set-to-storage! {:scroll-position position}))
    300))

(defn reset-atoms-state []
  (cmp-search/reset-atoms-state)
  (reset! track-slider-down? false)
  (reset! volume-slider-down? false))

(defn get-slider-percent [element x-value]
  (let [rect (.getBoundingClientRect element)
        min-left (.-left rect)
        max-right (.-width rect)
        position-value (min max-right (max 0 (- x-value min-left)))]
    (/ (* position-value 100) (.-width rect))))

(defn get-track-slider-time [x-value]
  (let [slider-element (first (goog.dom.getElementsByClass "audio_page_player_track_slider"))
        percent (get-slider-percent slider-element x-value)]
    (/ (* @app-state/duration percent) 100)))

(defn get-volume-percent [x-value]
  (let [slider-element (first (goog.dom.getElementsByClass "audio_page_player_volume_slider"))]
    (get-slider-percent slider-element x-value)))

(defn get-str-percent [a b]
  (gstring/format "%s%" (if (or (zero? a) (zero? b)) 0 (max 0 (min 100 (/ (* a 100) b))))))

(defn play-btn []
  [:button {:class (jcls ["audio_page_player_ctrl"
                          "audio_page_player_play"
                          "_audio_page_player_play"
                          (when @app-state/plays? "audio_playing")])
            :on-click handlers/play-bnt-click!}
   [:span {:class (jcls ["blind_label"
                         "_play_blind_label"])} constants/play]
   [:div {:class "icon"}]])

(defn prev-btn []
  [:button {:class (jcls ["audio_page_player_ctrl"
                          "audio_page_player_prev"])
            :on-click base-app/prev-track!}
   [:span {:class "blind_label"} constants/prev]
   [:div {:class "icon"}]])

(defn next-btn []
  [:button {:class (jcls ["audio_page_player_ctrl"
                          "audio_page_player_next"])
            :on-click base-app/next-track!}
   [:span {:class "blind_label"} constants/next]
   [:div {:class "icon"}]])

(defn add-btn []
  (let [active-track @app-state/active-track
        [hashes-exist [add-hash remove-hash edit-hash]] (base-app/get-hashes active-track)
        added (contains? @cmp-audio/just-added-atom active-track)
        add (or (not hashes-exist) (and hashes-exist (not= add-hash "")))]
    [:button {:id "add"
              :class (jcls ["audio_page_player_btn"
                            "audio_page_player_add"
                            "_audio_page_player_add"
                            (when added "audio_player_btn_added")])
              :style {:display (if (or add added) "block" "none")}
              :on-click (fn [event]
                          (if added
                            (go
                              (<! (handlers/delete-track! (get @cmp-audio/just-added-atom active-track)))
                              (swap! cmp-audio/just-added-atom dissoc active-track))
                            (go
                              (let [added-item (<! (handlers/add-track! active-track))]
                                (when added-item
                                  (swap! cmp-audio/just-added-atom assoc active-track added-item))))))}
     [:span {:class "blind_label"} constants/add]
     [:div {:class "icon"}]]))

(defn shuffle-btn []
  (let [shuffle? @app-state/shuffle?]
    [:button {:class (jcls ["audio_page_player_btn"
                            "audio_page_player_shuffle"
                            (when shuffle? "audio_page_player_btn_enabled")])
              :on-click handlers/shuffle-btn-click!}
     [:span {:class "blind_label"} constants/shuffle]
     [:div {:class "icon"}]]))

(defn repeat-btn []
  (let [repeat? @app-state/repeat?]
    [:button {:class (jcls ["audio_page_player_btn"
                            "audio_page_player_repeat"
                            "_audio_page_player_repeat"
                            (when repeat? "audio_page_player_btn_enabled")])
              :on-click handlers/repeat-btn-click!}
     [:span {:class (jcls ["blind_label"
                           "_blind_label"])} constants/repeat]
     [:div {:class "icon"}]]))

(defn recoms-btn []
  [:button {:class (jcls ["audio_page_player_btn"
                          "audio_page_player_recoms"])
            :on-click handlers/click-on-recoms!}
   [:span {:class "blind_label"} constants/similar]
   [:div {:class "icon"}]])

(defn translation-btn []
  [:div {:class (jcls ["audio_page_player_btn"
                       "audio_page_player_status"
                       "_audio_page_player_status"])
         :style {:display "none"}}
   [:div {:class "icon"}]])

(defn volume-slider []
  (let [show-hint @mouse-on-volume-slider?]
    (when show-hint (reset! hint-volume-slider-percent (int (get-volume-percent @base-popup/mouse-x-atom))))
    [:div {:class "audio_page_player_volume_wrap"}
     [:div {:class (jcls ["slider"
                          "audio_page_player_volume_slider"
                          "slider_size_1"])
            :on-mouse-down #(reset! volume-slider-down? true)
            :on-mouse-over #(reset! mouse-on-volume-slider? true)
            :on-mouse-out #(reset! mouse-on-volume-slider? false)}
      [:div {:class "slider_slide"}
       [:div {:class "slider_loading_bar"
              :style {:opacity 0
                      :display "none"}}]
       [:div {:class "slider_amount"
              :style {:width (+ @app-state/volume "%")}}]
       [:div {:class "slider_handler"
              :style {:left (+ @app-state/volume "%")}}]]
      [:div {:id "slider_hint"
             :class (jcls ["slider_hint"
                           "audio_player_hint"
                           (when show-hint "visible")])
             :style {:left (+ (- (- @base-popup/mouse-x-atom (count (str @hint-volume-slider-percent))) 15) "px")
                     :top "11px"}}
       (+ @hint-volume-slider-percent "%")]]]))

(defn track-info []
  (let [active-track @app-state/active-track
        duration @app-state/duration
        current-time @app-state/current-time
        countdown? @app-state/countdown?
        performer (gstring/unescapeEntities (nth active-track constants/AUDIO_ITEM_INDEX_PERFORMER ""))
        title (gstring/unescapeEntities (gstring/format "&nbsp;– %s" (nth active-track constants/AUDIO_ITEM_INDEX_TITLE "")))]
    [:div {:class "audio_page_player_track_info_wrap clear_fix"}
     [:div {:class "audio_page_player_duration"
            :on-click handlers/click-on-countdown!}
      (base-app/extra-format-duration countdown? duration current-time)]
     [:div {:class "audio_page_player_title"
            :title ""}
      [:span {:class "audio_page_player_title_performer"} performer]
      [:span {:class "audio_page_player_title_song"} title]]]))

(defn track-slider []
  (let [buffered @app-state/buffered
        duration @app-state/duration
        current-time (if @track-slider-down? @track-slider-time @app-state/current-time)
        show-hint @mouse-on-track-slider?
        x-value @base-popup/mouse-x-atom
        data-loading? @app-state/data-loading?]
    (when show-hint (reset! hint-track-slider-time (base-app/format-duration (get-track-slider-time x-value))))
    [:div {:class "slider audio_page_player_track_slider slider_size_1"
           :on-mouse-down (fn [e]
                            (reset! track-slider-time current-time)
                            (reset! track-slider-down? true))
           :on-mouse-over #(reset! mouse-on-track-slider? true)
           :on-mouse-out #(reset! mouse-on-track-slider? false)}
     [:div {:class "slider_slide"}
      [:div {:class "slider_loading_bar"
             :style {:opacity (if data-loading? "1" "0")
                     :display (if data-loading? "block" "none")}}]
      [:div {:class "slider_back slider_back_transition"
             :style {:width (get-str-percent buffered duration)}}]
      [:div {:class "slider_amount"
             :style {:width (get-str-percent current-time duration)}}]
      [:div {:class "slider_handler"
             :style {:left (get-str-percent current-time duration)}}]]
     [:div {:id "slider_hint"
            :class (jcls ["slider_hint"
                          "audio_player_hint"
                          (when show-hint "visible")])
            :style {:left (+ (- x-value (+ (count (str @hint-track-slider-time)) 14)) "px")
                    :top "11px"}} @hint-track-slider-time]]))

(defn repeat-tooltip []
  [:div {:class "tt_w tt_black tt_down"
         :style {:position "absolute"
                 :opacity "0.0120416"
                 :top "-10.4px"
                 :left "708px"
                 :poiner-events "auto"
                 :display "none"}}
   [:div {:class "tt_text"}
    constants/repeat]])

(defn recoms-tooltip []
  [:div {:class "tt_w tt_black tt_down"
         :style {:position "absolute"
                 :opacity "0.00744534"
                 :top "-10.4px"
                 :left "734px"
                 :poiner-events "auto"
                 :display "none"}}
   [:div {:class "tt_text"}
    constants/similar]])

(defn scrollbar []
  [:div {:class "scrollbar_cont"
         :style {:margin-top "0px"
                 :margin-left "0px"
                 :height "550px"
                 :right "0px"
                 :left "auto"}}
   [:div {:class "scrollbar_inner"
          :style {:height "81px"
                  :margin-top "3px"}}]])

(defn cmp-application []
  [:div {:id "top_audio_layer_place"
         :class (join " " ["eltt" "top_audio_layer" "eltt_bottom eltt_vis"])
         :style {:width "790px"
                 :display "block"
                 :user-select (if (or @track-slider-down? @volume-slider-down?) :none :auto)}
         :on-click (fn [e]
                     (when @track-slider-down?
                       (let [current-time (get-track-slider-time (.-clientX e))]
                         (reset! app-state/current-time current-time)
                         (base-app/send-message {:type "current-time"
                                                 :value current-time})))
                     (when @volume-slider-down?
                       (let [volume-percent (get-volume-percent (.-clientX e))]
                         (base-app/send-message {:type "volume"
                                                 :value volume-percent})))
                     (reset-atoms-state))
         :on-mouse-move (fn [e]
                          (let [x-value e.clientX
                                y-value e.clientY]
                            (reset! base-popup/mouse-x-atom x-value)
                            (reset! base-popup/mouse-y-atom y-value)
                            (when @track-slider-down?
                              (reset! track-slider-time (get-track-slider-time x-value)))
                            (when @volume-slider-down?
                              (base-app/send-message {:type "volume"
                                                      :value (get-volume-percent x-value)}))))}
   [:div {:id "cmp-settings"}]
   [:div {:id "cmp-info"}]
   [:div {:class "_audio_layout audio_layout _audio_layer"}
    [:div {:class "audio_page_player _audio_page_player _audio_row clear_fix"}
     (play-btn)
     (prev-btn)
     (next-btn)
     [:div {:class "audio_page_player_ctrl audio_page_player_btns _audio_page_player_btns clear_fix"}
      (add-btn)
      (shuffle-btn)
      (repeat-btn)
      (recoms-btn)
      (translation-btn)]
     [:div {:class "audio_page_player_ctrl audio_page_player_track clear_fix"}
      (volume-slider)
      [:div {:class "audio_page_player_track_wrap"}
       (track-info)
       (track-slider)]]
     (repeat-tooltip)
     (recoms-tooltip)]
    [:div {:class "audio_layer_columns"}
     ;;      (scrollbar)
     [:div {:id "audio_layer_rows"
            :class (jcls ["audio_layer_rows_wrap"
                          "_audio_layer_rows_wrap"])
            :style {:min-height "520px"
                    :overflow "auto"
                    :margin-right "-16.5px"}
            :on-scroll (fn [e]
                         (save-scroll-position e.target.scrollTop)
                         (when (>= (+ e.target.scrollTop 200)
                                   (- e.target.scrollHeight e.target.clientHeight))
                           (handlers/load-next-part!)))}
      [:div {:class "audio_layer_rows_cont"}
       [:div {:class "_audio_rows_header audio_rows_header fixed"
              :style {:width "562.2px"}}]
       [:div {:class "_audio_padding_cont"}
        (cmp-search/search)

        [:div {:id "cmp-audios" :style {:display (if (base-popup/audio? (:type @app-state/content)) :block :none)}}]
        [:div {:id "cmp-user-list" :style {:display (if (base-popup/user-list? (:type @app-state/content)) :block :none)}}]
        [:div {:id "cmp-wall" :style {:display (if (base-popup/wall? (:type @app-state/content)) :block :none)}}]]]]

     [:div {:class (jcls ["audio_layer_menu_wrap"
                          "_audio_layer_menu_wrap"])
            :style {:top "0px"}
            :id "cmp-right-menu"}]

     [:div {:class (jcls ["audio_layer_menu_wrap"
                          "_audio_layer_menu_wrap"])
            :style {:bottom "0px" :top :auto}
            :id "cmp-bottom-right-menu"}]]]])

(defn recovery-scroll [render-func]
  (fn []
    (let [out (render-func)]
      (when (and (not (nil? out)) @first-open-page?)
        (js/setTimeout
         #(base-popup/apply-scroll-position @app-state/scroll-position))
        (reset! first-open-page? false))
      out)))

(r/render [cmp-application] (goog.dom.getElement "cmp-application"))
(r/render [cmp-menu/right-menu] (goog.dom.getElement "cmp-right-menu"))
(r/render [cmp-menu/bottom-right-menu] (goog.dom.getElement "cmp-bottom-right-menu"))
(r/render [(recovery-scroll cmp-audio/audios)] (goog.dom.getElement "cmp-audios"))
(r/render [(recovery-scroll cmp-users/users)] (goog.dom.getElement "cmp-user-list"))
(r/render [(recovery-scroll cmp-wall/wall)] (goog.dom.getElement "cmp-wall"))
(r/render [cmp-settings/settings] (goog.dom.getElement "cmp-settings"))
;; (r/render [cmp-info/info] (goog.dom.getElement "cmp-info"))


(handlers/set-user-id!)
