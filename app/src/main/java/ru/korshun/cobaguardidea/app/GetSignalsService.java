package ru.korshun.cobaguardidea.app;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.telephony.TelephonyManager;

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
import java.util.Timer;
import java.util.TimerTask;

import ru.korshun.cobaguardidea.app.fragments.FragmentSignals;


/**
 *  Сервис, отвечающий за загрузку файлов с сигналами
 */
public class GetSignalsService
        extends MyServiceSignalsAndGuard {


    private int                     objectToCheckSignals;
    private String                  cobaSignalsPath;

//    private final String            SERVER_IP =                                 "192.168.43.138";
//    private final String            SERVER_IP =                                 Settings.SERVERS_IP_ARRAY[0];
//    private final String            SERVER_IP =                                 Settings.SERVERS_IP_ARRAY[3]; "192.168.43.138" 85.12.240.55


    private Socket                  connectSocket;
    private PrintWriter             out =                                       null;
    private BufferedReader          in =                                        null;


    /**
     *  PI для "общения" с Activity
     *  Для отсыла статусов соединения, счетчика переданных файлов, общего числа файлов и ошибок
     */
    private PendingIntent piRequest;







    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        System.out.println("myapp : onStartCommand");

        if(intent != null) {
            piRequest =                                 intent.getParcelableExtra(RootActivity.PI_REQUEST);
            objectToCheckSignals =                      intent.getIntExtra(FragmentSignals.OBJECT_TO_CHECK_SIGNALS, 0);
            cobaSignalsPath =                           intent.getStringExtra(FragmentSignals.SIGNALS_PATH);

            Timer timer =                               new Timer();
            ScheduledCheckSignals st =                  new ScheduledCheckSignals(objectToCheckSignals, timer, startId);

            timer.schedule(st, 0, Settings.CHECK_SIGNALS_REPEAT_IN_SECONDS * 1000);
        }

        return START_STICKY;
    }




    /**
     *  В случае успеха или превышения счетчика проверок, останавливаем таймер и закрываем
     *  соединение с сервером
     */
    protected void disconnect() {

        out.println("disconnect");
        out.flush();

        if(out != null) {
            out.close();
        }

        if(in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            connectSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }




    /**
     *  Функция соединения с сервером и создания PrintWriter'а
     */
    protected boolean connect() {

        connectSocket =                             new Socket();

        try {
            connectSocket.connect(new InetSocketAddress(InetAddress.getByName(SERVER_IP), Settings.PORT), Settings.CONNECTION_TIMEOUT_SIGNALS);
        } catch (IOException e) {
            e.printStackTrace();

            try {
                piRequest.send(FragmentSignals.SIGNALS_STATUS_CONNECT_ERROR);
            } catch (PendingIntent.CanceledException e1) {
                e1.printStackTrace();
            }

            return false;
        }


        // создаем Reader для работы с ответами сервера
        try {
            this.in =                               new BufferedReader(new InputStreamReader(connectSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }


        // создаем Writer для работы с сервером
        try {
            out =                                   new PrintWriter(new BufferedWriter(new OutputStreamWriter(connectSocket.getOutputStream())), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }





    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }














    /**
     *  Класс таймера для создания периодического задания проверки файла с сигналами
     */
    class ScheduledCheckSignals
        extends TimerTask {



        private Timer timer;

        private int connectsCount =                     0;

        private int objectNumber, startId;


        private TelephonyManager tManager =             (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        private String deviceId =                       tManager.getDeviceId();

        private int fileSize =                          0;




        public ScheduledCheckSignals(int objectNumber, Timer timer, int startId) {
            this.objectNumber =                         objectNumber;
            this.startId =                              startId;
            this.timer =                                timer;
        }












        @Override
        public void run() {

            DbHelper dbHelper =                         new DbHelper(getApplicationContext(), Settings.DB_NAME, Settings.DB_VERSION);
            SQLiteDatabase db =                         dbHelper.getWritableDatabase();

            if(checkConnection()) {


                // Если попытка соединения не удалась - выходим из функции и делаем запись в БД
                if(!connect()) {
                    setStatusToDb(db, FragmentSignals.SIGNALS_STATUS_CONNECT_ERROR, objectToCheckSignals, DbHelper.DB_TABLE_SIGNALS);
                    dbHelper.close();
                    timer.cancel();
                    try {
                        piRequest.send(FragmentSignals.SIGNALS_STATUS_CONNECT_ERROR);
                    } catch (PendingIntent.CanceledException e1) {
                        e1.printStackTrace();
                    } finally {
                        stopSelf(startId);
                    }

                    return;
                }


                connectsCount++;



                if(out != null) {

                    // отправляем на сервер команду запроса файла
                    out.println("getSignalFile:" + objectToCheckSignals + ":" + deviceId);
                    out.flush();

                    // читаем ответ сервера
                    if (in != null) {


                        try {
                            String query =              in.readLine();

                            if (query != null) {
                                fileSize =              Integer.parseInt(query);
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                            disconnect();
                            timer.cancel();
                            try {
                                piRequest.send(FragmentSignals.SIGNALS_STATUS_ERROR);
                            } catch (PendingIntent.CanceledException e1) {
                                e1.printStackTrace();
                            } finally {
                                stopSelf(startId);
                            }

                            return;
                        }


                        // если размер файла равен -4 - ошибка, IMEI не найден
                        if (fileSize == FragmentSignals.SIGNALS_STATUS_AUTH_ERROR) {

                            setStatusToDb(db, FragmentSignals.SIGNALS_STATUS_AUTH_ERROR, objectNumber, DbHelper.DB_TABLE_SIGNALS);
                            dbHelper.close();
                            try {
                                piRequest.send(FragmentSignals.SIGNALS_STATUS_AUTH_ERROR);
                            } catch (PendingIntent.CanceledException e) {
                                e.printStackTrace();
                                disconnect();
                                timer.cancel();

                                try {
                                    piRequest.send(FragmentSignals.SIGNALS_STATUS_ERROR);
                                } catch (PendingIntent.CanceledException e1) {
                                    e1.printStackTrace();
                                } finally {
                                    stopSelf(startId);
                                }

                                return;
                            }

                        }


                        // если сервер "плюнул" число больше, чем 0, это размер файла - начинаем загрузку
                        if (fileSize > 0) {

                            String fileName;

                            try {
                                fileName =              in.readLine();
                            } catch (IOException e) {
                                e.printStackTrace();
                                disconnect();
                                timer.cancel();
                                try {
                                    piRequest.send(FragmentSignals.SIGNALS_STATUS_ERROR);
                                } catch (PendingIntent.CanceledException e1) {
                                    e1.printStackTrace();
                                } finally {
                                    stopSelf(startId);
                                }

                                return;
                            }

                            Socket serverFile =         new Socket();

                            try {
                                serverFile.connect(new InetSocketAddress(InetAddress.getByName(SERVER_IP), Settings.PORT_FILE), Settings.CONNECTION_TIMEOUT_SIGNALS);
                            } catch (IOException e) {
                                e.printStackTrace();
                                setStatusToDb(db, FragmentSignals.SIGNALS_STATUS_CONNECT_ERROR, objectToCheckSignals, DbHelper.DB_TABLE_SIGNALS);
                                dbHelper.close();
                                disconnect();
                                timer.cancel();
                                try {
                                    piRequest.send(FragmentSignals.SIGNALS_STATUS_CONNECT_ERROR);
                                } catch (PendingIntent.CanceledException e1) {
                                    e1.printStackTrace();
                                } finally {
                                    stopSelf(startId);
                                }

                                return;
                            }

                            byte[] buffer = new byte[8 * 1024];

                            try {
                                FileOutputStream fos =          new FileOutputStream(cobaSignalsPath + File.separator + fileName);
                                BufferedOutputStream bos =      new BufferedOutputStream(fos);
                                DataInputStream dis =           new DataInputStream(serverFile.getInputStream());

                                int count, totalLength = 0;

                                while ((count = dis.read(buffer, 0, buffer.length)) != -1) {

                                    totalLength +=              count;
                                    bos.write(buffer, 0, count);
                                    bos.flush();

                                    if (totalLength == fileSize) {
                                        break;
                                    }

                                }

                                fos.close();
                                bos.close();
                                dis.close();

                                serverFile.close();

                                setStatusToDb(db, FragmentSignals.SIGNALS_STATUS_COMPLITE, objectNumber, DbHelper.DB_TABLE_SIGNALS);
                                dbHelper.close();
                                disconnect();
                                timer.cancel();

                                try {
                                    piRequest.send(FragmentSignals.SIGNALS_STATUS_COMPLITE);
                                } catch (PendingIntent.CanceledException e) {
                                    e.printStackTrace();
                                } finally {
                                    stopSelf(startId);
                                }

                                return;

                            } catch (IOException e) {
                                e.printStackTrace();
                                disconnect();
                                timer.cancel();
                                try {
                                    piRequest.send(FragmentSignals.SIGNALS_STATUS_ERROR);
                                } catch (PendingIntent.CanceledException e1) {
                                    e1.printStackTrace();
                                } finally {
                                    stopSelf(startId);
                                }

                                return;
                            }

                        }

                    }

                }

                // если число попыток скачать файл достигло максимума, то останавливаем
                // таймер, закрываем соединение и отсылаем код ошибки
                if(connectsCount >= Settings.CHECK_SIGNALS_CONNECTS_MAX_COUNT) {

                    setStatusToDb(db, FragmentSignals.SIGNALS_STATUS_ERROR, objectNumber, DbHelper.DB_TABLE_SIGNALS);
                    dbHelper.close();
                    disconnect();
                    timer.cancel();

                    try {
                        piRequest.send(FragmentSignals.SIGNALS_STATUS_ERROR);
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    } finally {
                        stopSelf(startId);
                    }

                    return;
                }


                disconnect();

            }



            // если соединения с интернетом нет, ставим метку в БД
            else {
                setStatusToDb(db, FragmentSignals.SIGNALS_STATUS_NO_INTERNET, objectToCheckSignals, DbHelper.DB_TABLE_SIGNALS);
                dbHelper.close();
                disconnect();
                timer.cancel();
                try {
                    piRequest.send(FragmentSignals.SIGNALS_STATUS_NO_INTERNET);
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                } finally {
                    stopSelf(startId);
                }
            }

        }

    }


}
