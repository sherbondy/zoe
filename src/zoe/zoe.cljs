(ns zoe.core
  (:require [cljs.core.async :as async
            :refer [<! >! chan close! put! sliding-buffer]]
            [dommy.core :as dommy])
  (:require-macros [cljs.core.async.macros :as am :refer [go alt!]]
                   [dommy.macros :as dm :refer [deftemplate node sel sel1]]))

(deftemplate onion-skin-btn [onion-info]
  [:div#onion-skin
   [:input#prev-skin {:type "range" :min 0 :max 10 :step 1
                      :value (:prev onion-info)}]
   [:button#toggle-skin "Onion Skin"]
   [:input#next-skin {:type "range" :min 0 :max 10 :step 1
                      :value (:next onion-info)}]])

(deftemplate toolbar [tool-info]
  [:div#toolbar
   [:button#pen "Pen"]
   [:button#eraser "Eraser"]
   (onion-skin-btn (:onion-skin tool-info))])

(defn px [n]
  (str n "px"))

(deftemplate bounding-box [box-info]
  [:div#bounding-box
   {:style
     {:width  (px (:width box-info))
      :height (px (:height box-info))
      :margin-left (px (* -0.5 (:width box-info)))
      :margin-top (px (* -0.5 (:height box-info)))}}

   [:input {:type "text" :placeholder "width"
            :value (:width box-info)}]
   [:span "x"]
   [:input {:type "text" :placeholder "height"
            :value (:height box-info)}]])

(deftemplate frame [frame-info]
  [:div.frame
   [:input.count {:type "text" :placeholder "repeat"
                  :value (:repeat frame-info)}]
   [:label "x"]])

(def frames
  (repeat 10 {:repeat 1}))

(deftemplate playback-controls [playback]
  [:div#playback
   [:div (str "Frame "
              (:current-frame playback)
              " of "
              (count frames))]
   [:button#prev "Prev"]
   [:button#play-pause "Play"]
   [:button#next "Next"]])

(def state
  (atom
    {:playback {:playing? false :current-frame 0}
     :toolbar
     {:current-tool {:type :pen :diameter 10}
      :onion-skin {:enabled? false :prev 0 :next 0}}
     :bounding-box {:width 640 :height 480}
     :frames frames}))

(deftemplate app [state]
  [:div#content
   (toolbar (:toolbar state))
   (bounding-box (:bounding-box state))
   [:canvas#canvas]
   (doseq [frame-info frames]
     (frame frame-info))
   (playback-controls (:playback state))])

(defn render-zoe []
  (->
   (sel1 :body)
   (dommy/replace-contents! (app @state))))

(render-zoe)

(add-watch state :state-change (fn [k r o n] (render-zoe)))

(js/setTimeout
 #(swap! state assoc-in [:bounding-box :width] 480)
 2000)