package ru.korshun.cobaguardidea.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.Calendar;

import ru.korshun.cobaguardidea.app.fragments.FragmentPassportsUpdate;
import ru.korshun.cobaguardidea.app.fragments.FragmentSettings;


public class ChangeNetworkStateReceiver
        extends BroadcastReceiver {


    private static long             startTimeInMs =                 0l;
    private long                    pauseInMs =                     60000l;


    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getExtras() != null) {

            final ConnectivityManager connectivityManager =         (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo ni =                                  connectivityManager.getActiveNetworkInfo();

            if (ni != null && ni.isConnected()) {

                if((Calendar.getInstance().getTimeInMillis() - startTimeInMs) > pauseInMs) {

                    boolean autoUpdate =                            Boot.sharedPreferences.getBoolean(FragmentSettings.AUTO_UPDATE_KEY, false);
                    boolean autoUpdateWiFi =                        Boot.sharedPreferences.getBoolean(FragmentSettings.AUTO_UPDATE_KEY_WIFI, false);

                    if(autoUpdate & !Functions.isServiceRunning(UpdatePassportsService.class, context)) {

                        if((autoUpdateWiFi & ni.getType() == ConnectivityManager.TYPE_WIFI) | !autoUpdateWiFi) {

                            startTimeInMs =                         Calendar.getInstance().getTimeInMillis();

                            Intent updateDbServiceIntent =          new Intent(context, UpdatePassportsService.class);

                            updateDbServiceIntent
                                    .putExtra(FragmentPassportsUpdate.DOWNLOAD_TYPE, 0);

                            context.startService(updateDbServiceIntent);

                        }

                    }

                }

            }

        }

    }
}
