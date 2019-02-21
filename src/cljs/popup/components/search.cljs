(ns popup.components.search
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
            [popup.state :as app-state]))

(def display-search-options (r/atom false))
(def display-sort-by-block (r/atom false))
(def sort-by-popularity (r/atom true))

(defn reset-atoms-state []
  (reset! display-search-options false)
  (reset! display-sort-by-block false))

(defn search []
  (let [search-state @app-state/search
        search-value (:value search-state)]
    [:div {:class (jcls ["ui_search_new"
                         "ui_search"
                         (when
                          (or (= search-value "") (= search-value nil))
                           "ui_search_field_empty")
                         "_audio_search_input"
                         "audio_search_input"
                         "ui_search_custom"
                         "_wrap"])}
     [:div {:class "ui_search_input_block"}
      [:button {:class (jcls ["ui_search_button_search"
                              "_ui_search_button_search"])
                :on-click handlers/audio-search!}]
      [:div {:class "ui_search_input_inner"}
       [:div {:class "ui_search_progress"}]
       [:div {:class "ui_search_controls"}
        [:button {:class (jcls ["ui_search_params_button"
                                "_ui_search_params_button"
                                "ui_search_button_control"])
                  :on-click (fn [event]
                              (.stopPropagation event)
                              (reset! display-search-options (not @display-search-options)))}]
        [:button {:class (jcls ["ui_search_reset_button"
                                "ui_search_button_control"])
                  :on-click handlers/clear-search-click!}]]
       [:div {:class (jcls ["ui_search_suggester_shadow"
                            "_ui_search_suggester_shadow"])}]
       [:input {:type "input"
                :class "ui_search_field _field"
                :id "audio_layer_search"
                :autoComplete "off"
                :autoCorrect "off"
                :autoCapitalize "off"
                :spellCheck "off"
                :placeholder constants/search-track
                :value search-value
                :on-change handlers/search-input-change!
                :on-key-press handlers/search-input-key-press!}]]]
     [:div {:class (jcls ["ui_search_sugg_list"
                          "_ui_search_sugg_list"])}]
     [:div {:class "ui_search_filters_pane expanded"
            :style {:display "block"}}
      [:div {:class "ui_search_filters"}
       (when
        (:performer search-state)
         [:div {:class "token"
                :on-click #(go
                             (<! (base-app/set-to-storage! {:search (assoc search-state :performer nil)}))
                             (handlers/audio-search!))}
          [:div {:class "token_title"}
           constants/search-by-performer]
          [:div {:class "token_del"}]])
       (when
        (:sort search-state)
         [:div {:class "token"
                :on-click #(go
                             (<! (base-app/set-to-storage! {:search (assoc search-state :sort nil)}))
                             (handlers/audio-search!))}
          [:div {:class "token_title"} constants/by-duration]
          [:div {:class "token_del"}]])
       (when
        (:lyrics search-state)
         [:div {:class "token"
                :on-click #(go
                             (<! (base-app/set-to-storage! {:search (assoc search-state :lyrics nil)}))
                             (handlers/audio-search!))}
          [:div {:class "token_title"} constants/text-only]
          [:div {:class "token_del"}]])]]
     (search-options search-state)]))

(defn search-options [search-state]
  [:div {:class "eltt eltt_bottom eltt_vis"
         :style {:display (if @display-search-options :block :none)
                 :left "343.4px"
                 :top "50.2px"}
         :on-click (fn [event]
                     (.stopPropagation event)
                     (reset! display-sort-by-block false))}
   [:div {:class "ui_search_params_wrap"}
    [:div {:class "ui_search_fltr_label"}
     constants/search]
    [:div {:class "ui_search_fltr_sel"}
     [:div {:class (jcls ["radiobtn"
                          "_audio_search_type"
                          "_audio_search_type_all"
                          (when (not (:performer search-state)) "on")])
            :on-click #(go
                         (<! (base-app/set-to-storage! {:search (assoc search-state :performer nil)}))
                         (handlers/audio-search!))}
      constants/search-all]
     [:div {:class (jcls ["radiobtn"
                          "_audio_search_type"
                          "_audio_search_type_performer"
                          (when (:performer search-state) "on")])
            :on-click #(go
                         (<! (base-app/set-to-storage! {:search (assoc search-state :performer 1)}))
                         (handlers/audio-search!))}
      constants/search-by-performer]]
    [:div {:class "ui_search_fltr_label"} constants/sort-by]
    [:div {:class "ui_search_fltr_sel"
           :on-click (fn [event]
                       (.stopPropagation event)
                       (reset! display-sort-by-block (not @display-sort-by-block)))}
     [:div {:id "container1"
            :class (jcls ["selector_container"
                          "dropdown_container"
                          "big"])
            :style {:width "200px"}}
      [:table {:cellSpacing "0"
               :cellPadding "0"
               :class "selector_table"}
       [:tbody {}
        [:tr {}
         [:td {:class "selector"}
          [:div {:class "placeholder_wrap1"
                 :style {:display "none"}}
           [:div {:class "placeholder_wrap2"}
            [:div {:class "placeholder_content"
                   :style {:color "rgb(124, 127, 130)"}}]
            [:div {:class "placeholder_cover"}]]]
          [:span {:class "selected_items"}]
          [:input {:type "text"
                   :class (jcls ["selector_input"
                                 "selected"])
                   :readOnly "true"
                   :style {:color "rgb(124, 127, 130)"
                           :width "163px"}
                   :placeholder (if
                                 (:sort search-state)
                                  constants/by-duration
                                  constants/by-popularity)}]
          [:input {:type "hidden"
                   :name "selectedItems"
                   :id "selectedItems"
                   :value "0"
                   :class "resultField"}]
          [:input {:type "hidden"
                   :name "selectedItems_custom"
                   :id "selectedItems_custom"
                   :value ""
                   :class "customField"}]]
         [:td {:id "dropdown1"
               :class "selector_dropdown"
               :style {:width "26px"}}
          (gstring/unescapeEntities "&nbsp;")]]]]
      [:div {:class "results_container"}
       [:div {:class "result_list"
              :style {:display (if @display-sort-by-block :block :none)
                      :opacity "1"
                      :width "200px"
                      :height "auto"
                      :bottom "auto"}}
        [:ul {:style {:position "relative"
                      :visibility "visible"}}
         [:li {:class (jcls ["first" (when @sort-by-popularity "active")])
               :on-click #(go
                            (<! (base-app/set-to-storage! {:search (assoc search-state :sort nil)}))
                            (handlers/audio-search!))
               :on-mouse-over #(reset! sort-by-popularity true)}
          constants/by-popularity]
         [:li {:class (jcls ["last" (when (not @sort-by-popularity) "active")])
               :on-click #(go
                            (<! (base-app/set-to-storage! {:search (assoc search-state :sort 1)}))
                            (handlers/audio-search!))
               :on-mouse-over #(reset! sort-by-popularity false)}
          constants/by-duration]]]]]]
    [:div {:class (jcls ["checkbox"
                         "_audio_fltr_with_lyrics"
                         (when (:lyrics search-state) "on")])
           :on-click #(go
                        (<! (base-app/set-to-storage! {:search (assoc search-state :lyrics (if (:lyrics search-state) nil 1))}))
                        (handlers/audio-search!))}
     [:div {} constants/text-only]]]])
