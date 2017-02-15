(defproject zoe "0.1.0-SNAPSHOT"
  :description "Frame-by-frame animation in the web browser."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.473"]]

  :plugins [[lein-figwheel "0.5.9"]]

  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src/"]
     :figwheel {:websocket-host :js-client-host}
     :compiler {:main "zoe.core"
                :optimizations :none
                :pretty-print true
                :asset-path "js/out"
                :output-dir "resources/public/js/out"
                :output-to "resources/public/js/zoe.js"}}]})

