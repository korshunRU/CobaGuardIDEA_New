package ru.korshun.cobaguardidea.app;


import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import ru.korshun.cobaguardidea.app.fragments.FragmentObjects;
import ru.korshun.cobaguardidea.app.fragments.FragmentSignals;


/**
 *  Сервис, отвечающий за загрузку файлов с сигналами
 */
public class SetGuardStatusService
        extends MyServiceSignalsAndGuard {


    private int                     objectToSetGuard;
    private String                  objectGuardStatus;

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
//    private PendingIntent piRequest;
    private Intent intent;






    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        System.out.println("myapp : onStartCommand");

        if(intent != null) {
//            piRequest =                                 intent.getParcelableExtra(RootActivity.PI_REQUEST);
            objectToSetGuard =                          intent.getIntExtra(FragmentObjects.OBJECT_TO_SET_GUARD, 0);
            this.intent =                               new Intent(FragmentObjects.BROADCAST_ACTION);

            switch (intent.getIntExtra(FragmentObjects.OBJECT_GUARD_STATUS, 0)) {

                case FragmentObjects.GUARD_STATUS_ON:
                    objectGuardStatus =                 "*";
                    break;

                case FragmentObjects.GUARD_STATUS_OFF:
                    objectGuardStatus =                 "#";
                    break;

                case FragmentObjects.GUARD_STATUS_CLEAR_FILE:
                    objectGuardStatus =                 "0";
                    break;

            }



            Timer timer =                               new Timer();
            ScheduledCheckGuardAnswer st =              new ScheduledCheckGuardAnswer(objectToSetGuard, objectGuardStatus, timer, startId);

            timer.schedule(st, 0, Settings.CHECK_GUARD_REPEAT_IN_SECONDS * 1000);
        }

        return START_REDELIVER_INTENT;
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
            connectSocket.connect(new InetSocketAddress(InetAddress.getByName(SERVER_IP), Settings.PORT), Settings.CONNECTION_TIMEOUT_GUARD);
        } catch (IOException e) {
            e.printStackTrace();

            intent.putExtra(FragmentObjects.PI_STATUS, FragmentObjects.GUARD_STATUS_CONNECT_ERROR);
            sendBroadcast(intent);

//            try {
//                piRequest.send(FragmentObjects.GUARD_STATUS_CONNECT_ERROR);
//            } catch (PendingIntent.CanceledException e1) {
//                e1.printStackTrace();
//            }

            return false;
        }


        // создаем Reader для работы с ответами сервера
        try {
            this.in =                               new BufferedReader(new InputStreamReader(connectSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();

            intent.putExtra(FragmentObjects.PI_STATUS, FragmentObjects.GUARD_STATUS_ERROR);
            sendBroadcast(intent);

            return false;
        }


        // создаем Writer для работы с сервером
        try {
            out =                                   new PrintWriter(new BufferedWriter(new OutputStreamWriter(connectSocket.getOutputStream())), true);
        } catch (IOException e) {
            e.printStackTrace();

            intent.putExtra(FragmentObjects.PI_STATUS, FragmentObjects.GUARD_STATUS_ERROR);
            sendBroadcast(intent);

            return false;
        }

        return true;
    }





    /**
     *  Установка статуса операции в БД
     * @param db                - ссылка на объект SQLiteDatabase
     * @param status            - числовое значение статуса объекта (охрана\без охраны)
     * @param objectNumber      - номер объекта, для которого устанавливаем статус
     */
    protected void setGuardStatusToDb(SQLiteDatabase db, int status, int objectNumber, String tableName) {
        db.execSQL("UPDATE " + tableName + " " +
                "SET status = " + status + " " +
                "WHERE number = " + objectNumber);
//        dbHelper.close();
    }





    /**
     *  Установка статуса операции в БД
     * @param db                - ссылка на объект SQLiteDatabase
     * @param objectNumber      - номер объекта
     */
    protected void deleteItemFromDb(SQLiteDatabase db, int objectNumber, String tableName) {
        db.execSQL( "DELETE FROM " + tableName + " " +
                    "WHERE number = " + objectNumber);
//        dbHelper.close();
    }





    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }














    /**
     *  Класс таймера для создания периодического задания проверки файла с сигналами
     */
    class ScheduledCheckGuardAnswer
        extends TimerTask {



        private Timer timer;

        private int connectsCount =                     0;

        private int objectNumber, startId;

        private String objectGuardStatus;


//        private DbHelper dbHelper;
//        private SQLiteDatabase db;

        private TelephonyManager tManager =             (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        private String deviceId =                       tManager.getDeviceId();

        private int guardStatusFromServer =             0;




        public ScheduledCheckGuardAnswer(int objectNumber, String objectGuardStatus, Timer timer, int startId) {
            this.objectNumber =                         objectNumber;
            this.objectGuardStatus =                    objectGuardStatus;
            this.startId =                              startId;
            this.timer =                                timer;
//            this.dbHelper =                             dbHelper;
//            this.db =                                   db;
        }












        @Override
        public void run() {


            DbHelper dbHelper =                         new DbHelper(getApplicationContext(), Settings.DB_NAME, Settings.DB_VERSION);
            SQLiteDatabase db =                         dbHelper.getWritableDatabase();


            if(checkConnection()) {

                // Если попытка соединения не удалась - выходим из функции и делаем запись в БД
                if(!connect()) {
                    setStatusToDb(db, FragmentObjects.GUARD_STATUS_CONNECT_ERROR, objectToSetGuard, DbHelper.DB_TABLE_GUARD);
                    dbHelper.close();
                    timer.cancel();

                    intent.putExtra(FragmentObjects.PI_STATUS, FragmentObjects.GUARD_STATUS_CONNECT_ERROR);
                    sendBroadcast(intent);

                    stopSelf(startId);

//                    try {
//                        piRequest.send(FragmentObjects.GUARD_STATUS_CONNECT_ERROR);
//                    } catch (PendingIntent.CanceledException e1) {
//                        e1.printStackTrace();
//                    } finally {
//                        stopSelf(startId);
//                    }

                    return;
                }

                connectsCount++;


                if(out != null) {

                    // отправляем на сервер команду запроса файла
                    out.println("setObjectStatus:" + objectToSetGuard + ":" + objectGuardStatus + ":" + deviceId);
                    out.flush();

                    // читаем ответ сервера
                    if (in != null) {


                        try {
                            String query =              in.readLine();

                            if (query != null) {
                                guardStatusFromServer = Integer.parseInt(query);
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                            setStatusToDb(db, FragmentObjects.GUARD_STATUS_ERROR, objectNumber, DbHelper.DB_TABLE_GUARD);
                            dbHelper.close();
                            disconnect();
                            timer.cancel();

                            intent.putExtra(FragmentObjects.PI_STATUS, FragmentObjects.GUARD_STATUS_ERROR);
                            sendBroadcast(intent);

                            stopSelf(startId);

//                            try {
//                                piRequest.send(FragmentObjects.GUARD_STATUS_ERROR);
//                            } catch (PendingIntent.CanceledException e1) {
//                                e1.printStackTrace();
//                            } finally {
//                                stopSelf(startId);
//                            }
                            return;
                        }



                        // если ответ равен -1 - ошибкa
                        if (guardStatusFromServer == FragmentObjects.GUARD_STATUS_ERROR) {

                            setStatusToDb(db, FragmentObjects.GUARD_STATUS_ERROR, objectNumber, DbHelper.DB_TABLE_GUARD);
                            dbHelper.close();
                            disconnect();
                            timer.cancel();

                            intent.putExtra(FragmentObjects.PI_STATUS, FragmentObjects.GUARD_STATUS_ERROR);
                            sendBroadcast(intent);

                            stopSelf(startId);

//                            try {
//                                piRequest.send(FragmentObjects.GUARD_STATUS_ERROR);
//                            } catch (PendingIntent.CanceledException e) {
//                                e.printStackTrace();
//                            } finally {
//                                stopSelf(startId);
//                            }

                            return;

                        }





                        // если ответ равен -4 - ошибка, IMEI не найден
                        else if (guardStatusFromServer == FragmentObjects.GUARD_STATUS_AUTH_ERROR) {

                            setStatusToDb(db, FragmentObjects.GUARD_STATUS_AUTH_ERROR, objectNumber, DbHelper.DB_TABLE_GUARD);
                            dbHelper.close();
                            disconnect();
                            timer.cancel();

                            intent.putExtra(FragmentObjects.PI_STATUS, FragmentObjects.GUARD_STATUS_AUTH_ERROR);
                            sendBroadcast(intent);

                            stopSelf(startId);

//                            try {
//                                piRequest.send(FragmentObjects.GUARD_STATUS_AUTH_ERROR);
//                            } catch (PendingIntent.CanceledException e) {
//                                e.printStackTrace();
//                            } finally {
//                                stopSelf(startId);
//                            }

                            return;

                        }


                        // если ответ равен -5 - файл запроса на сервере обнулен
                        if (guardStatusFromServer == FragmentObjects.GUARD_STATUS_CLEAR_FILE) {

                            deleteItemFromDb(db, objectNumber, DbHelper.DB_TABLE_GUARD);
                            dbHelper.close();
                            disconnect();
                            timer.cancel();

                            intent.putExtra(FragmentObjects.PI_STATUS, FragmentObjects.GUARD_STATUS_CLEAR_FILE);
                            sendBroadcast(intent);

                            stopSelf(startId);

//                            try {
//                                piRequest.send(FragmentObjects.GUARD_STATUS_CLEAR_FILE);
//                            } catch (PendingIntent.CanceledException e) {
//                                e.printStackTrace();
//                            } finally {
//                                stopSelf(startId);
//                            }

                            return;

                        }


                        // если ответ равен -6 - файл запроса занят другой записью
                        if (guardStatusFromServer == FragmentObjects.GUARD_STATUS_QUERY_ERROR) {

                            setStatusToDb(db, FragmentObjects.GUARD_STATUS_QUERY_ERROR, objectNumber, DbHelper.DB_TABLE_GUARD);
                            dbHelper.close();
                            disconnect();
                            timer.cancel();

                            intent.putExtra(FragmentObjects.PI_STATUS, FragmentObjects.GUARD_STATUS_QUERY_ERROR);
                            sendBroadcast(intent);

                            stopSelf(startId);

//                            try {
//                                piRequest.send(FragmentObjects.GUARD_STATUS_QUERY_ERROR);
//                            } catch (PendingIntent.CanceledException e) {
//                                e.printStackTrace();
//                            } finally {
//                                stopSelf(startId);
//                            }

                            return;

                        }


                        // если ответ равен 1 - объект снят с охраны
                        else if(guardStatusFromServer == FragmentObjects.GUARD_STATUS_OFF) {

                            setStatusToDb(db, FragmentObjects.GUARD_STATUS_OFF, objectNumber, DbHelper.DB_TABLE_GUARD);
                            setGuardStatusToDb(db, FragmentObjects.GUARD_STATUS_OFF, objectNumber, DbHelper.DB_TABLE_GUARD);
                            dbHelper.close();
                            disconnect();
                            timer.cancel();

                            intent.putExtra(FragmentObjects.PI_STATUS, FragmentObjects.GUARD_STATUS_OFF);
                            sendBroadcast(intent);

                            stopSelf(startId);

//                            try {
//                                piRequest.send(FragmentObjects.GUARD_STATUS_OFF);
//                            } catch (PendingIntent.CanceledException e) {
//                                e.printStackTrace();
//                            } finally {
//                                stopSelf(startId);
//                            }

                            return;

                        }

                        // если ответ равен 2 - объект поставлен на охрану
                        else if(guardStatusFromServer == FragmentObjects.GUARD_STATUS_ON) {

                            setStatusToDb(db, FragmentObjects.GUARD_STATUS_ON, objectNumber, DbHelper.DB_TABLE_GUARD);
                            setGuardStatusToDb(db, FragmentObjects.GUARD_STATUS_ON, objectNumber, DbHelper.DB_TABLE_GUARD);
                            dbHelper.close();
                            disconnect();
                            timer.cancel();

                            intent.putExtra(FragmentObjects.PI_STATUS, FragmentObjects.GUARD_STATUS_ON);
                            sendBroadcast(intent);

                            stopSelf(startId);

//                            try {
//                                piRequest.send(FragmentObjects.GUARD_STATUS_ON);
//                            } catch (PendingIntent.CanceledException e) {
//                                e.printStackTrace();
//                            } finally {
//                                stopSelf(startId);
//                            }

                            return;
                        }

                    }

                }

                // если число попыток скачать файл достигло максимума, то останавливаем
                // таймер, закрываем соединение и отсылаем код ошибки
                if(connectsCount >= Settings.CHECK_GUARD_CONNECTS_MAX_COUNT) {

                    setStatusToDb(db, FragmentObjects.GUARD_STATUS_ERROR, objectNumber, DbHelper.DB_TABLE_GUARD);
                    dbHelper.close();

                    intent.putExtra(FragmentObjects.PI_STATUS, FragmentObjects.GUARD_STATUS_ERROR);
                    sendBroadcast(intent);

                    // отправляем на сервер команду очистки файла запросов
                    out.println("setObjectStatus:" + objectToSetGuard + ":0:" + deviceId);
                    out.flush();

                    disconnect();
                    timer.cancel();

                    stopSelf(startId);

//                    try {
//                        piRequest.send(FragmentObjects.GUARD_STATUS_ERROR);
//                    } catch (PendingIntent.CanceledException e) {
//                        e.printStackTrace();
//                    } finally {
//                        stopSelf(startId);
//                    }




                    return;

                }


                disconnect();

            }




            // если соединения с интернетом нет, ставим метку в БД
            else {
                setStatusToDb(db, FragmentObjects.GUARD_STATUS_NO_INTERNET, objectToSetGuard, DbHelper.DB_TABLE_GUARD);
                dbHelper.close();
                timer.cancel();

                intent.putExtra(FragmentObjects.PI_STATUS, FragmentObjects.GUARD_STATUS_NO_INTERNET);
                sendBroadcast(intent);

                stopSelf(startId);

//                try {
//                    piRequest.send(FragmentObjects.GUARD_STATUS_NO_INTERNET);
//                } catch (PendingIntent.CanceledException e) {
//                    e.printStackTrace();
//                } finally {
//                    stopSelf(startId);
//                }

            }

        }

    }


}
