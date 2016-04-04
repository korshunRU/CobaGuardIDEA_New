package ru.korshun.cobaguardidea.app;


import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.text.SimpleDateFormat;
import java.util.Date;

import ru.korshun.cobaguardidea.app.fragments.FragmentPassports;

/**
 *  Файл с пользовательскими функциями
 */
public final class Functions {



    /**
     *  Функция проверяет, является ли строка целым числом
     * @param str               - строка, которую будем проверять
     * @return                  - в случае успеха возвращается TRUE
     */
    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
        } catch(NumberFormatException e) {
            return false;
        }
        return true;
    }






    /**
     *  Функция выдергивает первое слово из строки, либо саму строку, если она состоит из одного слова
     * @param str               - строка, из которой будем выдергивать первое слово
     * @return                  - возвращается первое слово в строке
     */
    public static String getFirstWordInString(String str) {
        return str.contains(" ") ? str.substring(0, str.indexOf(" ")) : str;
    }





    /**
     *  Функция создает массив из двух элементов из входящей строки. [0] - это тип объекта (Сова,
     *  Скит, Око и т.д.), [1] - это номер объекта
     * @param str               - входящая строка формата OKO_1234 или просто 1234
     * @return                  - возвращается массив
     */
    public static String[] getNumberAndTypeFromString(String str) {
        String[] numberAndType =                            new String[2];

        if(str.contains("_")) {
            numberAndType =                                 str.split("_");
        }
        else {
            numberAndType[0] =                              FragmentPassports.DEFAULT_OBJECT_PREFIX;
            numberAndType[1] =                              str;
        }

        return numberAndType;
    }







    /**
     *  Функция проверяет доступность интернет соединения (wifi или мобильный интернет)
     * @param cnt               - ссылка на контекст
     * @return                  - в случае доступности возвращется TRUE
     */
    public static boolean isNetworkAvailable(Context cnt) {
        ConnectivityManager connectivityManager = (ConnectivityManager) cnt.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }






    /**
     *  Функция возвращает текущую метку времени и даты в заданном формате
     * @param format            - формат даты на выходе (yyyy/MM/dd HH:mm:ss)
     * @return                  - сама дата
     */
    public static String getCurrentDate(String format) {
        return new SimpleDateFormat(format).format(new Date());
    }






    /**
     *  Функция проверяет работает ли сервис в данный момент
     * @param serviceClass      - ссылка на класс сервиса
     * @param cnt               - ссылка на Context
     * @return                  - в случае если служба найдена среди работающих возвращается true
     */
    public static boolean isServiceRunning(Class<?> serviceClass, Context cnt) {
        ActivityManager manager = (ActivityManager) cnt.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {

            if (serviceClass.getName().equals(service.service.getClassName())) {

                return true;

            }

        }

        return false;

    }



}
