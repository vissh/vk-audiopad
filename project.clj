(defproject vk-audiopad "2.0"

  :url "https://github.com/vissh/vk-audiopad"

  :cljsbuild {:builds
              [{:source-paths ["src/cljs/base" "src/cljs/popup"]
                :compiler {:optimizations :none
                           :foreign-libs [{:file "libs/anchorme.js"
                                           :file-min "libs/anchorme.min.js"
                                           :provides ["anchorme"]}]
                           :output-to "extension/resources/public/js/popup.js"
                           :output-dir "extension/out/popup/"
                           :externs ["externs/chrome_extensions.js"]
                           :pretty-print false}}
               {:source-paths ["src/cljs/base"
                               "src/cljs/background"]
                :compiler {:optimizations :none
                           :foreign-libs [{:file "libs/unmask.js"
                                           :file-min "libs/unmask.min.js"
                                           :provides ["unmask"]},
                                          {:file "libs/hls.min.js"
                                           :file-min "libs/hls.min.js"
                                           :provides ["hls_lib"]}]
                           :output-to "extension/resources/public/js/background.js"
                           :output-dir "extension/out/background/"
                           :externs ["externs/chrome_extensions.js"]
                           :pretty-print false}}]}

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-cljfmt "0.6.4"]]

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/core.async "0.4.490"]
                 [reagent "0.8.1"]
                 [cljs-http "0.1.46"]
                 [hickory "0.7.1"]])
