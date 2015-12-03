package ru.korshun.cobaguardidea.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import ru.korshun.cobaguardidea.app.fragments.FragmentPassportsUpdate;
import ru.korshun.cobaguardidea.app.fragments.FragmentSettings;


public class ChangeNetworkStateReceiver
        extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getExtras() != null) {


            final ConnectivityManager connectivityManager =         (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo ni =                                  connectivityManager.getActiveNetworkInfo();

            if (ni != null && ni.isConnected()) {

                boolean autoUpdate =                                Boot.sharedPreferences.getBoolean(FragmentSettings.AUTO_UPDATE_KEY, false);
                boolean autoUpdateWiFi =                            Boot.sharedPreferences.getBoolean(FragmentSettings.AUTO_UPDATE_KEY_WIFI, false);

                if(autoUpdate & !Functions.isServiceRunning(UpdatePassportsService.class, context)) {

                    if((autoUpdateWiFi & ni.getType() == ConnectivityManager.TYPE_WIFI) | !autoUpdateWiFi) {
                        Intent updateDbServiceIntent =              new Intent(context, UpdatePassportsService.class);

                        updateDbServiceIntent
                                .putExtra(FragmentPassportsUpdate.DOWNLOAD_TYPE, 0);

                        context.startService(updateDbServiceIntent);
                    }

                }

//                Toast
//                        .makeText(context, ni.getTypeName() + " CONNECTED, autoUpdate: " + autoUpdate, Toast.LENGTH_LONG)
//                        .show();
            }

        }

    }
}
