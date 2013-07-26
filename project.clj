(defproject zoe "0.1.0-SNAPSHOT"
  :description "Frame-by-frame animation in the web browser."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [core.async "0.1.0-SNAPSHOT"]
                 [prismatic/dommy "0.1.1"]]

  :plugins [[lein-cljsbuild "0.3.2"]]

  :cljsbuild
  {:builds
   [{:id "simple"
     :source-paths ["src"]
     :compiler {:optimizations :simple
                :pretty-print true
                :output-to "zoe.js"}}]}
  )