package ru.korshun.cobaguardidea.app;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Calendar;
import java.util.Formatter;
import java.util.concurrent.TimeUnit;

import ru.korshun.cobaguardidea.app.fragments.FragmentPassportsUpdate;
import ru.korshun.cobaguardidea.app.fragments.FragmentSettings;


/**
 *  СЕрвис обновления паспортов
 */
public class UpdatePassportsService
        extends Service {



    /**
     *  Сокет для скачивания всех новых файлов
     */
    private Socket allFilesSocket;


    /**
     *  Числовое значение номера объекта для скачивания
     *  Если номер равен нулю, значит сделан запрос на обновление всех новых паспортов
     */
    private int objectToDownload;


    /**
     *  PI для "общения" с Activity
     *  Для отсыла статусов соединения, счетчика переданных файлов, общего числа файлов и ошибок
     */
//    private PendingIntent piRequest;
    private PendingIntent piRequest, piCounter, piTotal;


    /**
     *  Reader входящей информации
     */
    private BufferedReader in;


    /**
     *  Writer исходящих запросов
     */
    private PrintWriter out;


    /**
     *  Переменная, обозначающая наличие или отсутствие активной загрузки файлов
     */
    private boolean isRunning =                         false;



    /**
     *  ID записи в панели нотификации
     */
    private int notificationId =                        1002;



    /**
     *  IP адресс сервера обновлений
     */
    private String serverIp;



    /**
     *  Дата последнего обновления файлов в формате Long
     */
    private long lastUpdatedDate;



    private NotificationCompat.Builder mBuilder;
    private NotificationManager nm;




    @Override
    public void onCreate() {
        super.onCreate();
    }




    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {

        objectToDownload =                              intent.getIntExtra(FragmentPassportsUpdate.DOWNLOAD_TYPE, 0);
        piRequest =                                     intent.getParcelableExtra(RootActivity.PI_REQUEST);
        piCounter =                                     intent.getParcelableExtra(FragmentPassportsUpdate.PI_FILES_COUNTER);
        piTotal =                                       intent.getParcelableExtra(FragmentPassportsUpdate.PI_FILES_TOTAL);

        serverIp =                                      StartActivity.sharedPreferences.getString(FragmentSettings.SERVER_ADDRESS_KEY, null);
        lastUpdatedDate =                               StartActivity.sharedPreferences.getLong(FragmentPassportsUpdate.LAST_UPDATE_DATE_KEY, 0);

        if(checkConnection()) {

            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        piRequest.send(FragmentPassportsUpdate.CODE_STATUS_CONNECT);
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                        stopSelf(startId);
                    }

                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        stopSelf(startId);
                    }

                    int count = 30;

                    try {
                        piTotal.send(count);
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                        stopSelf(startId);
                    }

                    for(int x = 0; x < count; x++) {

                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            stopSelf(startId);
                        }

                        try {
                            piCounter.send(x + 1);
                            piTotal.send(count);
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                            stopSelf(startId);
                        }

                    }

                    try {
                        piRequest.send(FragmentPassportsUpdate.CODE_STATUS_DISCONNECT);
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    } finally {
                        stopSelf(startId);
                    }

                }
            }).start();

        }

        else {
            stopSelf(startId);
        }

        return START_STICKY;
    }



    @Override
    public void onDestroy() {
        super.onDestroy();

        isRunning =                                     false;
        disconnectFromServer();
    }




    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }




    /**
     *  Проверяем доступность интернет соединения
     * @return      возвращается TRUE в случае успеха
     */
    private boolean checkConnection() {

        if(serverIp != null) {

            if (lastUpdatedDate < Calendar.getInstance().getTimeInMillis()) {

                if (Functions.isNetworkAvailable(getBaseContext())) {

                    return true;

                }

                else {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.no_internet), Toast.LENGTH_LONG).show();
                }

            }

            else {
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.sys_data_change), Toast.LENGTH_LONG).show();
            }

        }
        else {
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.no_server_ip), Toast.LENGTH_LONG).show();
        }

        return false;
    }





    /**
     *  Функция закачивания файлов
     *  Если передан в аргументах второй параметр, то закачиваются только файлы с таким номером
     * @param startId       - ID сервиса
     * @param objectNumber  - номер объекта для загрузки
     */
    private void getFiles(int startId, int objectNumber) {

        allFilesSocket =                                    new Socket();
        int total;

        try {

            allFilesSocket.connect(new InetSocketAddress(InetAddress.getByName(serverIp), Settings.PORT), Settings.CONNECTION_TIMEOUT_PASSPORTS);

            piRequest.send(FragmentPassportsUpdate.CODE_STATUS_CONNECT);

            in =                                            new BufferedReader(new InputStreamReader(allFilesSocket.getInputStream()));
            out =                                           new PrintWriter(new BufferedWriter(new OutputStreamWriter(allFilesSocket.getOutputStream())), true);

            TelephonyManager tManager =                     (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String deviceId =                               tManager.getDeviceId();

            if (objectNumber > 0) {
                out.println("getFile:" + objectNumber + ":" + deviceId);
            }

            else {
                out.println("getFilesNew:" + lastUpdatedDate + ":" + deviceId);
            }

            out.flush();

            total =                                         Integer.parseInt(this.in.readLine());

            if(total == 0) {

                disconnectFromServer();

                mBuilder.setContentTitle(getResources().getString(R.string.update_no_files))
                        .setProgress(0, 0, false)
                        .setOngoing(false);

                nm.notify(notificationId, mBuilder.build());

                piRequest.send(FragmentPassportsUpdate.CODE_STATUS_DISCONNECT);

                return;
            }

            out.println("download");
            out.flush();

            isRunning =                                     true;

            for(int x = 1; x <= total; x++) {

                if(!isRunning) {
                    return;
                }

                String fileName =                           this.in.readLine();
                String fileSize =                           this.in.readLine();

                Socket serverFile =                         new Socket();
                serverFile.connect(new InetSocketAddress(InetAddress.getByName(serverIp), Settings.PORT_FILE), Settings.CONNECTION_TIMEOUT_PASSPORTS);

                byte[] buffer =                             new byte[8 * 1024];

                FileOutputStream fos =                      new FileOutputStream(
                                                                StartActivity
                                                                        .sharedPreferences
                                                                        .getString(FragmentSettings.PASSPORTS_PATH_KEY, null) + File.separator + fileName
                                                            );
                BufferedOutputStream bos =                  new BufferedOutputStream(fos);
                DataInputStream dis =                       new DataInputStream(serverFile.getInputStream());

                int count, totalLength =                    0;

                while ((count = dis.read(buffer, 0, buffer.length)) != -1) {

                    totalLength +=                          count;
                    bos.write(buffer, 0, count);
                    bos.flush();

                    if (totalLength == Long.parseLong(fileSize)) {

                        String t =                                  new Formatter()
                                .format(
                                        getResources().getString(R.string.update_set_progress),
                                        String.valueOf(x + "/" + total)
                                )
                                .toString();

                        mBuilder.setContentTitle(getResources().getString(R.string.update_continue))
                                .setContentText(t)
                                .setProgress(total, x, false)
                                .setNumber(x);

                        nm.notify(notificationId, mBuilder.build());


                        break;
                    }

                }

                fos.close();
                bos.close();
                dis.close();

                serverFile.close();

            }


            disconnectFromServer();

            piRequest.send(FragmentPassportsUpdate.CODE_STATUS_DISCONNECT);

            mBuilder.setContentTitle(getResources().getString(R.string.update_done))
                    .setProgress(0, 0, false)
                    .setOngoing(false);

            nm.notify(notificationId, mBuilder.build());


            //  Ставим метку о последнем обновлении файлов
            if(isRunning) {

                if(objectNumber == 0) {
//                    Functions.setPrefOption(Settings.lastUpdatedDate, String.valueOf(Calendar.getInstance().getTimeInMillis()), getApplicationContext());
                    StartActivity
                            .sharedPreferences
                            .edit()
                            .putString(FragmentPassportsUpdate.LAST_UPDATE_DATE_KEY, String.valueOf(Calendar.getInstance().getTimeInMillis()))
                            .apply();
                }

                isRunning =                                 false;
            }


        } catch (IOException e) {
            e.printStackTrace();
            mBuilder.setContentTitle(getResources().getString(R.string.no_server_connect))
                    .setProgress(0, 0, false)
                    .setOngoing(false);

            nm.notify(notificationId, mBuilder.build());

            try {
                piRequest.send(FragmentPassportsUpdate.CODE_STATUS_DISCONNECT);
            } catch (PendingIntent.CanceledException e1) {
                e1.printStackTrace();
            }

        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
            try {
                piRequest.send(FragmentPassportsUpdate.CODE_STATUS_DISCONNECT);
            } catch (PendingIntent.CanceledException e1) {
                e1.printStackTrace();
            }
        }


        stopSelf(startId);



    }





    /**
     *  Функция закрытия соединения с сервером обновлений
     */
    private void disconnectFromServer() {

        if(out != null && in != null) {

            out.println("disconnect");
            out.flush();


            try {
                allFilesSocket.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            out.close();

        }
    }


}
