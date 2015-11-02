package ru.korshun.cobaguardidea.app;

import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.support.annotation.Nullable;


/**
 *  Родительский класс для классов загрузки файлов с сигналами и снятия\постановки объектов на охрану
 */
public abstract class MyServiceSignalsAndGuard
        extends Service {






    protected final String SERVER_IP =                                 Settings.SERVERS_IP_ARRAY[0];
//    protected final String          SERVER_IP =                                 "192.168.43.138";






    @Override
    public void onCreate() {
        super.onCreate();
//        System.out.println("myapp : onCreate");
    }






    @Override
    public void onDestroy() {
//        System.out.println("myapp : onDestroy");
        super.onDestroy();
    }






    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }







    /**
     *  Проверяем доступность интернет соединения
     * @return                  - возвращается TRUE в случае успеха
     */
    protected boolean checkConnection() {
        return Functions.isNetworkAvailable(getBaseContext());
    }






    /**
     *  Установка статуса операции в БД
     * @param db                - ссылка на объект SQLiteDatabase
     * @param status            - числовое значение статуса операции
     * @param objectNumber      - номер объекта, для которого устанавливаем статус
     */
    protected void setStatusToDb(SQLiteDatabase db, int status, int objectNumber, String tableName) {
        db.execSQL( "UPDATE " + tableName + " " +
                    "SET complite_status = " + status + " " +
                    "WHERE number = " + objectNumber);
//        dbHelper.close();
    }




    /**
     *  В случае успеха или превышения счетчика проверок, останавливаем таймер и закрываем
     *  соединение с сервером
     */
    protected abstract void disconnect();




    /**
     *  Функция соединения с сервером и создания PrintWriter'а
     */
    protected abstract boolean connect();





}
