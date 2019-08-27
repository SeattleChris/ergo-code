# Running plans for tasks

## Things to Tackle

* Generate a Heavy Curve with no Number/Function row (4x5 and 4x6).

* Curvature between rows options
  * Original curves all rows the same. Assumes they will curve in.
  * Different curve parameters for home columns vs. outside "stretch" columns
    * May need to adjust computation of adjusting position when curving out.
* Modified variations of which columns have a lastrow button
  * Originally was columns 2 (middle finger) and 3 (ring finger)
  * Can now add lastrow buttons to any columns. Assumes they are continuos (no gaps).
* Improve algorithm for the z-offset.
  * Dependent on number of columns, tilt angle, row and column curve, etc.
  * It is not linear with number of columns when all else held consistent.
* Modified variations, allowing some columns NOT to have their top row key.
  * Originally all columns got top row
  * Column 5 (pinkie finger home) may not want a top row key
  * Any additional columns after the pinkie home row may not want a top row key
  * Column 0 (reaching with pointer finger) may not want a top row key
* Improve screw hole placement method.
  * Better computation for the main key section.
  * Separate out and improve method for thumb cluster screws.
* Create different thumb cluster categories to allow different designs for thumb keys.
  * Original Dactyl
  * Original Manuform
  * Manuform with thumb keys more normally aligned
  * Thumb Bowl (rotated)

## Ready to test for fit

* Heavy Curve (5x5) model: fix the blank space for hood over thumb section.
* Heavy Curve (5x5) model: fix the lastrow keys since the walls block the key (on column 2 to 3)
* Updated column offset. Should fix depth, height, and lastrow collisions.
* Adjusted thumb cluster orientation and depth placement.
* Heavy Curve (5x6) model: all 5x5 fixes, added lastrow column, 2 row curve parameters.
* All models: wall-thickness can be increased. Doesn't waste material since less support structure will be needed.
  * was 2.0, now 3.5 which matches the web-thickness
* All future models: depth (z-offset) can be dropped even more. May need to adjust tenting parameters.
  * this changes drastically depending on the column-offset or any of the tilt settings.
* Heavy Curve (5x5) model: move, or change thumb-valley so thumb cluster does not block the main section key switches
* Curvature between rows options
  * Currently set to really low
  * Maybe the goal of this is best dealt with by column-offset
