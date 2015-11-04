package ru.korshun.cobaguardidea.app;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class MyNotification
        extends Service {

    /**
     *  ID записи в панели нотификации
     */
    private int notificationId =                        1001;



    @Override
    public void onCreate() {
        super.onCreate();

        createNotification();
    }


    public void createNotification() {

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent resultIntent = new Intent(this, StartActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                this,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_app_ico)
                .setTicker(Settings.PD_TITLE + " active")
                .setContentTitle(Settings.PD_TITLE)
                .setContentText(getString(R.string.notification_title_legend))
                .setContentIntent(resultPendingIntent)
                .setOngoing(true);

        nm.notify(notificationId, mBuilder.build());

    }



    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

}
