package ru.korshun.cobaguardidea.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Стартовая Activity
 */
public class StartActivity
        extends Activity {




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

//        this.deleteDatabase(Settings.DB_NAME);

        DbHelper dbHelper =                                 new DbHelper(StartActivity.this, Settings.DB_NAME, Settings.DB_VERSION);
        SQLiteDatabase db =                                 dbHelper.getWritableDatabase();

        db.execSQL("DELETE FROM " + DbHelper.DB_TABLE_SIGNALS + " WHERE date < " + (System.currentTimeMillis() - (Settings.DB_SIGNALS_LIFE_HOURS * 60 * 60 * 1000)));
        db.execSQL("DELETE FROM " + DbHelper.DB_TABLE_GUARD + " WHERE date < " + (System.currentTimeMillis() - (Settings.DB_GUARD_LIFE_HOURS * 60 * 60 * 1000)));

        dbHelper.close();

        new CheckDir(this).execute();

    }





    @Override
    public void onBackPressed() {
    }













    class CheckDir
        extends AsyncTask<Void, Void, Void> {

        Context cnt;
        int totalPassport =                                     0;
        boolean isCreateTempPassportsDir =                      false;
        boolean isCreateSignalsDir =                            false;
        String passportsDir, cobaTempPassportsPath, cobaSignalsPath;

        CheckDir(Context cnt) {
            this.cnt =                                          cnt;
        }




        /**
         *  Вытаскиваем все "диски" телефона
         */
        private String[] getStorageDirectories() {

            // Final set of paths
            final Set<String> rv = new HashSet<>();
            // Primary physical SD-CARD (not emulated)
            final String rawExternalStorage = System.getenv("EXTERNAL_STORAGE");
            // All Secondary SD-CARDs (all exclude primary) separated by ":"
            final String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
            // Primary emulated SD-CARD
            final String rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET");

            if(TextUtils.isEmpty(rawEmulatedStorageTarget)) {
                // Device has physical external storage; use plain paths.
                if(TextUtils.isEmpty(rawExternalStorage)) {
                    // EXTERNAL_STORAGE undefined; falling back to default.
                    rv.add("/storage/sdcard0");
                }
                else {
                    rv.add(rawExternalStorage);
                }
            }
            else {
                // Device has emulated storage; external storage paths should have
                // userId burned into them.
                final String rawUserId;

                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    rawUserId = "";
                }
                else {
                    final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                    final String[] folders = Settings.DIR_SEPARATOR.split(path);
                    final String lastFolder = folders[folders.length - 1];
                    boolean isDigit = false;
                    try {
                        Integer.valueOf(lastFolder);
                        isDigit = true;
                    }
                    catch(NumberFormatException ignored) {
                    }
                    rawUserId = isDigit ? lastFolder : "";
                }

                // /storage/emulated/0[1,2,...]
                if(TextUtils.isEmpty(rawUserId)) {
                    rv.add(rawEmulatedStorageTarget);
                }
                else {
                    rv.add(rawEmulatedStorageTarget + File.separator + rawUserId);
                }

            }

            // Add all secondary storages
            if(!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
                // All Secondary SD-CARDs splited into array
                final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);
                Collections.addAll(rv, rawSecondaryStorages);
            }

            if(!rv.contains("/storage/sdcard1")) {
                rv.add("/storage/sdcard1");
            }
            if(!rv.contains("/storage/sdcard0")) {
                rv.add("/storage/sdcard0");
            }
            return rv.toArray(new String[rv.size()]);
        }




        /**
         *  Очистка временной директории
         */
        private void clearCacheDir() {

            for(File tmpFile : new File(cobaSignalsPath).listFiles()) {
                if (tmpFile.lastModified() < (System.currentTimeMillis() - (Settings.DB_SIGNALS_LIFE_HOURS * 60 * 60 * 1000))) {
                    tmpFile.delete();
                }
            }

        }




        /**
         *  Проверка директории с паспортами
         */
        private void checkCobaDir(Context cnt) {

            passportsDir =                                       Functions.getPrefOption(Settings.PASSPORTS_DIR, this.cnt) != null ?
                                                                    Functions.getPrefOption(Settings.PASSPORTS_DIR, this.cnt) + File.separator + Settings.COBA_PASSPORTS_PATH :
                                                                    null;

            cobaTempPassportsPath =                             getExternalCacheDir() != null ?
                                                                    getExternalCacheDir().getAbsolutePath() +
                                                                            File.separator +
                                                                            "coba_db_temp" :
                                                                    getCacheDir().getAbsolutePath() +
                                                                            File.separator +
                                                                            "coba_db_temp";

            cobaSignalsPath =                                   getExternalCacheDir() != null ?
                                                                    getExternalCacheDir().getAbsolutePath() +
                                                                            File.separator +
                                                                            "signals" :
                                                                    getCacheDir().getAbsolutePath() +
                                                                            File.separator +
                                                                            "signals";

            isCreateTempPassportsDir =                          new File(cobaTempPassportsPath).isDirectory() | new File(cobaTempPassportsPath).mkdir();

            isCreateSignalsDir =                                new File(cobaSignalsPath).isDirectory() | new File(cobaSignalsPath).mkdir();

            if(passportsDir != null) {

                if (new File(passportsDir).isDirectory()) {

                    totalPassport =                             new File(passportsDir).listFiles().length - 1;

                }
            }

            else {

                final String[] STORAGE_DIRS = getStorageDirectories();

                for (String STORAGE_DIR : STORAGE_DIRS) {

//                    Log.d(Settings.LOG_TAG, STORAGE_DIR + " - " + new File(STORAGE_DIR + File.separator + Settings.COBA_PASSPORTS_PATH).isDirectory());

                    if (new File(STORAGE_DIR + File.separator + Settings.COBA_PASSPORTS_PATH).isDirectory()) {
                        Functions.setPrefOption(Settings.PASSPORTS_DIR, STORAGE_DIR, cnt);
                        totalPassport =                         new File(STORAGE_DIR + File.separator + Settings.COBA_PASSPORTS_PATH).listFiles().length - 1;
                        break;
                    }

                }

            }

        }






        @Override
        protected Void doInBackground(Void... params) {

            checkCobaDir(cnt);
            clearCacheDir();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            Intent rootActivity =                               new Intent(this.cnt, RootActivity.class);

            rootActivity.putExtra("totalPassport",              totalPassport);
            rootActivity.putExtra("isCreateTempPassportsDir",   isCreateTempPassportsDir);
            rootActivity.putExtra("isCreateSignalsDir",         isCreateSignalsDir);
            rootActivity.putExtra("cobaTempPassportsPath",      cobaTempPassportsPath);
            rootActivity.putExtra("cobaSignalsPath",            cobaSignalsPath);

            startActivity(rootActivity);

            finish();

        }
    }


}