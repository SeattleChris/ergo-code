(ns dactyl-keyboard.dactyl
  (:refer-clojure :exclude [use import])
  (:require [clojure.core.matrix :refer [array matrix mmul]]
            [scad-clj.scad :refer :all]
            [scad-clj.model :refer :all]
            [unicode-math.core :refer :all]))

(defn deg2rad [degrees]   ; 1 pi radians = 180 degrees
  (* (/ degrees 180) pi))

;;;;;;;;;;;;;;;;;;;;;;
;; Shape parameters ;;
;;;;;;;;;;;;;;;;;;;;;;
(def nrows 5)
(def ncols 5)
(def α (deg2rad 36))                    ; curvature of the columns (front to back)- 30 to 36 degrees seems max 
(def β (deg2rad 2))             ; Was 6 ; curvature of the rows (left to right) - adds to tenting
(def extra-width 2)                     ; extra space between the base of keys; Normal specification when flat is 1.65
(def extra-height 0.5)                  ; original= 0.5; to spec when flat is 1.65
(def wall-z-offset -12)                 ; length of the first downward-sloping part of the wall (negative) ; original: -15
(def wall-xy-offset 3)                  ; offset in the x and/or y direction for the first downward-sloping part of the wall (negative)
(def wall-thickness 3.5)                  ; Was 2; wall thickness parameter
(def tilt-pivotrow (- nrows 1 (/ nrows 2))) ; controls front-back tilt: Even nrows means flat home row. Odd nrows means flat is between home row and 1 row up. 
(def tent-pivotcol 4 )                       ; controls left-right tilt / tenting (higher number is more tenting)
(def tenting-angle (deg2rad 30))            ; or, change this for more precise tenting control
(def keyboard-z-offset 8)  ; 1 @ 4, 3 @ 5, 9 @ 6            ; controls overall height, affected by tenting; original=9 with tent-pivotcol=3; use 16 for tent-pivotcol=2
(def column-style
  (if (> nrows 3) :orthographic :standard))  ; options include :standard, :orthographic, and :fixed
(def cherry-brand-keyswitch false)
(defn column-offset [column] (cond
                               (= column 2)  [0 13.82 -3.5 ]  ; tried [0 14.82 -4.5 ]  ; original [0 2.82 -4.5]
                               (= column 3)  [0  7.82 -2.25]  ; tried [0  7.82 -2.25]  ; original [0 0 0]
                               (>= column 4) [0 -5.18  2.39]  ; tried [0 -5.18  3.39]  ; original [0 -5.8 5.64], [0 -12 5.64]
                               :else [0 0 0]))  ; Column 0 & 1 are the pointer finger
;; Settings for column-style == :fixed
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
(def lastrow (dec nrows))
(def cornerrow (dec lastrow))
(def lastcol (dec ncols))

;;;;;;;;;;;;;;;;;
;; Switch Hole ;;
;;;;;;;;;;;;;;;;;
(def keyswitch-height (if (= cherry-brand-keyswitch true) 14.4 14.35)) ; try 14.15, 14.25 ; Was 14.1, then 14.25, then 14.4 
(def keyswitch-width  (if (= cherry-brand-keyswitch true) 14.4 14.35 ))  ; try ?? ; Was 14.4 
(def clip-keyswitch   (if (= cherry-brand-keyswitch true)  1.0  0.5 ))  ; Was 1 for cherry, for others: 0.5 with width at 14.4 was too loose. 
(def sa-profile-key-height 12.7)
(def plate-thickness 3.5)  ; was 4 ; TODO: Decide - Should this be 1.5? according to keyswitch specifications, should be 1.5
; For key spacing (on flat layout) 19.05mm x 19.05mm is standard placeholder per key
; Standard keycaps are about 18mm x 18mm 
(def mount-width (+ keyswitch-width 3))     
(def mount-height (+ keyswitch-height 3))
(def mount-wall-thickness 1.5)  
(def single-plate
  (let [top-wall (->> (cube mount-width mount-wall-thickness plate-thickness)
                      (translate [0
                                  (+ (/ mount-wall-thickness 2) (/ keyswitch-height 2))
                                  (/ plate-thickness 2)]))
        left-wall (->> (cube mount-wall-thickness mount-height plate-thickness)
                       (translate [(+ (/ mount-wall-thickness 2) (/ keyswitch-width 2))
                                   0
                                   (/ plate-thickness 2)]))
        side-nub (->> (binding [*fn* 30] (cylinder clip-keyswitch 2.75))
                      (rotate (/ π 2) [1 0 0])
                      (translate [(+ (/ keyswitch-width 2)) 0 1])
                      (hull (->> (cube mount-wall-thickness 2.75 plate-thickness)
                                 (translate [(+ (/ mount-wall-thickness 2) (/ keyswitch-width 2))
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
(def sa-length 18)  ; originally 18.25
(def default-spacing 19.05)  ; Normal keyboards have a placeholder space of 19.05mm for 1u keys
(def key-gap (- default-spacing sa-length))  
(def sa-double-length (+ key-gap (* 2 sa-length)))
(def sa-cap {1 (let [bl2 (/ sa-length 2)
                     m (/ 17 2)
                     lt2 (* 2 (/ bl2 3))
                     key-cap (hull (->> (polygon [[bl2 bl2] [bl2 (- bl2)] [(- bl2) (- bl2)] [(- bl2) bl2]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 0.05]))
                                   (->> (polygon [[m m] [m (- m)] [(- m) (- m)] [(- m) m]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 (/ key-depth 2)]))
                                   (->> (polygon [[6 6] [6 -6] [-6 -6] [-6 6]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 key-depth])))]
                 (->> key-cap
                      (translate [0 0 key-base-lift])
                      (color [220/255 163/255 163/255 1])))
             2 (let [bl2 (/ sa-double-length 2)
                     bw2 (/ sa-length 2)
                     key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 0.05]))
                                   (->> (polygon [[6 16] [6 -16] [-6 -16] [-6 16]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 key-depth])))]
                 (->> key-cap
                      (translate [0 0 key-base-lift])
                      (color [230/255 193/255 169/255 1])))
             1.5 (let [bl2 (/ sa-length 2)
                       bw2 (/ (- (* default-spacing 1.5) key-gap) 2)  ; (/ 28 2)
                       key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                                          (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                          (translate [0 0 0.05]))
                                     (->> (polygon [[11 6] [-11 6] [-11 -6] [11 -6]])
                                          (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                          (translate [0 0 key-depth])))]
                   (->> key-cap
                        (translate [0 0 key-base-lift])
                        (color [240/255 223/255 175/255 1])))
             1.25 (let [bl2 (/ sa-length 2)
                       bw2 (/ (- (* default-spacing 1.25) key-gap) 2)
                       key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                                          (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                          (translate [0 0 0.05]))
                                     (->> (polygon [[8.5 6] [-8.5 6] [-8.5 -6] [8.5 -6]])
                                          (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                          (translate [0 0 key-depth])))]
                   (->> key-cap
                        (translate [0 0 key-base-lift])
                        (color [127/255 159/255 127/255 0.7])))
              }
)
;;;;;;;;;;;;;;;;;;;;;;;;;
;; Placement Functions ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(def columns (range 0 ncols))
(def rows (range 0 nrows))
(def cap-top-height (+ plate-thickness sa-profile-key-height))
(def row-radius (+ (/ (/ (+ mount-height extra-height) 2)
                      (Math/sin (/ α 2)))
                   cap-top-height))
(def column-radius (+ (/ (/ (+ mount-width extra-width) 2)
                         (Math/sin (/ β 2)))
                      cap-top-height))
(def column-x-delta (+ -1 (- (* column-radius (Math/sin β)))))
(def column-base-angle (* β (- tent-pivotcol 2)))

(defn apply-key-geometry [translate-fn rotate-x-fn rotate-y-fn column row shape]
  (let [column-angle (* β (- tent-pivotcol column))
        placed-shape (->> shape
                          (translate-fn [0 0 (- row-radius)])
                          (rotate-x-fn  (* α (- tilt-pivotrow row)))
                          (translate-fn [0 0 row-radius])
                          (translate-fn [0 0 (- column-radius)])
                          (rotate-y-fn  column-angle)
                          (translate-fn [0 0 column-radius])
                          (translate-fn (column-offset column)))
        column-z-delta (* column-radius (- 1 (Math/cos column-angle)))
        placed-shape-ortho (->> shape  
                                (translate-fn [0 0 (- row-radius)])
                                (rotate-x-fn  (* α (- tilt-pivotrow row)))
                                (translate-fn [0 0 row-radius])
                                (rotate-y-fn  column-angle)
                                (translate-fn [(- (* (- column tent-pivotcol) column-x-delta)) 0 column-z-delta])
                                (translate-fn (column-offset column)))
        placed-shape-fixed (->> shape
                                (rotate-y-fn  (nth fixed-angles column))
                                (translate-fn [(nth fixed-x column) 0 (nth fixed-z column)])
                                (translate-fn [0 0 (- (+ row-radius (nth fixed-z column)))])
                                (rotate-x-fn  (* α (- tilt-pivotrow row)))
                                (translate-fn [0 0 (+ row-radius (nth fixed-z column))])
                                (rotate-y-fn  fixed-tenting)
                                (translate-fn [0 (second (column-offset column)) 0])
                                )]
    (->> (case column-style
          :orthographic placed-shape-ortho
          :fixed        placed-shape-fixed
                        placed-shape)
         (rotate-y-fn  tenting-angle)
         (translate-fn [0 0 keyboard-z-offset]))
    ))

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

(defn key-position [column row position]
  (apply-key-geometry (partial map +) rotate-around-x rotate-around-y column row position))

(def key-holes
  (apply union
         (for [column columns
               row rows
               :when (or (.contains [2 3] column)
                         (not= row lastrow))]
           (->> single-plate
                (key-place column row)))))
(def caps
  (apply union
         (for [column columns
               row rows
               :when (or (.contains [2 3] column)
                         (not= row lastrow))]
           (->> (sa-cap (if (= column 5) 1 1))
                (key-place column row)))))

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
          (for [column (range 0 (dec ncols))
                row (range 0 lastrow)]
            (triangle-hulls
             (key-place (inc column) row web-post-tl)
             (key-place column row web-post-tr)
             (key-place (inc column) row web-post-bl)
             (key-place column row web-post-br)))

          ;; Column connections (except the bonus on lastrow)
          (for [column columns
                row (range 0 cornerrow)]
            (triangle-hulls
             (key-place column row web-post-bl)
             (key-place column row web-post-br)
             (key-place column (inc row) web-post-tl)
             (key-place column (inc row) web-post-tr)))
          ;; Diagonal connections
          (for [column (range 0 (dec ncols))
                row (range 0 cornerrow)]
            (triangle-hulls
             (key-place column row web-post-br)
             (key-place column (inc row) web-post-tr)
             (key-place (inc column) row web-post-bl)
             (key-place (inc column) (inc row) web-post-tl))))
          ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;     Thumbs with Updated Params & Placement                         ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Explored settings: 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Thumb: test-column-space 0, test-row-space -5, rollin 30 degrees, tilt +- 40 degress, 
;; Main: α 36 deg, β 6 deg, tilt-pivotrow 1.5, tent-pivotcol 5, tenting-angle (/ π 12), 
;; extra-width 2.5, extra-height 0.5, keyboard-z-offset 0, wall-z-offset -15
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Thumb: test-column-space 0, test-row-space -5, rollin 30 degrees, tilt +- 40 degress, 
;; Main: α 36 deg, β 6 deg, tilt-pivotrow (- nrows 3), tent-pivotcol 5, tenting-angle (/ π 12), 
;; extra-width 2.5, extra-height 1.0, keyboard-z-offset 2, wall-z-offset -15
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Thumb: test-column-space (/ 1.65 +-2), test-row-space -4|-6|-8, rollin 45 degrees, tilt +- 45 degress, 
;; Main: α (/ π 5), β (/ π 30), tilt-pivotrow (- nrows 3), tent-pivotcol 5, tenting-angle (/ π 12), 
;; extra-width 2.5, extra-height 1.0, keyboard-z-offset 2, wall-z-offset -15
;; 

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parameters for test thumb ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def test-column-space 0)  ; like extra-width (2.5); to spec when flat is 1.65
(def test-row-space -5 )   ; like extra-height (1.0)to spec when flat is 1.65;  at one point, 3.5 seemed good.
(def rollin-default (deg2rad 30) )    ; we want to do radians since java Math trig functions take in radian values.
(def rollin-top (deg2rad 30) )
(def tilt-top (deg2rad 40) )
(def tilt-default (deg2rad 0) )            ; tilt settings are also in radians
(def tilt-last (deg2rad -40))   ; TODO: parameterize to allow deciding what angle the bottom section ends at.
(def thumb-tent (* 1 (+ tenting-angle (* β (- tent-pivotcol 1)) )))
(def slope-thumb (deg2rad -60))
(def deflect (deg2rad -90))  ; (/ π -3)
(def half-width (/ mount-width 2))
(def larger-plate-height (/ (+ sa-double-length keyswitch-height) 2) )
(def base-offset (+ half-width test-column-space) )     ; original was 14 or 15
(def row-offset (+ mount-height test-row-space) )
(def deflect-fudge [0 0 0])  ; previoiusly: (def deflect-fudge [-6 7 4])
(def thumb-offsets [(* -4 half-width) (* -1.0 mount-height) (* -2.5 mount-height)])            ; original [6 -3 7], [20 -3 7]
(def thumborigin
  (map + (key-position 0 lastrow [(* -0 mount-width) (* -0 mount-height) (* 0 mount-height)])  ; [(* -2 mount-width) (/ mount-height -20) (/ mount-height -2)]
       thumb-offsets))  ; original: (map + (key-position 1 cornerrow thumb-offsets)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn larger-plate [orientation]
  (let [plate-height (/ (- sa-double-length mount-height) 3)
        top-plate (->> (cube mount-width plate-height web-thickness)
                       (translate [0 (/ (+ plate-height mount-height) 2)
                                   (- plate-thickness (/ web-thickness 2))]))
        rotated (if (= orientation 1) 0 (/ π 2))
        ]
    (rotate rotated [0 0  1] (union top-plate (mirror [0 1 0] top-plate)))
    ))
(defn coord-y [plate ra rb] (* (/ plate 2) (+ (Math/sin ra) (Math/sin rb))) )
(defn coord-x [plate ra rb] (* (/ plate 2) (+ (Math/cos ra) (Math/cos rb))) )
(def key-ttl-height (+ key-base-lift key-depth))
(def upper-plate-hyp (Math/sqrt (+ (Math/pow (+ base-offset 0) 2) (Math/pow (/ larger-plate-height 2) 2)))) 
(defn deflect-offset [angle] (map + deflect-fudge [(* upper-plate-hyp (Math/cos angle)) (* upper-plate-hyp (Math/sin angle)) 0]))
(def x-point (- 0 (/ keyswitch-width 2)))
(def y-point key-ttl-height)
(defn displacement-edge [angle]
  (- (* x-point (Math/cos angle)) (* y-point (Math/sin angle)) x-point )  ; older: (- (* half-width (Math/cos (mod rollin (* 2 π))) (* key-ttl-height (Math/sin (mod rollin (* 2 π)))) half-width ))
)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Thumb Placement Functions          ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Used for the buttons, but also     ;;;
;;; for the wall and inter-connections ;;; 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn thumb-tl-place [shape]
  (def rollin rollin-top)
  (def tilt tilt-top)
  (->> shape
       (rotate rollin [0 1 0])
       (rotate tilt [1 0 0])
       (translate (map * [1 1 1] [(displacement-edge rollin) 0 0]))    ; space out accounting for rollin
       (translate (map * [-1 1 1] [base-offset row-offset 0]))
       (translate (map * [1 1 1] [0 (* 1 key-ttl-height (Math/sin tilt)) (* 1 key-ttl-height (Math/cos tilt))]))  ; space out accounting for tilt
       ; Fit to Keyboard settings. 
       (rotate (deg2rad 90) [0 1 0])
       (rotate (deg2rad 90) [0 0 -1])
       (rotate slope-thumb [1 0 0])
       (rotate thumb-tent [0 1 0])
      ;  (rotate deflect [0 0 1])
      ;  (translate (map * [-1 1 1] (deflect-offset deflect)))
       (translate thumborigin)
       ))
(defn thumb-tr-place [shape]
  (def rollin rollin-top)
  (def tilt tilt-top)
  (->> shape
       (rotate rollin [0 -1 0])
       (rotate tilt [1 0 0])
       (translate (map * [-1 1 1] [(displacement-edge rollin) 0 0]))   ; space out accounting for rollin
       (translate (map * [1 1 1] [base-offset row-offset 0]))
       (translate (map * [1 1 1] [0 (* 1 key-ttl-height (Math/sin tilt)) (* 1 key-ttl-height (Math/cos tilt))])) ; space out accounting for tilt
       ; Fit to Keyboard settings. 
       (rotate (deg2rad 90) [0 1 0])
       (rotate (deg2rad 90) [0 0 -1])
       (rotate slope-thumb [1 0 0])
       (rotate thumb-tent [0 1 0])
      ;  (rotate deflect [0 0 1])
      ;  (translate (map * [-1 1 1] (deflect-offset deflect)))
       (translate thumborigin)
  ))
(defn thumb-ml-place [shape]
  (def rollin rollin-default)
  (def tilt tilt-default)
  (->> shape
       (rotate rollin [0 1 0])
       (rotate tilt [1 0 0])
       (translate (map * [1 1 1] [(displacement-edge rollin) 0 0]))    ; space out accounting for rollin
       (translate (map * [-1 1 1] [base-offset 0 0]))
       ; Fit to Keyboard settings. 
       (rotate (deg2rad 90) [0 1 0])
       (rotate (deg2rad 90) [0 0 -1])
       (rotate slope-thumb [1 0 0])
       (rotate thumb-tent [0 1 0])
      ;  (rotate deflect [0 0 1])
      ;  (translate (map * [-1 1 1] (deflect-offset deflect)))
       (translate thumborigin)
  ))
(defn thumb-mr-place [shape]
  (def rollin rollin-default)
  (def tilt tilt-default)
  (->> shape
       (rotate rollin [0 -1 0])
       (rotate tilt [1 0 0])
       (translate (map * [-1 1 1] [(displacement-edge rollin) 0 0]))   ; space out accounting for rollin
       (translate (map * [1 1 1] [base-offset 0 0]))
       ; Fit to Keyboard settings. 
       (rotate (deg2rad 90) [0 1 0])
       (rotate (deg2rad 90) [0 0 -1])
       (rotate slope-thumb [1 0 0])
       (rotate thumb-tent [0 1 0])
      ;  (rotate deflect [0 0 1])
      ;  (translate (map * [-1 1 1] (deflect-offset deflect)))
       (translate thumborigin)
  ))
(defn thumb-bl-place [shape]
  (def rollin rollin-default)
  (def tilt tilt-last)
  (->> shape
       (rotate rollin [0 1 0])
       (rotate tilt [1 0 0])
       (translate (map * [1 1 1] [(displacement-edge rollin) 0 0]))    ; space out accounting for rollin
       (translate (map * [-1 -1 1] [base-offset row-offset 0]))
       (translate (map * [1 1 1] [0 (* 1 key-ttl-height (Math/sin tilt)) (* 1 key-ttl-height (Math/cos tilt))]))  ; space out accounting for tilt
       ; Fit to Keyboard settings. 
       (rotate (deg2rad 90) [0 1 0])
       (rotate (deg2rad 90) [0 0 -1])
       (rotate slope-thumb [1 0 0])
       (rotate thumb-tent [0 1 0])
      ;  (rotate deflect [0 0 1])
      ;  (translate (map * [-1 1 1] (deflect-offset deflect)))
       (translate thumborigin)
  ))
(rotate (map + [slope-thumb thumb-tent 0] [0 (deg2rad 90) (* -1 (deg2rad 90))])  )
(defn thumb-br-place [shape]
  (def rollin rollin-default)
  (def tilt tilt-last)
  (->> shape
       (rotate rollin [0 -1 0])
       (rotate tilt [1 0 0])
       (translate (map * [-1 1 1] [(displacement-edge rollin) 0 0]))   ; space out accounting for rollin
       (translate (map * [1 -1 1] [base-offset row-offset 0]))
       (translate (map * [1 1 1] [0 (* 1 key-ttl-height (Math/sin tilt)) (* 1 key-ttl-height (Math/cos tilt))]))  ; space out accounting for tilt
       ; Fit to Keyboard settings. 
       (rotate (deg2rad 90) [0 1 0])
       (rotate (deg2rad 90) [0 0 -1])
       (rotate slope-thumb [1 0 0])
       (rotate thumb-tent [0 1 0])
      ;  (rotate deflect [0 0 1])
      ;  (translate (map * [-1 1 1] (deflect-offset deflect)))
       (translate thumborigin)
       ))

(defn thumb-lower-layout [shape]
  (union
   (thumb-ml-place shape)
   (thumb-mr-place shape)
   (thumb-bl-place shape)
   (thumb-br-place shape)
   ))

(defn thumb-upper-layout [shape]
  (union
   (thumb-tl-place shape)
   (thumb-tr-place shape) 
   ))
(def thumbcaps
  (union
   (thumb-lower-layout (sa-cap 1))
   (thumb-upper-layout (sa-cap 1))  ; (thumb-upper-layout (rotate (/ π 2) [0 0 1] (sa-cap 1.25)))  ; 
   )) 
(def thumb
  (union
   (thumb-lower-layout single-plate)
   (thumb-upper-layout single-plate)
  ;  (thumb-upper-layout (larger-plate 1))
   ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; These define edges when we use the bigger key plate ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Use if thumb-upper-layout is only unsing 1u caps. 
(def thumb-post-tr web-post-tr)
(def thumb-post-tl web-post-tl)
(def thumb-post-bl web-post-bl)
(def thumb-post-br web-post-br)

; Original, if using large plate for buttons tl & tr
; (def thumb-post-tr (translate [(- (/ mount-width 2) post-adj)  (- (/ mount-height  1.15) post-adj) 0] web-post))
; (def thumb-post-tl (translate [(+ (/ mount-width -2) post-adj) (- (/ mount-height  1.15) post-adj) 0] web-post))
; (def thumb-post-bl (translate [(+ (/ mount-width -2) post-adj) (+ (/ mount-height -1.15) post-adj) 0] web-post))
; (def thumb-post-br (translate [(- (/ mount-width 2) post-adj)  (+ (/ mount-height -1.15) post-adj) 0] web-post))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; We want to fill in the gaps between the thumb keys ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def thumb-connectors
  (union
   (triangle-hulls    ; top two column gap
    (thumb-tl-place thumb-post-tr)
    (thumb-tr-place thumb-post-tl)
    (thumb-tl-place thumb-post-br)
    (thumb-tr-place thumb-post-bl))
   (triangle-hulls    ; Column gaps of the bottom four
    (thumb-br-place web-post-bl)  
    (thumb-bl-place web-post-br)  
    (thumb-br-place web-post-tl)  
    (thumb-bl-place web-post-tr)  
    (thumb-mr-place web-post-bl)  
    (thumb-ml-place web-post-br)  
    (thumb-mr-place web-post-tl)   
    (thumb-ml-place web-post-tr)   
    )
   (triangle-hulls  ; Column gap within the row gap of top to middle 
    (thumb-mr-place web-post-tl)
    (thumb-ml-place web-post-tr)
    (thumb-tr-place thumb-post-bl)
    (thumb-tl-place thumb-post-br)
    )
   (triangle-hulls  ; row gap on left, between top and middle
    (thumb-tl-place thumb-post-br)
    (thumb-ml-place web-post-tr)
    (thumb-tl-place thumb-post-bl)
    (thumb-ml-place web-post-tl)
    )
   (triangle-hulls    ; row gap on right, between top and middle
    (thumb-tr-place thumb-post-bl)
    (thumb-mr-place web-post-tl)
    (thumb-tr-place thumb-post-br)
    (thumb-mr-place web-post-tr)
    )
   (triangle-hulls  ; Row gap on left, between middle and bottom 
    (thumb-ml-place web-post-bl)
    (thumb-bl-place web-post-tl)
    (thumb-ml-place web-post-br)
    (thumb-bl-place web-post-tr))
   (triangle-hulls  ; Row gap on right, between middle and bottom
    (thumb-mr-place web-post-br)
    (thumb-br-place web-post-tr)
    (thumb-mr-place web-post-bl)
    (thumb-br-place web-post-tl))
   ))

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
  (map - (key-position 0 row [(* mount-width -0.5) (* direction mount-height 0.5) 0]) [left-wall-x-offset 0 left-wall-z-offset]) )

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

(def thumb-walls-normal-align 
        (union
         (wall-brace thumb-bl-place -1 0 web-post-bl thumb-bl-place -1 1 web-post-tl)  ; outside left lower wall
         (wall-brace thumb-ml-place -1 1 web-post-bl thumb-bl-place -1 0 web-post-tl)  ; outside left wall between lower & middle
                      ; (wall-brace thumb-bl-place -1 1 web-post-tl thumb-ml-place -1 0 web-post-bl)  ; outside left wall between lower & middle
         (wall-brace thumb-ml-place -1 0 web-post-bl thumb-ml-place -1 -1 web-post-tl)  ; outside left middle wall
                      ; (wall-brace thumb-ml-place -1 1 web-post-tl thumb-tl-place -1 0 thumb-post-bl)  ; outside left between middle and top
                      ; (wall-brace thumb-tl-place -1 0 thumb-post-bl thumb-tl-place -1 0 thumb-post-tl)  ; outside left upper wall
                      ; (wall-brace thumb-br-place  0 -1 web-post-tr thumb-br-place  0 -1 web-post-br)  ; outside of lower right
                      ; (wall-brace thumb-mr-place  0 -1 web-post-tr thumb-br-place  0 -1 web-post-tr)  ; outside right middle wall
                      ; (wall-brace thumb-mr-place  0  -1 web-post-tr thumb-tr-place  0 -1 thumb-post-br)  ; right wall between middle and top thumbs
                      ; (wall-brace thumb-tr-place  0  -1 thumb-post-tr thumb-tr-place  0 -1 thumb-post-br)  ; right wall under top row (when not rotated)
                      ; (wall-brace thumb-bl-place -1  0 web-post-br thumb-br-place -1  0 web-post-bl)  ; center middle to floor
                      ; (wall-brace thumb-bl-place -1  -1 web-post-br thumb-bl-place -1 -1 web-post-bl)  ; left lower to floor
                      ; (wall-brace thumb-br-place  0  -1 web-post-br thumb-br-place  0 -1 web-post-bl)  ; right lower to floor
                      ; (wall-brace thumb-br-place -1  0 web-post-bl thumb-br-place  0 -1 web-post-bl)  ; inside of lower right thumb to floor 
         (wall-brace thumb-tr-place  0 -1 thumb-post-tr (partial key-place 3 lastrow)  0 -1 web-post-bl)  ; When lower rotated from upper - Connect back right corner to keys
                      ; (wall-brace thumb-tr-place  0 -1 thumb-post-tr (partial key-place 3 lastrow)  0 -1 web-post-bl)  ; When lower & upper is normal aligned. - Connect back right corner to keys
                      ;  clunky bit on the top left thumb connection  (normal connectors don't work well)
         (bottom-hull  ; wall connection of bottom left keys to thumb left-side section. 
          (left-key-place cornerrow -1 (translate (wall-locate2 -1 0) web-post))
          (left-key-place cornerrow -1 (translate (wall-locate3 -1 0) web-post))
          (thumb-ml-place thumb-post-tl)
          (thumb-tl-place thumb-post-bl)
          (left-key-place cornerrow -1 (translate (wall-locate1 -1 0) web-post)))
         (triangle-hulls
          (left-key-place cornerrow -1 (translate (wall-locate1 -1 0) web-post))
          (thumb-tl-place thumb-post-bl)
          (key-place 0 cornerrow web-post-bl)
          (thumb-tl-place thumb-post-tl)
          (key-place 0 cornerrow web-post-br)
          (key-place 1 cornerrow web-post-bl))
         (triangle-hulls
          (thumb-tl-place thumb-post-tl)
          (key-place 1 cornerrow web-post-bl)
          (thumb-tl-place thumb-post-tr))
         (triangle-hulls
          (key-place 1 cornerrow web-post-br)
          (thumb-tr-place thumb-post-tl)
          (key-place 2 lastrow web-post-bl)
          (key-place 2 lastrow web-post-br))
         (thumb-tl-place thumb-post-tl)
         (thumb-tl-place thumb-post-tr)
         (key-place 1 cornerrow web-post-br)
                        ; (left-key-place cornerrow -1 web-post)    
         (triangle-hulls  ; left of thumb valley
          (thumb-ml-place web-post-tl)
          (thumb-ml-place (translate (wall-locate2 -1.2 0) web-post-tl))
          (thumb-tl-place thumb-post-bl)
          (thumb-tl-place (translate (wall-locate2 -1.2 0) thumb-post-bl))
          (thumb-tl-place thumb-post-tl))
         (triangle-hulls  ; left of thumb valley to first lastrow (middle finger)
          (left-key-place cornerrow -1 (translate (wall-locate3 -1 0) web-post))
          (thumb-tl-place (translate (wall-locate2 -1.2 0) thumb-post-bl))
          (left-key-place cornerrow -1 (translate (wall-locate1 -1 0) web-post))
          (key-place 0 cornerrow (translate (wall-locate1 -1 0) web-post-bl))
          (key-place 0 cornerrow web-post-bl)
          (thumb-tl-place (translate (wall-locate2 -1.2 0) thumb-post-bl))
          (key-place 0 cornerrow web-post-br)
          (thumb-tl-place thumb-post-tl)
          (key-place 1 cornerrow web-post-bl)
          (thumb-tl-place thumb-post-tr)
          (key-place 1 cornerrow web-post-br)
          (thumb-tr-place thumb-post-tl)
          (key-place 2 lastrow web-post-tl)
          (key-place 2 lastrow (translate (wall-locate3 0 -1) web-post-bl))
          (key-place 2 lastrow web-post-bl))
         (hull  ; extend base of row 2 (middle finger)
          (key-place 2 lastrow web-post-bl)
          (key-place 2 lastrow (translate (wall-locate1 0 -1) web-post-bl))
          (key-place 2 lastrow (translate (wall-locate2 0 -1) web-post-bl))
          (key-place 2 lastrow (translate (wall-locate3 0 -1) web-post-bl))
                       ; (key-place 3 lastrow web-post-bl)
          (key-place 2 lastrow (translate (wall-locate3 -1.25 -1) web-post-br))
          (key-place 2 lastrow (translate (wall-locate2 -1.25 -1) web-post-br))
          (key-place 2 lastrow (translate (wall-locate1 0 -1) web-post-br))
          (key-place 2 lastrow web-post-br))
         (hull ; gap fill behind middle finger
          (key-place 2 lastrow (translate (wall-locate3 0 -1) web-post-bl))
          (key-place 2 lastrow (translate (wall-locate3 -1.25 -1) web-post-br))
          (thumb-tr-place thumb-post-tl)
          (thumb-tr-place thumb-post-tr))
         (hull
          (left-key-place cornerrow -1 (translate (wall-locate2 -1 0) web-post))
          (left-key-place cornerrow -1 (translate (wall-locate3 -1 0) web-post))
          (thumb-tl-place thumb-post-bl))
         (hull  ; outside edge of main connecting to top of top thumb row
          (left-key-place cornerrow -1 web-post)
          (left-key-place cornerrow -1 (translate (wall-locate1 -1 0) web-post))
          (left-key-place cornerrow -1 (translate (wall-locate2 -1 0) web-post))
          (left-key-place cornerrow -1 (translate (wall-locate3 -1 0) web-post))
          (thumb-tl-place thumb-post-tl))
         (hull  ; outside edge of main (upper part) to top of top thumb row
          (left-key-place cornerrow -1 web-post)
          (left-key-place cornerrow -1 (translate (wall-locate1 -1 0) web-post))
          (key-place 0 cornerrow web-post-bl)
          (key-place 0 cornerrow (translate (wall-locate1 -1 0) web-post-bl))
          (thumb-tl-place thumb-post-tl))
         )  ; end union for normal-align
  )
(def thumb-walls-early 
  (union
   (wall-brace thumb-bl-place -1 0 web-post-bl thumb-bl-place -1 1 web-post-tl)  ; outside left lower wall
   (wall-brace thumb-ml-place -1 1 web-post-bl thumb-bl-place -1 0 web-post-tl)  ; outside left wall between lower & middle
                      ; (wall-brace thumb-bl-place -1 1 web-post-tl thumb-ml-place -1 0 web-post-bl)  ; outside left wall between lower & middle
   (wall-brace thumb-ml-place -1 0 web-post-bl thumb-ml-place -1 -1 web-post-tl)  ; outside left middle wall
                      ; (wall-brace thumb-ml-place -1 1 web-post-tl thumb-tl-place -1 0 thumb-post-bl)  ; outside left between middle and top
                      ; (wall-brace thumb-tl-place -1 0 thumb-post-bl thumb-tl-place -1 0 thumb-post-tl)  ; outside left upper wall
                      ; (wall-brace thumb-br-place  0 -1 web-post-tr thumb-br-place  0 -1 web-post-br)  ; outside of lower right
                      ; (wall-brace thumb-mr-place  0 -1 web-post-tr thumb-br-place  0 -1 web-post-tr)  ; outside right middle wall
                      ; (wall-brace thumb-mr-place  0  -1 web-post-tr thumb-tr-place  0 -1 thumb-post-br)  ; right wall between middle and top thumbs
                      ; (wall-brace thumb-tr-place  0  -1 thumb-post-tr thumb-tr-place  0 -1 thumb-post-br)  ; right wall under top row (when not rotated)
                      ; (wall-brace thumb-bl-place -1  0 web-post-br thumb-br-place -1  0 web-post-bl)  ; center middle to floor
                      ; (wall-brace thumb-bl-place -1  -1 web-post-br thumb-bl-place -1 -1 web-post-bl)  ; left lower to floor
                      ; (wall-brace thumb-br-place  0  -1 web-post-br thumb-br-place  0 -1 web-post-bl)  ; right lower to floor
                      ; (wall-brace thumb-br-place -1  0 web-post-bl thumb-br-place  0 -1 web-post-bl)  ; inside of lower right thumb to floor 
   (wall-brace thumb-tr-place  0 -1 thumb-post-tr (partial key-place 3 lastrow)  0 -1 web-post-bl)  ; When lower rotated from upper - Connect back right corner to keys
                      ;  clunky bit on the top left thumb connection  (normal connectors don't work well)
   (bottom-hull  ; wall connection of bottom left keys to thumb left-side section. 
    (left-key-place cornerrow -1 (translate (wall-locate2 -1 0) web-post))
    (left-key-place cornerrow -1 (translate (wall-locate3 -1 0) web-post))
    (thumb-ml-place thumb-post-tl)
    (thumb-tl-place thumb-post-bl)
    (left-key-place cornerrow -1 (translate (wall-locate1 -1 0) web-post)))
   (triangle-hulls
    (left-key-place cornerrow -1 (translate (wall-locate1 -1 0) web-post))
    (thumb-tl-place thumb-post-bl)
    (key-place 0 cornerrow web-post-bl)
    (thumb-tl-place thumb-post-tl)
    (key-place 0 cornerrow web-post-br)
    (key-place 1 cornerrow web-post-bl))
   (triangle-hulls
    (thumb-tl-place thumb-post-tl)
    (key-place 1 cornerrow web-post-bl)
    (thumb-tl-place thumb-post-tr))
   (hull  ; When lower is rotated from upper - connect main to above ml key 
    (left-key-place cornerrow -1 (translate (wall-locate2 -1 0) web-post))
    (left-key-place cornerrow -1 (translate (wall-locate3 -1 0) web-post))
    (thumb-ml-place web-post-tl)
    (thumb-ml-place (translate (wall-locate2 -1.2 0) web-post-tl))
    (thumb-ml-place (translate (wall-locate3 -1.2 0) web-post-tl))
    (thumb-tl-place thumb-post-tl))
   (hull  ; outside edge of main connecting to top of top thumb row
    (left-key-place cornerrow -1 web-post)
    (left-key-place cornerrow -1 (translate (wall-locate1 -1 0) web-post))
    (left-key-place cornerrow -1 (translate (wall-locate2 -1 0) web-post))
    (left-key-place cornerrow -1 (translate (wall-locate3 -1 0) web-post))
    (thumb-tl-place thumb-post-tl))
   (hull  ; outside edge of main (upper part) to top of top thumb row
    (left-key-place cornerrow -1 web-post)
    (left-key-place cornerrow -1 (translate (wall-locate1 -1 0) web-post))
    (key-place 0 cornerrow web-post-bl)
    (key-place 0 cornerrow (translate (wall-locate1 -1 0) web-post-bl))
    (thumb-tl-place thumb-post-tl))
   (hull  ; original, currently not needed: under the tl thumb key
    (thumb-ml-place web-post-tr)
    (thumb-ml-place (translate (wall-locate1 -0.3 1) web-post-tr))
    (thumb-ml-place (translate (wall-locate2 -0.3 1) web-post-tr))
    (thumb-ml-place (translate (wall-locate3 -0.3 1) web-post-tr))
    (thumb-tl-place thumb-post-tl))
   )
  )
(def extra-key-top-gap
  (union 
   (hull  ; main keys - first extra key (column 2) top wall [only if gap to second extra key (column 3)]
    (key-place 2 lastrow (translate (wall-locate1 0 0) web-post-bl))
    (key-place 2 lastrow (translate (wall-locate3 0 0) web-post-bl))
    (key-place 2 lastrow (translate (wall-locate2 0 0) web-post-bl))
                  ; (key-place 2 lastrow web-post-br)
    (key-place 2 lastrow (translate (wall-locate1 0 0) web-post-br))
                ;  (key-place 2 lastrow (translate (wall-locate3 0 0) web-post-br))
                ;  (key-place 2 lastrow (translate (wall-locate2 0 0) web-post-br))
    (key-place 3 lastrow (translate (wall-locate3 0 -1) web-post-bl))

                  ; Then it connext to what? 
    )
   (hull  ; main keys, first extra key (column 2) right wall
    (key-place 2 lastrow web-post-tr)
    (key-place 2 lastrow (translate (wall-locate3 0 0) web-post-tr))
    (key-place 2 lastrow (translate (wall-locate2 0 0) web-post-tr))
    (key-place 2 lastrow web-post-br)
    (key-place 2 lastrow (translate (wall-locate3 0 0) web-post-br))
    (key-place 2 lastrow (translate (wall-locate2 0 0) web-post-br))
                 ; Then it connects to what? 
                ;  (thumb-tl-place (translate (wall-locate1 0 0) thumb-post-tl))
    )
   (hull  ; main keys, second extra key (column 3) left wall
    (key-place 3 lastrow web-post-tl)
    (key-place 3 lastrow (translate (wall-locate3 0 0) web-post-tl))
    (key-place 3 lastrow (translate (wall-locate2 0 0) web-post-tl))
    (key-place 3 lastrow web-post-bl)
    (key-place 3 lastrow (translate (wall-locate3 0 -1) web-post-bl))
    (key-place 3 lastrow (translate (wall-locate2 0 -1) web-post-bl))
                 ; Then it connects to what? 
                ;  (thumb-tl-place (translate (wall-locate1 0 0) thumb-post-tl))
   )
   (triangle-hulls  ;; Extra keys (column 2 & 3) column gap
       (key-place 2 lastrow web-post-br)
       (key-place 2 lastrow web-post-tr)
       (key-place 3 lastrow web-post-bl)
       (key-place 3 lastrow web-post-tl)
   )
))  ;; end extra-key-top-gap
(def tight-column-cleanup  ;; coumn gap & top walls for 1st & 2nd extra keys (columns 2 & 3)
  (union
   (triangle-hulls  ;; Extra keys (column 2 & 3) column gap
    (key-place 2 lastrow web-post-br)
    (key-place 3 lastrow (translate (wall-locate1 -0.6 0) web-post-bl))
    (key-place 2 lastrow web-post-tr)
    (key-place 3 lastrow (translate (wall-locate1 -0.6 0) web-post-tl))
    (key-place 3 lastrow web-post-tl)
    (key-place 3 lastrow (translate (wall-locate1 -0.6 0) web-post-bl))
    (key-place 3 lastrow web-post-bl))
   (hull  ; main keys top wall - first extra key (column 2) top wall sloped to second extra key (column 3) top wall
    (key-place 2 lastrow web-post-bl)
    (key-place 2 lastrow (translate (wall-locate1  0    0.3) web-post-bl))
    (key-place 2 lastrow (translate (wall-locate2  0    0.3) web-post-bl))
    (key-place 2 lastrow (translate (wall-locate3  0    0  ) web-post-bl))
    ; special case, not connecting to web-post-bl
    (key-place 3 lastrow (translate (wall-locate1 -0.6  0  ) web-post-bl))
    (key-place 3 lastrow (translate (wall-locate1 -0.6  0.3) web-post-bl))
    (key-place 3 lastrow (translate (wall-locate2  0    0.3) web-post-bl))
    (key-place 3 lastrow (translate (wall-locate3  0    0) web-post-bl)))
   (hull  ;; main key top wall - gap fixer for transition between 1st & 2nd extra keys (columns 2 & 3) 
    (key-place 2 lastrow web-post-bl)
    (key-place 2 lastrow (translate (wall-locate1  0    0.3) web-post-bl))
    (key-place 2 lastrow web-post-br)
    (key-place 2 lastrow (translate (wall-locate1  0    0.3) web-post-br))
    (key-place 3 lastrow (translate (wall-locate1 -0.6  0  ) web-post-bl))
    (key-place 3 lastrow (translate (wall-locate1 -0.6  0.3) web-post-bl)))
   (hull  ; main keys top wall - second extra key (column 3)
    (key-place 3 lastrow web-post-bl)
    (key-place 3 lastrow (translate (wall-locate1 -0.6  0) web-post-bl))  ; special for tight columns
    (key-place 3 lastrow (translate (wall-locate1 -0.6  0.3) web-post-bl))
    (key-place 3 lastrow (translate (wall-locate2  0    0.3) web-post-bl))
    (key-place 3 lastrow (translate (wall-locate3  0    0  ) web-post-bl))
    (key-place 3 lastrow web-post-br)
    (key-place 3 lastrow (translate (wall-locate1  0    0.3) web-post-br))
    (key-place 3 lastrow (translate (wall-locate2  0    0.3) web-post-br))
    (key-place 3 lastrow (translate (wall-locate3  0    0  ) web-post-br))
                  ; Then it connext to what? 
    )
   (triangle-hulls  ; First extra key (column 2) front wall to thumb corner 
    (thumb-tl-place thumb-post-tl)
    (key-place 2 lastrow (translate (wall-locate3 0 0) web-post-bl))
    (key-place 3 lastrow (translate (wall-locate3 0 0) web-post-bl))
    )
  ) ; end union 
)  ;; end tight-column-cleanup
(defn connect-adjacent [a b] 
  " Usually used to connect the 'extra' keys of the lastrow to 
    the corner of the adjent column "
  
  )
(defn key-wall [row side]
  " Usually used to create a left or right wall for the 'extra' 
    keys of the lastrow (that aren't connected when generated)"
  (let [p1 (if (= side 'left') 0 -0.3)
        p2 (if (= side 'left') 0.3 0)
        top-post (if (= side 'left') web-post-tl web-post-tr)
        bottom-post (if (= side 'left') web-post-bl web-post-br)
        ]
    (hull
     (key-place row lastrow top-post)
     (key-place row lastrow   (translate (wall-locate1 p1 p2) top-post))
     (key-place row lastrow   (translate (wall-locate2 p1 p2) top-post))
     (key-place row lastrow   (translate (wall-locate3 0  0 ) top-post))
     (key-place row lastrow bottom-post)
     (key-place row lastrow   (translate (wall-locate1 p1 p2) bottom-post))
     (key-place row lastrow   (translate (wall-locate2 p1 p2) bottom-post))
     (key-place row lastrow   (translate (wall-locate3 0  0 ) bottom-post))
  ; End key-wall 
)))

(def main-key-cleanup 
  (union
   (triangle-hulls  ;; Extra keys (column 2 & 3) row gap
    (key-place 2 cornerrow web-post-bl)
    (key-place 2 lastrow web-post-tl)
    (key-place 2 cornerrow web-post-br)
    (key-place 2 lastrow web-post-tr)
    (key-place 3 cornerrow web-post-bl)
    (key-place 3 lastrow web-post-tl)
    (key-place 3 cornerrow web-post-br)
    (key-place 3 lastrow web-post-tr))
   (hull  ;; First extra key (column 2) connect to adjacent key
    (key-place 2 cornerrow web-post-bl)
    (key-place 2 cornerrow (translate (wall-locate1 0  0) web-post-bl))
    (key-place 2 lastrow web-post-tl)
    (key-place 2 lastrow   (translate (wall-locate1 0  0.3) web-post-tl))
    (key-place 1 cornerrow web-post-br)
    (key-place 1 cornerrow (translate (wall-locate1 0 -0.5) web-post-br)))
   (hull  ; First extra key (column 2) left wall
    (key-wall 2 'left')
    ; Then it connects to what?
    (key-place 1 cornerrow web-post-br)
    (key-place 1 cornerrow (translate (wall-locate1 0   -0.5) web-post-br))
    (thumb-tl-place thumb-post-tl)
    ; (thumb-tl-place (translate (wall-locate1 0.3 0.3) thumb-post-tl))  ; ?? check this
    )
   (hull  ;; Second extra key (column 3) connect to adjacent key
    (key-place 3 cornerrow web-post-br)
    (key-place 3 cornerrow (translate (wall-locate1  0    0  ) web-post-br))
    (key-place 3 lastrow web-post-tr)
    (key-place 3 lastrow   (translate (wall-locate1 -0.3  0  ) web-post-tr))
    (key-place 4 cornerrow web-post-bl)
    (key-place 4 cornerrow (translate (wall-locate1  0   -0.5) web-post-bl))
    ; (key-place 3 lastrow web-post-br)
    )
   (hull  ; Second extra key (column 3) right wall
      (key-wall 3 'right')    
    ;              ; Then it connects to what? 
    (key-place 4 cornerrow web-post-bl)
    (key-place 4 cornerrow (translate (wall-locate1  0 -0.5) web-post-bl))
    (key-place 4 cornerrow (translate (wall-locate2 -0.3 -1) web-post-bl))
    (key-place 4 cornerrow (translate (wall-locate3  0   -1) web-post-bl)) ; connects to default front wall of key-place 4 cornerrow
    ; ; temp hidden 
    ; ; working 
    )
   (hull ; Front wall for some of 1st, but most 2nd extra keys (columns 2 & 3).
    (key-place 3 lastrow   (translate (wall-locate3 0 0) web-post-bl))
    (thumb-tl-place thumb-post-tl)
    (key-place 3 lastrow   (translate (wall-locate3 0 0) web-post-br))
    (key-place 4 cornerrow (translate (wall-locate3 0 -1) web-post-bl)) ; connects to default front wall of key-place 4 cornerrow
    )
   tight-column-cleanup  ; column gap & top walls for 1st & 2nd extra keys (columns 2 & 3)
   ;; if there is a gap between first & second extra keys (column 2 & 3) then: 
  ;  extra-key-top-gap
   (bottom-hull  ; front wall of extra keys and final main section. 
    (thumb-tl-place thumb-post-tl)
    (key-place 4 cornerrow (translate (wall-locate3 0 -1) web-post-bl))
    )
   ; end of main-key-cleanup
   ))
(def thumb-valley
  (union
   (hull  ;; main keys top wall - first two key columns, connecting to thumb section
    (key-place 0 cornerrow web-post-bl)
    (key-place 0 cornerrow (translate (wall-locate1 0 -0.5) web-post-bl))
    (key-place 0 cornerrow (translate (wall-locate2 0 -1) web-post-bl))
    (key-place 0 cornerrow (translate (wall-locate3 0 -1) web-post-bl))
    (key-place 0 cornerrow web-post-br)
    (key-place 1 cornerrow web-post-bl)
    (key-place 1 cornerrow web-post-br)
    (key-place 1 cornerrow (translate (wall-locate1 0 -0.5) web-post-br))
    (key-place 1 cornerrow (translate (wall-locate2 0 -1) web-post-br))
    (key-place 1 cornerrow (translate (wall-locate3 0 -1) web-post-br))
                  ; Then it connext to what? 
    (thumb-bl-place (translate (wall-locate1 0 -0.5) web-post-bl))
    (thumb-bl-place web-post-bl)
    (thumb-tl-place web-post-tl)
    ; connects to internal back wall of thumb section    
    )
   (hull  ;; thumb-hood top wall
    (thumb-bl-place (translate (wall-locate2 0 -1) web-post-bl))
    (thumb-bl-place (translate (wall-locate3 0 -1) web-post-bl))
    (thumb-bl-place (translate (wall-locate1 0 -0.5) web-post-bl))
    (thumb-bl-place web-post-bl)
    (key-place 0 cornerrow web-post-bl)
    (key-place 0 cornerrow (translate (wall-locate1 0 -0.5) web-post-bl))
    ; connects other thumb-hood sections and main keys top wall
    )
  ;  (hull  ;; thumb-hood top wall as a block: small section of thumb bl-bl to bl-bl of main. 
  ;   (key-place 0 cornerrow web-post-tl)
  ;   (key-place 0 cornerrow (translate (wall-locate1 0 0) web-post-tl))
  ;   (key-place 0 cornerrow web-post-bl)
  ;   (key-place 0 cornerrow (translate (wall-locate1 0 -0.5) web-post-bl))
  ;   (thumb-bl-place web-post-bl)
  ;   (thumb-bl-place (translate (wall-locate1 0 -0.5) web-post-bl))
  ;   (thumb-bl-place (translate (wall-locate3 0 -1) web-post-bl))
  ;   (thumb-bl-place (translate (wall-locate2 0 -1) web-post-bl))
  ;   (key-place 0 cornerrow (translate (wall-locate3 0 -1) web-post-bl))
  ;   ; Do we really want to fill in so much? 
  ;   )
   (hull  ;; Internal back wall of thumb section, connecting to main section top wall of first 2 columns 
    (thumb-ml-place web-post-bl)
    (thumb-ml-place web-post-tl)
    (thumb-bl-place web-post-bl)
    (thumb-tl-place thumb-post-tl)
    (thumb-ml-place (translate (wall-locate1 -0.3 0) web-post-bl))
    (thumb-ml-place (translate (wall-locate1 -0.3 0) web-post-tl))
    (key-place 1 cornerrow (translate (wall-locate2 0 -1) web-post-br))  ; -2.15 < second param wall-locate2 < 0.99
    (key-place 0 cornerrow (translate (wall-locate2 0 -1) web-post-bl))  ; -2.15 < second param wall-locate2 < 0.99
    ; connects to main keys top wall - first two key columns
    )
  ;; end thumb-valley
   ))  

(def thumb-walls
  (union  ;; currently being made for tipped-bowl version 
   thumb-valley
   (wall-brace thumb-br-place 0 -1 web-post-br thumb-br-place -1 -1 web-post-bl)  ; outside left lower wall - right (more front) key
   (wall-brace thumb-br-place -1 -1 web-post-bl thumb-br-place -1 -1 (translate [(- (displacement-edge rollin-default) base-offset) 0 4] web-post-bl))  ; outside left lower wall - psuedo between keys
   (bottom-hull  ;; Improved wall-brace for outside left lower wall - connect to main from psuedo
    (thumb-br-place (translate (wall-locate3 -1  -1) (translate [(- (displacement-edge rollin-default) base-offset) 0 4] web-post-bl)))
    (thumb-br-place (translate (wall-locate2 -1  -1) (translate [(- (displacement-edge rollin-default) base-offset) 0 4] web-post-bl)))
    (left-key-place cornerrow 1 web-post)
    (left-key-place cornerrow 1 (translate (wall-locate1 -1 0) web-post))
    (left-key-place cornerrow 1 (translate (wall-locate3 -1 0) web-post))
    )  
   (hull  ;; top wall over thumb key sticking the highest up after rotation (the bl key)
    (thumb-bl-place web-post-bl)
    (thumb-bl-place (translate (wall-locate1 0 -0.5) web-post-bl))
    (thumb-bl-place (translate (wall-locate2 0 -1) web-post-bl))
    (thumb-bl-place (translate (wall-locate3 0 -1) web-post-bl))
    (thumb-br-place (translate (wall-locate1 -1 -1) (translate [(- (displacement-edge rollin-default) base-offset) 0 4] web-post-bl)))
    (thumb-br-place (translate (wall-locate2 -1  -1) (translate [(- (displacement-edge rollin-default) base-offset) 0 4] web-post-bl)))
    (thumb-br-place (translate (wall-locate3 -1  -1) (translate [(- (displacement-edge rollin-default) base-offset) 0 4] web-post-bl)))
    (thumb-bl-place (translate (wall-locate1 0 -0.5) web-post-br))
    (thumb-bl-place web-post-br)
    ; Leaves a gap due to displaced wall.
    )
   (hull  ;; Gap fill for connecting displaced wall and space between two bottom thumb keys. 
    (thumb-br-place (translate (wall-locate1 -1 -1) (translate [(- (displacement-edge rollin-default) base-offset) 0 4] web-post-bl)))
    (thumb-bl-place (translate (wall-locate1 0 -0.5) web-post-br))
    (thumb-bl-place web-post-br)
    (thumb-br-place (translate (wall-locate1 -1 -1) web-post-bl))
    (thumb-br-place web-post-bl)
    ; connects to top wall over thumb key sticking the highest up after rotation (the bl key)
    )
   (triangle-hulls  ;; thumb-hood: connect the back of the top wall for the thumb highest outside key to main keys
    (thumb-br-place (translate (wall-locate3 -1  -1) (translate [(- (displacement-edge rollin-default) base-offset) 0 4] web-post-bl)))
    (left-key-place cornerrow 1 (translate (wall-locate1 -1 0) web-post))
    (thumb-bl-place (translate (wall-locate3 0 -1) web-post-bl))
    (left-key-place cornerrow 1 web-post)
    (key-place 0 cornerrow web-post-tl)
    (thumb-bl-place (translate (wall-locate3 0 -1) web-post-bl))
    (key-place 0 cornerrow web-post-bl)
    (key-place 0 cornerrow (translate (wall-locate1 0 0) web-post-tl))
    ; remaining section handled in thumb-valley (search thumb-hood) section.
    )
   (wall-brace thumb-br-place  0 -1 web-post-br thumb-br-place  1.5  1 web-post-tr)  ; outside left lower wall cornering to front wall
   (wall-brace thumb-br-place  1.5  1 web-post-tr thumb-mr-place 1  1 web-post-br)  ; front wall
   (wall-brace thumb-mr-place  1  1 web-post-br thumb-mr-place  1  -1.5 web-post-tr)  ; front wall
   (wall-brace thumb-mr-place  1  -1.5 web-post-tr thumb-tr-place  0.5  1 thumb-post-br)  ; front wall
   (wall-brace thumb-tr-place  0.5  1 thumb-post-br thumb-tr-place  0  1 thumb-post-tr)  ; front wall
   (wall-brace thumb-tr-place  0  1 thumb-post-tr thumb-tr-place  0  1 thumb-post-tl)  ; right wall of thumb, front section
   (wall-brace thumb-tr-place  0  1 thumb-post-tl thumb-tl-place  0  1 thumb-post-tr)  ; right wall of thumb, middle section
   (wall-brace thumb-tl-place  0  1 thumb-post-tr thumb-tl-place  6  1 thumb-post-tl)  ; right wall of thumb, upper section
   )  ;; end union
)  ; End of thumb-walls

(def case-walls
  (union
   main-key-cleanup
   ; back wall
   (for [x (range 0 ncols)] (key-wall-brace x 0 0 1 web-post-tl x       0 0 1 web-post-tr))
   (for [x (range 1 ncols)] (key-wall-brace x 0 0 1 web-post-tl (dec x) 0 0 1 web-post-tr))
   (key-wall-brace lastcol 0 0 1 web-post-tr lastcol 0 1 0 web-post-tr)
   ; right wall
   (for [y (range 0 lastrow)] (key-wall-brace lastcol y 1 0 web-post-tr lastcol y       1 0 web-post-br))
   (for [y (range 1 lastrow)] (key-wall-brace lastcol (dec y) 1 0 web-post-br lastcol y 1 0 web-post-tr))
   (key-wall-brace lastcol cornerrow 0 -1 web-post-br lastcol cornerrow 1 0 web-post-br)
   ; left wall
   (for [y (range 0 cornerrow)] (union (wall-brace (partial left-key-place y 1)       -1 0 web-post (partial left-key-place y -1) -1 0 web-post)
                                     (hull (key-place 0 y web-post-tl)
                                           (key-place 0 y web-post-bl)
                                           (left-key-place y  1 web-post)
                                           (left-key-place y -1 web-post))))
   (for [y (range 1 lastrow)] (union (wall-brace (partial left-key-place (dec y) -1) -1 0 web-post (partial left-key-place y  1) -1 0 web-post)
                                     (hull (key-place 0 y       web-post-tl)
                                           (key-place 0 (dec y) web-post-bl)
                                           (left-key-place y        1 web-post)
                                           (left-key-place (dec y) -1 web-post))))
   (wall-brace (partial key-place 0 0) 0 1 web-post-tl (partial left-key-place 0 1) 0 1 web-post)  ; back corner: coming from the back wall to left wall 
   (wall-brace (partial left-key-place 0 1) 0 1 web-post (partial left-key-place 0 1) -1 0 web-post)  ; back corner: remaining sliver from left wall to connect above line
   ; front wall
   (for [x (range 4 ncols)] (key-wall-brace x cornerrow 0 -1 web-post-bl x       cornerrow 0 -1 web-post-br))
   (for [x (range 5 ncols)] (key-wall-brace x cornerrow 0 -1 web-post-bl (dec x) cornerrow 0 -1 web-post-br))
  ;  thumb-walls
   ))
;;;;;;;;;;;;;;;;;;;;;;;;
;;; Other Components ;;;
;;;;;;;;;;;;;;;;;;;;;;;;
(def rj9-start  (map + [0 -3  0] (key-position 0 0 (map + (wall-locate3 0 1) [0 (/ mount-height  2) 0]))))
(def rj9-position  [(first rj9-start) (second rj9-start) 11])
(def rj9-cube   (cube 14.78 13 22.38))
(def rj9-space  (translate rj9-position rj9-cube))
(def rj9-holder (translate rj9-position
                  (difference rj9-cube
                              (union (translate [0 2 0] (cube 10.78  9 18.38))   ; add 1mm for y value? 
                                     (translate [0 0 5] (cube 10.78 13  5))))))  ; add 1mm for y value? 

(def usb-holder-position (key-position 1 0 (map + (wall-locate2 0 1) [0 (/ mount-height 2) 0])))
(def usb-holder-size [6.5 10.0 13.6])
(def usb-holder-thickness 4)
(def usb-holder
    (->> (cube (+ (first usb-holder-size) usb-holder-thickness) (second usb-holder-size) (+ (last usb-holder-size) usb-holder-thickness))
         (translate [(first usb-holder-position) (second usb-holder-position) (/ (+ (last usb-holder-size) usb-holder-thickness) 2)])))
(def usb-holder-hole
    (->> (apply cube usb-holder-size)
         (translate [(first usb-holder-position) (second usb-holder-position) (/ (+ (last usb-holder-size) usb-holder-thickness) 2)])))  ; Maybe add 1mm on z direction for usb holder hole

(def teensy-width 20)
(def teensy-height 12)
(def teensy-length 33)
(def teensy2-length 53)
(def teensy-pcb-thickness 2)
(def teensy-holder-width  (+ 7 teensy-pcb-thickness))
(def teensy-holder-height (+ 6 teensy-width))
(def teensy-offset-height 5)
(def teensy-holder-top-length 18)
(def teensy-top-xy (key-position 0 (- tilt-pivotrow 1) (wall-locate3 0.5 0)))
(def teensy-bot-xy (key-position 0 (+ tilt-pivotrow 1) (wall-locate3 0.5 0)))
(def teensy-holder-length (- (second teensy-top-xy) (second teensy-bot-xy)))
(def teensy-holder-offset (/ teensy-holder-length -2))
(def teensy-holder-top-offset (- (/ teensy-holder-top-length 2) teensy-holder-length))

(def teensy-holder
    (->>
        (union
          (->> (cube 3 teensy-holder-length (+ 6 teensy-width))
               (translate [1.5 teensy-holder-offset 0]))
          (->> (cube teensy-pcb-thickness teensy-holder-length 3)
               (translate [(+ (/ teensy-pcb-thickness 2) 3) teensy-holder-offset (- -1.5 (/ teensy-width 2))]))
          (->> (cube 4 teensy-holder-length 4)
               (translate [(+ teensy-pcb-thickness 5) teensy-holder-offset (-  -1 (/ teensy-width 2))]))
          (->> (cube teensy-pcb-thickness teensy-holder-top-length 3)
               (translate [(+ (/ teensy-pcb-thickness 2) 3) teensy-holder-top-offset (+ 1.5 (/ teensy-width 2))]))
          (->> (cube 4 teensy-holder-top-length 4)
               (translate [(+ teensy-pcb-thickness 5) teensy-holder-top-offset (+ 1 (/ teensy-width 2))])))
        (translate [(- teensy-holder-width) 0 0])
        (translate [-1.4 0 0])
        (translate [(first teensy-top-xy)
                    (- (second teensy-top-xy) 1)
                    (/ (+ 6 teensy-width) 2)])
           ))

(defn screw-insert-shape [bottom-radius top-radius height]
   (union (cylinder [bottom-radius top-radius] height)
          (translate [0 0 (/ height 2)] (sphere top-radius))))

(defn screw-insert [column row bottom-radius top-radius height]
  (let [shift-right   (= column lastcol)
        shift-left    (= column 0)
        shift-up      (and (not (or shift-right shift-left)) (= row 0))  ; if row is 0, but column is not 0 or lastcol 
        shift-down    (and (not (or shift-right shift-left)) (>= row lastrow)) ; if row is lastrow (or greater), but column is not 0 or lastcol 
        shift-thumb   (and (or shift-right shift-left) (>= row lastrow)) ; if row is lastrow (or greater) AND the column IS 0 or lastcol
        position 
        (if (and shift-left shift-thumb) (key-position column row (map + (wall-locate2 0 0) [-85 0 -38] ))   ; if nrows=4, [-67 0 -38]  
        (if (and shift-right shift-thumb) (key-position column row (map + (wall-locate2 0 0) [-72 5 -32] ))  ; if nrows=4,  [-70 7 -36]
            (if shift-up     (key-position column row (map + (wall-locate2  -0.5  -0.5) [0 (/ mount-height 2) 2]))
                (if shift-down  (key-position column row (map - (wall-locate2  0 -15) [-1 (/ mount-height 2) 11]))   ; if nrows=4, [-7 (/ mount-height 2) -14]
                    (if (and shift-left (>= row cornerrow)) (map + (left-key-position row 1) (wall-locate3 0 0) [-9 2 0])  
                    (if shift-left (map + (left-key-position row 1) (wall-locate3 0 0) [3 (/ mount-height 2) 11])
                        (key-position column row (map + (wall-locate2  0  1) [(+ (/ mount-width 2) 0) 0 0] ))))))))]  ; if nrows=4, [(+ (/ mount-width 2) 2) 0 -3]
    (->> (screw-insert-shape bottom-radius top-radius height)
         (translate [(first position) (second position) (/ height 2)])
    )))

(defn screw-insert-all-shapes [bottom-radius top-radius height]
  (union 
   (screw-insert 0 (+ 0.3 (* 0.5 (- nrows 3))) bottom-radius top-radius height)  ; back/top left  ;; rows=4, x=0.8, rows=5, x=1.3
   (screw-insert 0 (+ cornerrow 0.4)           bottom-radius top-radius height)  ; front/bottom left
   (screw-insert 2 (+ lastrow 0)               bottom-radius top-radius height)  ; front/bottom right
   (screw-insert 2 0                           bottom-radius top-radius height)  ; back/top center
   (screw-insert lastcol 0                     bottom-radius top-radius height)  ; back/top right
   (screw-insert lastcol (+ lastrow 0.1)       bottom-radius top-radius height)  ; thumb screw 
   (screw-insert 0 (+ lastrow 0.1)             bottom-radius top-radius height)  ; thumb screw 
   ))
(def screw-insert-height 3.8)
(def screw-insert-bottom-radius (/ 5.31 2))
(def screw-insert-top-radius (/ 5.1 2))
(def screw-insert-holes  (screw-insert-all-shapes screw-insert-bottom-radius screw-insert-top-radius screw-insert-height))
(def screw-insert-outers (screw-insert-all-shapes (+ screw-insert-bottom-radius 1.6) (+ screw-insert-top-radius 1.6) (+ screw-insert-height 1.5)))
(def screw-insert-screw-holes  (screw-insert-all-shapes 1.7 1.7 350))

(def wire-post-height 7)
(def wire-post-overhang 3.5)
(def wire-post-diameter 2.6)
(defn wire-post [direction offset]
   (->> (union (translate [0 (* wire-post-diameter -0.5 direction) 0] (cube wire-post-diameter wire-post-diameter wire-post-height))
               (translate [0 (* wire-post-overhang -0.5 direction) (/ wire-post-height -2)] (cube wire-post-diameter wire-post-overhang wire-post-diameter)))
        (translate [0 (- offset) (+ (/ wire-post-height -2) 3) ])
        (rotate (/ α -2) [1 0 0])
        (translate [3 (/ mount-height -2) 0])))

(def wire-posts
  (union
   (thumb-ml-place (translate [-5 0 -2] (wire-post  1 0)))
   (thumb-ml-place (translate [0 0 -2.5] (wire-post -1 6)))
   (thumb-ml-place (translate [5 0 -2] (wire-post  1 0)))
   (for [column (range 0 lastcol)
         row (range 0 cornerrow)]
     (union
      (key-place column row (translate [-5 0 0] (wire-post 1 0)))
      (key-place column row (translate [0 0 0] (wire-post -1 6)))
      (key-place column row (translate [5 0 0] (wire-post  1 0)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Main Model (used for both sides) ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def model-right (difference
                   (union
                    key-holes
                    connectors
                    case-walls
                    thumb-walls
                    thumb
                    thumb-connectors
                    (difference (union case-walls
                                       thumb-walls
                                       screw-insert-outers
                                       teensy-holder
                                       usb-holder)
                                rj9-space
                                usb-holder-hole
                                screw-insert-holes)
                    rj9-holder
                    ; wire-posts
                    ; thumbcaps
                    ; caps
                    )
                   (translate [0 0 -20] (cube 350 350 40))
                  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Create the SCAD files ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(spit "things/right.scad"
      (write-scad model-right))

(spit "things/left.scad"
      (write-scad (mirror [-1 0 0] model-right)))

(spit "things/right-test.scad"
      (write-scad (union model-right 
                         thumbcaps 
                         caps
                  )))

(spit "things/right-plate.scad"
      (write-scad
       (union
        (translate [0 0 (* 0 wall-thickness)]
                   (extrude-linear {:height (* 1 wall-thickness) :twist 0 :convexity 0}
                                   (cut
                                    (translate [0 0 (* -1 wall-thickness)]
                                               (difference
                                                (union case-walls
                                                       thumb-walls
                                                       teensy-holder
                                                       ; rj9-holder
                                                       screw-insert-outers)
                                                (translate [0 0 -10] screw-insert-screw-holes))))  ; end cut
                                   )  ; end extrude-linear
                   )
        ;; would like to add the filled in bottom plate here
;        (extrude-linear {:height (* 1 wall-thickness) :twist 0 :convexity 0}
;                        (cut
;                         (translate [0 0 (* -1 wall-thickness)]
;                                    (difference
;                                     (union (hull case-walls)
;                                            (hull thumb-walls)
;                                            teensy-holder
;                                           ; rj9-holder
;                                            screw-insert-outers)
;                                     (translate [0 0 -10] screw-insert-screw-holes))))  ; end cut
;        )  ; end extrude-linear
        )))

(defn reset-thumb-placement [object]
  (translate [0 0 (* 0.25 mount-height)]
  (rotate (deg2rad 15) [1 0 0] 
          (rotate (deg2rad 63) [0 1 0]
                  (rotate slope-thumb [-1 0 0]
                          (rotate (+ thumb-tent (deg2rad 90)) [0 -1 0]
                                  (rotate (deg2rad 90) [0 0 1]
                                          ;; stuff 
                                          (translate (map * [-1 -1 -1] thumborigin)
                                                     object
                                                     
                                             ) ; end translate  
                                  ))))) ; end nested rotates
  )
  )  ; end reset-thumb-placement

(spit "things/thumbpad.scad"
      (write-scad
       (reset-thumb-placement 
       (union
        thumb
        thumb-connectors
        thumb-valley
        ;  thumbcaps
        ; thumb-walls
        ;  case-walls
       )  ; end union 
        )
       )) 

(defn -main [dum] 1)  ; dummy to make it easier to batch