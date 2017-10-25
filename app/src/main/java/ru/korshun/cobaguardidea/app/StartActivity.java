package ru.korshun.cobaguardidea.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.os.EnvironmentCompat;
import android.text.TextUtils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ru.korshun.cobaguardidea.app.fragments.FragmentPassportsUpdate;
import ru.korshun.cobaguardidea.app.fragments.FragmentSettings;

/**
 * Стартовая Activity
 */
public class StartActivity
        extends Activity {



//    public static SharedPreferences sharedPreferences;

    public final static String PASSPORTS_COUNT_KEY =                    "pref_passports_count";
    public final static String CREATE_TEMP_PASSPORTS_DIR_KEY =          "pref_create_temp_passports_dir";
    public final static String CREATE_TEMP_SIGNALS_DIR_KEY =            "pref_create_temp_signals_dir";
    public final static String TEMP_PASSPORTS_DIR_KEY =                 "pref_temp_passports_dir";
    public final static String TEMP_SIGNALS_DIR_KEY =                   "pref_temp_signals_dir";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        if(Boot.sharedPreferences == null) {
            Boot.sharedPreferences =                                    PreferenceManager.getDefaultSharedPreferences(this);
        }



        // временно отключаем автообновление
//        if(Boot.sharedPreferences.getBoolean(FragmentSettings.AUTO_UPDATE_KEY, false)) {
//            Boot
//                    .sharedPreferences
//                    .edit()
//                    .putBoolean(FragmentSettings.AUTO_UPDATE_KEY, false)
//                    .apply();
//            Boot
//                    .sharedPreferences
//                    .edit()
//                    .putBoolean(FragmentSettings.AUTO_UPDATE_KEY_WIFI, false)
//                    .apply();
//        }



        // Если дата последнего обновления не задана - устанавливаем ее в текущее время
        if(Boot.sharedPreferences.getLong(FragmentPassportsUpdate.LAST_UPDATE_DATE_KEY, 0) == 0) {
            Boot
                    .sharedPreferences
                    .edit()
                    .putLong(FragmentPassportsUpdate.LAST_UPDATE_DATE_KEY, Calendar.getInstance().getTimeInMillis())
                    .apply();
        }



        // Если сервер обновлений не задан - берем из соответствующего массива первое значение и записываем
        if(Boot.sharedPreferences.getString(FragmentSettings.SERVER_ADDRESS_KEY, null) == null) {
            Boot
                    .sharedPreferences
                    .edit()
                    .putString(FragmentSettings.SERVER_ADDRESS_KEY, Settings.SERVERS_IP_ARRAY[0])
                    .apply();
        }



        // Если номер отправителя смс не задан - берем из соответствующего массива первое значение и записываем
        if(Boot.sharedPreferences.getString(FragmentSettings.SMS_OWNER_KEY, null) == null) {
            Boot
                    .sharedPreferences
                    .edit()
                    .putString(FragmentSettings.SMS_OWNER_KEY, Settings.SMS_NUMBERS_ARRAY[0])
                    .apply();
        }



        // Если не задано время обновления - записываем второе значение из массива, это 30 минут
        if(Boot.sharedPreferences.getString(FragmentSettings.ALARM_PERIOD_KEY, null) == null) {
            Boot
                    .sharedPreferences
                    .edit()
                    .putString(FragmentSettings.ALARM_PERIOD_KEY, Settings.ALARM_PERIOD_ARRAY[1])
                    .apply();
            Alarm.getInstance(getApplicationContext()).createAlarm();
        }


//        this.deleteDatabase(Settings.DB_NAME);



        DbHelper dbHelper =                                             new DbHelper(StartActivity.this, Settings.DB_NAME, Settings.DB_VERSION);
        SQLiteDatabase db =                                             dbHelper.getWritableDatabase();



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
            if(!rv.contains("/storage/sdcard2")) {
                rv.add("/storage/sdcard2");
            }
            if(!rv.contains("/mnt/sdcard0")) {
                rv.add("/mnt/sdcard0");
            }
            if(!rv.contains("/mnt/sdcard1")) {
                rv.add("/mnt/sdcard1");
            }
            if(!rv.contains("/mnt/sdcard2")) {
                rv.add("/mnt/sdcard2");
            }

//            HashSet<String> ttt = getExternalMounts();

            Collections.addAll(rv, getExternalStorageDirectories());

            return rv.toArray(new String[rv.size()]);
        }



        private String[] getExternalStorageDirectories() {

            List<String> results = new ArrayList<>();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //Method 1 for KitKat & above

                File[] externalDirs = getExternalFilesDirs(null);

                for (File file : externalDirs) {

                    if(file == null) {
                        continue;
                    }

                    if(file.getPath().split("/Android")[0] != null) {

                        String path = file.getPath().split("/Android")[0];

                        boolean addPath = false;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            addPath = Environment.isExternalStorageRemovable(file);
                        } else {
                            addPath = Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(file));
                        }

                        if (addPath) {
                            results.add(path);
                        }

                    }
                }

            }

            if(results.isEmpty()) { //Method 2 for all versions
                // better variation of: http://stackoverflow.com/a/40123073/5002496
                String output = "";
                try {
                    final Process process = new ProcessBuilder().command("mount | grep /dev/block/vold")
                            .redirectErrorStream(true).start();
                    process.waitFor();
                    final InputStream is = process.getInputStream();
                    final byte[] buffer = new byte[1024];
                    while (is.read(buffer) != -1) {
                        output = output + new String(buffer);
                    }
                    is.close();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                if(!output.trim().isEmpty()) {
                    String devicePoints[] = output.split("\n");
                    for(String voldPoint: devicePoints) {
                        results.add(voldPoint.split(" ")[2]);
                    }
                }
            }

            //Below few lines is to remove paths which may not be external memory card, like OTG (feel free to comment them out)
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (int i = 0; i < results.size(); i++) {
                    if (!results.get(i).toLowerCase().matches(".*[0-9a-f]{4}[-][0-9a-f]{4}")) {
//                        Log.d(LOG_TAG, results.get(i) + " might not be extSDcard");
                        results.remove(i--);
                    }
                }
            } else {
                for (int i = 0; i < results.size(); i++) {
                    if (!results.get(i).toLowerCase().contains("ext") && !results.get(i).toLowerCase().contains("sdcard")) {
//                        Log.d(LOG_TAG, results.get(i)+" might not be extSDcard");
                        results.remove(i--);
                    }
                }
            }

            String[] storageDirectories = new String[results.size()];
            for(int i=0; i<results.size(); ++i) storageDirectories[i] = results.get(i);

            return storageDirectories;
        }




//        private HashSet<String> getExternalMounts() {
//            final HashSet<String> out = new HashSet<>();
//            String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
//            String s = "";
//            try {
//                final Process process = new ProcessBuilder().command("mount")
//                        .redirectErrorStream(true).start();
//                process.waitFor();
//                final InputStream is = process.getInputStream();
//                final byte[] buffer = new byte[1024];
//                while (is.read(buffer) != -1) {
//                    s = s + new String(buffer);
//                }
//                is.close();
//            } catch (final Exception e) {
//                e.printStackTrace();
//            }
//
//            // parse output
//            final String[] lines = s.split("\n");
//            for (String line : lines) {
//                if (!line.toLowerCase(Locale.US).contains("asec")) {
//                    if (line.matches(reg)) {
//                        String[] parts = line.split(" ");
//                        for (String part : parts) {
//                            if (part.startsWith("/"))
//                                if (!part.toLowerCase(Locale.US).contains("vold"))
//                                    out.add(part);
//                        }
//                    }
//                }
//            }
//            return out;
//        }



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
        private void checkCobaDir() {

            passportsDir =                                      Boot.sharedPreferences.getString(FragmentSettings.PASSPORTS_PATH_KEY, null) != null ?
                                                                    Boot.sharedPreferences.getString(FragmentSettings.PASSPORTS_PATH_KEY, null) :
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

                final String[] STORAGE_DIRS =                   getStorageDirectories();

                for (String STORAGE_DIR : STORAGE_DIRS) {

//                    Log.d(Settings.LOG_TAG, STORAGE_DIR + " - " + new File(STORAGE_DIR + File.separator + Settings.COBA_PASSPORTS_PATH).isDirectory());

                    if (new File(STORAGE_DIR + File.separator + Settings.COBA_PASSPORTS_PATH).isDirectory()) {
//                        Functions.setPrefOption(Settings.PASSPORTS_DIR, STORAGE_DIR, cnt);
                        Boot.sharedPreferences
                                .edit()
                                .putString(FragmentSettings.PASSPORTS_PATH_KEY, STORAGE_DIR + File.separator + Settings.COBA_PASSPORTS_PATH)
                                .apply();
                        totalPassport =                         new File(STORAGE_DIR + File.separator + Settings.COBA_PASSPORTS_PATH).listFiles().length - 1;
                        break;
                    }

                }

            }


            /* Дополнение для непонятных телефонов, где не получается получить путь к диску,
             * например как у Зубова */
            if(Boot.sharedPreferences.getString(FragmentSettings.PASSPORTS_PATH_KEY, null) == null) {

                String cobaDbPath = getExternalCacheDir().getAbsolutePath() + File.separator + "coba_db";

                if(!new File(cobaDbPath).isDirectory()) {
                    new File(cobaDbPath).mkdir();
                }

                Boot.sharedPreferences
                        .edit()
                        .putString(FragmentSettings.PASSPORTS_PATH_KEY, cobaDbPath)
                        .apply();
            }

        }






        @Override
        protected Void doInBackground(Void... params) {

            checkCobaDir();
            clearCacheDir();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            Intent rootActivity =                               new Intent(this.cnt, RootActivity.class);

            Boot.sharedPreferences
                    .edit()
                    .putInt(PASSPORTS_COUNT_KEY,                totalPassport)
                    .apply();

            Boot.sharedPreferences
                    .edit()
                    .putBoolean(CREATE_TEMP_PASSPORTS_DIR_KEY,  isCreateTempPassportsDir)
                    .apply();

            Boot.sharedPreferences
                    .edit()
                    .putBoolean(CREATE_TEMP_SIGNALS_DIR_KEY,    isCreateSignalsDir)
                    .apply();

            Boot.sharedPreferences
                    .edit()
                    .putString(TEMP_PASSPORTS_DIR_KEY,          cobaTempPassportsPath)
                    .apply();

            Boot.sharedPreferences
                    .edit()
                    .putString(TEMP_SIGNALS_DIR_KEY,            cobaSignalsPath)
                    .apply();

//            rootActivity.putExtra("totalPassport",              totalPassport);
//            rootActivity.putExtra("isCreateTempPassportsDir",   isCreateTempPassportsDir);
//            rootActivity.putExtra("isCreateSignalsDir",         isCreateSignalsDir);
//            rootActivity.putExtra("cobaTempPassportsPath",      cobaTempPassportsPath);
//            rootActivity.putExtra("cobaSignalsPath",            cobaSignalsPath);

            startActivity(rootActivity);

            finish();

        }
    }


}