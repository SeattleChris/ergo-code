# ergo-code

Better Computer Programmer Input Device:
This is for exploring and creating an ergonomic and efficient setup for user input, with a focus on improving the input interface when computer programming. This includes notes on custom keyboards and other user input devices. When writing code, there are some characters that are far more common than typical writing, as well as room for improvements for layout for typical english punctuation and access to useful characters. There is a special attention paid to to general navigation and modifer keys and how they can be set up for more efficient user interface.

Would be great to have:
parameterized, split-hand, concave, columnar, ergonomic keyboard.

## Layout: Considerations in where to place keys

- Where should the most popular keys go?: Home, stronger fingers
- Where do modifers go?: Thumb, pointer reach? pinky?
- Thumb section: Most common characters/letters or no No letters?
- Arrow Keys:
  - Seperate keys or HJKL (vim style) or IJKL (inverted T)
    - Sepeate Arrow Keys is not compatible with minimum key keyboard, unless maybe if Maltron thumb keys
    - IJKL seems slighty better [according to here](http://xahlee.info/kbd/vi_hjkl_vs_inverted_t_ijkl_arrow_keys.html)

### Layout: Modifiers and Possible Thumb Keys

Most important keys:

- Shift (left and right): keep on pinky? ... Also on thumb to adjust?
- Ctrl (left and right): Both Thumbs
- Alt (left and right): Both Thumbs
- Super (both sides): Both Thumbs
- Tab: Left Thumb
- Backspace: Left Thumb
- Layer Mode (single hit, one or two?): Left Thumb
- Space (just right): Right Thumb
- Del: Right Thumb
- Underscore / dash: Right Thumb

Less important Func/Nav Buttons:

- Compose (just one): Right First Finger Extra Reach
- AltGr (just one): First Finger extra Reach
- Shift (left and right): keep on pinky? ... Also on thumb to adjust?
- Hyper (one or both sides?):
  - Front
  - Top

## Layout: Character Popularity for Coding

What are the most popular characters and keys used in writing computer programs?

- overall
- by language
- One source: http://xahlee.info/comp/computer_language_char_distribution.html
- Can we find what is the most popular characters in code on GitHub?

## Keyboard Parts

- [Key Switches](key-switches.md)
- [Key Caps](keycaps.md)
- [Sound Dampening - O-Rings](sound-dampen.md)
- Keyboard base
- [Cable](cable.md)
- Wrist pads
- Tool: Key cap puller - The wire puller is better.

## Additional Resources and Forums

- [Approach to Optimizing Keyboard Layouts](https://www.allthingsergo.com/case-study-one-approach-optimizing-ergonomic-keyboard-layouts/)
- [Keyboard Layout Editor](http://www.keyboard-layout-editor.com/)
- [keyboard enthusiasts](http://geekhack.org)

## Build Notes

### 3D modeling software

- OpenSCAD
  - Issues: not functional, no namespaces, all kinds of messes.

### On keyboard controllers

Look into: Teensy 2.0, Teensy++ 2.0, Teensy 3.x

## My User Story/Preferences

- I want good access to doc-nav-keys: PgUp, PgDn, Home, End. Possibly in a arrow key layout
- I want good access to arrow keys
- I want to be able to do modifers for both the arrow and other doc-nav-keys.
- I like having a numpad for entering larger numbers, or a series of numbers
  - Numpad should have easy access to period, comma, math operators, enter.
- I want backspace and del keys to be easy to reach (possibly thumb section).
- Easy reach: tab, ~/`, (, ), [{, ]}, /, \, period, comma
- Both hands should have access to shift, ctrl, alt(meta), super, hyper, and ??
  - Space Cadet modifers:
    - bucky bits: Control, Meta (Alt), Super, Hyper.
    - shift: Shift, Top, Front (Greek).
    - other?: AltGraph, Compose
- Potentially want a compose key (start a sequence of characters)
- I want full 12 function keys, PrtSc/SysRq, Ins/ScrLk, Pause/Break.
- Potentially I may also want some additional macros.
- Led for locks: Caps, Num, scroll?, ??
- Trackball on thumb, left, right, center buttons. (fwd & back not needed)
- Scrollwheel

## Keyboard History

- adm-3A
  - 1974
  - huge influence on Vi
  - h, j, k, l have arrows, left, down, up, right
  - Tilda (~) is on home key in upper right
  - ctrl and shift present
- Symbolics "Space Cadet"
  - 1974
  - huge influence on Emacs
  - physical keys: hyper, super, meta, ctrl
  - case, greek letters, symbols
- Malton
  - 1977
  - thumb cluster!
  - split key wells angled out
  - keys are in concave arc
  - had it's own layout ('e' key under thumb)
- Kinesis Advantage
  - 1992
  - similiar to Malton 2 hand keyboard
- ErgoDox
  - 2011
  - only as a kit
  - split, but not concave keys.
  - some thumb keys
- Dactyl
  - parameterized, split-hand, concave, columnar, ergonomic keyboard.
- DataHand: Very different setup
  - Each finger has 5 switches: N, S, E, W, down. Except down, these are paddle triggers.
  - Claims this approach is less repetitive, shorter, gentler, and less stressful on the fingers.
- Strap
  - Different input device, worn on one hand, using motion sensors. Best if used against a hard surface
  - Does not seem to have the potential speed of physical keys?
