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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
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
    private PendingIntent piRequest = null, piCounter = null, piTotal = null;


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

        serverIp =                                      Boot.sharedPreferences.getString(FragmentSettings.SERVER_ADDRESS_KEY, null);
        lastUpdatedDate =                               Boot.sharedPreferences.getLong(FragmentPassportsUpdate.LAST_UPDATE_DATE_KEY, 0);

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


            if(piRequest != null) {
                try {
                    piRequest.send(FragmentPassportsUpdate.CODE_STATUS_CONNECT);
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                    try {
                        piRequest.send(FragmentPassportsUpdate.CODE_STATUS_CONNECT);
                    } catch (PendingIntent.CanceledException e1) {
                        e1.printStackTrace();
                        try {
                            piRequest.send(FragmentPassportsUpdate.CODE_STATUS_CONNECT);
                        } catch (PendingIntent.CanceledException e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            }

            new Thread(new Runnable() {
                @Override
                public void run() {

                    getFiles(startId, objectToDownload);







//
//                    try {
//                        TimeUnit.SECONDS.sleep(3);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                        stopSelf(startId);
//                    }
//
//                    int count = 30;
//
//
//                    for(int x = 0; x < count; x++) {
//
//                        try {
//                            TimeUnit.SECONDS.sleep(1);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                            stopSelf(startId);
//                        }
//
//
//                    }

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

        allFilesSocket =                                new Socket();
        int total;



        // СОединение с сервером и обработка ошибки соединения
        try {
            allFilesSocket.connect(new InetSocketAddress(InetAddress.getByName(serverIp), Settings.PORT), Settings.CONNECTION_TIMEOUT_PASSPORTS);
        } catch (IOException e) {
            e.printStackTrace();

            mBuilder.setContentTitle(getResources().getString(R.string.no_server_connect))
                    .setProgress(0, 0, false)
                    .setOngoing(false);
            nm.notify(notificationId, mBuilder.build());

            if(piRequest != null) {
                try {
                    piRequest.send(FragmentPassportsUpdate.CODE_STATUS_NO_CONNECT);
                } catch (PendingIntent.CanceledException e1) {
                    e1.printStackTrace();
                    try {
                        piRequest.send(FragmentPassportsUpdate.CODE_STATUS_NO_CONNECT);
                    } catch (PendingIntent.CanceledException e2) {
                        e2.printStackTrace();
                        try {
                            piRequest.send(FragmentPassportsUpdate.CODE_STATUS_NO_CONNECT);
                        } catch (PendingIntent.CanceledException e3) {
                            e3.printStackTrace();
                        }
                    }
                }
            }
            stopSelf(startId);
            return;
        }




        // СОздание BufferedReader и обработка ошибки создания
        try {
            in =                                        new BufferedReader(new InputStreamReader(allFilesSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            sendErrorMsg();
            try {
                allFilesSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            stopSelf(startId);
            return;
        }




        // СОздание PrintWriter и обработка ошибки создания
        try {
            out =                                       new PrintWriter(new BufferedWriter(new OutputStreamWriter(allFilesSocket.getOutputStream())), true);
        } catch (IOException e) {
            e.printStackTrace();
            sendErrorMsg();
            try {
                allFilesSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            stopSelf(startId);
            return;
        }



        TelephonyManager tManager =                     (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId =                               tManager.getDeviceId();



        if (objectNumber > 0) {
            out.println("getFile:" + objectNumber + ":" + deviceId);
        }



        else {
            out.println("getFilesNew:" + lastUpdatedDate + ":" + deviceId);
        }



        out.flush();





        // Чтение ответа сервера на запрос количества файлов и обработка ошибки
        try {
            total =                                     Integer.parseInt(this.in.readLine());
        } catch (IOException e) {
            e.printStackTrace();
            sendErrorMsg();
            try {
                allFilesSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            stopSelf(startId);
            return;

        }






        // Если файлов для обновления нет - сообщаем об этом и грохаем соединение с сервером
        if(total == 0) {

            disconnectFromServer();

            mBuilder.setContentTitle(getResources().getString(R.string.update_no_files))
                    .setProgress(0, 0, false)
                    .setOngoing(false);

            nm.notify(notificationId, mBuilder.build());

            if(piRequest != null) {
                try {
                    piRequest.send(FragmentPassportsUpdate.CODE_STATUS_NO_FILES);
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                    try {
                        piRequest.send(FragmentPassportsUpdate.CODE_STATUS_NO_FILES);
                    } catch (PendingIntent.CanceledException e1) {
                        e1.printStackTrace();
                        try {
                            piRequest.send(FragmentPassportsUpdate.CODE_STATUS_NO_FILES);
                        } catch (PendingIntent.CanceledException e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            }

            try {
                allFilesSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            stopSelf(startId);
            return;
        }




        if(piTotal != null) {
            try {
                piTotal.send(total);
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
                try {
                    piTotal.send(total);
                } catch (PendingIntent.CanceledException e1) {
                    e1.printStackTrace();
                    try {
                        piTotal.send(total);
                    } catch (PendingIntent.CanceledException e2) {
                        e2.printStackTrace();
                    }
                }
            }
        }





        out.println("download");
        out.flush();

        isRunning =                                     true;



        // Начинаем передачу файлов
        for(int x = 1; x <= total; x++) {

            if (!isRunning) {
                return;
            }

            String fileName, fileSize;


            // ПОлучаем ответ сервера об имени файла и его размере и обработка ошибки
            try {
                fileName =                              this.in.readLine();
                fileSize =                              this.in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                sendErrorMsg();

                try {
                    allFilesSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                stopSelf(startId);
                return;
            }


            Socket serverFile =                         new Socket();

            // СОединение с сервером для скачивания и обработка ошибки соединения
            try {
                serverFile.connect(new InetSocketAddress(InetAddress.getByName(serverIp), Settings.PORT_FILE), Settings.CONNECTION_TIMEOUT_PASSPORTS);
            } catch (IOException e) {
                e.printStackTrace();
                sendErrorMsg();

                try {
                    allFilesSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                stopSelf(startId);
                return;
            }


            byte[] buffer =                             new byte[8 * 1024];


            // СОздание файла, в который будет записанные принятые данные с сервера т обработка оишбки
            FileOutputStream fos;
            try {
                fos =                                   new FileOutputStream(
                                                                    Boot
                                                                        .sharedPreferences
                                                                        .getString(FragmentSettings.PASSPORTS_PATH_KEY, null) + File.separator + fileName
                                                        );
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                sendErrorMsg();

                try {
                    allFilesSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                stopSelf(startId);
                return;
            }


            // СОздание буфера для принятия информации с сервера т обработка оишбки
            BufferedOutputStream bos =                  new BufferedOutputStream(fos);
            DataInputStream dis;
            try {
                dis =                                   new DataInputStream(serverFile.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                sendErrorMsg();

                try {
                    allFilesSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                stopSelf(startId);
                return;
            }


            int count, totalLength =                    0;





            // Прием файла и обработка ошибки
            try {

                while ((count = dis.read(buffer, 0, buffer.length)) != -1) {

                    totalLength +=                      count;
                    bos.write(buffer, 0, count);
                    bos.flush();

                    if (totalLength == Long.parseLong(fileSize)) {

                        mBuilder.setContentTitle(getResources().getString(R.string.passports_update_proccess_title))
                                .setContentText(x + "/" + total)
                                .setProgress(total, x, false)
                                .setNumber(x);

                        nm.notify(notificationId, mBuilder.build());


                        if(piTotal != null & piCounter != null) {
                            try {
                                piCounter.send(x);
                                piTotal.send(total);
                            } catch (PendingIntent.CanceledException e) {
                                e.printStackTrace();
                                try {
                                    piCounter.send(x);
                                    piTotal.send(total);
                                } catch (PendingIntent.CanceledException e1) {
                                    e1.printStackTrace();
                                    try {
                                        piCounter.send(x);
                                        piTotal.send(total);
                                    } catch (PendingIntent.CanceledException e2) {
                                        e2.printStackTrace();
                                    }
                                }
                            }
                        }


                        break;
                    }


                }

            } catch (IOException e) {
                e.printStackTrace();
                sendErrorMsg();

                try {
                    allFilesSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                stopSelf(startId);
                return;
            } finally {

                // Закрытие всех буферов
                try {
                    fos.close();
                    bos.close();
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                // Отключение от порта передачи файлов
                try {
                    serverFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }



        }




        // Отключение от сервера
        disconnectFromServer();

        mBuilder.setContentTitle(getResources().getString(R.string.passports_update_complite_title))
                .setProgress(0, 0, false)
                .setOngoing(false);

        nm.notify(notificationId, mBuilder.build());

        if(piRequest != null) {
            try {
                piRequest.send(FragmentPassportsUpdate.CODE_STATUS_DISCONNECT);
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
                try {
                    piRequest.send(FragmentPassportsUpdate.CODE_STATUS_DISCONNECT);
                } catch (PendingIntent.CanceledException e1) {
                    e1.printStackTrace();
                    try {
                        piRequest.send(FragmentPassportsUpdate.CODE_STATUS_DISCONNECT);
                    } catch (PendingIntent.CanceledException e2) {
                        e2.printStackTrace();
                    }
                }
            }
        }

        //  Ставим метку о последнем обновлении файлов
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
     *  Функция отправляет сообщение об ошибке в панель нотификации и в приложение в фрагмент с обновлением
     */
    private void sendErrorMsg() {
        mBuilder.setContentTitle(getResources().getString(R.string.err_data))
                .setProgress(0, 0, false)
                .setOngoing(false);
        nm.notify(notificationId, mBuilder.build());

        if(piRequest != null) {
            try {
                piRequest.send(FragmentPassportsUpdate.CODE_STATUS_ERROR);
            } catch (PendingIntent.CanceledException e1) {
                e1.printStackTrace();
                try {
                    piRequest.send(FragmentPassportsUpdate.CODE_STATUS_ERROR);
                } catch (PendingIntent.CanceledException e2) {
                    e2.printStackTrace();
                    try {
                        piRequest.send(FragmentPassportsUpdate.CODE_STATUS_ERROR);
                    } catch (PendingIntent.CanceledException e3) {
                        e3.printStackTrace();
                    }
                }
            }
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
