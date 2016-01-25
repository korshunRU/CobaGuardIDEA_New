package ru.korshun.cobaguardidea.app;


import java.io.File;
import java.util.regex.Pattern;


public final class Settings {


//    public static final String SMS_SENDER_NUMBER =              "+79221307233";
//    public static final String SMS_SENDER_NUMBER =              "0883";
//    public static final String SMS_SENDER_NUMBER =              "684";
//    public static final String SMS_SENDER_NUMBER =              "InternetSMS";
//    public static final String SMS_SENDER_NUMBER =              "+79049820133";

    public static final int SMS_LIFE_TIME_SERVICE =             36000; // в секундах!!!
    public static final int SMS_LIFE_TIME_GBR =                 18000; // в секундах!!!

    public static final int PORT =                              6666;
    public static final int PORT_FILE =                         6667;

    public static final int CONNECTION_TIMEOUT_PASSPORTS =      10000;
    public static final int CONNECTION_TIMEOUT_SIGNALS =        10000;
    public static final int CONNECTION_TIMEOUT_GUARD =          10000;

    public static final int NO_UPDATE_DAYS_ALERT =              3; // в днях!!!

    public static final String COBA_PASSPORTS_PATH =            "Android" + File.separator +
                                                                "data" + File.separator +
                                                                "ru.korshun.cobaguardidea.app" + File.separator +
                                                                "coba_db";

//    public static final String PREFERENCES_FILE_NAME =          "cobaGuardPref";

//    public static final String LAST_UPDATED_DATE =              "lastUpdatedDate";

//    public static final String SERVER_IP =                      "serverIp";

//    public static final String SMS_NUMBER =                     "smsNumber";

//    public static final String PASSPORTS_DIR =                  "passportsDir";

//    public static final String CHANGE_GUARD_INFO =              "changeGuardInfo";

    public static final String PD_TITLE =                       "COBA GUARD";

//    public static final String LOG_TAG =                        "myLog";

    public static final String OBJECT_PART_DIVIDER =            "-";

    public static final String ADDRESS_SYMBOL_START =           "[";

    public static final String ADDRESS_SYMBOL_FINISH =          "]";

    public static Pattern DIR_SEPARATOR =                       Pattern.compile("/");

    public static final String[] SERVERS_IP_ARRAY =             new String[]{"85.12.240.55",    "192.168.0.2",      "192.168.43.138"};
    public static final String[] SERVERS_IP_ARRAY_LEGEND =      new String[]{"Интернет",        "Локальная сеть",   "TEST"};

    public static final String[] SMS_NUMBERS_ARRAY =            new String[]{"COBA",            "+79049820133",     "InternetSMS"};
    public static final String[] SMS_NUMBERS_ARRAY_LEGEND =     new String[]{"ГБР",             "Сервис",           "TEST"};

    public static final String SMS_NUMBER_VIDOK =               "+79126976842";

//    public static final String CHANGE_GUARD_NUMBER =            "InternetSMS";
//    public static final String CHANGE_GUARD_NUMBER =            "+79122271554";
//    public static final String CHANGE_GUARD_NUMBER =            "+79049820133";
//    public static final String CHANGE_GUARD_NUMBER =            "+79826398186";

    public static final int CHECK_SIGNALS_REPEAT_IN_SECONDS =   15;
    public static final int CHECK_SIGNALS_CONNECTS_MAX_COUNT =  3;

    public static final int CHECK_GUARD_REPEAT_IN_SECONDS =     15;
    public static final int CHECK_GUARD_CONNECTS_MAX_COUNT =    3;

    public static final int DB_VERSION =                        2;
    public static final String DB_NAME =                        "COBA_DB";
    public static final int DB_SIGNALS_LIFE_HOURS =             12;
    public static final int DB_GUARD_LIFE_HOURS =               12;

}