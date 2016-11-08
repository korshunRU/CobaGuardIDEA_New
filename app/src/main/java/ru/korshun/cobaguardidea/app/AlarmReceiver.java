package ru.korshun.cobaguardidea.app;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

@SuppressWarnings("FieldCanBeLocal")
public class AlarmReceiver
        extends BroadcastReceiver {

    private final int CURRENT_GUARD_STATUS_ON =                 2;

    @Override
    public void onReceive(Context context, Intent intent) {

        System.out.println("ALARM: onReceive");

        if(getGuardObjectsCount(context) == 0) {
            Uri notification =                                  RingtoneManager.getDefaultUri(
                                                                    RingtoneManager.TYPE_ALARM);
            Ringtone ringtone =                                 RingtoneManager.getRingtone(context
                                                                    .getApplicationContext(), notification);
            ringtone.play();

            createDialog(context);
        }

        Alarm.getInstance(context).createAlarm();
    }

    private int getGuardObjectsCount(Context context) {

        int count =                                             0;
        DbHelper dbHelper =                                     new DbHelper(context,
                                                                    Settings.DB_NAME, Settings.DB_VERSION);
        SQLiteDatabase db =                                     dbHelper.getWritableDatabase();

        Cursor c =                                              db.rawQuery(
                                                                    "SELECT * " +
                                                                            "FROM " + DbHelper.DB_TABLE_GUARD +
                                                                            " ORDER BY date DESC;", null);

        if(c != null && c.getCount() > 0) {

            if (c.moveToFirst()) {

                do {

                    if(c.getInt(c.getColumnIndex("status")) == CURRENT_GUARD_STATUS_ON) {
                        count++;
                    }

                } while (c.moveToNext());

            }

            c.close();

        }

        dbHelper.close();

        return count;
    }

    private void createDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setTitle(R.string.alert_guard_dialog_title)
                .setMessage(R.string.alert_guard_dialog_text)
                .setPositiveButton(R.string.positive_dialog_button, null)
                .create();
        builder.show();
    }

}