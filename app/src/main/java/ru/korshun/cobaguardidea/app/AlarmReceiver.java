package ru.korshun.cobaguardidea.app;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import ru.korshun.cobaguardidea.app.fragments.FragmentObjects;

@SuppressWarnings("FieldCanBeLocal")
public class AlarmReceiver
        extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        System.out.println("ALARM: AlarmReceiver onReceive");

        if(Boot.sharedPreferences == null) {
            Boot.sharedPreferences =                            PreferenceManager
                    .getDefaultSharedPreferences(context);
        }

        if(getGuardObjectsCount(context) > 0) {
            Uri notification =                                  RingtoneManager.getDefaultUri(
                                                                    RingtoneManager.TYPE_NOTIFICATION);
            MediaPlayer ringtone = MediaPlayer.create(context, notification);

            ringtone.setLooping(false);
            ringtone.start();

            ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(3000);

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

                    if(c.getInt(c.getColumnIndex("status")) == FragmentObjects.GUARD_STATUS_OFF) {
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
        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alertDialog.setTitle(R.string.alert_guard_dialog_title);
        alertDialog.setMessage(context.getResources().getString(R.string.alert_guard_dialog_text));
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                context.getResources().getString(R.string.positive_dialog_button),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }

}