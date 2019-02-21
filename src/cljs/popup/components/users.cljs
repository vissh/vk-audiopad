(ns popup.components.users
  (:require-macros [cljs.core.async.macros :refer (go)])
  (:require [cljs.core.async :refer (chan >! <!)]
            [goog.style]
            [goog.dom.classes]
            [reagent.core :as r]
            [base.constants :as constants]
            [base.core :as base-app]
            [base.vk :as vk]
            [popup.base :as base-popup]
            [popup.state :as app-state]
            [popup.handlers :as handlers]))

(def user-id-atom (r/atom nil))
(def user-type-atom (r/atom nil))
(def action-atom (r/atom nil))

(defn get-user-id []
  (norm-user-id @user-type-atom @user-id-atom))

(defn norm-user-id [user-type user-id]
  (str (if (= user-type "profile") "" "-") user-id))

(defn users []
  (let [content @app-state/content]
    (when (base-popup/user-list? (:type content))
      (if (zero? (:count content))
        (base-popup/empty-block constants/content-not-found)
        [:div {:class "fans_rows"}
         [:div {:id "user-menu-wrap"
                :class "ui_actions_menu_wrap _ui_menu_wrap"}
          [:div {:id "user-menu"
                 :class "ui_actions_menu _ui_menu"
                 :style {:right "auto"
                         :z-index 300}
                 :on-mouse-leave #(do-action "remove")}
           [:a {:class "ui_actions_menu_item"
                :on-click (fn []
                            (do-action "remove")
                            (user-tracks-click (get-user-id)))} constants/audios]
           (when (= @user-type-atom "profile")
             [:a {:class "ui_actions_menu_item"
                  :on-click (fn []
                              (do-action "remove")
                              (friends-click (get-user-id)))} constants/friends])
           [:a {:class "ui_actions_menu_item"
                :on-click (fn []
                            (do-action "remove")
                            (handlers/wall-click! (get-user-id)))} constants/wall]]]
         (doall
          (for [[idx item] (map-indexed vector (:items content))]
            (let [user-id (:id item)
                  user-type (:type item "profile")
                  first-name (:first_name item)
                  last-name (:last_name item)
                  user-name (:name item)]
              ^{:key item}
              [:div {:class "fans_fan_row inl_bl"
                     :on-mouse-enter (fn [e]
                                       (when (not= @user-id-atom user-id)
                                         (reset! user-id-atom user-id)
                                         (reset! user-type-atom user-type)
                                         (do-action "remove")))}
               (when (zero? idx)
                 (base-popup/scroll-upper))
               [:div {:class "fans_fanph_wrap ui_zoom_wrap"}
                [:div {:class "fans_fan_ph"
                       :style {:cursor "pointer"}}
                 [:div {:class (str "fans_fan_bl_wrap" " user-" user-id)
                        :on-mouse-over (partial menu-over-handler user-type user-id)
                        :on-click (partial menu-over-handler user-type user-id)}
                  [:div {:class (str "fans_fan_bl" " user-" user-id)}]]
                 [:img {:class "fans_fan_img"
                        :alt (str first-name " " last-name)
                        :src (:photo_100 item)
                        :on-click #(user-tracks-click (norm-user-id user-type user-id))}]]]
               [:div {:class "fans_fan_name"}
                [:a {:class "fans_fan_lnk"
                     :on-click #(user-tracks-click (norm-user-id user-type user-id))}
                 (if user-name
                   user-name
                   (list first-name [:br {:key user-id}] last-name))]]])))]))))

(defn menu-over-handler [user-type user-id e]
  (let [rect-el (goog.dom.getElementByClass "fans_fan_bl" (goog.dom.getAncestorByClass e.target "fans_fan_row"))
        rect (.getBoundingClientRect rect-el)]
    (js/setTimeout (fn []
                     (let [element (js/document.elementFromPoint @base-popup/mouse-x-atom @base-popup/mouse-y-atom)]
                       (when (goog.dom.classes.has element (str "user-" user-id))
                         (reset! action-atom "add")
                         (defn close-unused-menu []
                           (js/setTimeout #(let [element (js/document.elementFromPoint @base-popup/mouse-x-atom @base-popup/mouse-y-atom)]
                                             (when-not (or (goog.dom.classes.has element "ui_actions_menu")
                                                           (goog.dom.classes.has element "ui_actions_menu_item"))
                                               (do-action "remove")))
                                          1200))

                         (defn display-menu []
                           (js/setTimeout (fn []
                                            (reset! user-id-atom user-id)
                                            (reset! user-type-atom user-type)
                                            (goog.dom.classes.add (goog.dom.getElement "user-menu-wrap") "shown")
                                            (close-unused-menu))
                                          130))

                         (js/setTimeout (fn []
                                          (when (= @action-atom "add")
                                            (def style (clj->js {:top (str (- (int rect.top) 30) "px")
                                                                 :left (str (- (int rect.left) 90) "px")}))
                                            (goog.style.setStyle (goog.dom.getElement "user-menu") style)
                                            (display-menu)))
                                        200))))
                   100)))

(defn do-action [action]
  (reset! action-atom action)
  (js/setTimeout #(let [user-menu-el (goog.dom.getElement "user-menu-wrap")]
                    (when user-menu-el
                      (cond
                        (= @action-atom action "remove")
                        (goog.dom.classes.remove user-menu-el "shown")

                        (= @action-atom action "add")
                        (goog.dom.classes.add user-menu-el "shown"))))
                 300))

(defn user-tracks-click [user-id]
  (base-app/set-to-storage! {:menu-position "user-music"
                             :search {:value ""}})
  (go
    (let [resp (<! (vk/user-audio! user-id))]
      (base-app/set-to-storage! {:content (or resp {:type "album"})
                                 :menu-position "user-music"}))))

(defn friends-click [user-id]
  (base-app/set-to-storage! {:search {:value ""}
                             :menu-position ""})
  (go
    (let [resp (<! (vk/friends-get user-id))]
      (base-app/set-to-storage! {:content (assoc (:response resp) :type "users")
                                 :menu-position ""}))))
