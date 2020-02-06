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
(def column-per-finger [2 1 1 2])
(def ncols (reduce + column-per-finger))
(def middle-finger-col (get column-per-finger 0))  ; First column is 0, and middle finger comes after the first (pointer) finger. 
(def has-lastrow       [middle-finger-col (+ middle-finger-col 1) (+ middle-finger-col 2) (+ middle-finger-col 2)])   
(def has-firstrow      [(- middle-finger-col 2) (- middle-finger-col 1) middle-finger-col (+ middle-finger-col 1)])
(def no-firstrow       [(+ middle-finger-col 2) (+ middle-finger-col 3)])
(def is-stretch-column [0 5 6 7])  ; 5 (or greater) ignored if ncols<=5, but is there just in case we add a second pinkie column.
(def α (deg2rad 34))                    ; curvature of the columns (front to back)- 30 to 36 degrees seems max
(def β (deg2rad -5))             ; Was 6 ; curvature of the rows (left to right) - adds to tenting
(def γ (deg2rad 6))              ; Stretch columns (not the home columns) have a different curve.
(def extra-width 1.5)                     ; extra space between the base of keys; Normal specification when flat is 1.65
(def extra-height 0.5)                  ; original= 0.5; to spec when flat is 1.65
(def wall-z-offset -14)                 ; length of the first downward-sloping part of the wall (negative) ; original: -15 | Jan -12 | Feb -14
(def wall-xy-offset 6)                  ; offset in the x and/or y direction for the first downward-sloping part of the wall: Jan 3 | Feb 6
(def wall-thickness 2.5)                  ; Was 2; Jan 3.5; wall thickness parameter. Thickness probably has only moderate cost of material. 
(def tilt-pivotrow (- nrows 1.25 (/ nrows 2))) ; controls front-back tilt: Even nrows means flat home row. Odd nrows means flat is between home row and 1 row up.
(def tent-pivotcol 4 )                       ; controls left-right tilt / tenting (higher number is more tenting)
(def tenting-angle (deg2rad 55))            ; or, change this for more precise tenting control
(def keyboard-z-offset (+ 2 (* 13 (- ncols tent-pivotcol))))  ; 1 @ 4, 3 @ 5, 9 @ 6            ; controls overall height, affected by tenting; original=9 with tent-pivotcol=3; use 16 for tent-pivotcol=2
(def cherry-brand-keyswitch false)
(def tight-extra-keys false)
(def plate-thickness 3.5)  ; was 4 ; 
(defn column-offset [column] (cond
                               (= column  (+ 0 middle-finger-col)) [0 13.82 -2.5 ]  ; tried [0 14.82 -4.5 ]  ; original [0 2.82 -4.5]
                               (= column  (+ 1 middle-finger-col)) [0  7.82 -1.25]  ; tried [0  7.82 -2.25]  ; original [0 0 0]
                               (>= column (+ 2 middle-finger-col)) [0 -5.18  1.39]  ; tried [0 -5.18  3.39]  ; original [0 -5.8 5.64], [0 -12 5.64]
                               :else [0 0 0]))  ; The pointer finger
;; Doesn't work quite like was hoped - Settings for column-style == :fixed
;; The defaults roughly match Maltron settings http://patentimages.storage.googleapis.com/EP0219944A2/imgf0002.png
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
(def columns (range 0 ncols))
(def rows (range 0 nrows))
(def sa-profile-key-height 12.7)
(def column-style
  (if (> nrows 3) :orthographic :standard))  ; options include :standard, :orthographic, and :fixed
;;;;;;;;;;;;;;;;;
;; Switch Hole ;;
;;;;;;;;;;;;;;;;;
(def keyswitch-height (if (= cherry-brand-keyswitch true) 14.4 14.15 )) ; try 14.15, 14.25 ; 14.3 was slightly loose; Was 14.1, then 14.25, then 14.4
(def keyswitch-width  (if (= cherry-brand-keyswitch true) 14.4 14.15 ))  ; try ?? ; Was 14.4
(def clip-keyswitch   (if (= cherry-brand-keyswitch true)  1.0  0 ))  ; Was 1 for cherry, for others: 0.5 with width at 14.4 was too loose.
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
;; Placement Variables ;;
;;;;;;;;;;;;;;;;;;;;;;;;;
(def cap-top-height (+ plate-thickness sa-profile-key-height))
(def web-thickness 3.5)  ; 3.5

;;;;;;;;;;;;;;;;;;;;;;;;;
;; Placement Functions ;;
;;;;;;;;;;;;;;;;;;;;;;;;;
(def row-radius (+ (/ (/ (+ mount-height extra-height) 2)
                      (Math/sin (/ α 2)))
                   cap-top-height))
(def column-radius (+ (/ (/ (+ mount-width extra-width) 2)
                         (Math/sin (/ β 2)))
                      cap-top-height))
(def stretch-column-radius (+ (/ (/ (+ mount-width extra-width) 2)
                                 (Math/sin (/ γ 2)))
                              cap-top-height)
  )
(def column-x-delta (+ -1 (- (* column-radius (Math/sin β)))))
(def stretch-column-x-delta (+ -1 (- (* stretch-column-radius (Math/sin γ)))))

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
        stretch-col-z-adjust (Math/cos (- γ β ))
        placed-shape-ortho (->> shape
                                (translate-fn [0 0 (- row-radius)])
                                (rotate-x-fn  (* α (- tilt-pivotrow row)))
                                (translate-fn [0 0 row-radius])
                                (rotate-y-fn  column-angle)
                                (rotate-y-fn  (if (.contains is-stretch-column column) (* (if (< column middle-finger-col) 1 -1) (- γ β)) 0))
                                (translate-fn (if (.contains is-stretch-column column) [(- (* (if (< column middle-finger-col) 1 -1) (- column-x-delta stretch-column-x-delta))) 0 stretch-col-z-adjust] [0 0 0]))
                                (translate-fn [(- (* (- column tent-pivotcol) column-x-delta)) 0 column-z-delta])
                                (translate-fn (column-offset column)))
        placed-shape-fixed (->> shape
                                (rotate-y-fn  (nth fixed-angles column))
                                (translate-fn [(nth fixed-x column) 0 (nth fixed-z column)])
                                (translate-fn [0 0 (- (+ row-radius (nth fixed-z column)))])
                                (rotate-x-fn  (* α (- tilt-pivotrow row)))
                                (translate-fn [0 0 (+ row-radius (nth fixed-z column))])
                                (rotate-y-fn  fixed-tenting)
                                (translate-fn [0 (second (column-offset column)) 0]))]
    (->> (case column-style
           :orthographic placed-shape-ortho
           :fixed        placed-shape-fixed
           placed-shape)
         (rotate-y-fn  tenting-angle)
         (translate-fn [0 0 keyboard-z-offset]))
    ; end apply-key-geometry
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
               :when (and
                      (or
                       (.contains has-lastrow column)
                       (not= row lastrow))
                      (or
                       (.contains has-firstrow column)
                       (not= row 0))
                      )]
           (->> single-plate
                (key-place column row)))))

;;;;;;;;;;;;;;;;;;;;
;; Helper Objects ;;
;;;;;;;;;;;;;;;;;;;;
(def clearance
  (color [127/255 159/255 127/255 0.7]
         (hull
          (translate [0 0 (* -1 plate-thickness)] single-plate)
          (translate [0 0 (* -1 (+ plate-thickness 5))] single-plate))))
(def caps
  (apply union
         (for [column columns
               row rows
               :when (and
                      (or
                       (.contains has-lastrow column)
                       (not= row lastrow))
                      (or
                       (.contains has-firstrow column)
                       (not= row 0))
                      )]
           (->> (sa-cap (if (= column 5) 1 1))
                (key-place column row)))))

;;;;;;;;;;;;;;;;;;;;
;; Web Connectors ;;
;;;;;;;;;;;;;;;;;;;;
(def post-size 0.1)
(def web-post (->> (cube post-size post-size web-thickness)
                   (translate [0 0 (+ (/ web-thickness -2)
                                      plate-thickness)])))
(def post-adj (/ post-size 2))
(def web-post-tr (translate [(- (/ mount-width 2) post-adj) (- (/ mount-height 2) post-adj) 0] web-post))
(def web-post-tl (translate [(+ (/ mount-width -2) post-adj) (- (/ mount-height 2) post-adj) 0] web-post))
(def web-post-br (translate [(- (/ mount-width 2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))
(def web-post-bl (translate [(+ (/ mount-width -2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))
(defn wall-locate1 [dx dy] [(* dx wall-thickness) (* dy wall-thickness) -1])
(defn wall-locate2 [dx dy] [(* dx wall-xy-offset) (* dy wall-xy-offset) wall-z-offset])
(defn wall-locate3 [dx dy] [(* dx (+ wall-xy-offset wall-thickness)) (* dy (+ wall-xy-offset wall-thickness)) wall-z-offset])
(defn triangle-hulls [& shapes]
  (apply union
         (map (partial apply hull)
              (partition 3 1 shapes))))
(defn col-gap [row a b tight]
  " Fill between columns a & b on given row. Tight can be true (the column to right is close) or false. "
  (triangle-hulls  ;; Extra keys (column 2 & 3) column gap
   (key-place a row web-post-br)
   (if tight
     (key-place b row (translate (wall-locate1 -0.6 0) web-post-bl))
     (key-place b row web-post-bl))
   (key-place a row web-post-tr)
   (if tight
     (key-place b row (translate (wall-locate1 -0.6 0) web-post-tl))
     (key-place b row web-post-tl))
   (if tight (key-place b row web-post-tl))
   (if tight (key-place b row (translate (wall-locate1 -0.6 0) web-post-bl)))
   (if tight (key-place b row web-post-bl))
   ; (defn col-gap [row a b]  ; old definition doesn't work for tight columns
   ;   (triangle-hulls
   ;    (key-place b row web-post-tl)
   ;    (key-place a row web-post-tr)
   ;    (key-place b row web-post-bl)
   ;    (key-place a row web-post-br)))
   ))
 (defn diag-gap [column row]
   " Typically needs to be called whenever col-gap called. Fills column gap that is between row gaps. "
   (triangle-hulls
    (key-place column (dec row) web-post-br)
    (key-place column row web-post-tr)
    (key-place (inc column) (dec row) web-post-bl)
    (key-place (inc column) row web-post-tl)))
(defn row-gap [column a b]
  " Fills the space between keys that are in the given column. Be mindful of the diag-gap and col-gap. "
  (triangle-hulls
   (key-place column a web-post-bl)
   (key-place column a web-post-br)
   (key-place column b web-post-tl)
   (key-place column b web-post-tr)))
(def connectors
  (apply union
         (concat
          ;; Row connections
          (for [column (range 0 (dec ncols) ) :when (.contains has-firstrow (+ column 1))
                row (range 0 lastrow)]
            (union
             (col-gap row column (inc column) false)
             (if (not= row 0)
               (diag-gap column row))))
          (for [column (range 0 (dec ncols)) :when (not (.contains has-firstrow (+ column 1)))
                row (range 1 lastrow)]
            (union
              (col-gap row column (inc column) false)
              (if (not= row 1)
                (diag-gap column row))))
          ;; Column connections (except the bonus on lastrow)
          (for [column columns :when (.contains has-firstrow column)
                row (range 0 cornerrow)]
            (row-gap column row (inc row)))
          (for [column columns :when (not (.contains has-firstrow column))
                row (range 1 cornerrow)]
            (row-gap column row (inc row)))
          ;; Diagonal connections - currently done inside row connections
            ; (for [column (range 0 (dec ncols))
            ;       row (range 0 cornerrow)]
            ;   (diag-gap column row)
            ;   )
          )
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;     Thumbs with Updated Params & Placement                         ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parameters for thumb      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def test-column-space 0)  ; like extra-width (2.5); to spec when flat is 1.65
(def test-row-space -5 )   ; like extra-height (1.0)to spec when flat is 1.65;  at one point, 3.5 seemed good.
(def rollin-default (deg2rad 30) )    ; we want to do radians since java Math trig functions take in radian values.
(def rollin-top (deg2rad 30) )
(def tilt-top (deg2rad 40) )
(def tilt-default (deg2rad 0) )            ; tilt settings are also in radians
(def tilt-last (deg2rad -40))   ; TODO: parameterize to allow deciding what angle the bottom section ends at.
(def thumb-tent (* -0.25 (+ tenting-angle (* β (- tent-pivotcol 1)) )))
(def slope-thumb (deg2rad -60))
(def deflect (deg2rad -2.5))  ; (/ π -3)
(def half-width (/ mount-width 2))
(def larger-plate-height (/ (+ sa-double-length keyswitch-height) 2) )
(def base-offset (+ half-width test-column-space) )     ; original was 14 or 15
(def row-offset (+ mount-height test-row-space) )
(def deflect-fudge [0 0 0])  ; previoiusly: (def deflect-fudge [-6 7 4])
(def thumb-offsets [(* -1 half-width) (* -1.1 mount-height) (* -3.5 mount-height)])            ; original [6 -3 7], [20 -3 7]
;;;;;;;;;;;;; Corner posts for thumb keys. ;;;;;;;;;;;;;;;;;
; Use if thumb-upper-layout is only unsing 1u caps.
(def thumb-post-tr web-post-tr)
(def thumb-post-tl web-post-tl)
(def thumb-post-bl web-post-bl)
(def thumb-post-br web-post-br)
; Original (for tr and tl) - These define edges when we use the bigger key plate. 
; (def thumb-post-tr (translate [(- (/ mount-width 2) post-adj)  (- (/ mount-height  1.15) post-adj) 0] web-post))
; (def thumb-post-tl (translate [(+ (/ mount-width -2) post-adj) (- (/ mount-height  1.15) post-adj) 0] web-post))
; (def thumb-post-bl (translate [(+ (/ mount-width -2) post-adj) (+ (/ mount-height -1.15) post-adj) 0] web-post))
; (def thumb-post-br (translate [(- (/ mount-width 2) post-adj)  (+ (/ mount-height -1.15) post-adj) 0] web-post))
;;;;'''''';;;;; Derived Variables ;;;;;;;;;;;;;;;;;
(def thumborigin
  (map + (key-position 0 lastrow [(* -0 mount-width) (* -0 mount-height) (* 0 mount-height)])  ; [(* -2 mount-width) (/ mount-height -20) (/ mount-height -2)]
       thumb-offsets))  ; original: (map + (key-position 1 cornerrow thumb-offsets)))
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
  (- (* x-point (Math/cos angle)) (* y-point (Math/sin angle)) x-point))  ; older: (- (* half-width (Math/cos (mod rollin (* 2 π))) (* key-ttl-height (Math/sin (mod rollin (* 2 π)))) half-width ))
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
       (rotate deflect [0 0 1])
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
       (rotate deflect [0 0 1])
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
       (rotate deflect [0 0 1])
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
       (rotate deflect [0 0 1])
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
       (rotate deflect [0 0 1])
      ;  (translate (map * [-1 1 1] (deflect-offset deflect)))
       (translate thumborigin)
  ))
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
       (rotate deflect [0 0 1])
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Web connectors for thumb keys - filling in gaps between keys. ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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

;;;;;;;;;;;;;;;;;;;;;
;; Case Parameters ;;
;;;;;;;;;;;;;;;;;;;;;
(def left-wall-x-offset 10)
(def left-wall-z-offset  3)
(def thumb-lastrow-connect (thumb-tl-place thumb-post-tr))
; HERE HERE
; (def thumb-lastrow-connect (thumb-tl-place (translate (wall-locate3 2.9 -0.75) thumb-post-tr)))
; thumb-tl-place 2.9 -0.75 thumb-post-tr
(def thumb-corner-connect (thumb-tl-place thumb-post-tl))
(def thumb-left-connect (union
                         thumb-corner-connect
                         (thumb-tl-place thumb-post-bl)
                         ))
; (def thumb-lastrow-connect (thumb-tl-place (translate (wall-locate3 0.25 0) thumb-post-tr)))
; (def thumb-lastrow-connect (key-place lastcol cornerrow (translate (wall-locate3 -2 -1) web-post-bl) ))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Case Placement Functions          ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn bottom [height p]
  (->> (project p)
       (extrude-linear {:height height :twist 0 :convexity 0})
       (translate [0 0 (- (/ height 2) 10)])))
(defn bottom-hull [& p]
  " Takes in multiple positions and creates a filled in wall to the ground"
  (hull p (bottom 0.001 p)))
(defn left-key-position [row direction]
  (map - (key-position 0 row [(* mount-width -0.5) (* direction mount-height 0.5) 0]) [left-wall-x-offset 0 left-wall-z-offset]) )
(defn left-key-place [row direction shape]
  (translate (left-key-position row direction) shape))
(defn wall-brace [place1 dx1 dy1 post1 place2 dx2 dy2 post2]
  " Create a wall to the ground that is a little bit away from the two given positions with the provided x & y thickness parameters"
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

(defn key-top-wall [col row adj-l adj-r]
  " The lastrow keys often need a top wall. The adj-l and adj-r variables used if the columns are too tight"
  (union
   (hull  ; main keys top wall - first extra key (column 2) top wall sloped to second extra key (column 3) top wall
    (key-place col row web-post-bl)
    (if (not= adj-l 0) (key-place col row (translate (wall-locate1 adj-l 0) web-post-bl)))
    (key-place col row (translate (wall-locate1  adj-l    0.3) web-post-bl))
    (key-place col row (translate (wall-locate2  0    0.3) web-post-bl))
    (key-place col row (translate (wall-locate3  0    0) web-post-bl))
    (if (and (not= adj-r 0) (if (= row lastrow) (.contains has-lastrow (inc col)) true) )
      (key-place (inc col) row (translate (wall-locate1 adj-r  0) web-post-bl))
      (key-place col       row web-post-br)
      ) 
    (if (and (not= adj-r 0) (if (= row lastrow) (.contains has-lastrow (inc col)) true) )
      (key-place (inc col) row (translate (wall-locate1 adj-r 0.3) web-post-bl))
      (key-place col       row (translate (wall-locate1   0   0.3) web-post-br))
      )
    (if (and (not= adj-r 0) (if (= row lastrow) (.contains has-lastrow (inc col)) true) )
      (key-place (inc col) row (translate (wall-locate2   0   0.3) web-post-bl))
      (key-place col       row (translate (wall-locate2   0   0.3) web-post-br))
      )
    (if (and (not= adj-r 0) (if (= row lastrow) (.contains has-lastrow (inc col)) true) )
      (key-place (inc col) row (translate (wall-locate3   0   0) web-post-bl))
      (key-place col       row (translate (wall-locate3   0   0) web-post-br))
      )
   ; end hull
    )
   (if (and (not= adj-r 0) (if (= row lastrow) (.contains has-lastrow (inc col)) true) )
     (hull  ;; main key top wall - gap fixer for transition between 1st & 2nd extra keys (columns 2 & 3)
      (key-place col       row web-post-bl)
      (key-place col       row (translate (wall-locate1  0    0.3) web-post-bl))
      (key-place col       row web-post-br)
      (key-place col       row (translate (wall-locate1  0    0.3) web-post-br))
      (key-place (inc col) row (translate (wall-locate1 adj-r  0 ) web-post-bl))
      (key-place (inc col) row (translate (wall-locate1 adj-r 0.3) web-post-bl)))
    ; end if
     )
  ; end key-top-wall
   ))
(defn top-wall-cleanup [thumb tight]  ;; column gap & top walls for 1st & 2nd extra keys (columns 2 & 3)
  " Lastrow extra keys may need a top wall. Input thumb is true or false, for connecting to thumb cluster. 
    Input tight is false for normal column gaps or true if these keys are very close together. "
  (union
   (hull  ; First extra key (column 2) front wall to thumb corner
    (for [col columns :when (and (.contains has-lastrow col) (not (.contains is-stretch-column col)))]
      (key-place col lastrow (translate (wall-locate3 0 0) web-post-bl)))
    (if (= thumb true) (union thumb-lastrow-connect thumb-corner-connect))
    ; end hull 
    )
   (for [col columns :when (.contains has-lastrow col)]
     (if tight
       (key-top-wall col lastrow (if (= col (get has-lastrow 0)) 0 -0.6) (if (= col (last has-lastrow)) 0 -0.6))
       (key-top-wall col lastrow 0 0)) ; end if else
     ; end for loop
   )
   (if (not tight)
     (for [col columns :when (and (.contains has-lastrow col) (.contains has-lastrow (inc col)))]
       (union
        (hull  ; fill in the edge from the back (locate3) of the top wall (some left empty by the connector to the thumb)
         (key-place col lastrow (translate (wall-locate3 0 0) web-post-bl))
         (key-place col lastrow (translate (wall-locate2 0 0.3) web-post-bl))
         (key-place col lastrow (translate (wall-locate3 0 0) web-post-br))
         (key-place col lastrow (translate (wall-locate2 0 0.3) web-post-br))
         (key-place (inc col) lastrow (translate (wall-locate3 0 0) web-post-bl))
         (key-place (inc col) lastrow (translate (wall-locate2 0 0.3) web-post-bl)))
        (hull  ; fill in the column gap on the top wall (this is not an issue when tight is true)
         (key-place col lastrow web-post-br)
         (key-place col lastrow (translate (wall-locate1 0 0.3) web-post-br))
         (key-place col lastrow (translate (wall-locate2 0 0.3) web-post-br))
         (key-place col lastrow (translate (wall-locate3 0 0) web-post-br))
         (key-place (inc col) lastrow web-post-bl)
         (key-place (inc col) lastrow (translate (wall-locate1 0 0.3) web-post-bl))
         (key-place (inc col) lastrow (translate (wall-locate2 0 0.3) web-post-bl))
         (key-place (inc col) lastrow (translate (wall-locate3 0 0) web-post-bl)))))
     ; end if not tight 
     )
   ; end union at top of the function
   ))  ;; end top-wall-cleanup
(defn connect-adjacent [column side]
  " Usually used to connect the 'extra' lastrow keys to the corner of the adjent column "
  (let [p1 (if (= side 'left') 0 -0.3)
        p2 (if (= side 'left') 0.3  0)
        top-post (if (= side 'left') web-post-tl web-post-tr)
        bottom-post (if (= side 'left') web-post-bl web-post-br)
        adjacent (if (= side 'left') (dec column) (inc column))
        adj-post (if (= side 'left') web-post-br web-post-bl)
        ]
    (hull
     (key-place column cornerrow bottom-post)
     (key-place column cornerrow (translate (wall-locate1 0  0) bottom-post))
     (key-place column lastrow top-post)
     (key-place column lastrow   (translate (wall-locate1 p1  p2) top-post))
     (key-place adjacent cornerrow adj-post)
     (key-place adjacent cornerrow (translate (wall-locate1 0 -0.5) adj-post))
     ; end connect-adjacent
     )))
(defn key-wall [column side]
  " Usually used to create a 'left' or 'right' side wall for the 'extra'
    keys of the lastrow (that aren't connected when generated)"
  (let [p1 (if (= side 'left') 0 -0.3)
        p2 (if (= side 'left') 0.3 0)
        top-post (if (= side 'left') web-post-tl web-post-tr)
        bottom-post (if (= side 'left') web-post-bl web-post-br)
        ]
    (hull
     (key-place column lastrow top-post)
     (key-place column lastrow   (translate (wall-locate1 p1 p2) top-post))
     (key-place column lastrow   (translate (wall-locate2 p1 p2) top-post))
     (key-place column lastrow   (translate (wall-locate3 0  0 ) top-post))
     (key-place column lastrow bottom-post)
     (key-place column lastrow   (translate (wall-locate1 p1 p2) bottom-post))
     (key-place column lastrow   (translate (wall-locate2 p1 p2) bottom-post))
     (key-place column lastrow   (translate (wall-locate3 0  0 ) bottom-post))
  ; End key-wall
     )))

(defn main-key-cleanup [thumb]
  " Deals with lastrow keys formatting that is not automatically managed like other main section keys. "
  (union
   (for [col columns :when (.contains has-lastrow col)]
     (row-gap col cornerrow lastrow))
   (for [col (range 0 lastcol) :when (and (.contains has-lastrow (inc col)) (.contains has-lastrow col))]
     (union
      (col-gap lastrow col (inc col) tight-extra-keys)
      (diag-gap col lastrow)))
   (connect-adjacent (get has-lastrow 0) 'left')
   (hull  ; First extra key (usually column 2) left wall
    (key-wall (get has-lastrow 0) 'left')  ; Then it connects to what?
    (key-place (dec (get has-lastrow 0)) cornerrow web-post-br)
    (key-place (dec (get has-lastrow 0)) cornerrow (translate (wall-locate1 0   -0.5) web-post-br))
    (if (= thumb true) thumb-left-connect)  ;; Comment out if no thumb section printing.
    )
   ; TODO: Deal with condition that we have a lastrow key on the lastcol column. 
  ;  (if (= lastcol (last has-lastrow)) 
  ;    )
   (connect-adjacent (last has-lastrow) 'right')
   (hull  ; Last extra key right wall
    (key-wall (last has-lastrow) 'right')
    ; Then it connects to what?
    (key-place (inc (last has-lastrow)) cornerrow web-post-bl)
    (key-place (inc (last has-lastrow)) cornerrow (translate (wall-locate1  0 -0.5) web-post-bl))
    (key-place (inc (last has-lastrow)) cornerrow (translate (wall-locate2 -0.3 -1) web-post-bl))
    (key-place (inc (last has-lastrow)) cornerrow (translate (wall-locate3  0   -1) web-post-bl))
    ; connects to default front wall of the cornerrow for the next column to the right. 
    )
   (if (.contains is-stretch-column (last has-lastrow))
     (hull ; Front wall fill in: Capturing the gap on the non-stretch column (because the last lastrow key is on a stretch column). 
      (key-place (dec (last has-lastrow)) lastrow   (translate (wall-locate3 0 0) web-post-bl))
      (key-place (dec (last has-lastrow)) lastrow   (translate (wall-locate3 0 0) web-post-br))
      (key-place (last has-lastrow) lastrow   (translate (wall-locate3 0 0) web-post-bl))
      (if (= thumb true) thumb-lastrow-connect)  ;; Comment out if no thumb section printing.

      ; done? 
      ))
   (hull ; Front wall for Last extra keys, then connect to thub-lastrow-connect. 
    (key-place (last has-lastrow) lastrow   (translate (wall-locate3 0 0) web-post-bl))
    (key-place (last has-lastrow) lastrow   (translate (wall-locate3 0 0) web-post-br))
    (key-place (last has-lastrow) lastrow   (translate (wall-locate3 0 0) web-post-tr))  ; TODO: Is needed? Check for conditions this is problematic
    (key-place (inc (last has-lastrow)) cornerrow (translate (wall-locate3 0 -1) web-post-bl)) ; connects to default front wall of next/last column cornerrow
    (if (= thumb true) thumb-lastrow-connect)  ;; Comment out if no thumb section printing.
    )
   (if (= thumb true)
     (bottom-hull  ; front wall of extra keys and final main section.
      (key-place (inc (last has-lastrow)) cornerrow (translate (wall-locate3 0 -1) web-post-bl))
      thumb-lastrow-connect  ;; Comment out if no thumb section printing.
      ))
   (top-wall-cleanup thumb tight-extra-keys)  ; if no gap: column gap & top walls for 1st & 2nd extra keys (columns 2 & 3)
   (hull
    
    )
  ; end of main-key-cleanup
   ))

(def valley-clearance
  (union
   (key-place 0 cornerrow clearance)
   (key-place 1 cornerrow clearance)
  ;  (thumb-tl-place clearance)
  ;  (thumb-ml-place clearance)
   (thumb-bl-place clearance)
   (thumb-tl-place clearance)
   (thumb-tr-place clearance)
   (thumb-br-place clearance)
   (key-place (+ middle-finger-col 1) 1       clearance)
   (key-place (+ middle-finger-col 1) 1       clearance)
   (key-place middle-finger-col 1       clearance)
   (key-place (- lastcol 1)     1       clearance)
   (key-place (- lastcol 1)     2       clearance)
   (key-place (- lastcol 1)   cornerrow clearance)
   (key-place lastcol           1       clearance)
   (key-place lastcol           2       clearance)
   (key-place lastcol         cornerrow clearance)
   (for [x (range -1 2) :when (.contains has-firstrow (+ middle-finger-col x))] (key-place (+ middle-finger-col x) 0 clearance) )
   (for [x (range 0 lastcol) :when (.contains has-lastrow x)] (key-place x lastrow clearance))  ; HERE
  ;  (if (.contains has-lastrow lastcol) (key-place lastcol lastrow clearance))
   ;; any more?
   ))
(def thumb-valley
  (union
   (hull  ;; upper part of last key left wall (but without the wall-brace to the ground)
    (key-place 0 cornerrow web-post-tl)
    (key-place 0 cornerrow web-post-bl)
    (left-key-place cornerrow  1 web-post)
    (left-key-place cornerrow -1 web-post))
   (hull  ;; thumb-hood left wall: updated for reverse tilt thumb cluster
    (left-key-place cornerrow  1 (translate (wall-locate1 -1 0) web-post))
    (left-key-place cornerrow  1 web-post)
    (left-key-place cornerrow -1 web-post)
    (left-key-place cornerrow -1 (translate (wall-locate1 -1 0) web-post))
    (thumb-bl-place web-post-bl)
    (thumb-bl-place (translate (wall-locate1 0 -0.5) web-post-bl))
    ; connect to what?
    )
   (hull  ;; thumb-hood updated for reverse tilt thumb
    (left-key-place cornerrow -1 web-post)
    (left-key-place cornerrow -1 (translate (wall-locate1 -1 0) web-post))
    (key-place 0 cornerrow (translate (wall-locate2 0 0.3) web-post-bl))
    (key-place 0 cornerrow (translate (wall-locate3 0 0) web-post-bl))
    (thumb-bl-place (translate (wall-locate1 0 -0.5) web-post-bl))
    (thumb-bl-place web-post-bl)
    ; connect to what?
    )
   (hull
    (left-key-place cornerrow -1 web-post)
    (left-key-place cornerrow -1 (translate (wall-locate1 -1 0) web-post))
    (key-place 0 cornerrow web-post-bl)
    (key-place 0 cornerrow (translate (wall-locate1 0 0.3) web-post-bl))
    (key-place 0 cornerrow (translate (wall-locate2 0 0.3) web-post-bl))
    (key-place 0 cornerrow (translate (wall-locate3 0 0) web-post-bl))
    ; connect to what?
    )
   (hull  ;; smooth out first two column key edge, then top wall to corner of thumb section
    (key-place 0 cornerrow web-post-bl)
    (key-place 0 cornerrow (translate (wall-locate1  0 0.3) web-post-bl))
    (key-place 0 cornerrow (translate (wall-locate2  0 0.3) web-post-bl))
    (key-place 0 cornerrow (translate (wall-locate3 0 0) web-post-bl))
    (key-place 0 cornerrow web-post-br)
    (key-place 0 cornerrow (translate (wall-locate1 0 0.3) web-post-br))
    ; (key-place 0 cornerrow (translate (wall-locate2 0 0) web-post-br))
    ; (key-place 0 cornerrow (translate (wall-locate3 0 0) web-post-br))
    (key-place 1 cornerrow web-post-bl)
    (key-place 1 cornerrow (translate (wall-locate1 0 0.3) web-post-bl))
    ; (key-place 1 cornerrow (translate (wall-locate2 0 0) web-post-bl))
    ; (key-place 1 cornerrow (translate (wall-locate3 0 0) web-post-bl))
    (key-place 1 cornerrow web-post-br)
    (key-place 1 cornerrow (translate (wall-locate1 0 0.3) web-post-br))
    (thumb-tl-place thumb-post-bl)
    (thumb-tl-place (translate (wall-locate1 0 -0.3) thumb-post-bl))
    )
   (hull  ;; smooth out thumb section close to main,
    ; (thumb-bl-place web-post-bl)
    (thumb-bl-place web-post-bl)
    (thumb-bl-place web-post-tl)
    (thumb-ml-place web-post-bl)
    (thumb-ml-place web-post-tl)
    (thumb-tl-place thumb-post-bl)
    (thumb-bl-place (translate (wall-locate1 -0.3 0) web-post-bl))
    (thumb-bl-place (translate (wall-locate1 -0.3 0) web-post-tl))
    (thumb-ml-place (translate (wall-locate1 -0.3 0) web-post-bl))
    (thumb-ml-place (translate (wall-locate1 -0.3 0) web-post-tl))
    (thumb-tl-place (translate (wall-locate1 -0.3 0) thumb-post-bl))
    ; should we remove bl thumb?
    )
   (triangle-hulls ;; Now main and thumb edges are smoother, connect remaining valley
    (thumb-bl-place web-post-bl)
    ; (thumb-bl-place (translate (wall-locate1 -0.3 0) web-post-bl))
    (key-place 0 cornerrow (translate (wall-locate3 0 0) web-post-bl))
    ; (thumb-bl-place web-post-tl)
    (thumb-tl-place thumb-post-bl)
    ; done?
    )
  ;  valley-clearance
  ;; end thumb-valley
   ))
(def thumb-walls
  (union  ;; currently being made for tipped-bowl version
   thumb-valley
   (wall-brace thumb-br-place -0.5 -1 web-post-bl thumb-br-place -1 -1 (translate [(- (displacement-edge rollin-default) base-offset) 0 4] web-post-bl))  ; outside left lower wall - psuedo between keys
   (wall-brace thumb-br-place -1 -1 web-post-br thumb-br-place -0.5 -1 web-post-bl)  ; outside left lower wall - right (more front) key
   (bottom-hull  ;; Improved wall-brace for outside left lower wall - connect to main from psuedo
    (thumb-br-place (translate (wall-locate3 -1  -1) (translate [(- (displacement-edge rollin-default) base-offset) 0 4] web-post-bl)))
    (thumb-br-place (translate (wall-locate2 -1  -1) (translate [(- (displacement-edge rollin-default) base-offset) 0 4] web-post-bl)))
    ; (thumb-bl-place (translate (wall-locate3  0  -1) web-post-bl))
    ; (thumb-bl-place (translate (wall-locate2  0  -1) web-post-bl))
    ; (left-key-place cornerrow 1 web-post)
    (left-key-place cornerrow 1 (translate (wall-locate1 -1 0) web-post))
    (left-key-place cornerrow 1 (translate (wall-locate3 -1 0) web-post))
    ; Leaves a gap due to displaced wall.
    )
   (hull  ;; Gap fill for connecting displaced wall and space between two bottom thumb keys.
    (thumb-br-place (translate (wall-locate1 -1 -1) (translate [(- (displacement-edge rollin-default) base-offset) 0 4] web-post-bl)))
    (thumb-bl-place (translate (wall-locate1 0 -0.5) web-post-br))
    (thumb-bl-place web-post-br)
    (thumb-br-place (translate (wall-locate1 -1 -1) web-post-bl))
    (thumb-br-place web-post-bl)
    ; connects to top wall over bl thumb key
    )
   (hull  ;; left wall passing around bl thumb key (near main keyboard section)
    (thumb-br-place (translate (wall-locate1 -1 -1) (translate [(- (displacement-edge rollin-default) base-offset) 0 4] web-post-bl)))
    (thumb-br-place (translate (wall-locate2 -1 -1) (translate [(- (displacement-edge rollin-default) base-offset) 0 4] web-post-bl)))
    (thumb-br-place (translate (wall-locate3 -1 -1) (translate [(- (displacement-edge rollin-default) base-offset) 0 4] web-post-bl)))
    (thumb-bl-place web-post-br)
    (thumb-bl-place (translate (wall-locate1 0 -0.5) web-post-br))
    (thumb-bl-place web-post-bl)
    (thumb-bl-place (translate (wall-locate1 0 -0.5) web-post-bl))
    ; (thumb-bl-place (translate (wall-locate2 0 -1) web-post-bl))
    ; (thumb-bl-place (translate (wall-locate3 0 -1) web-post-bl))
    (left-key-place cornerrow 1 (translate (wall-locate1 -1 0) web-post))
    (left-key-place cornerrow 1 web-post)
    ; connects to thumb-hood
    )
   ; Transition from Left to Front Thumb Wall
   (wall-brace thumb-br-place -1    -1 web-post-br   thumb-br-place 0    -0.25 web-post-br)  ; outside left lower wall cornering to front wall
   ;; Front Wall of Thumb
   (wall-brace thumb-br-place  0 -0.25 web-post-br   thumb-br-place 0.75  0 web-post-tr)  ; outside left lower wall cornering to front wall
   (wall-brace thumb-br-place  0.75  0 web-post-tr   thumb-mr-place 1.5   0 web-post-br)  ; front wall between bottom and middle
   (wall-brace thumb-mr-place  1.5   0 web-post-br   thumb-mr-place 1.5   0 web-post-tr)  ; front wall middle key
   (wall-brace thumb-mr-place  1.5   0 web-post-tr   thumb-tr-place 0.75  0 thumb-post-br)  ; front wall between middle and tp
   (wall-brace thumb-tr-place  0.75  0 thumb-post-br thumb-tr-place 0 -0.25 thumb-post-tr)  ; front wall top key
   ; Transition from Left to Front Thumb Wall
   (wall-brace thumb-tr-place  0 -0.25 thumb-post-tr thumb-tr-place -1 0.75 thumb-post-tr)  
   ;; Right Wall for Thumb Top keys
   (wall-brace thumb-tr-place -1    0.75 thumb-post-tr thumb-tr-place  -0.5  0.75 thumb-post-tl)  ; right wall of thumb, front section
   (wall-brace thumb-tr-place -0.5  0.75 thumb-post-tl thumb-tl-place 1.8 0.5 thumb-post-tr)  ; right wall of thumb, middle section
  ;  (wall-brace thumb-tl-place  0  1 thumb-post-tr thumb-tl-place  6  1 thumb-post-tl)  ; right wall of thumb, upper section
  ;  (triangle-hulls  ;; connect tl thumb to main keys front wall
  ;   (thumb-tr-place thumb-post-tl)
  ;   (thumb-tr-place (translate (wall-locate3 0 0) thumb-post-tl))
  ;   thumb-lastrow-connect
  ;   ; done?
  ;   )
  ;  (bottom-hull
  ;   thumb-lastrow-connect
  ;   (thumb-tr-place (translate (wall-locate3 0 0) thumb-post-tl)))  
  ; End union and thumb-walls
   ))  
(def back-y-edge 1)
(def right-x-edge 2)
(def right-case-wall
  (union
   (for [y (range 0 2) :when (.contains has-firstrow lastcol)] (key-wall-brace lastcol y right-x-edge 0 web-post-tr lastcol y       right-x-edge 0 web-post-br))
   (for [y (range 1 2) :when (.contains has-firstrow lastcol)] (key-wall-brace lastcol (dec y) right-x-edge 0 web-post-br lastcol y right-x-edge 0 web-post-tr))
   (for [y (range 0 1)] (key-wall-brace (last has-firstrow) y right-x-edge 0 web-post-tr (last has-firstrow) y       right-x-edge 0 web-post-br))
   (for [y (range 1 2)] (key-wall-brace (last has-firstrow) (dec y) right-x-edge 0 web-post-br (last has-firstrow) y right-x-edge 0 web-post-tr))
   (for [y (range 1 lastrow)] (key-wall-brace lastcol y right-x-edge 0 web-post-tr lastcol y       right-x-edge 0 web-post-br))
   (for [y (range 2 lastrow)] (key-wall-brace lastcol (dec y) right-x-edge 0 web-post-br lastcol y right-x-edge 0 web-post-tr))
   (key-wall-brace lastcol cornerrow 0 -1 web-post-br lastcol cornerrow right-x-edge 0 web-post-br))
)
(def left-case-wall
  (union
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
))
; back wall for columns that have a firstrow key (except middle finger)
(def back-case-wall-if-firstrow
  (union
   (for [x (range 0 ncols) :when (and (.contains has-firstrow x) (not= x (- middle-finger-col 1)) (not= x middle-finger-col))] (key-wall-brace x 0 0 back-y-edge web-post-tl x       0 0 back-y-edge web-post-tr))
   (for [x (range 1 ncols) :when (and (.contains has-firstrow x) (not= x (- middle-finger-col 1)) (not= x middle-finger-col))] (key-wall-brace x 0 0 back-y-edge web-post-tl (dec x) 0 0 back-y-edge web-post-tr))))
; back wall for columns that DO NOT have a firstrow key (but not including middle finger)
(def back-case-wall-if-not-firstrow
  (union
   (for [x (range 0 ncols) :when (not (or (.contains has-firstrow x) (= x (- middle-finger-col 1)) (= x middle-finger-col)))]       (key-wall-brace x 1 0 back-y-edge web-post-tl x       1 0 back-y-edge web-post-tr))
   (for [x (range 1 ncols) :when (not (or (.contains has-firstrow (- x 1)) (= x (- middle-finger-col 1)) (= x middle-finger-col)))] (key-wall-brace x 1 0 back-y-edge web-post-tl (dec x) 1 0 back-y-edge web-post-tr))))
; back wall middle finger (top row is determined by if-else depending on has-firstrow)
(def back-case-wall-middle-finger
  (union
   (if (.contains has-firstrow middle-finger-col)
     (union
      (key-wall-brace (dec middle-finger-col) 0 0 back-y-edge web-post-tl (dec middle-finger-col)       0 -1 back-y-edge web-post-tr)
      (key-wall-brace (dec middle-finger-col) 0 0 back-y-edge web-post-tl (dec (dec middle-finger-col)) 0   0 back-y-edge web-post-tr)
      (key-wall-brace middle-finger-col 0 -1 back-y-edge web-post-tl middle-finger-col       0  0 back-y-edge web-post-tr)
      (key-wall-brace middle-finger-col 0 -1 back-y-edge web-post-tl (dec middle-finger-col) 0 -1 back-y-edge web-post-tr))
     (union
      (key-wall-brace (dec middle-finger-col) 1 0 back-y-edge web-post-tl (dec middle-finger-col)       1 0 back-y-edge web-post-tr)
      (key-wall-brace (dec middle-finger-col) 1 0 back-y-edge web-post-tl (dec (dec middle-finger-col)) 1 0 back-y-edge web-post-tr)
      (key-wall-brace middle-finger-col 1 0 back-y-edge web-post-tl middle-finger-col       1  0 back-y-edge web-post-tr)
      (key-wall-brace middle-finger-col 1 0 back-y-edge web-post-tl (dec middle-finger-col) 1 -1 back-y-edge web-post-tr)))))
(def corners-case-wall-back-to-right
  (if (.contains has-firstrow lastcol)
    (key-wall-brace lastcol 0 0 back-y-edge web-post-tr lastcol 0 1 0 web-post-tr)
    (union
     (key-wall-brace lastcol 1 0 back-y-edge web-post-tr lastcol 1 right-x-edge 0 web-post-tr)
     (key-wall-brace (last has-firstrow) 1 right-x-edge 0 web-post-tr (inc (last has-firstrow)) 1 right-x-edge back-y-edge web-post-tl)
     (key-wall-brace (last has-firstrow) 0 0 back-y-edge web-post-tr (last has-firstrow) 0 right-x-edge 0 web-post-tr))))
(def front-case-wall
  (union
   (for [x (range 4 ncols) :when (not (.contains has-lastrow x))] (key-wall-brace x cornerrow 0 -1 web-post-bl x       cornerrow 0 -1 web-post-br))
   (for [x (range 5 ncols) :when (not (.contains has-lastrow x))] (key-wall-brace x cornerrow 0 -1 web-post-bl (dec x) cornerrow 0 -1 web-post-br))))
(def case-walls
  (union
   back-case-wall-if-firstrow
   back-case-wall-if-not-firstrow
   back-case-wall-middle-finger
   corners-case-wall-back-to-right
   right-case-wall
   left-case-wall
   front-case-wall
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
(defn screw-insert [column row bottom-radius top-radius height]  ;; TODO: Update to better approach
  (let [shift-right   (= column lastcol)
        shift-left    (= column 0)
        shift-up      (and (not (or shift-right shift-left)) (= row 0))  ; if row is 0, but column is not 0 or lastcol
        shift-down    (and (not (or shift-right shift-left)) (>= row lastrow)) ; if row is lastrow (or greater), but column is not 0 or lastcol
        shift-thumb   (and (or shift-right shift-left) (>= row lastrow)) ; if row is lastrow (or greater) AND the column IS 0 or lastcol
        position
        (if (and shift-left shift-thumb) (key-position column row (map + (wall-locate2 0 0) [-75 -2 -38] ))   ; if nrows=4, [-67 0 -38]
        (if (and shift-right shift-thumb) (key-position column row (map + (wall-locate2 0 0) [-70 6 -34] ))  ; if nrows=4,  [-70 7 -36]
            (if shift-up     (key-position column row (map + (wall-locate2  -0  -0.5) [0 (/ mount-height 2) 2]))
                (if shift-down  (key-position column row (map - (wall-locate2  0 -8) [-1 (/ mount-height 2) 11]))   ; if nrows=4, [-7 (/ mount-height 2) -14]
                    (if (and shift-left (>= row cornerrow)) (map + (left-key-position row 1) (wall-locate3 0 0) [-9 2 0])
                    (if shift-left (map + (left-key-position row 1) (wall-locate3 0 0) [3 (/ mount-height 2) 11])
                        (key-position column row (map + (wall-locate2  0  1) [(+ (/ mount-width 2) 0) 0 0] ))))))))]  ; if nrows=4, [(+ (/ mount-width 2) 2) 0 -3]
    (->> (screw-insert-shape bottom-radius top-radius height)
         (translate [(first position) (second position) (/ height 2)])
    )))
(defn screw-insert-all-shapes [bottom-radius top-radius height]
  (union
   (screw-insert 0 (+ 0.3 (* 0.5 (- nrows 3))) bottom-radius top-radius height)  ; back/top left      => shift-left  ;; rows=4, x=0.8, rows=5, x=1.3
   (screw-insert 0 (+ cornerrow 0.4)           bottom-radius top-radius height)  ; front/bottom left  => shift-left
   (screw-insert 2 (+ lastrow 0)               bottom-radius top-radius height)  ; front/bottom right => shift-down
   (screw-insert 2 0                           bottom-radius top-radius height)  ; back/top center    => shift-up
   (screw-insert lastcol 0                     bottom-radius top-radius height)  ; back/top right     => shift-right
   (screw-insert lastcol (+ lastrow 0.1)       bottom-radius top-radius height)  ; thumb screw        => shift-right & shift-thumb
   (screw-insert 0 (+ lastrow 0.1)             bottom-radius top-radius height)  ; thumb screw        => shift-left & shift-thumb
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
                    (main-key-cleanup true)
                    thumb-walls
                    thumb
                    thumb-connectors
                    ; case-walls
                    (union
                    back-case-wall-if-firstrow
                    back-case-wall-if-not-firstrow
                     back-case-wall-middle-finger
                     corners-case-wall-back-to-right
                    right-case-wall
                    left-case-wall
                    front-case-wall
                     )
                    ; (difference (union 
                    ;             ;  case-walls
                    ;              (union
                    ;               back-case-wall-if-firstrow
                    ;               back-case-wall-if-not-firstrow
                    ;               back-case-wall-middle-finger
                    ;               corners-case-wall-back-to-right
                    ;               right-case-wall
                    ;               left-case-wall
                    ;               front-case-wall
                    ;               )
                    ;              (main-key-cleanup true)
                    ;              thumb-walls
                    ;              screw-insert-outers
                    ;              teensy-holder
                    ;              usb-holder
                    ;              )
                    ;             rj9-space
                    ;             usb-holder-hole
                    ;             screw-insert-holes
                    ;             )
                    ; rj9-holder
                    ; wire-posts
                    ; thumbcaps
                    ; caps
                    )
                   (translate [0 0 -20] (cube 350 350 40))
                  ))  ; end model-right
(defn reset-thumb-placement [object]
  (translate [0 0 (* 0.5 mount-height)]
             (rotate (deg2rad -40) [1 0 0]
                     (rotate (deg2rad 25) [0 1 0]
                             (rotate (deg2rad 45) [0 0 1]
                                     (rotate deflect [0 0 -1]
                                             (rotate slope-thumb [-1 0 0]
                                                     (rotate thumb-tent [0 -1 0]
                                                             (rotate (deg2rad 90) [0 -1 0]
                                                                     (rotate (deg2rad 90) [0 0 1]
                                                                             (translate (map * [-1 -1 -1] thumborigin)
                                                                                        object) ; end translate
                                                                             )))))))) ; end nested rotates
  ))  ; end reset-thumb-placement
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
                         valley-clearance
                         )))
(spit "things/right-no-thumb.scad"
      (write-scad (difference
                   (union
                    key-holes
                    connectors
                    (main-key-cleanup false)
                    valley-clearance
                    ; case-walls
                    ; (difference (union case-walls
                    ;                    ( main-key-cleanup false)
                    ;                    screw-insert-outers
                    ;                    teensy-holder
                    ;                    usb-holder
                    ;                    )
                    ;             rj9-space
                    ;             usb-holder-hole
                    ;             screw-insert-holes
                    ;             )
                    ; rj9-holder
                    ; wire-posts
                    ; thumbcaps
                    ; caps
                    )
                   (translate [0 0 -20] (cube 350 350 40)))))
(spit "things/right-plate.scad"
      (write-scad
       (union
        (translate [0 0 (* 0 wall-thickness)]
                   (extrude-linear {:height (* 1 wall-thickness) :twist 0 :convexity 0}  ; make height 0.1 if a 2d output is desired
                                   (cut
                                    (translate [0 0 (* -1 wall-thickness)]
                                               (difference
                                                (union case-walls   ; to fill in: (hull case-walls)
                                                       (main-key-cleanup true)
                                                       thumb-walls  ; to fill in: (hull thumb-walls)
                                                       teensy-holder
                                                       ; rj9-holder
                                                       screw-insert-outers)
                                                (translate [0 0 -10] screw-insert-screw-holes))))  ; end cut
                                   )  ; end extrude-linear
                   )
        ;; would like to add the filled in bottom plate here. The above notes to fill do not work when the walls curve back in.
        )))
(spit "things/thumbpad.scad"
      (write-scad
       (reset-thumb-placement
        (union
         thumb
         thumb-connectors
         thumb-valley
        ;  (main-key-cleanup true)
          ; thumbcaps
         ; thumb-walls
         ;  case-walls
         )  ; end union
       )  ; end reset-thumb-placement
       ))

(defn -main [dum] 1)  ; dummy to make it easier to batch