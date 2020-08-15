package com.github.matthew.baran.tab_parser;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.*;
import android.view.animation.LinearInterpolator;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;

public class MainActivity extends AppCompatActivity
{
    private Toolbar mTopToolbar;

    private GestureDetector gesture_detector;
    private ScrollAnimation scroll_animation;

    private final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 0;
    private static final String LOG_NAME = "Tab Party";

    private String filename;
    private boolean animation_cancelled = false;
    private int animation_duration = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTopToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(mTopToolbar);

        filename = getIntent().getStringExtra(SongList.MSG_FILE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_STORAGE);
        }
        else
        {
            displayTab();
        }

        gesture_detector = new GestureDetector(this, new tabGestureListener());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_go_back)
        {
            Intent intent = new Intent(this, SongList.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            double scroll_ratio = (max_scroll - sv.getScrollY()) / (double) max_scroll;
            animator.setDuration((long) (duration * scroll_ratio));
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
        File file = new File(sdcard, "Download/" + filename);

        FormattedTab tab = new FormattedTab(this, file);

        getSupportActionBar().setTitle(tab.getArtist() + " - " + tab.getTitle());
        animation_duration = tab.getTabDuration();

        TextView tv = findViewById(R.id.tab_textview);
        tv.setText(tab.getFormattedText());
        tv.setMovementMethod(new ScrollingMovementMethod());
        tv.setOnTouchListener(new tabTouchListener());
    }

    @Override
    public void onRequestPermissionsResult(int request_code, String[] permissions,
                                           int[] grant_results)
    {
        super.onRequestPermissionsResult(request_code, permissions, grant_results);
        switch (request_code)
        {
            case MY_PERMISSIONS_REQUEST_READ_STORAGE:
                if (grant_results.length > 0 &&
                        grant_results[0] == PackageManager.PERMISSION_GRANTED)
                {
                    displayTab();
                }
                else
                {
                    TextView tv = findViewById(R.id.tab_textview);
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
            if (scroll_animation == null)
            {
                scroll_animation = new ScrollAnimation(animation_duration * 1000);
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
