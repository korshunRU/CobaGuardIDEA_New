package ru.korshun.cobaguardidea.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/**
 *  Класс для работы с БД
 */
public class DbHelper
        extends SQLiteOpenHelper {



    public static final String                      DB_TABLE_SIGNALS =                  "signals";
    public static final String                      DB_TABLE_GUARD =                    "guard";



    public DbHelper(Context context, String name, int version) {
        super(context, name, null, version);
//        System.out.println("myapp - DbHelper");
    }




    @Override
    public void onCreate(SQLiteDatabase db) {
//        System.out.println("myapp - DbHelper onCreate");
        db.execSQL("CREATE TABLE " + DB_TABLE_SIGNALS
                + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "number TEXT UNIQUE NOT NULL, "
                + "date INTEGER NOT NULL, "
                + "complite_status INTEGER DEFAULT 0);");

        db.execSQL("CREATE TABLE " + DB_TABLE_GUARD
                + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "number TEXT UNIQUE NOT NULL, "
                + "date INTEGER NOT NULL, "
                + "status INTEGER DEFAULT NULL, "
                + "complite_status INTEGER DEFAULT 0);");
    }




    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        System.out.println("myapp - DbHelper onUpgrade");
        db.execSQL("CREATE TABLE " + DB_TABLE_GUARD
                + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "number TEXT UNIQUE NOT NULL, "
                + "date INTEGER NOT NULL, "
                + "status INTEGER DEFAULT NULL, "
                + "complite_status INTEGER DEFAULT 0);");
    }



}
