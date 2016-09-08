package ru.korshun.cobaguardidea.app;

import android.Manifest;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Calendar;

import ru.korshun.cobaguardidea.app.fragments.FragmentPassportsUpdate;
import ru.korshun.cobaguardidea.app.fragments.FragmentSettings;


/**
 *  СЕрвис обновления паспортов
 */
public class UpdatePassportsService
        extends Service {



    /**
     *  Сокет для создания запроса и чтения ответов сервера
     */
    private Socket allFilesSocket;


    /**
     *  Сокет для непосредственного скачивания файлов
     */
    private Socket downloadFilesSocket;



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
//    private PendingIntent piRequest = null, piCounter = null, piTotal = null;


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
    private Intent intent;





    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {

        objectToDownload =                              intent.getIntExtra(FragmentPassportsUpdate.DOWNLOAD_TYPE, 0);

        if(Boot.sharedPreferences == null) {
            Boot.sharedPreferences =                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        }

        serverIp =                                      Boot.sharedPreferences.getString(FragmentSettings.SERVER_ADDRESS_KEY, null);
        lastUpdatedDate =                               Boot.sharedPreferences.getLong(FragmentPassportsUpdate.LAST_UPDATE_DATE_KEY, 0);

        this.intent =                                   new Intent(FragmentPassportsUpdate.BROADCAST_ACTION);

        nm =                                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder =                                      new NotificationCompat.Builder(getApplicationContext())
                                                                        .setSmallIcon(R.mipmap.ic_app_ico)
                                                                        .setOngoing(true);

        if(checkConnection()) {

            mBuilder
                    .setContentTitle(getResources().getString(R.string.passports_update_connect_title))
                    .setProgress(1, 1, true);

            nm
                    .notify(notificationId, mBuilder.build());


            this.intent.putExtra(FragmentPassportsUpdate.PI_STATUS, FragmentPassportsUpdate.CODE_STATUS_CONNECT);
            this.intent.putExtra(FragmentPassportsUpdate.PI_COUNT, 0);
            this.intent.putExtra(FragmentPassportsUpdate.PI_TOTAL, 0);

            sendBroadcast(this.intent);


            new Thread(new Runnable() {
                @Override
                public void run() {

                    getFiles(startId, objectToDownload);

                }
            }).start();

        }

        else {
            stopSelf(startId);
        }

        return START_REDELIVER_INTENT;
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
     *  Функция ищет все файлы, которые относятся к выбранному объекту, и удаляет их
     * @param objectNumber          - номер объекта, чьи файлы надо удалить
     */
    private void deleteEqualsFiles(final String objectNumber) {
        String passportsPath =                              Boot.sharedPreferences.getString(FragmentSettings.PASSPORTS_PATH_KEY, null);

        FilenameFilter filenameFilter =                     new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {

                return filename.contains(objectNumber);

            }

        };

        if(passportsPath != null) {
            File[] listFiles =                              new File(passportsPath).listFiles(filenameFilter);

            for(File searchFile : listFiles) {
                if(searchFile.getName().contains(Settings.OBJECT_PART_DIVIDER)) {
                    String fileNameSplit[] =                searchFile.getName().substring(
                                                                    0,
                                                                    searchFile
                                                                            .getName()
                                                                            .lastIndexOf(Settings.OBJECT_PART_DIVIDER)
                                                            ).split(",");

                    if (fileNameSplit.length > 1) {

                        for(String fileNamePart : fileNameSplit) {

                            if(fileNamePart.equals(objectNumber)) {
//                                    System.out.println("myLog: " + searchFile);
                                searchFile.delete();
                                break;
                            }

                        }

                    }

                    else {
                        if(fileNameSplit[0].equals(objectNumber)) {
//                                System.out.println("myLog: " + searchFile);
                            searchFile.delete();
                        }
                    }

                }

            }

        }

    }





    /**
     *  Функция закачивания файлов
     *  Если передан в аргументах второй параметр, то закачиваются только файлы с таким номером
     * @param startId       - ID сервиса
     * @param objectNumber  - номер объекта для загрузки
     */
    private void getFiles(int startId, final int objectNumber) {





        allFilesSocket =                                new Socket();



        // СОединение с сервером и обработка ошибки соединения
        try {
            allFilesSocket.connect(new InetSocketAddress(InetAddress.getByName(serverIp), Settings.PORT), Settings.CONNECTION_TIMEOUT_PASSPORTS);
        } catch (IOException e) {
            e.printStackTrace();

            mBuilder.setContentTitle(getResources().getString(R.string.no_server_connect))
                    .setProgress(0, 0, false)
                    .setOngoing(false);
            nm.notify(notificationId, mBuilder.build());

            intent.putExtra(FragmentPassportsUpdate.PI_STATUS, FragmentPassportsUpdate.CODE_STATUS_NO_CONNECT);
            sendBroadcast(intent);
            isRunning = false;

            stopSelf(startId);
            return;
        }

        int sizeFile;
        int sizeSend =                                      0;
        int total;

        in =                                            createBufferReader(allFilesSocket);
        out =                                           createPrintWriter(allFilesSocket);


        if(in == null || out == null) {

            try {
                allFilesSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(out != null) {
                out.close();
            }

            intent.putExtra(FragmentPassportsUpdate.PI_STATUS, FragmentPassportsUpdate.CODE_STATUS_ERROR);
            sendBroadcast(intent);

            mBuilder.setContentTitle(getResources().getString(R.string.err_data))
                    .setProgress(0, 0, false)
                    .setOngoing(false);
            nm.notify(notificationId, mBuilder.build());

            isRunning = false;
            stopSelf(startId);
            return;

        }


        intent.putExtra(FragmentPassportsUpdate.PI_STATUS, FragmentPassportsUpdate.CODE_STATUS_FIND_NEW_FILES);
        sendBroadcast(intent);


        TelephonyManager tManager =                     (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId =                               tManager.getDeviceId();



        if (objectNumber > 0) {

            deleteEqualsFiles(String.valueOf(objectNumber));

            out.println("getObjectFiles:" + objectNumber + ":" + deviceId + ":" + getResources().getString(R.string.version_short));

        }



        else {
            out.println("getNewFiles:" + lastUpdatedDate + ":" + deviceId + ":" + getResources().getString(R.string.version_short));
        }



        out.flush();



        int bufferSize;

        // Чтение ответа сервера на запрос количества файлов и обработка ошибки
        try {
            total =                                     Integer.parseInt(in.readLine());
            bufferSize =                                Integer.parseInt(in.readLine());
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            disconnectFromServer();

            intent.putExtra(FragmentPassportsUpdate.PI_STATUS, FragmentPassportsUpdate.CODE_STATUS_ERROR);
            sendBroadcast(intent);

            mBuilder.setContentTitle(getResources().getString(R.string.err_data))
                    .setProgress(0, 0, false)
                    .setOngoing(false);
            nm.notify(notificationId, mBuilder.build());

            isRunning = false;

            stopSelf(startId);
            return;

        }






        // Если файлов для обновления нет - сообщаем об этом и грохаем соединение с сервером
        if(total == 0) {

            disconnectFromServer();

            intent.putExtra(FragmentPassportsUpdate.PI_STATUS, FragmentPassportsUpdate.CODE_STATUS_NO_FILES);
            sendBroadcast(intent);

            mBuilder.setContentTitle(getResources().getString(R.string.passports_update_no_files))
                    .setProgress(0, 0, false)
                    .setOngoing(false);

            nm.notify(notificationId, mBuilder.build());

            stopSelf(startId);
            return;
        }




        intent.putExtra(FragmentPassportsUpdate.PI_TOTAL, total);
        intent.putExtra(FragmentPassportsUpdate.PI_COUNT, 0);
        sendBroadcast(intent);

        out.println("download");
        out.flush();


        intent.putExtra(FragmentPassportsUpdate.PI_STATUS, FragmentPassportsUpdate.CODE_STATUS_UPDATING);
        sendBroadcast(intent);



        downloadFilesSocket =                           new Socket();
        isRunning =                                     true;


            // СОединение с сервером для скачивания и обработка ошибки соединения
            try {
                downloadFilesSocket.connect(new InetSocketAddress(InetAddress.getByName(serverIp),
                        Settings.PORT_FILE), Settings.CONNECTION_TIMEOUT_PASSPORTS);
                downloadFilesSocket.setSoTimeout(15000);
            } catch (IOException e) {
                e.printStackTrace();

                disconnectFromServer();

                intent.putExtra(FragmentPassportsUpdate.PI_STATUS, FragmentPassportsUpdate.CODE_STATUS_NO_CONNECT);
                sendBroadcast(intent);

                mBuilder.setContentTitle(getResources().getString(R.string.no_server_connect))
                        .setProgress(0, 0, false)
                        .setOngoing(false);
                nm.notify(notificationId, mBuilder.build());

                isRunning = false;

                stopSelf(startId);
                return;
            }


        // Начинаем передачу файлов
        for(int x = 1; x <= total; x++) {

            if (!isRunning) {
                return;
            }

            String fileName;


            // ПОлучаем ответ сервера об имени файла и его размере и обработка ошибки
            try {
                fileName =                              this.in.readLine();
                sizeFile =                              Integer.parseInt(in.readLine());
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
                disconnectFromServer();

                intent.putExtra(FragmentPassportsUpdate.PI_STATUS, FragmentPassportsUpdate.CODE_STATUS_ERROR);
                sendBroadcast(intent);

                mBuilder.setContentTitle(getResources().getString(R.string.err_data))
                        .setProgress(0, 0, false)
                        .setOngoing(false);
                nm.notify(notificationId, mBuilder.build());

                isRunning = false;

                stopSelf(startId);
                return;
            }




            //СОздание файла, в который будет записанные принятые данные с сервера т обработка оишбки
            FileOutputStream fos;
            try {
                fos =                                   new FileOutputStream(
                                                                    Boot
                                                                        .sharedPreferences
                                                                        .getString(FragmentSettings.PASSPORTS_PATH_KEY, null) + File.separator + fileName
                                                        );
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                disconnectFromServer();

                intent.putExtra(FragmentPassportsUpdate.PI_STATUS, FragmentPassportsUpdate.CODE_STATUS_ERROR);
                sendBroadcast(intent);

                mBuilder.setContentTitle(getResources().getString(R.string.err_data))
                        .setProgress(0, 0, false)
                        .setOngoing(false);
                nm.notify(notificationId, mBuilder.build());

                isRunning = false;

                stopSelf(startId);
                return;
            }


            // СОздание буфера для принятия информации с сервера т обработка оишбки
            BufferedOutputStream bos =                  new BufferedOutputStream(fos);
            DataInputStream dis;
            DataOutputStream dosTestConnect;

            try {
                dis =                                   new DataInputStream(downloadFilesSocket.getInputStream());
                dosTestConnect =                        new DataOutputStream(downloadFilesSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                disconnectFromServer();

                intent.putExtra(FragmentPassportsUpdate.PI_STATUS, FragmentPassportsUpdate.CODE_STATUS_ERROR);
                sendBroadcast(intent);

                mBuilder.setContentTitle(getResources().getString(R.string.err_data))
                        .setProgress(0, 0, false)
                        .setOngoing(false);
                nm.notify(notificationId, mBuilder.build());

                isRunning = false;

                stopSelf(startId);
                return;
            }



            // Прием файла и обработка ошибки
            try {

                byte[] buffer =                         new byte[bufferSize * 1024];
                int sizeRead;



                // ЕБУЧАЯ ПРОВЕРКА РАЗРЕШЕНИЙ ДЛЯ 6го АНДРОИДА
//                if (Build.VERSION.SDK_INT >= 23) {
//
//                    if(!(ContextCompat.checkSelfPermission(getApplicationContext(),
//                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
//
//                    }
//
//                    else {
//
//                    }

//                    disconnectFromServer();
//
//                    intent.putExtra(FragmentPassportsUpdate.PI_STATUS, FragmentPassportsUpdate.CODE_STATUS_ERROR);
//                    sendBroadcast(intent);
//
//                    mBuilder.setContentTitle(getResources().getString(R.string.err_data))
//                            .setProgress(0, 0, false)
//                            .setOngoing(false);
//                    nm.notify(notificationId, mBuilder.build());
//
//                    isRunning = false;
//
//                    stopSelf(startId);
//                    return;
//
//                }


                while ((sizeRead = dis.read(buffer, 0, buffer.length)) != -1) {


                    sizeSend +=             sizeRead;
                    bos.write(buffer, 0, sizeRead);
                    bos.flush();

                    if (sizeSend == sizeFile) {

                        dosTestConnect.write(1);
                        dosTestConnect.flush();

                        intent.putExtra(FragmentPassportsUpdate.PI_COUNT, x);
                        sendBroadcast(intent);

                        mBuilder.setContentTitle(getResources().getString(R.string.passports_update_proccess_title))
                                .setContentText(x + "/" + total)
                                .setProgress(total, x, false)
                                .setNumber(x);

                        nm.notify(notificationId, mBuilder.build());

                        sizeSend =          0;

                        break;
                    }

                    if(sizeSend > sizeFile) {
                        dosTestConnect.write(0);
                        dosTestConnect.flush();
                        break;
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
                disconnectFromServer();

                try {
                    dosTestConnect.write(0);
                    dosTestConnect.flush();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    downloadFilesSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                intent.putExtra(FragmentPassportsUpdate.PI_STATUS, FragmentPassportsUpdate.CODE_STATUS_ERROR);
                sendBroadcast(intent);

                mBuilder.setContentTitle(getResources().getString(R.string.err_data))
                        .setProgress(0, 0, false)
                        .setOngoing(false);
                nm.notify(notificationId, mBuilder.build());

                isRunning = false;

                stopSelf(startId);
                return;
            }


        }




        // Отключение от сервера
        disconnectFromServer();

        intent.putExtra(FragmentPassportsUpdate.PI_STATUS, FragmentPassportsUpdate.CODE_STATUS_DISCONNECT);
        sendBroadcast(intent);

        mBuilder.setContentTitle(getResources().getString(R.string.passports_update_complite_title))
                .setProgress(0, 0, false)
                .setOngoing(false);

        nm.notify(notificationId, mBuilder.build());

        if(isRunning) {

            if(objectNumber == 0) {
                Boot
                        .sharedPreferences
                        .edit()
                        .putLong(FragmentPassportsUpdate.LAST_UPDATE_DATE_KEY, Calendar.getInstance().getTimeInMillis())
                        .apply();
            }

            isRunning =                                 false;
        }


        stopSelf(startId);


    }





    /**
     *  Создание BufferedReader для чтения ответов сервера
     * @param connectSocket             - ссылка на серверный сокет
     * @return                          - возвращается созданный BufferedReader
     */
    private BufferedReader createBufferReader(Socket connectSocket) {
        try {
            return new BufferedReader(new InputStreamReader(connectSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }




    /**
     *  Создание PrintWriter для отправки запросов на сервер
     * @param connectSocket             - ссылка на серверный сокет
     * @return                          - возвращается созданный PrintWriter
     */

    private PrintWriter createPrintWriter(Socket connectSocket) {
        try {
            return new PrintWriter(new BufferedWriter(new OutputStreamWriter(connectSocket.getOutputStream())), true);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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
