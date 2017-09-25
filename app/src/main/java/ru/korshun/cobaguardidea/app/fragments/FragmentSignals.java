package ru.korshun.cobaguardidea.app.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
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

    @SuppressWarnings("FieldCanBeLocal")
    private final String                                    LAST_OBJECTS_QUERY_KEY =            "pref_last_object_query";

//    private EditText                                        objectNumberEditText;
    private AutoCompleteTextView                            objectNumberEditText;
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

        if(Boot.sharedPreferences == null) {
            Boot.sharedPreferences =                        PreferenceManager
                                                                .getDefaultSharedPreferences(getContext());
        }

        cobaSignalsPath =                                   Boot
                                                                .sharedPreferences
                                                                .getString(StartActivity.TEMP_SIGNALS_DIR_KEY, null);


//        Boot
//                .sharedPreferences
//                .edit()
//                .putString(LAST_OBJECTS_QUERY_KEY, null)
//                .apply();


    }




    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v =                                            inflater.inflate(R.layout.fragment_signals, container, false);

//        objectNumberEditText =                              (EditText) v.findViewById(R.id.object_signals_edittext);
        objectNumberEditText =                              (AutoCompleteTextView) v.findViewById(R.id.object_signals_edittext);
        sendButton =                                        (Button) v.findViewById(R.id.object_signals_send_button);
        signalsList =                                       (ListView) v.findViewById(R.id.list_signals_listView);

        intentFilter =                                      new IntentFilter(BROADCAST_ACTION);

        objectNumberEditText.setThreshold(1);

        if(getLastObjectsQuery() != null) {
            objectNumberEditText.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.select_dialog_item, getLastObjectsQuery()));
        }

//        for(int x = 1; x <= 10; x++) {
//
//            addItemToLastObjectsQuery("100" + x);
//
//        }
//
//        addItemToLastObjectsQuery("1005");
//        addItemToLastObjectsQuery("1008");
//        addItemToLastObjectsQuery("1009");
//        addItemToLastObjectsQuery("1009");
//        addItemToLastObjectsQuery("1004");

        readItemsFromDb();

        // Клик на кнопке "ОК"
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (readItemsFromDb() > 0 && Functions.isServiceRunning(GetSignalsService.class, getActivity())) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.double_query_error), Toast.LENGTH_LONG).show();
                }
                else {
                    storeObjectQuery(objectNumberEditText.getText().toString());
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        });

//        String[] items = {"21548", "7585", "16457", "17587"};
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.select_dialog_item, items);

        // Клик в поле ввода объекта - показываем выпадающее меню
        objectNumberEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                objectNumberEditText.showDropDown();
                return false;
            }
        });


        // Клик на пункте в выпадающем меню
        objectNumberEditText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                storeObjectQuery(objectNumberEditText.getText().toString());
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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

                TextView status = (TextView) view.findViewById(R.id.signals_item_hide_status);

                // если в скрытом поле стоит статус успешного приема - пытаемся открыть соответствующий файл
                if (Integer.parseInt(status.getText().toString()) == SIGNALS_STATUS_COMPLITE) {

                    TextView object = (TextView) view.findViewById(R.id.signals_item_object_number);
                    String fileName = object.getText().toString() + ".xls";
                    String fileMime = URLConnection.guessContentTypeFromName(cobaSignalsPath + File.separator + fileName);
                    Intent xlsIntent = new Intent();

                    xlsIntent.setAction(Intent.ACTION_VIEW);

                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        xlsIntent.setDataAndType(Uri.parse("file://" + cobaSignalsPath + File.separator + fileName), fileMime);
                    }
                    else {
                        xlsIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        Uri uri = FileProvider.getUriForFile(
                                getActivity(),
                                "ru.korshun.cobaguardidea.app.provider",
                                new File(cobaSignalsPath + File.separator + fileName)
                        );
                        xlsIntent.setData(uri);
                    }

                    if (xlsIntent.resolveActivity(getActivity().getPackageManager()) != null &&
                            new File(cobaSignalsPath + File.separator + fileName).isFile()) {
                        startActivity(xlsIntent);
                    }
                }
            }
        });


        signalsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                TextView objectNumber =                     (TextView) view.findViewById(R.id.signals_item_object_number);
//                TextView status =                           (TextView) view.findViewById(R.id.signals_item_hide_status);

                // При долгом клике запускам запрос файла на объект, если:
                // - в БД нет записей со статусом "ожидание"
                // - в БД есть записи со статусом "ожидание", но сервис не запущен
                if((readItemsFromDb() == 0 && Functions.isInteger(objectNumber.getText().toString())) ||
                        (readItemsFromDb() > 0 && Functions.isInteger(objectNumber.getText().toString()) && !Functions.isServiceRunning(GetSignalsService.class, getActivity()))) {
                    storeObjectQuery(objectNumber.getText().toString());
                }
                else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.double_query_error), Toast.LENGTH_LONG).show();
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

                if(readItemsFromDb() == 0 ||
                        (readItemsFromDb() > 0 && !Functions.isServiceRunning(GetSignalsService.class, getActivity()))) {
                    objectNumberEditText.setEnabled(true);
                    objectNumberEditText.setText("");
                    sendButton.setEnabled(true);
                }

            }
        };

        return v;
    }



    /**
     *  Получаем из sharedPreferences список последних запрошенных объектов по сигналам
     * @return                  - возвращается массив типа String
     */
    private String[] getLastObjectsQuery() {
        String[] returnArray =                                          null;

        String prefStr =
                Boot.sharedPreferences.getString(LAST_OBJECTS_QUERY_KEY, null) == null ?
                        null :
                        Boot.sharedPreferences.getString(LAST_OBJECTS_QUERY_KEY, null);

        if(prefStr != null) {

            returnArray = (!prefStr.contains(Settings.OBJECT_PART_DIVIDER)) ?
                    new String[]{ prefStr } :
                    prefStr.split(Settings.OBJECT_PART_DIVIDER);

        }

        return returnArray;
    }




    /**
     *  Добавление номера в список последних запрашиваемых
     * @param item              - номер объекта для добавления
     */
    private void addItemToLastObjectsQuery(String item) {

        String[] lastObjects =                                          getLastObjectsQuery();
        String updatedStr =                                             "";

        if(lastObjects != null) {

            if(lastObjects.length >= Settings.GET_SIGNALS_OBJECTS_STORE_COUNT) {

                for(int x = Settings.GET_SIGNALS_OBJECTS_STORE_COUNT; x >= 1; x--) {

//                    System.out.println("myLog " + lastObjects[x - 1]);

                    if(lastObjects[x - 1].equals(item)) {
                        return;
                    }

                    if(x > 1) {
                        updatedStr =                                    Settings.OBJECT_PART_DIVIDER + lastObjects[x - 2] + updatedStr;
                    }
                }

                updatedStr =                                            item + updatedStr;

            }

            else if(lastObjects.length == 1) {

                if(lastObjects[0].equals(item)) {
                    return;
                }

                updatedStr +=
                                                                        (item.equals(lastObjects[0])) ?
                                                                                item :
                                                                                item + Settings.OBJECT_PART_DIVIDER + lastObjects[0];

            }

            else {

                for(int x = lastObjects.length; x >= 1; x--) {

                    if(lastObjects[x - 1].equals(item)) {
                        return;
                    }

                    updatedStr =                                        Settings.OBJECT_PART_DIVIDER + lastObjects[x - 1] + updatedStr;

                }

                updatedStr =                                            item + updatedStr;

            }

        }

        else {
            updatedStr =                                                item;
        }

//        System.out.println("myLog: " + updatedStr);

        Boot
                .sharedPreferences
                .edit()
                .putString(LAST_OBJECTS_QUERY_KEY, updatedStr)
                .apply();

        objectNumberEditText.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.select_dialog_item, getLastObjectsQuery()));
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
            addItemToLastObjectsQuery(objectNumber);
            objectNumberEditText.setText("");
            objectNumberEditText.setEnabled(false);
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
    private int readItemsFromDb() {

        int waitItemsCount =                                0;

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
                            waitItemsCount++;
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

        return waitItemsCount;
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
//        System.out.println("myLog : onStop");
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
//        System.out.println("myLog : onStart");
        if(readItemsFromDb() == 0 ||
                (readItemsFromDb() > 0 && !Functions.isServiceRunning(GetSignalsService.class, getActivity()))) {
            objectNumberEditText.setEnabled(true);
            objectNumberEditText.setText("");
            sendButton.setEnabled(true);
        }
        if(br != null) {
            getActivity().registerReceiver(br, intentFilter);
        }
    }



}