(ns dactyl-keyboard.thumbparam
  (:refer-clojure :exclude [use import])
  (:require [clojure.core.matrix :refer [array matrix mmul]]
            [scad-clj.scad :refer :all]
            [scad-clj.model :refer :all]
            [unicode-math.core :refer :all]))

(defn deg2rad [degrees]
  (* (/ degrees 180) pi))
; rad = pi * deg / 180
; 180 * rad / pi = deg

;;;;;;;;;;;;;;;;;;;;;;
;; Shape parameters ;;
;;;;;;;;;;;;;;;;;;;;;;

(def thumb-nrows 4)
(def thumb-ncols 2)

; (def tilt-default (deg2rad 30) )
(def thumb-α (deg2rad 45))                        ; curvature of the thumb-columns - 5 or 6?
(def thumb-β (deg2rad 60))                        ; curvature of the thumb-rows - 30 or 36?
(def thumb-centerrow (- thumb-nrows 3))             ; controls front-back tilt - 3
(def thumb-centercol 0.5)                       ; controls left-right tilt / tenting (higher number is more tenting) - 4
(def thumb-tenting-angle (deg2rad 0))            ; or, change this for more precise tenting control - 12
(def thumb-column-style
  (if (> thumb-nrows 4) :orthographic :standard))  ; options include :standard, :orthographic, and :fixed
; (def thumb-column-style :fixed)

; (defn thumb-column-offset [column] (cond
;                                (= column 2) [0 14.82 -4.5]            ; original [0 2.82 -4.5]
;                                (= column 3) [0 7.82 -2.25]            ; original [0 0 0]
;                                (>= column 4) [0 -5.18 3.39]             ; original [0 -5.8 5.64], [0 -12 5.64]
;                                :else [0 0 0]))
(defn thumb-column-offset [column] (cond
                                     (= column 2) [0 0 0]            ; original [0 2.82 -4.5]
                                     (= column 3) [0 0 0]            ; original [0 0 0]
                                     (>= column 4) [0 0 0]             ; original [0 -5.8 5.64], [0 -12 5.64]
                                     :else [0 0 0]))


(def keyboard-z-offset 0)               ; controls overall height; original=9 with centercol=3; use 16 for centercol=2
(def thumb-extra-width 0)                   ; extra space between the base of keys; original= 2, 2.5
(def thumb-extra-height 4)                  ; original= 0.5
(def wall-z-offset -15)                 ; length of the first downward-sloping part of the wall (negative)
(def wall-xy-offset 3)                  ; offset in the x and/or y direction for the first downward-sloping part of the wall (negative)
(def wall-thickness 2)                  ; wall thickness parameter; originally 5, 2

;; Settings for thumb-column-style == :fixed
;; The defaults roughly match Maltron settings
;;   http://patentimages.storage.googleapis.com/EP0219944A2/imgf0002.png
;; Fixed-z overrides the z portion of the column ofsets above.
;; NOTE: THIS DOESN'T WORK QUITE LIKE I'D HOPED.
(def fixed-angles [(deg2rad 10) (deg2rad 10) 0 0 0 (deg2rad -15) (deg2rad -15)])
(def fixed-x [-41.5 -22.5 0 20.3 41.4 65.5 89.6])  ; relative to the middle finger
(def fixed-z [12.1    8.3 0  5   10.7 14.5 17.5])
(def fixed-tenting (deg2rad 0))

;;;;;;;;;;;;;;;;;;;;;;;
;; General variables ;;
;;;;;;;;;;;;;;;;;;;;;;;

(def thumb-lastrow (dec thumb-nrows))
(def thumb-cornerrow (dec thumb-lastrow))
(def thumb-lastcol (dec thumb-ncols))

;;;;;;;;;;;;;;;;;
;; Switch Hole ;;
;;;;;;;;;;;;;;;;;

(def keyswitch-height 14.4) ;; Was 14.1, then 14.25, 14.4 before clc
(def keyswitch-width 14.4)  ; 14.4 before clc
(def sa-profile-key-height 12.7)
(def plate-thickness 4)
(def mount-width (+ keyswitch-width 3))
(def mount-height (+ keyswitch-height 3))

(def single-plate
  (let [top-wall (->> (cube mount-width 1.5 plate-thickness)
                      (translate [0
                                  (+ (/ 1.5 2) (/ keyswitch-height 2))
                                  (/ plate-thickness 2)]))
        left-wall (->> (cube 1.5 mount-height plate-thickness)
                       (translate [(+ (/ 1.5 2) (/ keyswitch-width 2))
                                   0
                                   (/ plate-thickness 2)]))
        side-nub (->> (binding [*fn* 30] (cylinder 1 2.75))
                      (rotate (/ π 2) [1 0 0])
                      (translate [(+ (/ keyswitch-width 2)) 0 1])
                      (hull (->> (cube 1.5 2.75 plate-thickness)
                                 (translate [(+ (/ 1.5 2) (/ keyswitch-width 2))
                                             0
                                             (/ plate-thickness 2)]))))
        plate-half (union top-wall left-wall (with-fn 100 side-nub))]
    (union plate-half
           (->> plate-half
                (mirror [1 0 0])
                (mirror [0 1 0])))))

;;;;;;;;;;;;;;;;
;; SA Keycaps ;;
;;;;;;;;;;;;;;;;
(def key-base-lift (+ 5 plate-thickness))
(def key-depth 12)
(def sa-length 18.25)
(def sa-double-length 37.5)
(def sa-cap {1 (let [bl2 (/ 18.5 2)
                     m (/ 17 2)
                     key-cap (hull (->> (polygon [[bl2 bl2] [bl2 (- bl2)] [(- bl2) (- bl2)] [(- bl2) bl2]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 0.05]))
                                   (->> (polygon [[m m] [m (- m)] [(- m) (- m)] [(- m) m]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 6]))
                                   (->> (polygon [[6 6] [6 -6] [-6 -6] [-6 6]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 key-depth])))]
                 (->> key-cap
                      (translate [0 0 key-base-lift])
                      (color [220/255 163/255 163/255 1])))
             2 (let [bl2 (/ sa-double-length 2)
                     bw2 (/ 18.25 2)
                     key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 0.05]))
                                   (->> (polygon [[6 16] [6 -16] [-6 -16] [-6 16]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 key-depth])))]
                 (->> key-cap
                      (translate [0 0 key-base-lift])
                      (color [230/255 193/255 169/255 1])))
             1.5 (let [bl2 (/ 18.25 2)
                       bw2 (/ 28 2)
                       key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                                          (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                          (translate [0 0 0.05]))
                                     (->> (polygon [[11 6] [-11 6] [-11 -6] [11 -6]])
                                          (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                          (translate [0 0 key-depth])))]
                   (->> key-cap
                        (translate [0 0 key-base-lift])
                        (color [240/255 223/255 175/255 1])))
             1.25 (let [bl2 (/ 18.25 2)
                        bw2 (/ 45 4)
                        key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                                           (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                           (translate [0 0 0.05]))
                                      (->> (polygon [[8.5 6] [-8.5 6] [-8.5 -6] [8.5 -6]])
                                           (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                           (translate [0 0 key-depth])))]
                    (->> key-cap
                         (translate [0 0 key-base-lift])
                         (color [127/255 159/255 127/255 0.7])))})


;;;;;;;;;;;;;;;;;;;;;;;;;
;; Placement Functions ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(def thumb-columns (range 0 thumb-ncols))
(def thumb-rows (range 0 thumb-nrows))

(def cap-top-height (+ plate-thickness sa-profile-key-height))
(def thumb-row-radius (+ (/ (/ (+ mount-height thumb-extra-height) 2)
                      (Math/sin (/ thumb-α 2)))
                   cap-top-height))
(def thumb-column-radius (+ (/ (/ (+ mount-width thumb-extra-width) 2)
                         (Math/sin (/ thumb-β 2)))
                      cap-top-height))
(def thumb-column-x-delta (+ -1 (- (* thumb-column-radius (Math/sin thumb-β)))))
(def column-base-angle (* thumb-β (- thumb-centercol 2)))

(defn apply-key-geometry [translate-fn rotate-x-fn rotate-y-fn column row shape]
  (let [column-angle (* thumb-β (- thumb-centercol column))
        placed-shape (->> shape
                          (translate-fn [0 0 (- thumb-row-radius)])
                          (rotate-y-fn  column-angle)
                          (rotate-x-fn  (* thumb-α (- thumb-centerrow row)))
                          (translate-fn [0 0 thumb-row-radius])
                          (translate-fn [0 0 (- thumb-column-radius)])
                          (translate-fn [0 0 thumb-column-radius])
                          (translate-fn (thumb-column-offset column)))
        column-z-delta (* thumb-column-radius (- 1 (Math/cos column-angle)))
        placed-shape-ortho (->> shape
                                (translate-fn [0 0 (- thumb-row-radius)])
                                (rotate-y-fn  column-angle)
                                (rotate-x-fn  (* thumb-α (- thumb-centerrow row)))
                                ; (rotate tilt-default [(- thumb-centercol column) 0 0])
                                (translate-fn [0 0 thumb-row-radius])
                                (translate-fn [(- (* (- column thumb-centercol) thumb-column-x-delta)) 0 column-z-delta])
                                (translate-fn (thumb-column-offset column)))
        placed-shape-fixed (->> shape
                                (rotate-y-fn  (nth fixed-angles column))
                                (translate-fn [(nth fixed-x column) 0 (nth fixed-z column)])
                                (translate-fn [0 0 (- (+ thumb-row-radius (nth fixed-z column)))])
                                (rotate-x-fn  (* thumb-α (- thumb-centerrow row)))
                                (translate-fn [0 0 (+ thumb-row-radius (nth fixed-z column))])
                                (rotate-y-fn  fixed-tenting)
                                (translate-fn [0 (second (thumb-column-offset column)) 0]))]
    (->> (case thumb-column-style
           :orthographic placed-shape-ortho
           :fixed        placed-shape-fixed
           placed-shape)
         (rotate-y-fn  thumb-tenting-angle)
         (translate-fn [0 0 keyboard-z-offset])
        ;  (rotate tilt-default [(- thumb-centercol column) 0 0])
         
         )))

(defn key-place [column row shape]
  (apply-key-geometry translate
                      (fn [angle obj] (rotate angle [1 0 0] obj))
                      (fn [angle obj] (rotate angle [0 1 0] obj))
                      column row shape))

(defn rotate-around-x [angle position]
  (mmul
   [[1 0 0]
    [0 (Math/cos angle) (- (Math/sin angle))]
    [0 (Math/sin angle)    (Math/cos angle)]]
   position))

(defn rotate-around-y [angle position]
  (mmul
   [[(Math/cos angle)     0 (Math/sin angle)]
    [0                    1 0]
    [(- (Math/sin angle)) 0 (Math/cos angle)]]
   position))

(defn thumb-key-position [column row position]
  (apply-key-geometry (partial map +) rotate-around-x rotate-around-y column row position))

(def key-holes
  (apply union
         (for [column thumb-columns
               row thumb-rows
               :when (or (.contains [2 3] column)
                         (not= row thumb-lastrow))]
           (->> single-plate
                (key-place column row)))))

(def caps
  (apply union
         (for [column thumb-columns
               row thumb-rows
               :when (or (.contains [2 3] column)
                         (not= row thumb-lastrow))]
           (->> (sa-cap (if (= column 5) 1 1))
                (key-place column row)))))

; (pr (rotate-around-y π [10 0 1]))
; (pr (thumb-key-position 1 thumb-cornerrow [(/ mount-width 2) (- (/ mount-height 2)) 0]))

;;;;;;;;;;;;;;;;;;;;
;; Web Connectors ;;
;;;;;;;;;;;;;;;;;;;;

(def web-thickness 3.5)  ; 3.5
(def post-size 0.1)
(def web-post (->> (cube post-size post-size web-thickness)
                   (translate [0 0 (+ (/ web-thickness -2)
                                      plate-thickness)])))

(def post-adj (/ post-size 2))
(def web-post-tr (translate [(- (/ mount-width 2) post-adj) (- (/ mount-height 2) post-adj) 0] web-post))
(def web-post-tl (translate [(+ (/ mount-width -2) post-adj) (- (/ mount-height 2) post-adj) 0] web-post))
(def web-post-br (translate [(- (/ mount-width 2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))
(def web-post-bl (translate [(+ (/ mount-width -2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))

(defn triangle-hulls [& shapes]
  (apply union
         (map (partial apply hull)
              (partition 3 1 shapes))))

(def connectors
  (apply union
         (concat
          ;; Row connections
          (for [column (range 0 (dec thumb-ncols))
                row (range 0 thumb-lastrow)]
            (triangle-hulls
             (key-place (inc column) row web-post-tl)
             (key-place column row web-post-tr)
             (key-place (inc column) row web-post-bl)
             (key-place column row web-post-br)))

          ;; Column connections
          (for [column thumb-columns
                row (range 0 thumb-cornerrow)]
            (triangle-hulls
             (key-place column row web-post-bl)
             (key-place column row web-post-br)
             (key-place column (inc row) web-post-tl)
             (key-place column (inc row) web-post-tr)))

          ;; Diagonal connections
          (for [column (range 0 (dec thumb-ncols))
                row (range 0 thumb-cornerrow)]
            (triangle-hulls
             (key-place column row web-post-br)
             (key-place column (inc row) web-post-tr)
             (key-place (inc column) row web-post-bl)
             (key-place (inc column) (inc row) web-post-tl))))))

;;;;;;;;;;
;; Case ;;
;;;;;;;;;;

(defn bottom [height p]
  (->> (project p)
       (extrude-linear {:height height :twist 0 :convexity 0})
       (translate [0 0 (- (/ height 2) 10)])))

(defn bottom-hull [& p]
  (hull p (bottom 0.001 p)))

(def left-wall-x-offset 10)
(def left-wall-z-offset  3)

(defn left-key-position [row direction]
  (map - (thumb-key-position 0 row [(* mount-width -0.5) (* direction mount-height 0.5) 0]) [left-wall-x-offset 0 left-wall-z-offset]))

(defn left-key-place [row direction shape]
  (translate (left-key-position row direction) shape))


(defn wall-locate1 [dx dy] [(* dx wall-thickness) (* dy wall-thickness) -1])
(defn wall-locate2 [dx dy] [(* dx wall-xy-offset) (* dy wall-xy-offset) wall-z-offset])
(defn wall-locate3 [dx dy] [(* dx (+ wall-xy-offset wall-thickness)) (* dy (+ wall-xy-offset wall-thickness)) wall-z-offset])

(defn wall-brace [place1 dx1 dy1 post1 place2 dx2 dy2 post2]
  (union
   (hull
    (place1 post1)
    (place1 (translate (wall-locate1 dx1 dy1) post1))
    (place1 (translate (wall-locate2 dx1 dy1) post1))
    (place1 (translate (wall-locate3 dx1 dy1) post1))
    (place2 post2)
    (place2 (translate (wall-locate1 dx2 dy2) post2))
    (place2 (translate (wall-locate2 dx2 dy2) post2))
    (place2 (translate (wall-locate3 dx2 dy2) post2)))
   (bottom-hull
    (place1 (translate (wall-locate2 dx1 dy1) post1))
    (place1 (translate (wall-locate3 dx1 dy1) post1))
    (place2 (translate (wall-locate2 dx2 dy2) post2))
    (place2 (translate (wall-locate3 dx2 dy2) post2)))
   ))

(defn key-wall-brace [x1 y1 dx1 dy1 post1 x2 y2 dx2 dy2 post2]
  (wall-brace (partial key-place x1 y1) dx1 dy1 post1
              (partial key-place x2 y2) dx2 dy2 post2))

(def case-walls
  (union
   ; back wall
   (for [x (range 0 thumb-ncols)] (key-wall-brace x 0 0 1 web-post-tl x       0 0 1 web-post-tr))
   (for [x (range 1 thumb-ncols)] (key-wall-brace x 0 0 1 web-post-tl (dec x) 0 0 1 web-post-tr))
   (key-wall-brace thumb-lastcol 0 0 1 web-post-tr thumb-lastcol 0 1 0 web-post-tr)
   ; right wall
   (for [y (range 0 thumb-lastrow)] (key-wall-brace thumb-lastcol y 1 0 web-post-tr thumb-lastcol y       1 0 web-post-br))
   (for [y (range 1 thumb-lastrow)] (key-wall-brace thumb-lastcol (dec y) 1 0 web-post-br thumb-lastcol y 1 0 web-post-tr))
   (key-wall-brace thumb-lastcol thumb-cornerrow 0 -1 web-post-br thumb-lastcol thumb-cornerrow 1 0 web-post-br)
   ; left wall
   (for [y (range 0 thumb-lastrow)] (union (wall-brace (partial left-key-place y 1)       -1 0 web-post (partial left-key-place y -1) -1 0 web-post)
                                     (hull (key-place 0 y web-post-tl)
                                           (key-place 0 y web-post-bl)
                                           (left-key-place y  1 web-post)
                                           (left-key-place y -1 web-post))))
   (for [y (range 1 thumb-lastrow)] (union (wall-brace (partial left-key-place (dec y) -1) -1 0 web-post (partial left-key-place y  1) -1 0 web-post)
                                     (hull (key-place 0 y       web-post-tl)
                                           (key-place 0 (dec y) web-post-bl)
                                           (left-key-place y        1 web-post)
                                           (left-key-place (dec y) -1 web-post))))
   (wall-brace (partial key-place 0 0) 0 1 web-post-tl (partial left-key-place 0 1) 0 1 web-post)
   (wall-brace (partial left-key-place 0 1) 0 1 web-post (partial left-key-place 0 1) -1 0 web-post)
  ;  ; front wall
  ;  (key-wall-brace thumb-lastcol 0 0 1 web-post-tr thumb-lastcol 0 1 0 web-post-tr)
  ;  (key-wall-brace 3 thumb-lastrow   0 -1 web-post-bl 3 thumb-lastrow 0.5 -1 web-post-br)
  ;  (key-wall-brace 3 thumb-lastrow 0.5 -1 web-post-br 4 thumb-cornerrow 1 -1 web-post-bl)
  ;  (for [x (range 4 thumb-ncols)] (key-wall-brace x thumb-cornerrow 0 -1 web-post-bl x       thumb-cornerrow 0 -1 web-post-br))
  ;  (for [x (range 5 thumb-ncols)] (key-wall-brace x thumb-cornerrow 0 -1 web-post-bl (dec x) thumb-cornerrow 0 -1 web-post-br))
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Model for the this thumb    ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def model-thumb (difference
                  (union
                   key-holes
                   connectors
                  ;  case-walls
                    caps
                   )
                  (translate [0 0 -20] (cube 350 350 40))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Create the SCAD files ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(spit "things/bowl.scad"
      (write-scad model-thumb) 
        )

(defn -main [dum] 1)  ; dummy to make it easier to batch
