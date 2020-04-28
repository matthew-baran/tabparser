package com.github.matthew.baran.tab_parser;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FormattedTab
{
    private AppCompatActivity context = null;

    private ArrayList<String> lines = new ArrayList<>();
    private String artist = "Unknown Artist";
    private String title = "Unknown Title";
    private Integer artist_title_idx = -1;
    private ArrayList<String> chorus = new ArrayList<>();
    private ArrayList<Integer> chorus_idx = new ArrayList<>();
    private Integer chord_count;
    private SpannableStringBuilder formatted_text;

    private boolean extra_blank = false;

    // @formatter:off
    private final Pattern chord_pattern =
            Pattern.compile(
                    "(?<=^|\\s)" + "[A-G][b#]?" +   // A, Bb, C#
                    "(min|m)?" +                    // Amin, Am
                    "\\d*" +                        // C5, B7
                    "(?i)(dim\\d*" +                // Adim,  Gdim9
                    "|add\\d+" +                    // Cadd9,  B7add6
                    "|sus\\d*" +                    // Dsus, Dsus4
                    "|aug|\\+" +                    // Eaug, E+
                    "|(M|maj)\\d*)?" +              // Cmaj, Cmaj7
                    "([b#]\\d+)?" +                 // Cmaj7b9, AmSus2#7
                    "([/\\\\][A-G][b#]?)?" +         // C/E, G7\A, D/F#
                    "(?=([:;,.]|\\s|$))");

    private final Pattern anchor_pattern = Pattern.compile(
                    "(?i)(Chorus" +
                    "|(\\w*\\s*)?Verse\\s*\\w*" +
                    "|Bridge|Intro|Outro|Interlude" +
                    "|Capo\\s*(I+|\\d+)|(Pre-)?chorus)(\\s|$)");

    private final Pattern chorus_pattern = Pattern.compile("(?i)Chorus(\\s|$)");
    private final Pattern blank_pattern = Pattern.compile("^\\s*$");
    // @formatter:on

    FormattedTab(AppCompatActivity context, File file)
    {
        this.context = context;

        try
        {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null)
            {
                lines.add(line);
            }
            br.close();
        } catch (IOException e)
        {
            // TODO: Add error handling.
            lines = null;
            this.context = null;
            return;
        }

        findArtistAndTitle();
        findChorusLines();
        findChorusLocations();
        formatTab();
    }

    SpannableStringBuilder getFormattedText()
    {
        return formatted_text;
    }

    String getTitle()
    {
        return title;
    }

    String getArtist()
    {
        return artist;
    }

    private void findArtistAndTitle()
    {
        Iterator<String> it = lines.iterator();
        int ctr = 0;

        while(it.hasNext())
        {
            String str = it.next();
            if (containsAnchor(str) || containsChords(str))
            {
                return;
            }
            if (!str.isEmpty())
            {
                // Recover artist/title from "artist - title" structure
                if (str.contains("-") && str.indexOf('-') == str.lastIndexOf('-'))
                {
                    String[] parsed = str.split("\\s*-\\s*");

                    artist = cleanString(parsed[0]);
                    title = cleanString(parsed[1]);
                    artist_title_idx = ctr;
                    return;
                }
                else
                {
                    // Try to recover artist/title from consecutive lines
                    artist = cleanString(str);
                    while (it.hasNext())
                    {
                        ++ctr;
                        str = it.next();
                        if (!str.isEmpty())
                        {
                            if (containsAnchor(str) || containsChords(str))
                            {
                                artist = "Unknown Artist";
                            }
                            else
                            {
                                title = cleanString(str);
                                artist_title_idx = ctr;
                            }
                            return;
                        }
                    }
                }
            }
            ++ctr;
        }
    }

    private String cleanString(String str)
    {
        // Trim whitespace and force capitalization of first letters only
        str = str.toLowerCase().trim();

        if (str.isEmpty())
        {
            return str;
        }

        String[] tokens = str.split("\\s+");
        str = "";
        for (String t : tokens)
        {
            if (t.isEmpty())
            {
                continue;
            }
            if (t.length()==1)
            {
                str += t.toUpperCase() + " ";
                continue;
            }
            str += t.substring(0, 1).toUpperCase() + t.substring(1) + " ";
        }
        str = str.substring(0, str.length()-1);
        return str;
    }

    private void formatTab()
    {
        formatted_text = new SpannableStringBuilder();
        int ctr = 0;
        boolean is_front_matter = true;

        // Start with some whitespace so the content has time to scroll past.
        formatted_text.append("\n\n\n");

        for (String line : lines)
        {
            if ((isBlank(line) && is_front_matter) || ctr <= artist_title_idx)
            {
                ++ctr;
                continue;
            }
            if (!chorus_idx.isEmpty() && ctr == chorus_idx.get(0))
            {
                formatted_text.append(formatChorus());
                chorus_idx.remove(0);
            }
            else
            {
                formatted_text.append(formatLine(line));
                formatted_text.append('\n');
            }
            is_front_matter = false;
            ++ctr;
        }
    }

    private SpannableString formatLine(String line)
    {
        SpannableString span_str = new SpannableString(line);
        span_str.setSpan(new TextAppearanceSpan(context, R.style.Lyrics), 0, span_str.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        Matcher string_matcher = chord_pattern.matcher(line);

        int num_tokens = line.split("\\s+").length;
        int num_chords = 0;

        while (string_matcher.find())
        {
            span_str.setSpan(new TextAppearanceSpan(context, R.style.Chords),
                    string_matcher.start(), string_matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            num_chords++;
        }

        // Try to eliminate chord style for capitalized lyrics "A", "Am", etc...
        if (num_tokens - num_chords > 2)
        {
            span_str.setSpan(new TextAppearanceSpan(context, R.style.Lyrics), 0, span_str.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

//        string_matcher = anchor_pattern.matcher(span_str.toString());
//        if (string_matcher.find())
//        {
//            span_str.setSpan(new TextAppearanceSpan(context, R.style.Anchors),
//                    string_matcher.start(),
//                    string_matcher.end(),
//                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        }

        return span_str;
    }

    private void findChorusLocations()
    {
        boolean first_chorus = true;

        for (int i = 0; i < lines.size(); ++i)
        {
            if (chorus_pattern.matcher(lines.get(i)).find())
            {
                if (first_chorus)
                {
                    first_chorus = false;
                    continue;
                }

                if (i + 1 >= lines.size())
                {
                    chorus_idx.add(i);
                    return;
                }

                if (containsAnchor(lines.get(i + 1)))
                {
                    chorus_idx.add(i);
                    continue;
                }

                if (isBlank(lines.get(i + 1)))
                {
                    if (!extra_blank)
                    {
                        chorus_idx.add(i);
                        continue;
                    }

                    if (i + 2 >= lines.size())
                    {
                        chorus_idx.add(i);
                        return;
                    }

                    if (isBlank(lines.get(i + 2)) || containsAnchor(lines.get(i + 2)))
                    {
                        chorus_idx.add(i);
                        continue;
                    }
                }
            }
        }
    }

    private void findChorusLines()
    {
        boolean waiting_for_lines = false;
        boolean capture_lines = false;

        Pattern non_empty_pattern = Pattern.compile("[^\\s]+");

        for (String line : lines)
        {
            if (capture_lines)
            {
                if (non_empty_pattern.matcher(line).find())
                {
                    chorus.add(line);
                    waiting_for_lines = false;
                }
                else
                {
                    if (waiting_for_lines)
                    {
                        extra_blank = true;
                    }
                    else
                    {
                        break;
                    }
                }
                continue;
            }

            if (chorus_pattern.matcher(line).find())
            {
                chorus.add(line);

                capture_lines = true;
                waiting_for_lines = true;

                if (containsChords(line))
                {
                    waiting_for_lines = false;
                }
            }
        }
    }

    private SpannableStringBuilder formatChorus()
    {
        SpannableStringBuilder formatted_chorus = new SpannableStringBuilder();

        for (String line : chorus)
        {
            formatted_chorus.append(formatLine(line));
            formatted_chorus.append('\n');
        }
        formatted_chorus.append('\n');
        return formatted_chorus;
    }

    private boolean containsAnchor(String str)
    {
        return anchor_pattern.matcher(str).find();
    }

    private boolean containsChords(String str)
    {
        return chord_pattern.matcher(str).find();
    }

    private boolean isBlank(String str)
    {
        return blank_pattern.matcher(str).find();
    }
}
