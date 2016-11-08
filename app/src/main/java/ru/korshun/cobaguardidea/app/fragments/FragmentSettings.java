package ru.korshun.cobaguardidea.app.fragments;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ru.korshun.cobaguardidea.app.Boot;
import ru.korshun.cobaguardidea.app.R;
import ru.korshun.cobaguardidea.app.Settings;
import ru.korshun.cobaguardidea.app.StartActivity;

public class FragmentSettings
        extends PreferenceFragment {


    public final static String VERSION =                                "pref_view_version";
    public final static String PASSPORTS_PATH_KEY =                     "pref_view_passports_path";
    public final static String SERVER_ADDRESS_KEY =                     "pref_set_server";
    public final static String SMS_OWNER_KEY =                          "pref_set_sms_sender";
    public final static String AUTO_UPDATE_KEY =                        "pref_set_auto_update";
    public final static String AUTO_UPDATE_KEY_WIFI =                   "pref_set_auto_update_wifi";


    public static SharedPreferences.OnSharedPreferenceChangeListener sharedPreferencesListener;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_settings);


        //Установки для просмотра версии программы
        final ListPreference viewVersion =                              (ListPreference) findPreference(VERSION);

        viewVersion.setSummary(getString(R.string.version));


        //Установки для просмотра расположения папки с паспортами
        final ListPreference viewPassportsPath =                        (ListPreference) findPreference(PASSPORTS_PATH_KEY);

        viewPassportsPath.setSummary(Boot.sharedPreferences.getString(PASSPORTS_PATH_KEY, "-"));



        //Установки для настройки сервера обновлений
        final ListPreference setupServer =                              (ListPreference) findPreference(SERVER_ADDRESS_KEY);

        setupServer.setEntries(Settings.SERVERS_IP_ARRAY_LEGEND);
        setupServer.setEntryValues(Settings.SERVERS_IP_ARRAY);
        setupServer.setSummary(setupServer.getEntry());



        //Установки для настройки источника смс
        final ListPreference setupSmsSender =                           (ListPreference) findPreference(SMS_OWNER_KEY);

        setupSmsSender.setEntries(Settings.SMS_NUMBERS_ARRAY_LEGEND);
        setupSmsSender.setEntryValues(Settings.SMS_NUMBERS_ARRAY);
        setupSmsSender.setSummary(setupSmsSender.getEntry());



        sharedPreferencesListener =                                     new SharedPreferences.OnSharedPreferenceChangeListener() {
                                                                            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
//                                                                                Toast
//                                                                                        .makeText(getActivity(), key + " " + prefs.getBoolean(key, true), Toast.LENGTH_LONG)
//                                                                                        .show();

                                                                                switch (key) {
                                                                                    case SERVER_ADDRESS_KEY:
                                                                                        findPreference(key)
                                                                                                .setSummary(setupServer.getEntry());
                                                                                        break;

                                                                                    case SMS_OWNER_KEY:
                                                                                        findPreference(key)
                                                                                                .setSummary(setupSmsSender.getEntry());
                                                                                        break;
                                                                                }

                                                                            }
                                                                        };
    }




    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

//        View v =                                                inflater.inflate(R.layout.fragment_settings, container, false);



        return super.onCreateView(inflater, container, savedInstanceState);
    }





    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(sharedPreferencesListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener);
    }





    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }








}