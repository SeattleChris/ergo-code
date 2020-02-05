# Ongoing plans for tasks

## Features added by this project

* Curvature between rows options
  * Original curves all rows the same. Assumes they will curve in.
  * Different curve parameters for home columns vs. outside "stretch" columns
    * May need to adjust computation of adjusting position when curving out.
* Modified variations of which columns have a lastrow button
  * Originally was columns 2 (middle finger) and 3 (ring finger)
  * Can now add lastrow buttons to any columns. Assumes they are continuos (no gaps).
* All models: wall-thickness can be increased. Doesn't waste material since less support structure will be needed.
  * was 2.0, now 3.5 which matches the web-thickness

## Things to Tackle

* Modified variations, allowing some columns NOT to have their top row key.
  * Originally all columns got top row
  * Column 5 (pinkie finger home) may not want a top row key
  * Any additional columns after the pinkie home row may not want a top row key
  * Column 0 (reaching with pointer finger) may not want a top row key
* Improve screw hole placement method.
  * Better computation for the main key section.
  * Separate out and improve method for thumb cluster screws.
* Improve algorithm for the z-offset.
  * Dependent on number of columns, tilt angle, row and column curve, etc.
  * It is not linear with number of columns when all else held consistent.
* Create different thumb cluster categories to allow different designs for thumb keys.
  * Original Dactyl
  * Original Manuform
  * Manuform with thumb keys more normally aligned
  * Thumb Bowl (rotated)
* Which columns are actually good to have the bonus lastrow buttons?
  * Not any reach column. Probably not pinkie. pointer may not fit.
* ? Generate a Heavy Curve with no Number/Function row (4x5 and 4x6)?

## Modifications for Jan 2020 not yet Reviewed

* Updated column offset. Should fix depth, height, and lastrow collisions.
* Adjusted thumb cluster orientation and depth placement.
* All future models: depth (z-offset) can be dropped even more. May need to adjust tenting parameters.
  * this changes drastically depending on the column-offset or any of the tilt settings.
* Curvature between rows options
  * Currently set to really low
  * Maybe the goal of this is best dealt with by column-offset

## Goals for Feb 2020 build 01

* [x] Slightly smaller/tighter key switch holes: old 14.3 | now 14.15
* [x] Reduce curve (front to back) slightly: was 36 (ttl 90) | now 34 (ttl 85) | could be 33 (ttl 82.5)
* [x] Roll home row back (more trigger action) - lastrow is perpendicular: was nrows-3.5|now nrows-3.75
* [x] Pinkie home has no top row.
* [x] Pinkie home has or does not have lastrow? (has lastrow)
* [x] Pinkie bonus column has 2-3 keys? (3)
* [x] Wall thickness (part that drops to ground) updated: was 2 | Jan 3.5 | now 2.5
* [x] Back Wall shape: allow extra left room for middle finger since it typically is furthest back column.
* [x] Wall offset: controls how far way from the plate the wall (drop to the floor) is set.
  * [x] wall-z-offset: original -15 | Jan -12 | now -14
  * [x] wall-xy-offset: original  ? | Jan   3 | now   6
* [x] Soften by rounding out bottom left corner (the thumb section BR key)
* [ ] ? Back Wall shape: Continue the left curve around top, connecting to middle finger left edge.
* [ ] Adjust pinkie and ring finger placement
* [ ] Adjust wall connection to thumb: Bonus row pinkie connect to thumb section
* [ ] Fix blank wall: thumb slope off off to right wall
* [ ] Can we fit a lastrow key for home pointer?

## Ready to test for fit
