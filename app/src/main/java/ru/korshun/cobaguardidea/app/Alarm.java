package ru.korshun.cobaguardidea.app;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

class Alarm {

    private static volatile Alarm instance;
    private Context context;
    private AlarmManager am;
    private PendingIntent pi;

    private Alarm(Context context) {
        this.context = context;
        createData();
    }

    static Alarm getInstance(Context context) {

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

    void createAlarm() {

        if (isAlarmExist()) {
            cancelAlarm();
            createData();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            am.setExact(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000 * 60 * Settings.ALARM_PERIOD_TIME, pi);
        }
        else {
            am.set(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000 * 60 * Settings.ALARM_PERIOD_TIME, pi);
        }

        System.out.println("ALARM: Set alarm");
    }

    private void createData() {
        am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        pi = PendingIntent.getBroadcast(context, 0, new Intent(context, AlarmReceiver.class), 0);
    }

    private void cancelAlarm() {
        am.cancel(pi);
        pi.cancel();
    }

    private boolean isAlarmExist() {
        return PendingIntent.getBroadcast(context, 0, new Intent(context, AlarmReceiver.class),
                PendingIntent.FLAG_NO_CREATE) != null;
    }

}
