# Running plans for tasks

## Things to Tackle

* Plate thickness needs to be researched, as well as adjusting the size of the latching leaf.
* Heavy Curve (5x5) model: fix the lastrow keys since the walls block the key (on column 3 to 4)
* Heay Curve (5x5) model: adjust the column offset (middle finger too low, pinkie too high, some other tweaks).
* Heavy Curve (5x5) model: raise the thumb cluster for better fit.
* Generate a Heavy Curve with no Number/Function row (4x5).

* Curvature between rows options
  * Currently curves all rows
  * Option to only curve rows outside of home columns
* Modified variations of which columns have a lastrow button
  * Originally was columns 2 (middle finger) and 3 (ring finger)
  * Option for column 5 (pinkie finger home)
  * Option for column 1 (pointer finger home)
* Modified variations, allowing some columns NOT to have their top row key.
  * Originally all columns got top row
  * Column 5 (pinkie finger home) may not want a top row key
  * Any additional columns after the pinkie home row may not want a top row key
  * Column 0 (reaching with pointer finger) may not want a top row key
* Improve screw hole placement method.
* Create different thumb cluster categories to allow different designs for thumb keys.
  * Original Dactyl
  * Original Manuform
  * Manuform with thumb keys more normally aligned
  * Thumb Bowl (rotated)


## Ready to test for fit

* Heavy Curve (5x5) model: fix the blank space for hood over thumb section.
* Heavy Curve (5x5) model: fix the lastrow keys since the walls block the key (on column 2 to 3)
* All models: wall-thickness can be increased. Doesn't waste material since less support structure will be needed.
  * was 2.0, now 3.5 which matches the web-thickness
* All future models: depth (z-offset) can be dropped even more. May need to adjust tenting parameters.
  * this changes drastically depending on the column-offset or any of the tilt settings.
* Heavy Curve (5x5) model: move, or change thumb-valley so thumb cluster does not block the main section key switches
* Curvature between rows options
  * Currently set to really low
  * Maybe the goal of this is best dealt with by column-offset
