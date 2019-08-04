# The Dactyl-ManuForm Keyboard

This is a fork of the [Tom Short's ManuForm](https://github.com/tshort/dactyl-keyboard) keyboard, which itself is a fork of the [Dactyl](https://github.com/adereth/dactyl-keyboard), a parameterized, split-hand, concave, columnar, ergonomic keyboard. This work builds off of the ideas of the [ManuForm](https://github.com/jeffgran/ManuForm) by Jeff Gran with discussions on ([geekhack](https://geekhack.org/index.php?topic=46015.0)).

![Imgur](http://i.imgur.com/LdjEhrR.jpg)

## Thumb Cluster

For ergonomic keyboards of this design there is a desire to make better use of the thumb, giving it multiple keys to press instead of how a traditional keyboard only has a single space bar button to be used by one or both thumbs.
This fork introduced a new design for the keys pressed by the thumbs. This collection of extra keys for the thumbs has come to be referred to as the "Thumb Cluster" within the mechanical keyboard community.

Here we are introducing the Thumb Bowl design for the thumb cluster. An option for the Manuform thumb cluster design.

## Paramaterized Settings

The keyboard is paramaterized to allow adjusting the following:

* Rows: 4 - 6 (or more)
* Columns: 4 and up
* Row curvature
* Column curvature
* Row tilt (tenting)
* Column tilt
* Column offsets
* Depth (raised off the table)
* Thumb cluster design style:
  * Manuform
  * Thumb-bowl

The 40% size (set as 4x5) and the 60% size (set as 5x6) models by Tom Short implement the Manuform thumb cluster design. The 40% version has a bit more tenting than the Dactyl. The Heavy Curve model (set as 5x5) is for those who want a deep bowl design for the See the following model files for configurations that may be most common:

* [40% size, (4x5)](https://github.com/SeattleChris/dactyl-keyboard/blob/master/things/right-4x5.stl)
* [60% size, (5x6)](https://github.com/SeattleChris/dactyl-keyboard/blob/master/things/right-5x6.stl)
* [Heavy Curve (5x5)](https://github.com/SeattleChris/dactyl-keyboard/blob/master/things/heavy-curve-5x5.stl)
* [Current Model](https://github.com/SeattleChris/dactyl-keyboard/blob/master/things/chris/right.stl)

## Features and Patches currently being worked on

* Plate thickness needs to be researched, as well as adjusting the size of the latching leaf.
* All models: wall-thickness can be increased. Doesn't waste material since less support structure will be needed.
* All future models: depth (z-offset) can be dropped even more.
* Heavy Curve (5x5) model: fix the blank space for hood over thumb section.
* Heavy Curve (5x5) model: fix the lastrow keys since the walls block the key (on column 4)
* Heay Curve (5x5) model: adjust the column offset (middle finger is too low, some other tweaks).
* Heavy Curve (5x5) model: move thumb cluster to not block the main section key switches (needs more depth).
* Heavy Curve (5x5) model: raise the thumb cluster for better fit.
* Generate a Heavy Curve with no Number/Function row (4x5).
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

## Assembly

### Generating a Design

#### Setting up the Clojure environment

* [Install the Clojure runtime](https://clojure.org)
* [Install the Leiningen project manager](http://leiningen.org/)
* [Install OpenSCAD](http://www.openscad.org/)

#### Generating the design

* Run `lein repl`
* Load the file `(load-file "src/dactyl_keyboard/dactyl.clj")`
* This will regenerate the `things/*.scad` files
* Use OpenSCAD to open a `.scad` file.
* Make changes to design, repeat the above `load-file` command, OpenSCAD will watch for changes and rerender.
* When done, use OpenSCAD to export STL files

##### Tips

* [Example designing with clojure](http://adereth.github.io/blog/2014/04/09/3d-printing-with-clojure/)

### Printing

Pregenerated STL files are available in the [things/](things/) directory.
When a model is generated, it also generates a `.scad` model for a bottom plate.
This can be exported to a DXF file in OpenSCAD.
The [things/](things/) directory also has DXF files for the bottom plate.
When laser cut, some of the inside cuts will need to be removed.

This model can be tricky to print.
It's wide, so I've had problems with PLA on a Makerbot with edges warping.
This can cause the printer to think its head is jammed.
Even if it successfully prints, warping can cause problems.
On one print, the RJ-9 holder was squished, so I had to cut down my connector to fit.

If printed at Shapeways or other professional shops, I would not expect such problems.

#### Thingiverse

[The 4x5 STL left/right pair](https://www.thingiverse.com/thing:2349390) from the [things/](things/) directory is in the thingiverse for public printing

### Build Notes

For guidance from previous versions, see the instructions for wiring and general build for the [original Dactyl](/guide/) and for [Tom Short's Manuform](/manuform/).

#### Wiring

Here are materials I used for wiring.

* Two Arduino Pro Micros
* [Heat-set inserts](https://www.mcmaster.com/#94180a331/=16yfrx1)
* [M3 wafer-head screws, 5mm](http://www.metricscrews.us/index.php?main_page=product_info&cPath=155_185&products_id=455)
* [#30 wire](https://www.amazon.com/dp/B07BLZ333V/)
* [3-mm cast acrylic](http://www.mcmaster.com/#acrylic/=144mfom)
* [1N4148 diodes](https://www.amazon.com/gp/product/B00LQPY0Y0)
* [Female RJ-9 connectors](https://www.amazon.com/gp/product/B01HU7BVDU/)

Following the traditional approach, the row connections can be made soldering the legs of the diodes. The columns can be wired with a bare wire, or insulated wire stripped at the connection points.

![Imgur](http://i.imgur.com/7kPvSgg.jpg)

The 3D printed part is the main keyboard.
You can attach a bottom plate with screws.
  The case has holes for heat-set inserts designed to hold 3- to 6-mm long M3 screws.
  Then use wafer-head screws to connect a bottom plate.
A bottom plate is technically optional, but can help clean things up and protect any dangling wires.
You need something on the bottom to keep the keyboard from sliding around such as a rubber pad (or PlastiDip the bottom).

This is how the rows/columns wire to the keys and the ProMicro
![Wire Diagram](https://docs.google.com/drawings/d/1s9aAg5bXBrhtb6Xw-sGOQQEndRNOqpBRyUyHkgpnSps/pub?w=1176&h=621)

##### Alternative row-driven wiring diagram for ProMicro

NOTE: you also make sure the firmware is set up correctly (ex: change row pins with col pins)

![Left Wire Diagram](/resources/dactyl_manuform_left_wire_diagram.png)
![Left Wire Diagram](/resources/dactyl_manuform_right_wire_diagram.png)

#### Firmware

Firmware goes hand in hand with how you wire the circuit. Some firmware options include:

* Tom Short's adaptation of QMK firmware [here](https://github.com/tshort/qmk_firmware/tree/master/keyboards/dactyl-manuform). This allows each side to work separately or together, and shows connections for the Arduino Pro Micro controllers.
* Keyboard firmware in Python [here](https://github.com/KMKfw/kmk_firmware)

## License

Copyright © 2015-2019 Matthew Adereth, Tom Short, Chris L Chapman

The source code for generating the models (everything excluding the [things/](things/) and [resources/](resources/) directories is distributed under the [GNU AFFERO GENERAL PUBLIC LICENSE Version 3](LICENSE).  The generated models and PCB designs are distributed under the [Creative Commons Attribution-NonCommercial-ShareAlike License Version 3.0](LICENSE-models).
