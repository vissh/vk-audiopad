(ns base.vk
  (:require-macros [cljs.core.async.macros :refer (go)])
  (:require [clojure.string :refer (join split starts-with?)]
            [goog.string :as gstring]
            [goog.string.format]
            [cljs.core.async :refer (chan >! <!)]
            [reagent.core :as r]
            [cljs-http.client :as http]
            [base.core :as base-app]
            [base.constants :as constants]))

;; open audiopad
(def access-token "b60d7390b60d7390b65c6aa58fb63f2319bb60db60d7390eedac2c5c4071666dd54f361")

(defn smart-parse [response]
  (let [payload (nth (split (:body response) "<!>") 5 nil)]
    (if-not (and payload (starts-with? payload "<!json>"))
      payload
      (.parse js/JSON (subs payload (count "<!json>"))))))

(defn audio-search! [ps]
  (let [params (merge ps {:type "search"
                          :act "load_section"
                          :al "1"})]
    (go
      (let [response (<! (http/post "https://vk.com/al_audio.php"
                                    {:form-params params
                                     :headers {"x-requested-with" "XMLHttpRequest"}}))]
        (js->clj (smart-parse response) :keywordize-keys true)))))

(defn reload-audio! [ids]
  (let [params {:act "reload_audio"
                :al "1"
                :ids (join "," ids)}]
    (go
      (let [response (<! (http/post "https://vk.com/al_audio.php"
                                    {:form-params params
                                     :headers {"x-requested-with" "XMLHttpRequest"}}))]
        (smart-parse response)))))

(defn user-audio! [owner-id]
  (let [params {:act "load_section"
                :al "1"
                :claim "0"
                :offset "0"
                :playlist_id "-1"
                :type "playlist"
                :owner_id owner-id}]
    (go
      (let [limit 600
            response (<! (http/post "https://vk.com/al_audio.php"
                                    {:form-params params
                                     :headers {"x-requested-with" "XMLHttpRequest"}}))
            response (js->clj (smart-parse response) :keywordize-keys true)]
        (if (>= (count (:list response)) limit)
          (assoc response :list (subvec (:list response) 0 limit))
          response)))))

(defn recom! [owner-id track]
  (go
    (let [playlist-id (goog.string.format "audio%s_%s"
                                          (nth track constants/AUDIO_ITEM_INDEX_OWNER_ID)
                                          (nth track constants/AUDIO_ITEM_INDEX_ID))
          params {:act "load_section"
                  :al "1"
                  :playlist_id playlist-id
                  :claim "0"
                  :offset "0"
                  :owner_id owner-id
                  :type "recoms"}
          response (<! (http/post "https://vk.com/al_audio.php"
                                  {:form-params params
                                   :headers {"x-requested-with" "XMLHttpRequest"}}))]
      (smart-parse response))))

(defn recoms-owner! [owner-id]
  (go
    (let [params {:act "load_section"
                  :al "1"
                  :album_id "-2"
                  :claim "0"
                  :offset "0"
                  :owner_id owner-id
                  :type "recoms"}
          response (<! (http/post "https://vk.com/al_audio.php"
                                  {:form-params params
                                   :headers {"x-requested-with" "XMLHttpRequest"}}))]
      (smart-parse response))))

(defn friends-get [user-id]
  (go
    (let [params {:user_id user-id
                  :fields ["photo_100"]
                  :access_token access-token}
          response (<! (http/post "https://api.vk.com/method/friends.get?v=5.62" {:form-params params}))]
      (js->clj (:body response) :keywordize-keys true))))

(defn users-get-subscriptions [user-id]
  (go
    (let [params {:user_id user-id
                  :fields ["photo_100"]
                  :extended 1
                  :count 200
                  :access_token access-token}
          response (<! (http/post "https://api.vk.com/method/users.getSubscriptions?v=5.62" {:form-params params}))]
      (js->clj (:body response) :keywordize-keys true))))

(defn wall-get [owner-id & {:keys [offset limit] :or {offset 0 limit 100}}]
  (go
    (let [params {:owner_id owner-id
                  :offset offset
                  :count limit
                  :extended 1
                  :access_token access-token}
          response (<! (http/post "https://api.vk.com/method/wall.get?v=5.62" {:form-params params}))]
      (js->clj (:body response) :keywordize-keys true))))

(defn add-track [track]
  (go
    (let [oid (nth track constants/AUDIO_ITEM_INDEX_OWNER_ID)
          aid (nth track constants/AUDIO_ITEM_INDEX_ID)
          [hashes-exist hashes] (base-app/get-hashes track)
          add-hash (if hashes-exist
                     (first hashes)
                     (first (second (base-app/get-hashes (first (<! (reload-audio! [(base-app/get-audio-id track)])))))))
          params {:act "add"
                  :audio_id aid
                  :al 1
                  :from "search"
                  :gid 0
                  :hash add-hash
                  :audio_owner_id oid}
          resp (<! (http/post "https://vk.com/al_audio.php"
                              {:form-params params
                               :headers {"x-requested-with" "XMLHttpRequest"}}))]
      (js->clj (smart-parse resp) :keywordize-keys true))))

(defn delete-track [track]
  (go
    (let [oid (nth track constants/AUDIO_ITEM_INDEX_OWNER_ID)
          aid (nth track constants/AUDIO_ITEM_INDEX_ID)
          [hashes-exist [add-hash edit-hash action-hash delete-hash replace-hash url-hash]] (base-app/get-hashes track)
          params {:act "delete_audio"
                  :aid aid
                  :al 1
                  :hash delete-hash
                  :oid oid}]
      (<! (http/post "https://vk.com/al_audio.php"
                     {:form-params params
                      :headers {"x-requested-with" "XMLHttpRequest"}})))))

(defn get-lyrics! [track]
  (go
    (let [params {:act "get_lyrics"
                  :aid (base-app/get-audio-id track)
                  :al 1
                  :genre 0
                  :lid (nth track constants/AUDIO_ITEM_INDEX_LYRICS)
                  :top 0}
          resp (<! (http/post "https://vk.com/al_audio.php"
                              {:form-params params
                               :headers {"x-requested-with" "XMLHttpRequest"}}))]
      (smart-parse resp))))
