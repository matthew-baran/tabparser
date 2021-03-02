package com.github.matthew.baran.tab_parser;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SongList extends ListActivity
{
    private List<String> list_values;
    public static final String MSG_FILE = "com.github.matthew.baran.tab_parser.FILE_CHOICE";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_songlist);

        // TODO: Add check for file read/write permissions
        File sdcard = Environment.getExternalStorageDirectory();
        File download = new File(sdcard, "Download");

        String[] extensions = {".txt", ".tab"};
        String[] pathnames = download.list(new TabFileFilter(extensions));

        // TODO: Test empty list handling
        if (pathnames != null)
        {
            list_values = new ArrayList<>(Arrays.asList(pathnames));
        }
        else
        {
            list_values = new ArrayList<>();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.list_item, R.id.list_text, list_values);

        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id)
    {
        super.onListItemClick(list, view, position, id);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MSG_FILE, list_values.get((int) id));
        startActivity(intent);
    }

    public static class TabFileFilter implements FilenameFilter
    {
        private String[] extensions;

        public TabFileFilter(String[] extensions)
        {
            this.extensions = extensions;
        }

        @Override
        public boolean accept(File dir, String name)
        {
            for (String ext : extensions)
            {
                if (name.endsWith(ext))
                {
                    return true;
                }
            }
            return false;
        }
    }
}
