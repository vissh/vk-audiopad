(ns popup.components.wall
  (:require-macros [cljs.core.async.macros :refer (go)])
  (:require [clojure.string :refer (join split starts-with? replace)]
            [cljs.core.async :refer (<!)]
            [goog.string :as gstring]
            [goog.string.format]
            [reagent.core :as r]
            [hickory.core :as hickory]
            [base.constants :as constants]
            [base.core :as base-app]
            [popup.base :as base-popup]
            [popup.handlers :as handlers]
            [popup.state :as app-state]
            [popup.components.audio :as cmp-audio]
            [popup.components.users :as cmp-users]
            [anchorme]))

(defn wall []
  (def content @app-state/content)
  (when (base-popup/wall? (:type content))
    (def profiles (apply array-map (flatten (map #(list (:id %) %) (:profiles content)))))
    (def groups (apply array-map (flatten (map #(list (:id %) %) (:groups content)))))

    (def wall-playlist-atom (atom {:type "wall-playlist" :list []}))
    (def audio-index-atom (atom 0))

    (defn profile? [item]
      (> (:owner_id item) 0))

    (defn get-owner [item]
      (let [owner-id (:owner_id item)]
        (get
         (if (profile? item) profiles groups)
         (if (pos? owner-id) owner-id (- owner-id)))))

    (defn get-img-srс [item]
      (:photo_50 (get-owner item)))

    (defn get-name [item]
      (let [owner (get-owner item)]
        (if (profile? item)
          (str (:first_name owner) " " (:last_name owner))
          (:name owner))))

    (if (zero? (count (:items content)))
      (base-popup/empty-block constants/content-not-found)
      [:div {:class "wall_module" :id "public_wall"}
       [:div {:id "page_wall_posts" :class "wall_posts own"}
        (doall
         (for [[idx item] (map-indexed vector (:items content))]
           [:div {:key (hash item)
                  :class "_post post page_block all own"}
            (when (zero? idx)
              (base-popup/scroll-upper))
            [:div {:class "_post_content"}
             [:div {:class "post_header"}
              [:a {:class "post_image"
                   :on-click #(cmp-users/user-tracks-click (:owner_id item))}
               [:img {:src (get-img-srс item) :width 50 :height 50 :class "post_img"}]
               [:span {:class "blind_label"} "."]]
              [:div {:class "post_header_info"}
               [:h5 {:class "post_author"}
                [:a {:class "author"
                     :on-click #(cmp-users/user-tracks-click (:owner_id item))}
                 (get-name item)]]
               [:div {:class "post_date"}
                [:div {:class "post_link"}
                 [:span {:class "rel_date rel_date_needs_update"}
                  (get-post-time item)]]]
               [:div {:class "ui_actions_menu_wrap _ui_menu_wrap"
                      :style {:display "none"}}
                [:div {:class "ui_actions_menu_icons"}
                 [:span {:class "blind_label"} "Действия"]]
                [:div {:class "ui_actions_menu _ui_menu"}
                 [:a {:class "ui_actions_menu_item"} "Пожаловаться"]]]]]

             [:div {:class "post_content"}
              [:div {:class "post_info"}
               [:div {:class "wall_text"}
                [:div {:class "_wall_post_cont"}
                 [:div {:class "wall_post_text"
                        :style {:cursor "auto"}}
                  (get-text item)]

                 (let [copy-item (first (:copy_history item))]
                   (when copy-item
                     [:div {:class "copy_quote"}
                      [:div {:class "copy_post_header"}
                       [:div {:class "copy_post_image"}
                        [:img {:src (get-img-srс copy-item) :width 40 :height 40 :class "copy_post_img"}]
                        [:span {:class "blind_label"}]]
                       [:div {:class "copy_post_header_info"}
                        [:h5 {:class "copy_post_author"}
                         [:a {:class "copy_author"
                              :on-click #(cmp-users/user-tracks-click (:owner_id copy-item))}
                          (get-name copy-item)]]
                        [:div {:class "copy_post_date"}
                         [:div {:class "published_by_date"}
                          (get-post-time copy-item)]]]]
                      [:div {:class "wall_post_text"
                             :style {:cursor "auto"}} (get-text copy-item)]
                      (post-image copy-item 508)
                      [:div {:class "wall_audio_rows _wall_audio_rows"}
                       (doall
                        (for [attach-audio (map :audio (filter #(= (:type %) "audio") (:attachments copy-item)))]
                          (let [audio (audio-item attach-audio)
                                idx @audio-index-atom]
                            (reset! audio-index-atom (+ idx 1))
                            (swap! wall-playlist-atom assoc :list (conj (:list @wall-playlist-atom) audio))
                            (cmp-audio/audio-row idx audio wall-playlist-atom))))]]))

                 (post-image item 522)
                 [:div {:class "wall_audio_rows _wall_audio_rows"}
                  (doall
                   (for [attach-audio (map :audio (filter #(= (:type %) "audio") (:attachments item)))]
                     (let [audio (audio-item attach-audio)
                           idx @audio-index-atom]
                       (reset! audio-index-atom (+ idx 1))
                       (swap! wall-playlist-atom assoc :list (conj (:list @wall-playlist-atom) audio))
                       (cmp-audio/audio-row idx audio wall-playlist-atom))))]]]]]]]))]])))

(defn get-post-image-src [item width]
  (let [photo (:photo (first (filter #(= (:type %) "photo") (:attachments item))))
        photo-variants (map name (filter #(starts-with? (name %) "photo_") (keys photo)))
        varinat-values (map int (map #(replace % #"photo_" "") photo-variants))
        variant-num (apply min-key #(js/Math.abs (- % width) %) varinat-values)
        src (get photo (keyword (str "photo_" variant-num)))]
    (list (str width "px") (str (int (/ (* (:height photo) (/ (* width 100) (:width photo))) 100)) "px") src)))

(defn post-image [item max-width]
  (let [[width height src] (get-post-image-src item max-width)]
    [:div {:class "page_post_sized_thumbs clear_fix"
           :style {:width width :height height}}
     [:div {:class "page_post_thumb_wrap image_cover page_post_thumb_last_column page_post_thumb_last_row"
            :style {:width width :height height :background-image (str "url(" src ")")}}]]))

(defn get-text [item]
  (let [text (js/anchorme (replace (:text item) #"\n" "<br>")
                          (clj->js {:attributes [{:name "target" :value "_blank"}]}))]
    (for [[idx p] (map-indexed vector (map hickory/as-hiccup (hickory/parse-fragment text)))]
      (if (vector? p)
        (if (> (count p) 1)
          (assoc p 1 (assoc (second p) :key idx))
          p)
        (gstring/unescapeEntities p)))))

(defn get-post-time [item]
  (let [date (js/Date. (* (:date item) 1000))
        now (js/Date.)
        months ["янв" "фев" "март" "апр" "май" "июн" "июл" "авг" "сен" "окт" "ноя" "дек"]
        now-day (.getDate now)
        date-day (.getDate date)
        date-month (.getMonth date)
        date-year (.getFullYear date)
        same-years (= date-year (.getFullYear now))
        same-month (= date-month (.getMonth now))
        str-day (if (and (= date-day now-day) same-month same-years)
                  "сегодня"
                  (if (and (= date-day (- now-day 1)) same-month same-years)
                    "вчера"
                    (str date-day " " (nth months date-month))))]
    (if same-years
      (str str-day " в " (.getHours date) ":" (gstring/format "%02d" (.getMinutes date)))
      (str date-day " " (nth months date-month) " " date-year))))

(defn audio-item [item]
  [(:id item)
   (:owner_id item)
   nil  ;; url
   (:title item)
   (:artist item)
   nil  ;; duration
   nil  ;; album-id
   nil  ;; author-id
   nil  ;; author-link
   nil  ;; lyrics
   nil  ;; flags
   nil  ;; context
   nil  ;; extra
   nil])
