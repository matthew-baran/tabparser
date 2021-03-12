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
import java.util.Iterator;
import java.util.List;

public class SongList extends AppCompatActivity {
    private List<File> tab_files = new ArrayList<>();
    public static final String MSG_FILE = "com.github.matthew.baran.tab_parser.FILE_CHOICE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_songlist);

        Toolbar bar = findViewById(R.id.songlist_toolbar);
        setSupportActionBar(bar);

        // TODO: Add check for file read/write permissions (and all other app permissions since this is the entry point)

        List<FormattedTab.ArtistAndTitle> list_info = getItemInfo();
        if (list_info == null) {
            ArrayAdapter<String> empty_adapter = new ArrayAdapter<>(this, R.layout.list_item, R.id.list_title,
                    Arrays.asList("No Tab Files Found."));
            ListView lv = findViewById(R.id.songlist);
            return;
        }

        TabItemAdapter adapter = new TabItemAdapter(this, list_info);

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
                intent.putExtra(MSG_FILE, tab_files.get((int) id));
                startActivity(intent);
            }
        };
    }

    private List<FormattedTab.ArtistAndTitle> getItemInfo() {
        File sdcard = Environment.getExternalStorageDirectory();
        File download = new File(sdcard, "Download");

        String[] extensions = {".txt", ".tab"};
        String[] pathnames = download.list(new TabFileFilter(extensions));

        if (pathnames == null) {
            return null;
        }

        List<FormattedTab.ArtistAndTitle> item_info = new ArrayList<>();
        for (String fn : Arrays.asList(pathnames)) {
            File filename = new File(sdcard, "Download" + File.separator + fn);
            tab_files.add(filename);

            ArrayList<String> file_lines = FormattedTab.readFile(filename);
            if (file_lines == null) {
                Log.d("tabparser", "Unable to parse " + fn);
                continue;
            }

            FormattedTab.ArtistAndTitle info = new FormattedTab.ArtistAndTitle(file_lines);
            if (info.title == "Unknown Title" && info.artist == "Unknown Artist") {
                info.title = fn;
                info.artist = "";
            }
            item_info.add(info);
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
