# FormattedTab Class
## What is the goal here?
### Basic Formatting
Chords and lyrics should be colored differently for readability.  For now, that means blue
for chords and black for lyrics.
### Chorus
Sometimes the 'Chorus' keyword appears instead of re-writing the chords and lyrics of the chorus.
A formatted tab should find those cases and insert the chords/lyrics, so the tab can be played
continuously while scrolling.

* Identify chords/lyrics associated with the first chorus
    * A blank line may or may not follow the first Chorus anchor
    * The Chorus keyword may appear inline with chords
    * Chord/lyric pairs that follow are copied until a blank line appears (not chord/lyrics)
* Identify subsequent chorus keywords that need chorus insertion
    * Chorus keyword appears at the end of the text (no further chords/lyrics found)
    * Chorus keyword appears and no chords/lyrics exist until the next keyword
    * A blank line follows the Chorus keyword, except when the first chorus contained a blank line

### Tablature Lines
Tablature lines provide specific notes to play for .  Those lines typically take up a larger amount
of space in the tab than they actually take to play.  Lots of tablature lines can artificially speed-up
or slow-down tab scrolling.  For now, tablature lines are removed to estimate scrolling time purely on
lyrics and chord sections.  In the future, it would be cool to display tablature in a split-screen or
overlay of some kind.

* Tablature lines are identified by a large number of dashes contained in a line

### Artist and Title
Artist and Title information is often contained at the beginning of a tab file.  If the first lines with
text content have the form "artist - title" or "artist" followed by blank lines then "title", those lines
will be used to populate the artist/title information for the tab.