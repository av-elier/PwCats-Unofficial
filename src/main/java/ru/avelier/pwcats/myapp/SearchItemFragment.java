package ru.avelier.pwcats.myapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import ru.avelier.pwcats.db.DbItemsHelper;
import ru.avelier.pwcats.db.DbRecentItemsContract.*;
import ru.avelier.pwcats.db.DbRecentItemsHelper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SearchItemFragment extends Fragment {

    private ViewGroup rootView;
    private SharedPreferences prefs;

    private List<AsyncTask<String, Void, Bitmap>> loadIconTasks;


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(this.toString(), "onCreate()");

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

        loadIconTasks = new LinkedList<AsyncTask<String, Void, Bitmap>>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.search_item, container, false);

        // fill server spinner
        Spinner spinner = (Spinner) rootView.findViewById(R.id.spinner_server);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.servers, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(prefs.getInt(getString(R.string.pref_server), 0));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                prefs.edit().putInt(getString(R.string.pref_server), position).apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        EditText edit = (EditText) rootView.findViewById(R.id.editText);
        edit.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
//                EditText edit = (EditText) findViewById(R.id.editText);
                String query = s.toString();
                search(query);
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        // show recent items
        search("");

        SearchView searchView = (SearchView)rootView.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                search(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                search(newText);
                return true;
            }
        });

        return rootView;
    }

    private void showRecentItems() {
        List<Integer> recent_ids = recentItemIds();
        for (int id : recent_ids) {
            add_item_line(id);
        }
    }

    private List<Integer> recentItemIds() {
        int recentCountLimit = prefs.getInt(getString(R.string.pref_recent_count), 30);
        List<Integer> res = new ArrayList<Integer>(recentCountLimit);

        SQLiteDatabase db = MainActivity.recent_items_db.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + RecentItemsEntry.COL_RECENT_ID +
                " FROM " + RecentItemsEntry.TABLE_NAME +
                " ORDER BY " + RecentItemsEntry.COL_RECENT_DATE + " DESC" +
                " LIMIT 0, 100", new String[]{});
        if (c.moveToFirst()) do {
            res.add(c.getInt(0));
        } while (c.moveToNext());
        c.close();
        db.close();
        return res;
    }

    // TODO optimise
    // TODO case-insensitive. Maybe create 1 more row with all in lower case. Ибо sqlite буржуйский.
    public void search(String subname) {
        ViewGroup insertPoint = (ViewGroup) rootView.findViewById(R.id.scrolledLinearView);

        stopLoadingIcons();

        if (subname == null || subname.equals("")) {
            insertPoint.removeAllViewsInLayout();
            showRecentItems();
            return;
        }

        // process search
        SQLiteDatabase db = MainActivity.items_db.getReadableDatabase();
        Cursor c;
        try {
            // || things because of https://code.google.com/p/android/issues/detail?id=3153
            String SELECT_WHERE = "SELECT _id, name FROM items WHERE lower_name LIKE '%' || ? || '%' OR name LIKE '%' || ? || '%' LIMIT 0, 50";
            String[] binds = new String[]{subname.toLowerCase()};
            c = db.rawQuery(SELECT_WHERE, binds);
        } catch (Exception e) {
            Log.wtf("db", subname);
            e.printStackTrace();
            return;
        }

// add proposials to list
        insertPoint.removeAllViewsInLayout();
        if (c.moveToFirst()) {
            do {
                final int id = c.getInt(0);
                final String itemName = c.getString(1);
                add_item_line(id, itemName);
            } while (c.moveToNext());
        } else { // empty
            Log.d("db", "empty select for subname=" + subname);
//            insertPoint.removeAllViewsInLayout();
        }
        c.close();
        db.close();
    }

    private void stopLoadingIcons() {
        for (AsyncTask<String, Void, Bitmap> task : loadIconTasks) {
            task.cancel(true);
        }
        loadIconTasks.clear();
    }

    private void add_item_line(int id){
        add_item_line(id, MainActivity.getItemNameById(id));
    }

    private void add_item_line(final int id, final String itemName) {
        ViewGroup insertPoint = (ViewGroup) rootView.findViewById(R.id.scrolledLinearView);
        LayoutInflater vi = (LayoutInflater) getActivity().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.search_item_line, null);

// load icon
        AsyncTask<String, Void, Bitmap> loadIconTask = new DownloadImageTask((ImageView) v.findViewById(R.id.itemIcon));
        loadIconTasks.add(loadIconTask);
        loadIconTask.execute(DownloadImageTask.getIconUrl(id));
// fill id (hidden)
        TextView textItemId = (TextView) v.findViewById(R.id.textCatNickname);
        textItemId.setText(id + "");
        textItemId.setVisibility(View.GONE);
// fill item name
        TextView textItemName = (TextView) v.findViewById(R.id.textCatTitle);
        textItemName.setText(itemName);
//
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showItemDetails(id, itemName);
            }
        });

// insert into main view
        insertPoint.addView(v, insertPoint.getChildCount(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }


    public void viewItemDetails(int id) {
        showItemDetails(id, MainActivity.getItemNameById(id));
    }

    public void showItemDetails(int id, String itemName) {
// hide keyboard
        hideKeyboard();
// db recent
        Log.d(this.toString(), "inserting new recent id: " + id);
        SQLiteDatabase db = MainActivity.recent_items_db.getWritableDatabase();
        db.execSQL("DELETE FROM " + RecentItemsEntry.TABLE_NAME +
                " WHERE " + RecentItemsEntry.COL_RECENT_ID + " = " + id);
        db.execSQL("INSERT INTO " + RecentItemsEntry.TABLE_NAME +
                " (" + RecentItemsEntry.COL_RECENT_ID + ") VALUES(" + id + ")");
        db.close();
// get selected server
        Spinner server_spinner = ((Spinner) rootView.findViewById(R.id.spinner_server));
        String server = (String)server_spinner.getItemAtPosition(server_spinner.getSelectedItemPosition());
// compose ItemDetailsFragment
        Fragment fragment = new ItemDetailsPagesFragment();
        Bundle args = new Bundle();
        args.putInt("id", id);
        args.putString("itemName", itemName);
        args.putString("server", server);
        fragment.setArguments(args);
// Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void hideKeyboard() {
        EditText editText = (EditText) rootView.findViewById(R.id.editText);
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(this.toString(), "onPause()");
        stopLoadingIcons();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(this.toString(), "onResume()");
        getActivity().setTitle(R.string.search_activity_label);
        search(((EditText)rootView.findViewById(R.id.editText)).getText().toString());
        ((MainActivity)getActivity()).setActiveFragment(this);
    }

    public void clearQuery() {
        ((EditText)rootView.findViewById(R.id.editText)).setText("");
    }
    public String getQuery() {
        return ((EditText)rootView.findViewById(R.id.editText)).getText().toString();
    }
}
