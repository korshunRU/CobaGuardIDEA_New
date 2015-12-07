package ru.korshun.cobaguardidea.app.fragments;


import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import ru.korshun.cobaguardidea.app.DbHelper;
import ru.korshun.cobaguardidea.app.Functions;
import ru.korshun.cobaguardidea.app.R;
import ru.korshun.cobaguardidea.app.RootActivity;
import ru.korshun.cobaguardidea.app.SetGuardStatusService;
import ru.korshun.cobaguardidea.app.Settings;

public class FragmentObjects
        extends Fragment {



    private EditText                                    objectNumberEditText;
    private Button                                      sendButton;
    private ListView                                    objectsList;

    public static final String                          OBJECT_TO_SET_GUARD =               "OBJECT_NUMBER";
    public static final String                          OBJECT_GUARD_STATUS =               "OBJECT_STATUS";

//    private final int                                   CODE_REQUEST =                      2;

    private final int                                   CURRENT_GUARD_STATUS_ON =           2;
    private final int                                   CURRENT_GUARD_STATUS_OFF =          1;

    public static final int                             GUARD_STATUS_ON =                   2;
    public static final int                             GUARD_STATUS_OFF =                  1;
    public static final int                             GUARD_STATUS_WAIT =                 0;
    public static final int                             GUARD_STATUS_ERROR =                -1;
    public static final int                             GUARD_STATUS_NO_INTERNET =          -2;
    public static final int                             GUARD_STATUS_CONNECT_ERROR =        -3;
    public static final int                             GUARD_STATUS_AUTH_ERROR =           -4;
    public static final int                             GUARD_STATUS_CLEAR_FILE =           -5;
    public static final int                             GUARD_STATUS_QUERY_ERROR =          -6;

    private final String                                SENDER_SMS_NUMBER =                 Settings.SMS_NUMBERS_ARRAY[1];
//    private final String                                SENDER_SMS_NUMBER =                 "InternetSMS";

    public static final String                          PI_STATUS =                         "piStatus";

    private BroadcastReceiver                           br =                                null;
    public final static String                          BROADCAST_ACTION =                  "ru.korshun.fragmentobjects";
    private IntentFilter                                intentFilter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }




    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v =                                            inflater.inflate(R.layout.fragment_objects, container, false);

        objectNumberEditText =                              (EditText) v.findViewById(R.id.object_guard_edittext);
        sendButton =                                        (Button) v.findViewById(R.id.object_guard_send_button);
        objectsList =                                       (ListView) v.findViewById(R.id.list_guard_objects_listView);

        intentFilter =                                      new IntentFilter(BROADCAST_ACTION);

        readItemsFromDb();

        // Клик на кнопке "ОК"
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // запускаем класс для фонового сканирования смс
                new ScanSms(new ProgressDialog(getActivity()), objectNumberEditText.getText().toString()).execute();

                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

            }
        });



        // Ввод текста в поле номера объекта
        objectNumberEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(objectNumberEditText.getText().length() >= 4 && objectNumberEditText.getText().length() <= 5);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        // "длинный" клик на элемента списка
        objectsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                TextView statusHide =                       (TextView) view.findViewById(R.id.objects_item_hide_status);
                TextView statusCompliteHide =               (TextView) view.findViewById(R.id.objects_item_hide_status_complite);

                int statusHideText =                        Integer.parseInt(statusHide.getText().toString());
                int statusCompliteHideText =                Integer.parseInt(statusCompliteHide.getText().toString());

//                Toast.makeText(getActivity(), statusCompliteHide.getText().toString(), Toast.LENGTH_LONG).show();


                // если в данный момент не идет никаких запросов - проверяем статус
                if(statusCompliteHideText != GUARD_STATUS_WAIT) {

                    // если в скрытом поле переменная, указывающая на то, что объект без охраны -
                    // отправляем запрос на постановку
                    if (statusHideText == GUARD_STATUS_OFF) {
                        TextView objectNumber =             (TextView) view.findViewById(R.id.objects_item_object_number);
                        createConfirmDialog(objectNumber.getText().toString(), GUARD_STATUS_ON);
                    }


                    // если в скрытом поле переменная, указывающая на то, что объект под охраной -
                    // отправляем запрос на очистку файла запросов и удаления самого объекта из списка
                    else if (Integer.parseInt(statusHide.getText().toString()) == GUARD_STATUS_ON) {
                        TextView objectNumber =             (TextView) view.findViewById(R.id.objects_item_object_number);
                        createService(Integer.parseInt(objectNumber.getText().toString()), GUARD_STATUS_CLEAR_FILE);
                    }

                }


                return false;
            }
        });


        br =                                                new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if(readItemsFromDb() == 0) {
                    objectNumberEditText.setEnabled(true);
                    objectNumberEditText.setText("");
                }

            }
        };


        return v;
    }



    /**
     *  Создаем окно подтверждения перед отправкой данных
     * @param objectNumber                      - номер объекта для запроса
     * @param type                              - тип запроса
     *                                           1 - снятие с охраны
     *                                           2 - постановка на охрану
     */
    private void createConfirmDialog(final String objectNumber, final int type) {

        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getText(R.string.confirm_question))
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        storeObjectStatusQuery(objectNumber, type);

                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        objectNumberEditText.setText("");
                    }
                })
                .create()
                .show();

        if(RootActivity.objectsListAdapter != null) {
            setAdapter();
        }

    }







    /**
     *  Ловим данные, которые прилетели из Activity
     *  Смотрим на requestCode - это порядковый номер фрагмента:
     *   0 - фрагмент списка паспортов
     *   1 - фрагмент сигналов
     *   2 - фрагмент снятия\постановки объекта
     *
     *   Тут ловим requestCode == 2
     */
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if(requestCode == RootActivity.CODE_REQUEST_OBJECTS) {
//
//            if(readItemsFromDb() == 0) {
//                objectNumberEditText.setEnabled(true);
//                objectNumberEditText.setText("");
//            }
//
//        }
//
//    }








    /**
     *  Функция проверяет корректно ли введен номер объекта и в случае успешной проверки запускает
     *  функции записи в БД и стартует сервис
     * @param objectNumber                      - номер объекта для запроса
     * @param type                              - тип запроса
     *                                           1 - снятие с охраны
     *                                           2 - постановка на охрану
     */
    private void storeObjectStatusQuery(String objectNumber, int type) {
        if (Functions.isInteger(objectNumber)) {
            objectNumberEditText.setEnabled(false);
            sendButton.setEnabled(false);
            insertObjectToDb(objectNumber, type);
            readItemsFromDb();
            createService(Integer.parseInt(objectNumber), type);
        }
        else {
            Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.err_data_object_number), Toast.LENGTH_LONG).show();
        }
    }






    /**
     *  Функция заносит или обновляет запись с номером объекта
     * @param str                               - номер объекта
     * @param type                              - тип запроса
     *                                           1 - снятие с охраны
     *                                           2 - постановка на охрану
     */
    private void insertObjectToDb(String str, int type) {
        DbHelper dbHelper =                         new DbHelper(getActivity(), Settings.DB_NAME, Settings.DB_VERSION);
        SQLiteDatabase db =                         dbHelper.getWritableDatabase();

        long currentTimeInMs =                      new Date().getTime();
        int status =                                (type == 1) ?
                CURRENT_GUARD_STATUS_ON :
                CURRENT_GUARD_STATUS_OFF;

        db.execSQL("INSERT OR REPLACE INTO " + DbHelper.DB_TABLE_GUARD + " (number, date, status, complite_status) " +
                "VALUES (" +
                str + ", " +
                currentTimeInMs + ", " +
                status +
                ", 0)");

        dbHelper.close();
    }








    /**
     *  Функция считывает данные из БД, формирует коллекцию и передает ее в функцию createListView()
     */
    private int readItemsFromDb() {

        int waitItemsCount =                                    0;

        ArrayList<HashMap<String, Object>> listPassports =      new ArrayList<>();

        DbHelper dbHelper =                                     new DbHelper(getActivity(), Settings.DB_NAME, Settings.DB_VERSION);
        SQLiteDatabase db =                                     dbHelper.getWritableDatabase();

        Cursor c =                                              db.rawQuery(
                        "SELECT * " +
                        "FROM " + DbHelper.DB_TABLE_GUARD +
                        " ORDER BY date DESC;", null);

        if(c != null && c.getCount() > 0) {

            if (c.moveToFirst()) {

                do {

                    HashMap<String, Object> listPassportItem =  new HashMap<>();

                    String date =                               new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(c.getLong(c.getColumnIndex("date"))));
                    String guardStatus =                        (c.getInt(c.getColumnIndex("status")) == CURRENT_GUARD_STATUS_OFF) ?
                                                                        getResources().getString(R.string.guard_off_text) :
                                                                        getResources().getString(R.string.guard_on_text);
                    String compliteStatus;
                    int statusImg =                             -1;

                    switch (c.getInt(c.getColumnIndex("complite_status"))) {
                        case GUARD_STATUS_ON:
                            compliteStatus =                    getResources().getString(R.string.guard_status_on);
                            statusImg =                         R.mipmap.ic_objects_guard_on;
                            break;

                        case GUARD_STATUS_OFF:
                            compliteStatus =                    getResources().getString(R.string.guard_status_off);
                            statusImg =                         R.mipmap.ic_objects_guard_off;
                            break;

                        case GUARD_STATUS_WAIT:
                            compliteStatus =                    getResources().getString(R.string.guard_status_wait);
                            statusImg =                         (c.getInt(c.getColumnIndex("status")) == CURRENT_GUARD_STATUS_ON) ?
                                                                    R.mipmap.ic_objects_guard_on :
                                                                    R.mipmap.ic_objects_guard_off;
                            waitItemsCount++;
                            break;

                        case GUARD_STATUS_ERROR:
                            compliteStatus =                    getResources().getString(R.string.guard_err_data);
                            statusImg =                         (c.getInt(c.getColumnIndex("status")) == CURRENT_GUARD_STATUS_ON) ?
                                                                    R.mipmap.ic_objects_guard_on :
                                                                    R.mipmap.ic_objects_guard_off;
                            break;

                        case GUARD_STATUS_NO_INTERNET:
                            compliteStatus =                    getResources().getString(R.string.no_internet);
                            statusImg =                         (c.getInt(c.getColumnIndex("status")) == CURRENT_GUARD_STATUS_ON) ?
                                                                    R.mipmap.ic_objects_guard_on :
                                                                    R.mipmap.ic_objects_guard_off;
                            break;

                        case GUARD_STATUS_CONNECT_ERROR:
                            compliteStatus =                    getResources().getString(R.string.no_server_connect);
                            statusImg =                         (c.getInt(c.getColumnIndex("status")) == CURRENT_GUARD_STATUS_ON) ?
                                                                    R.mipmap.ic_objects_guard_on :
                                                                    R.mipmap.ic_objects_guard_off;
                            break;

                        case GUARD_STATUS_AUTH_ERROR:
                            compliteStatus =                    getResources().getString(R.string.status_auth_error);
                            statusImg =                         (c.getInt(c.getColumnIndex("status")) == CURRENT_GUARD_STATUS_ON) ?
                                                                    R.mipmap.ic_objects_guard_on :
                                                                    R.mipmap.ic_objects_guard_off;
                            break;

                        case GUARD_STATUS_QUERY_ERROR:
                            compliteStatus =                    getResources().getString(R.string.guard_query_error);
                            statusImg =                         (c.getInt(c.getColumnIndex("status")) == CURRENT_GUARD_STATUS_ON) ?
                                                                    R.mipmap.ic_objects_guard_on :
                                                                    R.mipmap.ic_objects_guard_off;
                            break;

                        default:
                            compliteStatus =                    "";
                            break;
                    }

                    listPassportItem.put("img",                 statusImg);
                    listPassportItem.put("objectNumber",        c.getString(c.getColumnIndex("number")));
                    listPassportItem.put("guardStatus",         guardStatus);
                    listPassportItem.put("status",              compliteStatus);
                    listPassportItem.put("createDate",          date);
                    listPassportItem.put("statusHide",          c.getInt(c.getColumnIndex("status")));
                    listPassportItem.put("statusCompliteHide",  c.getInt(c.getColumnIndex("complite_status")));

                    listPassports.add(listPassportItem);

                } while (c.moveToNext());

            }

            c.close();
        }

        dbHelper.close();

//        createListView(listPassports);

        RootActivity.objectsListAdapter =                       new SimpleAdapter(
                                                                            getActivity(),
                                                                            listPassports,
                                                                            R.layout.objects_list_item,
                                                                            new String[]{
                                                                                    "img",
                                                                                    "objectNumber",
                                                                                    "guardStatus",
                                                                                    "status",
                                                                                    "createDate",
                                                                                    "statusHide",
                                                                                    "statusCompliteHide"
                                                                            },
                                                                            new int[]{
                                                                                    R.id.objects_item_img,
                                                                                    R.id.objects_item_object_number,
                                                                                    R.id.objects_item_guard_status,
                                                                                    R.id.objects_item_status,
                                                                                    R.id.objects_item_create_date,
                                                                                    R.id.objects_item_hide_status,
                                                                                    R.id.objects_item_hide_status_complite
                                                                            }
                                                                );

        setAdapter();

        return waitItemsCount;
    }




    /**
     *  Функция устанавливает адаптер в ListView, который отображает список паспортов
     */
    private void setAdapter() {
        objectsList.setAdapter(RootActivity.objectsListAdapter);
        ((SimpleAdapter)objectsList.getAdapter()).notifyDataSetChanged();
        objectsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }







    /**
     *  Фуекция запускает сервис и передает в него номер объекта
     * @param objectNumber                      - номер объекта
     * @param type                              - тип запроса
     *                                           0 - очистка файла запроса
     *                                           1 - снятие с охраны
     *                                           2 - постановка на охрану
     */
    private void createService(int objectNumber, int type) {
//        PendingIntent piRequest;

        Intent setObjectStatus =                new Intent(getActivity().getBaseContext(), SetGuardStatusService.class);
//        piRequest =                             getActivity().createPendingResult(RootActivity.CODE_REQUEST_OBJECTS, setObjectStatus, 0);

        setObjectStatus
                .putExtra(OBJECT_TO_SET_GUARD, objectNumber)
                .putExtra(OBJECT_GUARD_STATUS, type);
//                .putExtra(RootActivity.PI_REQUEST, piRequest);

        getActivity().startService(setObjectStatus);
    }










    /**
     *  ФОновый класс для поиска в тексте входящих смс номера объекта, который собираемся снять с охраны
     */
    class ScanSms
            extends AsyncTask<Void, Void, Boolean> {



        private ProgressDialog pd;
        private String objectNumber;




        public ScanSms(ProgressDialog pd, String objectNumber) {
            this.objectNumber =                     objectNumber;
            this.pd =                               pd;

            this.pd.setTitle(Settings.PD_TITLE);
            this.pd.setMessage(getResources().getString(R.string.sms_scan_title));
            this.pd.setCancelable(false);
        }




        @Override
        protected void onPreExecute() {
            this.pd.show();
        }



        @Override
        protected Boolean doInBackground(Void... params) {

            Cursor cursor =                         getActivity().getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

            if(cursor != null && cursor.moveToFirst()) {

                for (int idx = 0; idx < cursor.getCount(); idx++) {

                    String from =                   cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    String body =                   cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    Long timeDifference =           (Calendar.getInstance().getTimeInMillis() - cursor.getLong(cursor.getColumnIndexOrThrow("date"))) / 1000;

                    if(from.equals(SENDER_SMS_NUMBER) &&
                            timeDifference <= Settings.SMS_LIFE_TIME_SERVICE &&
                            Functions.isInteger(Functions.getFirstWordInString(body)) &&
                            Functions.getFirstWordInString(body).equals(objectNumber)) {

                        return true;

                    }

                    cursor.moveToNext();
                }

                cursor.close();

            }

            return false;
        }



        @Override
        protected void onPostExecute(Boolean result) {
            this.pd.dismiss();

            // если найдена смс с номером введенного объекта - отправляем команду на выполнение
            if(result) {
                createConfirmDialog(objectNumberEditText.getText().toString(), GUARD_STATUS_OFF);
            }

            // в противном случае сообщаем об ошибке
            else {
                Toast.makeText(getActivity(), getResources().getString(R.string.err_no_sms_for_guard_off), Toast.LENGTH_LONG).show();
            }

        }

    }






    @Override
    public void onStop() {
        super.onStop();
        if(br != null) {
            try {
                getActivity().unregisterReceiver(br);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(br != null) {
            getActivity().registerReceiver(br, intentFilter);
        }
    }



}