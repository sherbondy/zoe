(ns zoe.core)

(enable-console-print!)

(println "hello zoetrope.")


(def canvas (.getElementById js/document "canvas"))
(def w 640 #_(.-innerWidth js/window))
(def h 480 #_(.-innerHeight js/window))
(set! (.-width canvas) (* w 2))
(set! (.-height canvas) (* h 2))
(set! (.. canvas -style -width) (str w "px"))
(set! (.. canvas -style -height) (str h "px"))

(def px-ratio (or (.-devicePixelRatio js/window) 1))

(def ctx (.getContext canvas "2d"))
(.scale ctx px-ratio px-ratio)
(def img-data (.getImageData ctx 0 0 w h))

;; we are going to maintain a typed array with the drawing contents buffer
;; then we will blit additional ui on top of this (e.g. pointer hover cursor)
(def drawing-buf (js/ArrayBuffer. (.. img-data -data -length)))

;; two typed-array views into the drawing data buffer
(def drawing-buf8 (js/Uint8ClampedArray. drawing-buf))
(def drawing-data (js/Uint32Array. drawing-buf))

(aset drawing-data 1 0x0a0b0c0d)
(def little-endian? true)
(when (and
        (= (aget drawing-buf8 4) 0x0a)
        (= (aget drawing-buf8 5) 0x0b)
        (= (aget drawing-buf8 6) 0x0c)
        (= (aget drawing-buf8 7) 0x0d))
  (set! little-endian? false))


(def prev-point-x nil)
(def prev-point-y nil)

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

(defn handle-pointer-move [e]
  (let [x (js/Math.round (.-pageX e))
        y (js/Math.round (.-pageY e))
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

    (println (.-pointerType e) x y pressure (.-width e) (.-height e))))

(defn cancel-event [e]
  (.preventDefault e)
  (.stopPropagation e)
  false)

(defonce init
  (do
    (.addEventListener canvas "pointermove" handle-pointer-move)
    ;; on pointer up, we should save the ctx diff for undo/redo...
    (.addEventListener canvas "pointerup" handle-pointer-move)
    (.addEventListener canvas "pointerdown" handle-pointer-move)
    (.addEventListener js/window "contextmenu" cancel-event)))