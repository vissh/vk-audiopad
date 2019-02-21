(ns popup.components.menu
  (:require [clojure.string :refer (join)]
            [goog.string :as gstring]
            [base.constants :as constants]
            [popup.base :as base-popup]
            [popup.handlers :as handlers]
            [popup.state :as app-state]
            [popup.components.settings :as cmp-settings]))

(defn jcls [lst]
  (join " " lst))

(defn right-menu []
  (let [position @app-state/menu-position
        pos-index base-popup/menu-position-index]
    [:div {:class "audio_layer_menu_cont"}
     [:div {:class (jcls ["page_block"
                          "ui_rmenu"
                          "ui_rmenu_pr"
                          "ui_rmenu_sliding"])}

      [:a {:class (jcls ["ui_rmenu_item"
                         (when (= position "active-playlist")
                           "ui_rmenu_item_sel")])
           :on-click handlers/active-playlist-click!}
       [:span constants/current-playlist]]
      [:a {:class (jcls ["ui_rmenu_item"
                         (when (= position "my-music")
                           "ui_rmenu_item_sel")])
           :on-click handlers/my-music-click!}
       [:span constants/my-music]]

      [:a {:class (jcls ["ui_rmenu_item"
                         (when (= position "recoms")
                           "ui_rmenu_item_sel")])
           :on-click handlers/my-recoms-click!}
       [:span constants/recoms]]

      [:a {:class (jcls ["ui_rmenu_item"
                         (when (= position "recently-listened")
                           "ui_rmenu_item_sel")])
           :on-click handlers/recently-listened-click!}
       [:span constants/recently-listened]]

      [:a {:class (jcls ["ui_rmenu_item"
                         (when (= position "friend-list")
                           "ui_rmenu_item_sel")])
           :on-click handlers/friends-click!}
       [:span constants/friends]]

      [:a {:class (jcls ["ui_rmenu_item"
                         (when (= position "subscriptions")
                           "ui_rmenu_item_sel")])
           :on-click handlers/subscriptions-click!}
       [:span constants/subscriptions]]

      (when (contains? pos-index position)
        [:div {:class (jcls ["ui_rmenu_slider"
                             "_ui_rmenu_slider"])
               :style {:height "32px"
                       :top "6px"
                       :transform (gstring/format "translateY(%spx)"
                                                  (* (get pos-index position) 32))}}])]]))

(defn bottom-right-menu []
  [:div
   [:div {:style {:margin-bottom "20px"}}
    [:a {:style {:padding-left "20px" :color "#939699" :white-space "nowrap"}
         :on-click cmp-settings/click-handler}
     constants/settings]]
   [:div {:style {:margin-bottom "20px"}}
    [:a {:style {:padding-left "20px" :color "#939699" :white-space "nowrap"}
         :href "https://vk.me/vkaudiopad" :target "_blank"}
     constants/help]]])
