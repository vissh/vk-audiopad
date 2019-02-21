(ns popup.handlers
  (:require-macros [cljs.core.async.macros :refer (go)])
  (:require [clojure.string :refer (join)]
            [cljs.core.async :refer (chan >! <!)]
            [cljs-http.client :as http]
            [base.constants :as constants]
            [base.core :as base-app]
            [base.vk :as vk]
            [popup.base :as base-popup]
            [popup.state :as app-state]))

(defn up-scroll [func]
  (reset! base-popup/scroll-up? true))

(defn search-input-change! [event]
  (base-app/set-to-storage! {:search (assoc @app-state/search :value (-> event .-target .-value))}))

(defn search-input-key-press! [event]
  (when (= 13 (.-charCode event)) (audio-search!)))

(defn audio-search! []
  (up-scroll)
  (go
    (let [search-state @app-state/search
          params {:claim "0"
                  :album_id (:album_id search-state)
                  :offset (or (:offset search-state) 0)
                  :owner_id (or (:owner_id search-state) @app-state/user-id)
                  :search_history (or (:history search-state) 0)
                  :search_lyrics (or (:lyrics search-state) 0)
                  :search_performer (or (:performer search-state) 0)
                  :search_q (:value search-state)
                  :search_sort (or (:sort search-state) 0)}
          resp (<! (vk/audio-search! params))]
      (base-app/set-to-storage! {:content (or resp {:type "search"})
                                 :menu-position "search"}))))

(defn play-bnt-click! []
  (base-app/send-message {:type "play_pause"}))

(defn click-on-track! [idx track playlist-atom]
  (let [current-active-track @app-state/active-track]
    (if (= (base-app/get-audio-id track)
           (base-app/get-audio-id current-active-track))
      (base-app/send-message {:type "play_pause"})
      (base-app/set-to-storage! {:active-playlist @playlist-atom
                                 :active-track track
                                 :active-track-index idx
                                 :plays? true
                                 :shuffle? false
                                 :shuffle-queue []
                                 :shuffle-index -1}))))

(defn click-on-countdown! []
  (base-app/set-to-storage! {:countdown? (not @app-state/countdown?)}))

(defn active-playlist-click! []
  (up-scroll)
  (base-app/set-to-storage! {:menu-position "active-playlist"
                             :content @app-state/active-playlist
                             :search {:value ""}}))

(defn my-music-click! []
  (up-scroll)
  (base-app/set-to-storage! {:menu-position "my-music"
                             :search {:value ""}})
  (go
    (let [resp (<! (vk/user-audio! @app-state/user-id))]
      (base-app/set-to-storage! {:content (or resp {:type "album"})
                                 :menu-position "my-music"}))))

(defn clear-search-click! [e]
  (base-app/set-to-storage! {:search {:value ""}}))

(defn repeat-btn-click! [e]
  (base-app/set-to-storage! {:repeat? (not @app-state/repeat?)}))

(defn shuffle-btn-click! [e]
  (base-app/set-to-storage! {:shuffle? (not @app-state/shuffle?)
                             :shuffle-queue []
                             :shuffle-index -1}))

(def loading (atom false))

(defn load-next-part! []
  (let [content @app-state/content
        content-type (:type content)
        offset (:nextOffset content)
        has-more (:hasMore content)
        has-more (if (boolean? has-more) has-more (not (zero? has-more)))]
    (when (and
           (contains? #{"search" "recoms" "playlist"} content-type)
           has-more
           (not @loading))

      (reset! loading true)

      (cond
        (= "search" content-type)
        (go
          (let [search-state @app-state/search
                params {:claim "0"
                        :album_id (:albumId content)
                        :offset offset
                        :owner_id (:ownerId content)
                        :search_history (:history search-state)
                        :search_lyrics (:lyrics search-state)
                        :search_performer (:performer search-state)
                        :search_q (:value search-state)
                        :search_sort (:sort search-state)}
                resp (<! (vk/audio-search! params))
                content-list (distinct (concat (:list content) (:list resp)))
                new-content (assoc resp :list content-list)]

            (<! (base-app/set-to-storage! {:content new-content}))
            (reset! loading false)))

        (= "recoms" content-type)
        (go
          (let [params {:act "load_section"
                        :al "1"
                        :album_id (:albumId content)
                        :claim "0"
                        :offset offset
                        :owner_id (:ownerId content)
                        :type "recoms"}
                resp (js->clj (vk/smart-parse (<! (http/post "https://vk.com/al_audio.php" {:form-params params}))) :keywordize-keys true)
                content-list (distinct (concat (:list content) (:list resp)))
                new-content (assoc resp :list content-list)]

            (<! (base-app/set-to-storage! {:content new-content}))
            (reset! loading false)))

        (= "playlist" content-type)
        (go
          (let [params {:act "load_section"
                        :al "1"
                        :playlist_id (:id content)
                        :claim "0"
                        :offset offset
                        :owner_id (:ownerId content)
                        :type "playlist"}
                resp (js->clj (vk/smart-parse (<! (http/post "https://vk.com/al_audio.php" {:form-params params}))) :keywordize-keys true)
                content-list (distinct (concat (:list content) (:list resp)))
                new-content (assoc resp :list content-list)]

            (<! (base-app/set-to-storage! {:content new-content}))
            (reset! loading false)))))))

(defn click-on-track-recoms! [track]
  (up-scroll)
  (base-app/set-to-storage! {:search {:value ""}
                             :menu-position "similar"})
  (go
    (let [resp (<! (vk/recom! @app-state/user-id track))]
      (base-app/set-to-storage! {:content (or resp {:type "recoms"})
                                 :menu-position "similar"}))))

(defn click-on-recoms! []
  (up-scroll)
  (click-on-track-recoms! @app-state/active-track))

(defn my-recoms-click! []
  (up-scroll)
  (base-app/set-to-storage! {:search {:value ""}
                             :menu-position "recoms"})
  (go
    (let [resp (<! (vk/recoms-owner! @app-state/user-id))]
      (base-app/set-to-storage! {:content (or resp {:type "recoms"})
                                 :menu-position "recoms"}))))

(defn recently-listened-click! []
  (up-scroll)
  (base-app/set-to-storage! {:search {:value ""}
                             :content @app-state/recently-listened
                             :menu-position "recently-listened"}))

(defn friends-click! []
  (up-scroll)
  (base-app/set-to-storage! {:search {:value ""}
                             :menu-position "friend-list"})
  (go
    (let [user-id (:user-id (<! (base-app/get-from-storage! ["user-id"])))
          resp (<! (vk/friends-get user-id))]
      (base-app/set-to-storage! {:content (assoc (:response resp) :type "users")
                                 :menu-position "friend-list"}))))

(defn subscriptions-click! []
  (up-scroll)
  (base-app/set-to-storage! {:search {:value ""}
                             :menu-position "subscriptions"})
  (go
    (let [user-id (:user-id (<! (base-app/get-from-storage! ["user-id"])))
          resp (<! (vk/users-get-subscriptions user-id))]
      (base-app/set-to-storage! {:content (assoc (:response resp) :type "users")
                                 :menu-position "subscriptions"}))))

(defn wall-click! [user-id]
  (up-scroll)
  (base-app/set-to-storage! {:search {:value ""}
                             :menu-position "wall"})

  (defn get-items [user-id offset limit]
    (go
      (let [response (:response (<! (vk/wall-get user-id :offset offset :limit limit)))]
        [offset response])))

  (defn audios-with-limit [value]
    (let [audios-count (atom 0)]
      (fn [item]
        (let [audios (concat (:attachments item) (:attachments (first (:copy_history item))))
              audio-count (count (filter #(= (:type %) "audio") audios))]
          (when (and (not (zero? audio-count))
                     (<= @audios-count value))
            (reset! audios-count (+ @audios-count audio-count)))))))

  (defn $concat [k items]
    (apply concat (map k items)))

  (go
    (let [mc (cljs.core.async/merge [(get-items user-id 0 100)
                                     (get-items user-id 100 100)])
          result (map last (sort-by first [(<! mc) (<! mc)]))]
      (base-app/set-to-storage! {:content {:items (filter (audios-with-limit 500) ($concat :items result))
                                           :groups ($concat :groups result)
                                           :profiles ($concat :profiles result)
                                           :count (:count (first result))
                                           :type "wall"}
                                 :menu-position "wall"}))))

(defn add-track! [item]
  (go (<! (vk/add-track item))))

(defn delete-track! [item]
  (go (<! (vk/delete-track item))))

(defn get-lyrics! [item]
  (go (<! (vk/get-lyrics! item))))

(defn not? [value]
  (or (= value "") (not value) (= value "0")))

(defn set-user-id! []
  (defn show-log-in-msg []
    (base-app/show-notification constants/information
                                constants/login-to-vk
                                #(.. base-app/browser -tabs (create (clj->js {:url "https://vk.com/"})))))
  (go
    (let [init-user-id (:user-id (<! (base-app/get-from-storage! ["user-id"])))]
      (<! (base-app/set-user-id!))
      (let [user-id (:user-id (<! (base-app/get-from-storage! ["user-id"])))]
        (if (not? user-id)
          (if base-app/firefox?
            (let [response (<! (http/get "https://vk.com/"))
                  tracking-protection-enabled? (and (.. js/window -navigator -onLine)
                                                    (not (:success response)))]
              (if tracking-protection-enabled?
                (base-app/show-notification constants/information
                                            constants/tracking-protection)
                (show-log-in-msg)))
            (show-log-in-msg))
          (when (not? init-user-id)
            (my-music-click!)))))))
