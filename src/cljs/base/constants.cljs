(ns base.constants)

(def AUDIO_ITEM_INDEX_ID 0)
(def AUDIO_ITEM_INDEX_OWNER_ID 1)
(def AUDIO_ITEM_INDEX_URL 2)
(def AUDIO_ITEM_INDEX_TITLE 3)
(def AUDIO_ITEM_INDEX_PERFORMER 4)
(def AUDIO_ITEM_INDEX_DURATION 5)
(def AUDIO_ITEM_INDEX_ALBUM_ID 6)
(def AUDIO_ITEM_INDEX_AUTHOR_ID 7)
(def AUDIO_ITEM_INDEX_AUTHOR_LINK 8)
(def AUDIO_ITEM_INDEX_LYRICS 9)
(def AUDIO_ITEM_INDEX_FLAGS 10)
(def AUDIO_ITEM_INDEX_CONTEXT 11)
(def AUDIO_ITEM_INDEX_EXTRA 12)
(def AUDIO_ITEM_INDEX_HASHES 13)

;; AUDIO_ITEM_INLINED_BIT: 1,
;; AUDIO_ITEM_CLAIMED_BIT: 16,
;; AUDIO_ITEM_RECOMS_BIT: 64,
;; AUDIO_ITEM_TOP_BIT: 1024,
;; AUDIO_ITEM_LONG_PERFORMER_BIT: 16384,
;; AUDIO_ITEM_LONG_TITLE_BIT: 32768,
;; AUDIO_ENOUGH_LOCAL_SEARCH_RESULTS: 500,
;; AUDIO_PLAYING_CLS: "audio_row_playing",
;; AUDIO_CURRENT_CLS: "audio_row_current",
;; AUDIO_LAYER_HEIGHT: 550,
;; AUDIO_LAYER_MIN_WIDTH: 400,
;; AUDIO_LAYER_MAX_WIDTH: 1e3,
;; AUDIO_HQ_LABEL_CLS: "audio_hq_label_show",


(def get-message
  (.. (or js/chrome js/browser) -i18n -getMessage))

(def play (get-message "play"))
(def prev (get-message "prev"))
(def next (get-message "next"))
(def add (get-message "add"))
(def shuffle (get-message "shuffle"))
(def repeat (get-message "repeat"))
(def similar (get-message "similar"))
(def recoms (get-message "recoms"))
(def recently-listened (get-message "recently_listened"))
(def friends (get-message "friends"))
(def subscriptions (get-message "subscriptions"))
(def search (get-message "search"))
(def search-track (get-message "search_track"))
(def search-all (get-message "search_all"))
(def search-by-performer (get-message "search_by_performer"))
(def sort-by (get-message "sort_by"))
(def text-only (get-message "text_only"))
(def tracks-not-found (get-message "tracks_not_found"))
(def content-not-found (get-message "content_not_found"))
(def current-playlist (get-message "current_playlist"))
(def my-music (get-message "my_music"))
(def audios (get-message "audios"))
(def wall (get-message "wall"))
(def by-popularity (get-message "by_popularity"))
(def by-duration (get-message "by_duration"))
(def help (get-message "support"))
(def on (get-message "on"))
(def off (get-message "off"))
(def settings (get-message "settings"))
(def keyboard-shortcuts (get-message "keyboard_shortcuts"))
(def information (get-message "information"))
(def fixed (get-message "fixed"))
(def login-to-vk (get-message "login_to_vk"))
(def tracking-protection (get-message "tracking_protection"))

(def genres ["Rock" 1
             "Pop" 2
             "Rap & Hip-Hop" 3
             "Easy Listening" 4
             "House & Dance" 5
             "Instrumental" 6
             "Metal" 7
             "Alternative" 21
             "Dubstep" 8
             "Jazz & Blues" 1001
             "Drum & Bass" 10
             "Trance" 11
             "Chanson" 12
             "Ethnic" 13
             "Acoustic & Vocal" 14
             "Reggae" 15
             "Classical" 16
             "Indie Pop" 17
             "Speech" 19
             "Electropop & Disco" 22
             "Other" 18])

(def error-timeout 1200)
