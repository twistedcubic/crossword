# Crossword puzzle generator

This turns comma-separated word-clue pairs into crossword puzzles, along with solutions and clues.

## Examples

The generator can take inputs from simple to complex, e.g.:

- APPLE, green, BANANA, yellow
- A PRIORI, Not empirical, POSED, Not natural-looking, ARMHOLE, Sometimes hard-to-find shirt opening, ROUNDER, Drunkard, ALLUDES, Makes reference to, CZAR, Government policy chief, RIP, Headstone inits, CAMERA, Smartphone feature, FINESSE, Deft touch, GRANNIES, Ones in rocking chairs stereotypically, DOPE, Pretty cool in slang, GREECE, Mamma Mia setting, TREAT, Goody, ZOOM, Speed along, BURRITO, Chipotle choice, MENUS,Things in restaurant windows, FEAT, Herculean act 

For instance, "this,opposite of that,is,common verb,awesome,cool" creates:
![puzzle1](https://github.com/twistedcubic/crossword/blob/master/data/puzzle0.png)

[Try it out here](http://yihedong.me/crossword).

## Build
The Ant build can be run with the target run as `ant run`.

## Behind the scenes
The algorithm uses dynamic programming to efficiently generate possible puzzle tile placements, it then ranks them based on number of tile intersections, word placement distributions, etc, finally returning the best. Here is [the servlet and web frontend code](https://github.com/twistedcubic/crosswordServlet).

## Inspiration
This app was inspired by my granddad's love for puzzles. My family used it to created custom puzzles to keep him entertained during recovery from surgery.