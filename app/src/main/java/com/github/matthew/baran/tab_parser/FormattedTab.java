package com.github.matthew.baran.tab_parser;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FormattedTab {
    private AppCompatActivity context = null;

    private ArrayList<String> lines = new ArrayList<>();
    private String artist = "Unknown Artist";
    private String title = "Unknown Title";
    private Integer artist_title_idx = -1;
    private ArrayList<String> chorus = new ArrayList<>();
    private ArrayList<Integer> chorus_idx = new ArrayList<>();
    private Integer tab_duration = 150;
    private double bpm = 150;
    private SpannableStringBuilder formatted_text;

    private boolean extra_blank = false;

    // @formatter:off
    public static final Pattern chord_pattern =
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

    public static final Pattern anchor_pattern =
            Pattern.compile(
                "(?i)(Chorus" +
                "|(\\w*\\s*)?Verse\\s*\\w*" +
                "|Bridge|Intro|Outro|Interlude" +
                "|Capo\\s*(I+|\\d+)|(Pre-)?chorus)(?=([:;,.]|\\s|$))");

    public static final Pattern chorus_pattern = Pattern.compile("(?i)Chorus(?=([:;,.]|\\s|$))");
    public static final Pattern blank_pattern = Pattern.compile("^\\s*$");
    // @formatter:on

    FormattedTab(AppCompatActivity context, File file) {
        this.context = context;

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            br.close();
        } catch (IOException e) {
            // TODO: Add error handling.
            lines = null;
            this.context = null;
            return;
        }

        setArtistAndTitle();
        findChorusLines();
        findChorusLocations();
        formatTab();
        setDuration();
    }

    SpannableStringBuilder getFormattedText() {
        return formatted_text;
    }

    String getTitle() {
        return title;
    }

    String getArtist() {
        return artist;
    }

    Integer getTabDuration() {
        return tab_duration;
    }

    private void setArtistAndTitle() {
        ArtistAndTitle info = new ArtistAndTitle(lines);
        artist = info.artist;
        title = info.title;
        artist_title_idx = info.artist_title_idx;
    }

    public static class ArtistAndTitle {
        String artist = "Unknown Artist";
        String title = "Unknown Title";
        Integer artist_title_idx = -1;

        ArtistAndTitle(ArrayList<String> file_lines) {
            findArtistAndTitle(file_lines);
        }

        private void findArtistAndTitle(ArrayList<String> lines) {
            Iterator<String> it = lines.iterator();
            int ctr = 0;

            while (it.hasNext()) {
                String str = it.next();
                if (containsAnchor(str) || containsChords(str)) {
                    return;
                }
                if (!str.isEmpty()) {
                    // Recover artist/title from "artist - title" structure
                    if (str.contains("-") && str.indexOf('-') == str.lastIndexOf('-')) {
                        String[] parsed = str.split("\\s*-\\s*");

                        artist = cleanString(parsed[0]);
                        title = cleanString(parsed[1]);
                        artist_title_idx = ctr;
                        return;
                    } else {
                        // Try to recover artist/title from consecutive lines
                        artist = cleanString(str);
                        while (it.hasNext()) {
                            ++ctr;
                            str = it.next();
                            if (!str.isEmpty()) {
                                if (containsAnchor(str) || containsChords(str)) {
                                    artist = "Unknown Artist";
                                } else {
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
    }

    public static String cleanString(String str) {
        // Trim whitespace and force capitalization of first letters only
        str = str.toLowerCase().trim();

        if (str.isEmpty()) {
            return str;
        }

        String[] tokens = str.split("\\s+");
        str = "";
        for (String t : tokens) {
            if (t.isEmpty()) {
                continue;
            }
            if (t.length() == 1) {
                str += t.toUpperCase() + " ";
                continue;
            }
            str += t.substring(0, 1).toUpperCase() + t.substring(1) + " ";
        }
        str = str.substring(0, str.length() - 1);
        return str;
    }

    private void formatTab() {
        formatted_text = new SpannableStringBuilder();
        int ctr = 0;
        int lines_out = 3;
        int chord_line_cnt = 0;
        int first_chord_line = -1;
        int last_chord_line = -1;
        boolean is_front_matter = true;

        // Start with some whitespace so the content has time to scroll past.
        formatted_text.append("\n\n\n");

        for (String line : lines) {
            if ((isBlank(line) && is_front_matter) || ctr <= artist_title_idx ||
                    containsTablature(line)) {
                ++ctr;
                continue;
            }
            if (!chorus_idx.isEmpty() && ctr == chorus_idx.get(0)) {
                formatted_text.append(formatChorus());
                chorus_idx.remove(0);
                lines_out += chorus.size();
            } else {
                formatted_text.append(formatLine(line));
                formatted_text.append('\n');
                ++lines_out;
            }

            if (containsChords(line)) {
                ++chord_line_cnt;
                if (first_chord_line < 0) {
                    first_chord_line = ctr;
                }
                last_chord_line = ctr;
            }

            is_front_matter = false;
            ++ctr;
        }

        // Lots of assumptions here... make scroll speed configurable
        double beats_per_measure = 4;
        double measures_per_line = 4;
        double total_beats = beats_per_measure * measures_per_line * chord_line_cnt;
        double song_duration = Math.round(60 * (total_beats / bpm));
        if (last_chord_line > first_chord_line) {
            tab_duration = (int) (lines_out * song_duration / (last_chord_line - first_chord_line));
        } else {
            tab_duration = 100;
        }
    }

    private SpannableString formatLine(String line) {
        SpannableString span_str = new SpannableString(line);
        span_str.setSpan(new TextAppearanceSpan(context, R.style.Lyrics), 0, span_str.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        Matcher string_matcher = chord_pattern.matcher(line);

        int num_tokens = line.split("\\s+").length;
        int num_chords = 0;

        while (string_matcher.find()) {
            span_str.setSpan(new TextAppearanceSpan(context, R.style.Chords),
                    string_matcher.start(), string_matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            num_chords++;
        }

        // Try to eliminate chord style for capitalized lyrics "A", "Am", etc...
        if (num_tokens - num_chords > 2) {
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

    private void findChorusLocations() {
        boolean first_chorus = true;

        for (int i = 0; i < lines.size(); ++i) {
            if (chorus_pattern.matcher(lines.get(i)).find()) {
                if (first_chorus) {
                    first_chorus = false;
                    continue;
                }

                if (i + 1 >= lines.size()) {
                    chorus_idx.add(i);
                    return;
                }

                if (containsAnchor(lines.get(i + 1))) {
                    chorus_idx.add(i);
                    continue;
                }

                if (isBlank(lines.get(i + 1))) {
                    if (!extra_blank) {
                        chorus_idx.add(i);
                        continue;
                    }

                    if (i + 2 >= lines.size()) {
                        chorus_idx.add(i);
                        return;
                    }

                    if (isBlank(lines.get(i + 2)) || containsAnchor(lines.get(i + 2))) {
                        chorus_idx.add(i);
                        continue;
                    }
                }
            }
        }
    }

    private void findChorusLines() {
        boolean waiting_for_lines = false;
        boolean capture_lines = false;

        Pattern non_empty_pattern = Pattern.compile("[^\\s]+");

        for (String line : lines) {
            if (capture_lines) {
                if (non_empty_pattern.matcher(line).find()) {
                    chorus.add(line);
                    waiting_for_lines = false;
                } else {
                    if (waiting_for_lines) {
                        extra_blank = true;
                    } else {
                        break;
                    }
                }
                continue;
            }

            if (chorus_pattern.matcher(line).find()) {
                chorus.add(line);

                capture_lines = true;
                waiting_for_lines = true;

                if (containsChords(line)) {
                    waiting_for_lines = false;
                }
            }
        }
    }

    private SpannableStringBuilder formatChorus() {
        SpannableStringBuilder formatted_chorus = new SpannableStringBuilder();

        for (String line : chorus) {
            formatted_chorus.append(formatLine(line));
            formatted_chorus.append('\n');
        }
        formatted_chorus.append('\n');
        return formatted_chorus;
    }

    public static boolean containsAnchor(String str) {
        return anchor_pattern.matcher(str).find();
    }

    public static boolean containsChords(String str) {
        return chord_pattern.matcher(str).find();
    }

    public static boolean isBlank(String str) {
        return blank_pattern.matcher(str).find();
    }

    private boolean containsTablature(String str) {
        Pattern tab_pattern = Pattern.compile("-");
        Matcher tab_matcher = tab_pattern.matcher(str);
        int dash_ctr = 0;
        while (tab_matcher.find()) {
            ++dash_ctr;
        }
        return dash_ctr > 5;
    }

    private void setDuration() {
        if (artist.equals("Unknown Artist") || title.equals("Unknown Title")) {
            return;
        }

        String artist_url = artist.replace(" ", "%20");
        String title_url = title.replace(" ", "%20");
        String url = "https://musicbrainz.org/ws/2/recording?query=" +
                "recording:%22" + title_url + "%22%20AND%20" +
                "artist:%22" + artist_url + "%22&limit=5&offset=0&fmt=json";
        StringRequest request = new MusicBrainzSongDurationRequest(url);

        RequestQueue request_queue = Volley.newRequestQueue(this.context);
        request_queue.add(request);
    }

    private class MusicBrainzSongDurationRequest extends StringRequest {
        MusicBrainzSongDurationRequest(String url) {
            super(Request.Method.GET, url, new SongDurationRequestListener(),
                    new SongDurationErrorListener());
        }

        @Override
        public Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("User-agent", "TabParser/0.1 (matthewsbaran@gmail.com)");
            return headers;
        }
    }

    private class SongDurationRequestListener implements Response.Listener<String> {
        @Override
        public void onResponse(String response) {
            JSONArray recordings;
            try {
                JSONObject obj = new JSONObject(response);
                recordings = obj.getJSONArray("recordings");
            } catch (JSONException e) {
                Log.d("tabparser", e.getMessage());
                return;
            }

            Integer avg_duration = 0;
            for (int i = 0; i < recordings.length(); i++) {
                try {
                    JSONObject recording = recordings.getJSONObject(i);
                    Integer duration = recording.getInt("length");
                    avg_duration += (duration - avg_duration) / (i + 1);
                } catch (JSONException e) {
                    // If song length isn't found for this entry, skip it
                }
            }

            if (avg_duration > 0) {
                tab_duration = avg_duration / 1000;
            }
        }
    }

    private class SongDurationErrorListener implements Response.ErrorListener {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d("tabparser", "Song Duration Request Error: " + error.getMessage());
        }
    }
}
