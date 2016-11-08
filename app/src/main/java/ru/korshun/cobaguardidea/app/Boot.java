package ru.korshun.cobaguardidea.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;



public class Boot
        extends BroadcastReceiver {

    public static SharedPreferences     sharedPreferences =             null;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            if(Boot.sharedPreferences == null) {
                Boot.sharedPreferences =                                PreferenceManager
                                                                            .getDefaultSharedPreferences(context);
            }

            context.startService(new Intent(context, MyNotification.class));

            Alarm.getInstance(context).createAlarm();
//            context.startService(new Intent(context, MyCheckDataChange.class));
        }

    }
}