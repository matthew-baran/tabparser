package com.github.matthew.baran.tab_parser;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.TextAppearanceSpan;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.github.matthew.baran.tab_parser.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity
{
    private final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 0;
    private static final String LOGTAG = "Tab Parser";

    private GestureDetector gesture_detector;
    private boolean animation_cancelled = false;
    private ScrollAnimation scroll_animation;
    private int animation_duration = 100000;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_STORAGE);
        } else
        {
            displayTab();
        }

        gesture_detector = new GestureDetector(this, new tabGestureListener());
    }

    public class ScrollAnimation
    {
        private ObjectAnimator animator;
        private int duration;
        private ScrollView sv = findViewById(R.id.tab_scrollview);
        private int max_scroll = sv.getChildAt(0).getHeight() - sv.getHeight();
        
        public ScrollAnimation(int duration)
        {
            this.duration = duration;
            this.updateAnimation();
        }

        public void updateAnimation()
        {
            animator = ObjectAnimator.ofInt(sv, "scrollY", sv.getScrollY(), max_scroll);
            double scroll_ratio = (max_scroll - sv.getScrollY()) / (double)max_scroll;
            animator.setDuration((long)(duration * scroll_ratio));
            animator.setInterpolator(new LinearInterpolator());
        }

        public ObjectAnimator getAnimator()
        {
            return animator;
        }

        public void setDuration(int duration)
        {
            this.duration = duration;
        }
    }

    public void displayTab()
    {
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard, "Download/Peaceful Easy Feeling.txt");

        SpannableStringBuilder text = new SpannableStringBuilder();
        TextView tv = findViewById(R.id.tab_textview);

        try
        {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null)
            {
                SpannableString span_str = new SpannableString(line);

                span_str = formatTabString(span_str);

                text.append(span_str);
                text.append('\n');
            }
            br.close();
        } catch (IOException e)
        {
            tv.setText(e.getMessage());
            return;
        }

        tv.setText(text);
        tv.setMovementMethod(new ScrollingMovementMethod());
        tv.setOnTouchListener(new tabTouchListener());
    }

    public SpannableString formatTabString(SpannableString span_str)
    {

        Pattern pattern = Pattern.compile(
                        "[\\s^][A-G][b#]?" +                 // A, Bb, C#
                        "(min|m)?" +                    // Amin, Am
                        "\\d*" +                        // C5, B7
                        "(?i)(dim\\d*" +                // Adim,  Gdim9
                        "|add\\d+" +                    // Cadd9,  B7add6
                        "|sus\\d*" +                    // Dsus, Dsus4
                        "|aug\\d*|\\+" +                // Eaug, E+, Eaug9
                        "|(M|maj)\\d*)?" +              // Cmaj, Cmaj7
                        "([b#]\\d+)?[\\s$]");                // Cmaj7b9, AmSus2#7

        Matcher string_matcher = pattern.matcher(span_str.toString());

        if (string_matcher.find())
        {
            span_str.setSpan(new TextAppearanceSpan(this, R.style.Chords), 0, span_str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        else
        {
            span_str.setSpan(new TextAppearanceSpan(this, R.style.Lyrics), 0, span_str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return span_str;
    }

    @Override
    public void onRequestPermissionsResult(int request_code,
                                           String[] permissions, int[] grant_results)
    {
        super.onRequestPermissionsResult(request_code, permissions, grant_results);
        switch (request_code)
        {
            case MY_PERMISSIONS_REQUEST_READ_STORAGE:
                if (grant_results.length > 0 && grant_results[0] == PackageManager.PERMISSION_GRANTED)
                {
                    displayTab();
                } else
                {
                    TextView tv = (TextView) findViewById(R.id.tab_textview);
                    tv.setText("Permission Denied!");
                }
                return;
        }
    }

    public class tabTouchListener implements View.OnTouchListener
    {
        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            return gesture_detector.onTouchEvent(event);
        }
    }

    public class tabGestureListener extends GestureDetector.SimpleOnGestureListener
    {

        @Override
        public boolean onDown(MotionEvent event)
        {
            if (scroll_animation==null)
            {
                scroll_animation = new ScrollAnimation(animation_duration);
            }

            if (scroll_animation.getAnimator().isRunning())
            {
                scroll_animation.getAnimator().cancel();
                animation_cancelled = true;
            }
            else
            {
                animation_cancelled = false;
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event)
        {
            if (!animation_cancelled)
            {
                scroll_animation.updateAnimation();
                scroll_animation.getAnimator().start();
            }
            return true;
        }
    }
}
