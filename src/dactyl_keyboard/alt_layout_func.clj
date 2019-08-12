
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
    (thumb-tl-place thumb-post-tl)))  ; end union for normal-align
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
    (thumb-tl-place thumb-post-tl))))
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
    (key-place 3 lastrow web-post-tl))))  ;; end extra-key-top-gap


(def block-thumb-hood-top-wall
  (hull  ;; thumb-hood top wall as a block: small section of thumb bl-bl to bl-bl of main. 
   (key-place 0 cornerrow web-post-tl)
   (key-place 0 cornerrow (translate (wall-locate1 0 0) web-post-tl))
   (key-place 0 cornerrow web-post-bl)
   (key-place 0 cornerrow (translate (wall-locate1 0 -0.5) web-post-bl))
   (thumb-bl-place web-post-bl)
   (thumb-bl-place (translate (wall-locate1 0 -0.5) web-post-bl))
   (thumb-bl-place (translate (wall-locate3 0 -1) web-post-bl))
   (thumb-bl-place (translate (wall-locate2 0 -1) web-post-bl))
   (key-place 0 cornerrow (translate (wall-locate3 0 -1) web-post-bl))
    ; Do we really want to fill in so much? 
   )
; End block-thumb-hood-top-wall  
  )


(defn old-apply-key-geometry [translate-fn rotate-x-fn rotate-y-fn column row shape]
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
                                (translate-fn [0 (second (column-offset column)) 0]))]
    (->> (case column-style
           :orthographic placed-shape-ortho
           :fixed        placed-shape-fixed
           placed-shape)
         (rotate-y-fn  tenting-angle)
         (translate-fn [0 0 keyboard-z-offset]))))
