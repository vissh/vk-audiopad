(ns popup.components.info
  (:require [reagent.core :as r]
            [base.core :as base-app]
            [base.constants :as constants]))

(def display-atom (r/atom true))

(defn info []
  (when @display-atom
    [:div
     [:div {:id "box_layer_bg"
            :class "fixed"
            :style {:display "block"}}]
     [:div {:id "box_layer_wrap"
            :class "scroll_fix_wrap fixed"
            :style {:display "block"}
            :on-click #(reset! display-atom false)}
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
                 :on-click #(reset! display-atom false)}]
          [:div {:class "box_title_controls"}]
          [:div {:class "box_title"} constants/information]]
         [:div {:class "box_body"}
          '("После обновления раздела с музыкой "
            [:a {:href "https://vk.com/audio" :target "_blank"} "https://vk.com/audio"]
            ", расширение немного сломалось. В скором времени всё заработает ✌️")]]]]]]))
