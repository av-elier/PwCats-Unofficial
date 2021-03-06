package ru.avelier.pwcats.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.util.Log;
import ru.avelier.pwcats.db.DbRecentItemsContract.*;
import ru.avelier.pwcats.myapp.R;

/**
 * Created by Adelier on 28.06.2014.
 */
public class DbRecentItemsHelper extends SQLiteOpenHelper {
    private Context context;
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "PwcatsInternal.db";

    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + RecentItemsEntry.TABLE_NAME + " (" +
                    RecentItemsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
                    RecentItemsEntry.COL_RECENT_DATE + " INTEGER UNIQUE DEFAULT (datetime('now'))" + COMMA_SEP +
                    RecentItemsEntry.COL_RECENT_ID + " INTEGER " +
                    " )";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + RecentItemsEntry.TABLE_NAME;

    public DbRecentItemsHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }
    public void onCreate(SQLiteDatabase db) {
        Log.d(this.getClass().getName(), "onCreate() " + SQL_CREATE_ENTRIES);
        db.execSQL(SQL_CREATE_ENTRIES);

        int[] faq = context.getResources().getIntArray(R.array.popular_item_ids);
        for (int id : faq) {
            db.execSQL("INSERT INTO " + RecentItemsEntry.TABLE_NAME +
                    " (" + RecentItemsEntry.COL_RECENT_ID + ", " + RecentItemsEntry.COL_RECENT_DATE +
                    ") VALUES (" + id + ", " + id + ")");
        }
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO save stored data
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
