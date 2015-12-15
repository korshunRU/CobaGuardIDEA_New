package ru.korshun.cobaguardidea.app.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

import java.io.File;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import ru.korshun.cobaguardidea.app.Boot;
import ru.korshun.cobaguardidea.app.DbHelper;
import ru.korshun.cobaguardidea.app.Functions;
import ru.korshun.cobaguardidea.app.GetSignalsService;
import ru.korshun.cobaguardidea.app.R;
import ru.korshun.cobaguardidea.app.RootActivity;
import ru.korshun.cobaguardidea.app.Settings;
import ru.korshun.cobaguardidea.app.StartActivity;

public class FragmentSignals
        extends Fragment {



    private EditText                                        objectNumberEditText;
    private Button                                          sendButton;
    private ListView                                        signalsList;

    private String                                          cobaSignalsPath;

    public static final String                              OBJECT_TO_CHECK_SIGNALS =           "OBJECT_NUMBER";
    public static final String                              SIGNALS_PATH =                      "SIGNALS_PATH";

    public static final int                                 SIGNALS_STATUS_COMPLITE =           1;
    public static final int                                 SIGNALS_STATUS_WAIT =               0;
    public static final int                                 SIGNALS_STATUS_ERROR =              -1;
    public static final int                                 SIGNALS_STATUS_NO_INTERNET =        -2;
    public static final int                                 SIGNALS_STATUS_CONNECT_ERROR =      -3;
    public static final int                                 SIGNALS_STATUS_AUTH_ERROR =         -4;

    public static final String                              PI_STATUS =                         "piStatus";

    private BroadcastReceiver                               br =                                null;
    public final static String                              BROADCAST_ACTION =                  "ru.korshun.fragmentsignals";
    private IntentFilter                                    intentFilter;





    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cobaSignalsPath =                                   Boot
                                                                .sharedPreferences
                                                                .getString(StartActivity.TEMP_SIGNALS_DIR_KEY, null);
    }




    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v =                                            inflater.inflate(R.layout.fragment_signals, container, false);

        objectNumberEditText =                              (EditText) v.findViewById(R.id.object_signals_edittext);
        sendButton =                                        (Button) v.findViewById(R.id.object_signals_send_button);
        signalsList =                                       (ListView) v.findViewById(R.id.list_signals_listView);

        intentFilter =                                      new IntentFilter(BROADCAST_ACTION);

        readItemsFromDb();

        // Клик на кнопке "ОК"
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storeObjectQuery(objectNumberEditText.getText().toString());
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
                sendButton.setEnabled(objectNumberEditText.getText().length() >= 3 && objectNumberEditText.getText().length() <= 5);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        // Клик на пункте в ListView
        signalsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                TextView status =                           (TextView) view.findViewById(R.id.signals_item_hide_status);

                // если в скрытом поле стоит статус успешного приема - пытаемся открыть соответствующий файл
                if(Integer.parseInt(status.getText().toString()) == SIGNALS_STATUS_COMPLITE) {

                    TextView object =                       (TextView) view.findViewById(R.id.signals_item_object_number);
                    String fileName =                       object.getText().toString() + ".xls";
                    String fileMime =                       URLConnection.guessContentTypeFromName(cobaSignalsPath + File.separator + fileName);
                    Intent xlsIntent =                      new Intent();

                    xlsIntent.setAction(Intent.ACTION_VIEW);
                    xlsIntent.setDataAndType(Uri.parse("file://" + cobaSignalsPath + File.separator + fileName), fileMime);

                    if (xlsIntent.resolveActivity(getActivity().getPackageManager()) != null && new File(cobaSignalsPath + File.separator + fileName).isFile()) {
                        startActivity(xlsIntent);
                    }
                }
            }
        });


        signalsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                TextView objectNumber =                     (TextView) view.findViewById(R.id.signals_item_object_number);
                TextView status =                           (TextView) view.findViewById(R.id.signals_item_hide_status);

                if(Integer.parseInt(status.getText().toString()) != SIGNALS_STATUS_WAIT &&
                        Functions.isInteger(objectNumber.getText().toString())) {
                    storeObjectQuery(objectNumber.getText().toString());
                }

                return true;
            }
        });


        if(RootActivity.signalsListAdapter != null) {
            setAdapter();
        }


        br =                                                new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                readItemsFromDb();

            }
        };


        return v;
    }









    /**
     *  Функция проверяет корректно ли введен номер объекта и в случае успешной проверки запускает
     *  функции записи в БД и стартует сервис
     * @param objectNumber                  - номер объекта для проверки
     */
    private void storeObjectQuery(String objectNumber) {
        if(Functions.isInteger(objectNumber)) {
            new File(cobaSignalsPath + File.separator + objectNumber + ".xls").delete();
            writeItemToDb(objectNumber);
            createService(Integer.parseInt(objectNumber));
            objectNumberEditText.setText("");
            sendButton.setEnabled(false);
            readItemsFromDb();
        }
        else {
            Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.err_data_object_number), Toast.LENGTH_LONG).show();
        }
    }








    /**
     *  Функция заносит или обновляет запись с номером объекта
     * @param str                   - номер объекта
     */
    private void writeItemToDb(String str) {
        DbHelper dbHelper =                                 new DbHelper(getActivity(), Settings.DB_NAME, Settings.DB_VERSION);
        SQLiteDatabase db =                                 dbHelper.getWritableDatabase();

        long currentTimeInMs =                              new Date().getTime();

        db.execSQL("INSERT OR REPLACE INTO " + DbHelper.DB_TABLE_SIGNALS + " (number, date) " +
                "VALUES (" +
                str + ", " +
                currentTimeInMs + ")");
        dbHelper.close();
    }








    /**
     *  Функция считывает данные из БД, формирует коллекцию и передает ее в функцию createListView()
     */
    private void readItemsFromDb() {
        ArrayList<HashMap<String, Object>> listPassports =  new ArrayList<>();

        DbHelper dbHelper =                                 new DbHelper(getActivity(), Settings.DB_NAME, Settings.DB_VERSION);
        SQLiteDatabase db =                                 dbHelper.getWritableDatabase();

        Cursor c =                                          db.rawQuery(
                "SELECT * " +
                        "FROM " + DbHelper.DB_TABLE_SIGNALS +
                        " ORDER BY date DESC;", null);

        if(c != null && c.getCount() > 0) {

            if (c.moveToFirst()) {

                do {

                    HashMap<String, Object> listPassportItem =  new HashMap<>();

                    String date =                               new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(c.getLong(c.getColumnIndex("date"))));
                    String status;

                    switch (c.getInt(c.getColumnIndex("complite_status"))) {
                        case SIGNALS_STATUS_COMPLITE:
                            status =                            getResources().getString(R.string.signals_status_complite);
                            break;

                        case SIGNALS_STATUS_WAIT:
                            status =                            getResources().getString(R.string.signals_status_wait);
                            break;

                        case SIGNALS_STATUS_ERROR:
                            status =                            getResources().getString(R.string.err_data);
                            break;

                        case SIGNALS_STATUS_NO_INTERNET:
                            status =                            getResources().getString(R.string.no_internet);
                            break;

                        case SIGNALS_STATUS_CONNECT_ERROR:
                            status =                            getResources().getString(R.string.no_server_connect);
                            break;

                        case SIGNALS_STATUS_AUTH_ERROR:
                            status =                            getResources().getString(R.string.status_auth_error);
                            break;

                        default:
                            status =                            "";
                            break;
                    }

                    listPassportItem.put("img",                 R.mipmap.ic_file_excel);
                    listPassportItem.put("objectNumber",        c.getString(c.getColumnIndex("number")));
                    listPassportItem.put("status",              status);
                    listPassportItem.put("createDate",          date);
                    listPassportItem.put("statusHide",          c.getInt(c.getColumnIndex("complite_status")));

                    listPassports.add(listPassportItem);

                } while (c.moveToNext());

            }

            c.close();
        }

        dbHelper.close();

        RootActivity.signalsListAdapter =                       new SimpleAdapter(
                                                                        getActivity(),
                                                                        listPassports,
                                                                        R.layout.signals_list_item,
                                                                        new String[]{
                                                                                "img",
                                                                                "objectNumber",
                                                                                "status",
                                                                                "createDate",
                                                                                "statusHide"
                                                                        },
                                                                        new int[]{
                                                                                R.id.signals_item_img,
                                                                                R.id.signals_item_object_number,
                                                                                R.id.signals_item_status,
                                                                                R.id.signals_item_create_date,
                                                                                R.id.signals_item_hide_status
                                                                        }
                                                                );

        setAdapter();

    }







    /**
     *  Функция устанавливает адаптер в ListView, который отображает список паспортов
     */
    private void setAdapter() {
        signalsList.setAdapter(RootActivity.signalsListAdapter);
        ((SimpleAdapter)signalsList.getAdapter()).notifyDataSetChanged();
        signalsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }





    /**
     *  Фуекция запускает сервис и передает в него номер объекта
     * @param objectNumber                  - номер объекта
     */
    private void createService(int objectNumber) {
//        PendingIntent piRequest;

        Intent getSignalsFile =                 new Intent(getActivity().getBaseContext(), GetSignalsService.class);
//        piRequest =                             getActivity().createPendingResult(RootActivity.CODE_REQUEST_SIGNALS, getSignalsFile, 0);

        getSignalsFile
                .putExtra(OBJECT_TO_CHECK_SIGNALS, objectNumber)
                .putExtra(SIGNALS_PATH, cobaSignalsPath);
//                .putExtra(RootActivity.PI_REQUEST, piRequest);

        getActivity().startService(getSignalsFile);
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