package ru.korshun.cobaguardidea.app;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import ru.korshun.cobaguardidea.app.fragments.FragmentSettings;

public class Alarm {

    private static volatile Alarm instance;
    private Context context;

    private Alarm(Context context) {
        this.context = context;
    }

    public static Alarm getInstance(Context context) {

        Alarm localInstance = instance;

        if (localInstance == null) {
            synchronized (Alarm.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new Alarm(context);
                }
            }
        }

        return localInstance;

    }

    public void createAlarm() {

        String alarmPeriod = Boot.sharedPreferences.getString(
                FragmentSettings.ALARM_PERIOD_KEY, Settings.ALARM_PERIOD_ARRAY[1]);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0,
                new Intent(context, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);

        if(Integer.parseInt(alarmPeriod) > 0) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + 1000 * 60 * Integer.parseInt(alarmPeriod), pi);
            }
            else {
                am.set(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + 1000 * 60 * Integer.parseInt(alarmPeriod), pi);
            }

            System.out.println("ALARM: Set alarm to " + alarmPeriod);
        }

        else if(Integer.parseInt(alarmPeriod) == 0) {
            am.cancel(pi);
            pi.cancel();
            System.out.println("ALARM: alarm disabled");
        }

    }


}
