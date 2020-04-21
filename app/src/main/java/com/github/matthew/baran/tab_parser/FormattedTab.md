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

### Artist and Title

### Chord count