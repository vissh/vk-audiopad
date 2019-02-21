(ns popup.components.settings
  (:require [reagent.core :as r]
            [base.core :as base-app]
            [base.constants :as constants]))

(def display-settings-atom (r/atom false))

(defn settings []
  (when @display-settings-atom
    [:div
     [:div {:id "box_layer_bg"
            :class "fixed"
            :style {:display "block"}}]
     [:div {:id "box_layer_wrap"
            :class "scroll_fix_wrap fixed"
            :style {:display "block"}
            :on-click #(reset! display-settings-atom false)}
      [:div {:id "box_layer"}
       [:div {:class "popup_box_container"
              :style {:width "320px"
                      :height "auto"
                      :margin-top "142px"}}
        [:div {:class "box_layout"
               :on-click (fn [e]
                           (.stopPropagation e))}
         [:div {:class "box_title_wrap"}
          [:div {:class "box_x_button"
                 :on-click #(reset! display-settings-atom false)}]
          [:div {:class "box_title_controls"}]
          [:div {:class "box_title"} constants/settings]]
         [:div {:class "box_body"}
          (if base-app/firefox?
            [:strike [:a constants/keyboard-shortcuts]]
            [:a {:on-click (fn []
                             (let [url (if base-app/opera?
                                         "opera://settings/configureCommands"
                                         "chrome://extensions/configureCommands")]
                               (.. base-app/browser -tabs (create (clj->js {:url url}))))
                             (reset! display-settings-atom false))}
             constants/keyboard-shortcuts])]]]]]]))

(defn click-handler []
  (reset! display-settings-atom true))
