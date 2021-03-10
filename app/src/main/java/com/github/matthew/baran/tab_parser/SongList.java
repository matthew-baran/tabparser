package com.github.matthew.baran.tab_parser;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.lang.reflect.Array;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SongList extends AppCompatActivity {
    private List<String> list_values = new ArrayList<>();
    public static final String MSG_FILE = "com.github.matthew.baran.tab_parser.FILE_CHOICE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_songlist);

        Toolbar bar = findViewById(R.id.songlist_toolbar);
        setSupportActionBar(bar);

        // TODO: Add check for file read/write permissions
        File sdcard = Environment.getExternalStorageDirectory();
        File download = new File(sdcard, "Download");

        String[] extensions = {".txt", ".tab"};
        String[] pathnames = download.list(new TabFileFilter(extensions));

        // TODO: Test empty list handling
        if (pathnames != null) {
            list_values = new ArrayList<>(Arrays.asList(pathnames));
        }

        TabItemAdapter adapter = new TabItemAdapter(this, getItemInfo(list_values));
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
//                R.layout.list_item, R.id.list_title, list_values);

        ListView lv = findViewById(R.id.songlist);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(getClickListener());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_songlist, menu);
        return true;
    }

    AdapterView.OnItemClickListener getClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(SongList.this, MainActivity.class);
                intent.putExtra(MSG_FILE, list_values.get((int) id));
                startActivity(intent);
            }
        };
    }

    private List<FormattedTab.ArtistAndTitle> getItemInfo(List<String> filenames) {
        List<FormattedTab.ArtistAndTitle> item_info = new ArrayList<>();
        for (String fn : filenames) {
            File sdcard = Environment.getExternalStorageDirectory();
            File filename = new File(sdcard, "Download" + File.separator + fn);

            ArrayList<String> file_lines = FormattedTab.readFile(filename);
            if (file_lines == null) {
                Log.d("tabparser", "Unable to parse " + fn);
                continue;
            }

            FormattedTab.ArtistAndTitle tmp = new FormattedTab.ArtistAndTitle(file_lines);
            item_info.add(new FormattedTab.ArtistAndTitle(file_lines));
        }
        return item_info;
    }

    private class TabItemAdapter extends ArrayAdapter<FormattedTab.ArtistAndTitle> {
        TabItemAdapter(Context context, List<FormattedTab.ArtistAndTitle> item_info) {
            super(context, 0, item_info);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            FormattedTab.ArtistAndTitle info = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
            }

            TextView tvTitle = convertView.findViewById(R.id.list_title);
            TextView tvArtist = convertView.findViewById(R.id.list_artist);
            tvTitle.setText(info.title);
            tvArtist.setText(info.artist);
            return convertView;
        }
    }

    public static class TabFileFilter implements FilenameFilter {
        private String[] extensions;

        public TabFileFilter(String[] extensions) {
            this.extensions = extensions;
        }

        @Override
        public boolean accept(File dir, String name) {
            for (String ext : extensions) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }
    }
}
