(ns zoe.core
  (:require [reagent.core :as r]))

(enable-console-print!)

(println "hello zoetrope.")


(def gui (.getElementById js/document "gui"))
(def canvas (.getElementById js/document "canvas"))
(def w 480 #_(.-innerWidth js/window))
(def h 480 #_(.-innerHeight js/window))
(def px-ratio (or (.-devicePixelRatio js/window) 1))
(def ctx (.getContext canvas "2d"))

(defonce app-state
  (r/atom
    {:current-frame 0
     ;; a vector of typed arrays...
     :playing? false
     :frames []
     ;; a vector of image data url encoded values
     ;; to display thumbnail frame previews
     :thumbnails []}))


(defn handle-pointer-up [e]
  (let [img-data (.createImageData ctx (.-width canvas) (.-height canvas))]
    ;; we need to explicitly copy the image data
    ;; otherwise, we would just be storing a pointer...
    (.set (.-data img-data)
          (.-data (.getImageData ctx 0 0 (.-width canvas) (.-height canvas))))

    (swap! app-state
           (fn [s]
             (-> s
                 (assoc-in
                   [:thumbnails (:current-frame s)]
                   (.toDataURL canvas "img/png"))
                 (assoc-in
                   [:frames (:current-frame s)]
                   (.-data img-data)))))))


(defn add-frame [e]
  (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))
  (swap! app-state
         (fn [s]
           (assoc s :current-frame
                    (count (:frames s)))))
  (handle-pointer-up nil))


(defn change-frame! [i]
  (let [img-data (.createImageData ctx (.-width canvas) (.-height canvas))]
    (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))
    (.set (.-data img-data) (get-in @app-state [:frames i]))
    (.putImageData ctx img-data 0 0)
    (swap! app-state assoc :current-frame i)))

(defn play-pause! []
  (swap! app-state update :playing? not))

(defn timeline []
  [:div#timeline
   [:button {:id "play-pause"
             :on-click play-pause!}
    (if (:playing? @app-state) "Pause" "Play")]

   (let [thumbnails (:thumbnails @app-state)]
     (doall
       (for [i (range (count thumbnails))]
         [:img {:id (str "frame-" i)
                :key (str "frame-" i)
                :width 48 :height 48
                :on-click #(change-frame! i)
                :class (when (= i (:current-frame @app-state)) "active")
                :src (nth thumbnails i)}])))
   [:button {:id "add-frame"
             :on-click add-frame}
    "Add Frame"]])


(defn setup-canvas []
  (set! (.-width canvas) (* w 2))
  (set! (.-height canvas) (* h 2))
  (set! (.. canvas -style -width) (str w "px"))
  (set! (.. canvas -style -height) (str h "px"))
  (.scale ctx px-ratio px-ratio))


;(def img-data (.getImageData ctx 0 0 w h))
;(aset drawing-data
;      (+ (* y w) x)
;      (bit-or
;        (bit-shift-left 255 24) ;;a
;        (bit-shift-left 0   16) ;;b
;        (bit-shift-left 0   8)  ;;g
;        0)) ;;r
;(.set (.-data img-data) drawing-buf8)
;;; probably much faster to only put the single pixel that changed?
;(.putImageData ctx img-data 0 0))

;; we are going to maintain a typed array with the drawing contents buffer
;; then we will blit additional ui on top of this (e.g. pointer hover cursor)
;(def drawing-buf (js/ArrayBuffer. (.. img-data -data -length)))
;
;;; two typed-array views into the drawing data buffer
;(def drawing-buf8 (js/Uint8ClampedArray. drawing-buf))
;(def drawing-data (js/Uint32Array. drawing-buf))

(def little-endian? true)

(defn test-endianness []
  (let [img-data (.createImageData ctx 1 1)
        drawing-buf (js/ArrayBuffer. (.. img-data -data -length))
        drawing-buf8 (js/Uint8ClampedArray. drawing-buf)
        drawing-data (js/Uint32Array. drawing-buf)]
    (aset drawing-data 1 0x0a0b0c0d)
    (when (and
            (= (aget drawing-buf8 4) 0x0a)
            (= (aget drawing-buf8 5) 0x0b)
            (= (aget drawing-buf8 6) 0x0c)
            (= (aget drawing-buf8 7) 0x0d))
      (set! little-endian? false))))


(def prev-point-x nil)
(def prev-point-y nil)

(defn handle-pointer-move [e]
  (let [x (js/Math.round (- (.-pageX e) (.-offsetLeft canvas)))
        y (js/Math.round (- (.-pageY e) (.-offsetTop canvas)))
        pressure (or (.-pressure e) 0)]
    (when-not (= (.-pointerType e) "touch")
      (when (> pressure 0)
        (.beginPath ctx)
        (.moveTo ctx x y)

        (set! (.-lineWidth ctx) (* 2 pressure))
        (set! (.-strokeStyle ctx) "black")

        (if (and prev-point-x prev-point-y)
          ;; draw a line to smoothly connect mouse movements
          ;; if dragging pointer across screen
          (.lineTo ctx prev-point-x prev-point-y)
          ;; otherwise, draw a dot...
          (.lineTo ctx (inc x) (inc y)))
        (.stroke ctx)
        (set! prev-point-x x)
        (set! prev-point-y y))

      (when (= pressure 0)
        (set! prev-point-x nil)
        (set! prev-point-y nil)))

    ;; use finger as an eraser
    (when (and (= (.-pointerType e) "touch")
               (> pressure 0))
      (.beginPath ctx)

      (set! (.-fillStyle ctx) "white")
      (set! (.-lineWidth ctx) 0)

      (.arc ctx x y (.-width e) 0 (* js/Math.PI 2))
      (.fill ctx))

    #_(println (.-pointerType e) x y pressure (.-width e) (.-height e))))

(defn cancel-event [e]
  (.preventDefault e)
  (.stopPropagation e)
  false)

(defn cycle-animation []
  (let [s @app-state]
    (when (:playing? s)
      (if (< (inc (:current-frame s)) (count (:frames s)))
        (change-frame! (inc (:current-frame s)))
        (change-frame! 0)))))


(defonce init
  (do
    (setup-canvas)
    (test-endianness)
    ;; initialize empty frame
    (handle-pointer-up nil)

    (.addEventListener canvas "pointermove" handle-pointer-move)
    ;; on pointer up, we should save the ctx diff for undo/redo...
    (.addEventListener canvas "pointerup" handle-pointer-move)
    (.addEventListener canvas "pointerup" handle-pointer-up)
    (.addEventListener canvas "pointerdown" handle-pointer-move)
    (.addEventListener js/window "contextmenu" cancel-event)
    (r/render-component [timeline] gui)

    (js/setInterval cycle-animation 100)))
