package ru.korshun.cobaguardidea.app;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *  Файл с пользовательскими функциями
 */
public final class Functions {




    /**
     *  Функция УСТАНАВЛИВАЕТ из файла preferences опцию с определенным тегом
     * @param tag               - ссылка на метку опции
     * @param cnt               - ссылка на контекст
     */
    public static void setPrefOption(String tag, String str, Context cnt) {
        SharedPreferences.Editor ed = cnt.getSharedPreferences(Settings.PREFERENCES_FILE_NAME, 0x0000).edit();
        ed.putString(tag, String.valueOf(str));
        ed.apply();
    }







    /**
     *  Функция ПОЛУЧАЕТ из файла preferences опцию с определенным тегом
     * @param tag               - ссылка на метку опции
     * @param cnt               - ссылка на контекст
     * @return                  - в случае успеха возвращается значение
     */
    public static String getPrefOption(String tag, Context cnt) {
        return cnt.getSharedPreferences(Settings.PREFERENCES_FILE_NAME, 0x0000).contains(tag) ?
                cnt.getSharedPreferences(Settings.PREFERENCES_FILE_NAME, 0x0000).getString(tag, "") :
                null;
    }







    /**
     *  Функция УДАЛЯЕТ из файла preferences опцию с определенным тегом
     * @param tag               - ссылка на метку опции
     * @param cnt               - ссылка на контекст
     * @return                  - в случае успеха возвращается TRUE
     */
    public static boolean deletePrefOption(String tag, Context cnt) {
        return cnt.getSharedPreferences(Settings.PREFERENCES_FILE_NAME, 0x0000).edit().remove(tag).commit();
    }






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





}
