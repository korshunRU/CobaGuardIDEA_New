package ru.korshun.cobaguardidea.app.fragments;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import ru.korshun.cobaguardidea.app.Boot;
import ru.korshun.cobaguardidea.app.Functions;
import ru.korshun.cobaguardidea.app.ImgCryptoDecoder;
import ru.korshun.cobaguardidea.app.MapActivity;
import ru.korshun.cobaguardidea.app.R;
import ru.korshun.cobaguardidea.app.RootActivity;
import ru.korshun.cobaguardidea.app.Settings;
import ru.korshun.cobaguardidea.app.StartActivity;



public class FragmentPassports
        extends Fragment {

    @SuppressWarnings("FieldCanBeLocal")
    private final String                READ_UPDATE_MSG_KEY =
                                                            "pref_read_update_msg";
    private final int                   READ_UPDATE_MSG_ID =
                                                            1;

    public RootActivity                 rootActivity;

    private ListView                    listPassports;

    private String                      passportsPath;

    private String                      smsNumber;
    private int                         smsLiveTime;

    private ImgCryptoDecoder            decoder;

    private String                      tempPassportsPath;

//    private HashMap<String, String>             objectAddressMap =              null;

    public final static String          OBJECT_ADDRESS_MAP_INTENT_KEY =
                                                            "objectAddressMapKey";

    public final static String          DEFAULT_OBJECT_PREFIX =
                                                            "COBA";

//    private MediaScannerConnection  mediaScannerConnection =            null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootActivity =                                      (RootActivity) getActivity();
    }




    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v =                                                                inflater.inflate(R.layout.fragment_passports, container, false);

        int lastReadId =                                                        Boot.sharedPreferences.getInt(READ_UPDATE_MSG_KEY, 0);

        passportsPath =                                                         Boot.sharedPreferences.getString(FragmentSettings.PASSPORTS_PATH_KEY, null);
        tempPassportsPath =                                                     Boot.sharedPreferences.getString(StartActivity.TEMP_PASSPORTS_DIR_KEY, null);

        FloatingActionButton fabPassportsRefresh =                              (FloatingActionButton) v.findViewById(R.id.fab_passports_refresh);
        FloatingActionButton fabMapOpen =                                       (FloatingActionButton) v.findViewById(R.id.fab_map_open);
        FloatingActionButton fabPassportsInfo =                                 (FloatingActionButton) v.findViewById(R.id.fab_passports_info);

        listPassports =                                                         (ListView) v.findViewById(R.id.list_passports);

        smsNumber =                                                             Boot.sharedPreferences.getString(FragmentSettings.SMS_OWNER_KEY, Settings.SMS_NUMBERS_ARRAY[0]);
        smsLiveTime =                                                           setSmsTimeOut(smsNumber);

        final int totalPassports =                                              Boot
                                                                                    .sharedPreferences
                                                                                    .getInt(StartActivity.PASSPORTS_COUNT_KEY, 0);

        long daysAfterUpdate =                                                  (Calendar.getInstance().getTimeInMillis()
                                                                                -
                                                                                Boot.sharedPreferences.getLong(FragmentPassportsUpdate.LAST_UPDATE_DATE_KEY, 0)) / (24 * 60 * 60 * 1000);


        decoder =                                                               new ImgCryptoDecoder(passportsPath, tempPassportsPath);


        // Выкидываем сообщение с ошибкой, если не удалось создать временную директорию
        if(!Boot.sharedPreferences.getBoolean(StartActivity.CREATE_TEMP_SIGNALS_DIR_KEY, false) ||
                !Boot.sharedPreferences.getBoolean(StartActivity.CREATE_TEMP_PASSPORTS_DIR_KEY, false)) {
            Snackbar
                    .make(v, R.string.create_temp_folder_error, Snackbar.LENGTH_LONG)
                    .show();
        }


        // Если кол-во паспортов равно нулю - сообщаем об этом
        if(totalPassports == 0) {
            Snackbar
                    .make(v, R.string.passports_count_error, Snackbar.LENGTH_INDEFINITE)
                    .show();
        }


        // Если давно не было обновления
        if(daysAfterUpdate > Settings.NO_UPDATE_DAYS_ALERT) {
            Snackbar
                    .make(v, R.string.passports_no_update_error, Snackbar.LENGTH_LONG)
                    .show();
        }


        // Переход на страницу с картой
        fabMapOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!Functions.isNetworkAvailable(getActivity().getBaseContext())) {
                    Snackbar
                            .make(view, R.string.no_internet, Snackbar.LENGTH_LONG)
                            .show();
//                    Toast.makeText(getContext(), getString(R.string.no_internet), Toast.LENGTH_LONG).show();
                }

                else if(listPassports.getCount() == 0) {
                    Snackbar
                            .make(view, R.string.open_map_error, Snackbar.LENGTH_LONG)
                            .show();
//                    Toast.makeText(getContext(), R.string.open_map_error, Toast.LENGTH_LONG).show();
                }

                else {
                    Intent intent =                                     new Intent(getActivity().getApplicationContext(), MapActivity.class);
                    intent.putExtra(OBJECT_ADDRESS_MAP_INTENT_KEY, getObjectAddressMap());
                    startActivity(intent);
                }

            }
        });


        // Открытие панели с информацией о кол-ве паспортов
        fabPassportsInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Snackbar
                        .make(v, getString(R.string.total_passports) + totalPassports, Snackbar.LENGTH_LONG)
                        .show();
            }
        });


        // Загрузка паспортов в список
        fabPassportsRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new UpdateListView(new ProgressDialog(getActivity())).execute();
            }
        });




//        listPassports.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//
//                Toast.makeText(getActivity(), "dfsfd", Toast.LENGTH_LONG).show();
//
//                return true;
//            }
//        });



        // Открытие файла в галлерее
        listPassports.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                TextView tv = (TextView) view.findViewById(R.id.passports_item_hide);

//                Intent intent = new Intent();
//                intent.addCategory(Intent.CATEGORY_OPENABLE);
//                intent.setType("image/*");
//                intent.setAction(intent.ACTION_GET_CONTENT);
//                startActivityForResult(Intent.createChooser(intent, "SDSD"), 1);

                showSelectFileDialog(getFilesToDecodeArray(tv));

            }
        });


        // Если адаптер заполнен - загружаем список
        if(RootActivity.passportsListAdapter != null) {
            setAdapter();
        }


        if(lastReadId != READ_UPDATE_MSG_ID) {
            showUpdateMessageDialog();
        }


        return v;
    }


    /**
     *  Создание и отображение диалогового окна с информацией о добавленных опциях и изменениях
     */
    private void showUpdateMessageDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle(R.string.update_news_dialog_title)
                .setMessage(R.string.update_news_dialog_text_500)
                .setPositiveButton(R.string.update_news_dialog_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Boot.sharedPreferences
                                .edit()
                                .putInt(READ_UPDATE_MSG_KEY, READ_UPDATE_MSG_ID)
                                .apply();
                    }
                }).create();
        builder.show();

    }




    /**
     *  Создание коллекции типа <Объект, Адрес> для передачи в Activity карты для установки меток
     * @return                      - возвращается коллекция HashMap<String, String>
     */
    private HashMap<String, String> getObjectAddressMap() {

        HashMap<String, String> objectAddressMap = null;
        int listCount = listPassports.getCount();

        if(listCount > 0) {

            objectAddressMap = new HashMap<>();

            for(int x = 0; x < listPassports.getCount(); x++) {

                TextView objectNumber = (TextView)(listPassports.getAdapter().getView(x, null, null))
                                            .findViewById(R.id.passports_item_object_number);
                TextView objectAddress = (TextView)(listPassports.getAdapter().getView(x, null, null))
                                            .findViewById(R.id.passports_item_object_address);

                objectAddressMap.put(objectNumber.getText().toString(), objectAddress.getText().toString());

//                System.out.println("myLog: " + objectNumber.getText().toString() + " " + objectAddress.getText().toString());

            }

        }

        return objectAddressMap;
    }



    /**
     *  Создание и отображение модального диалога для выбора файла для открытия в галлерее
     * @param filesArray                    - массив из файлов паспортов на объект
     */
    private void showSelectFileDialog(final String[] filesArray) {

        new AlertDialog.Builder(getActivity())
                .setItems(filesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent =                                 new Intent();

                        decoder.setImgName(filesArray[which]);
                        decoder.decodeFile();

                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse("file://" + tempPassportsPath + File.separator + filesArray[which]), "image/*");

                        startActivity(intent);
                    }
                })
                .create()
                .show();

    }






    /**
     *  Функция возвращает строку со списком файлов, которая записана в скрытом поле каждого
     *  Item в ListView в виде массива
     * @param tv                - ссылка на TextView
     * @return                  - возвращается массив String[]
     */
    private String[] getFilesToDecodeArray(TextView tv) {

        String hideFilesString =                                        tv.getText().toString();
        String[] filesToDecodeArray =                                   new String[] { "" };

        if(hideFilesString.length() > 0) {
            filesToDecodeArray =                                        (hideFilesString.contains("#")) ?
                                                                            hideFilesString.split("#") :
                                                                            new String[] { hideFilesString };
        }

        return filesToDecodeArray;

    }





    /**
     *  Функция устанавливает адаптер в ListView, который отображает список паспортов
     */
    private void setAdapter() {
        listPassports.setAdapter(RootActivity.passportsListAdapter);
        ((SimpleAdapter) listPassports.getAdapter()).notifyDataSetChanged();
        listPassports.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }






    /**
     *  Установка времени "жизни" смс сообщений в зависимости от источника смс
     * @param number                    - "Источник" смс, номер телефона
     */
    private int setSmsTimeOut(String number) {
        return number.equals(Settings.SMS_NUMBERS_ARRAY[1]) ?
                Settings.SMS_LIFE_TIME_SERVICE :
                Settings.SMS_LIFE_TIME_GBR;
    }









    /**
     * Класс формирует список ListView на основании входящих смс
     */
    class UpdateListView
            extends AsyncTask<Void, Void, Void> {



        private ProgressDialog          pd;




        /**
         *  Констуктор класса
         * @param pd                        - ссылка на объект ProgressDialog
         */
        UpdateListView(ProgressDialog pd) {
            this.pd =                                                           pd;

            this.pd.setTitle(Settings.PD_TITLE);
            this.pd.setMessage(getResources().getString(R.string.sms_scan_title));

            this.pd.setCancelable(false);
        }











        /**
         *  Функция сканирует смс и отправляет данные для формирования списка
         */
        private void getSms() {

            Cursor cursor =                                 getActivity().getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
            ArrayList<String> listIncomingSms =             new ArrayList<>();

            if(cursor != null && cursor.moveToFirst()) {

                for (int idx = 0; idx < cursor.getCount(); idx++) {
                    String from =                           cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    String body =                           cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    Long timeDifference =                   (Calendar.getInstance().getTimeInMillis() -
                                                                cursor.getLong(cursor.getColumnIndexOrThrow("date"))) / 1000;
                    String[] numberAndType =                Functions.getNumberAndTypeFromString(
                                                                Functions.getFirstWordInString(body));

                    if(from.equals(smsNumber) &&
                            timeDifference <= smsLiveTime &&
                            Functions.isInteger(numberAndType[1]) &&
                            (smsNumber.equals(Settings.SMS_NUMBERS_ARRAY[0]) ||
                                    (!smsNumber.equals(Settings.SMS_NUMBERS_ARRAY[0]) &&
                                            numberAndType[0].equals(DEFAULT_OBJECT_PREFIX)))) {

//                        System.out.println("myLog " + body);
                        listIncomingSms.add(body);

                    }

                    cursor.moveToNext();
                }

                cursor.close();
            }


            createAdapter(listIncomingSms);

        }









        /**
         *  Функция формирует данные (адаптер) для формирования ListView
         * @param listIncomingSms               - коллекция с номерами объектов, которые пришли в
         *                                        виде смс
         */
        private void createAdapter(final ArrayList<String> listIncomingSms) {

            ArrayList<HashMap<String, Object>> listPassports =
                                                            new ArrayList<>();

            if(listIncomingSms.size() > 0) {

                for (final String smsBody : listIncomingSms) {

//                    System.out.println("myLog smsBody:" + smsBody);

                    final String[] numberAndType =          Functions.getNumberAndTypeFromString(
                                                                Functions.getFirstWordInString(smsBody));

                    FilenameFilter filenameFilter =         new FilenameFilter() {

                        @Override
                        public boolean accept(File dir, String filename) {

                            return (!numberAndType[0].equals(DEFAULT_OBJECT_PREFIX)) ?
                                        filename.contains(numberAndType[0]) &&
                                                filename.contains(numberAndType[1]) :
                                        filename.contains(numberAndType[1]) &&
                                                !filename.contains("_");

                        }

                    };

                    final File[] listFiles =            new File(passportsPath).listFiles(filenameFilter);

                    if (listFiles != null && listFiles.length > 0) {

                        HashMap<String, Object> listPassportItem =
                                                        new HashMap<>();
                        String objectNumber =           null,
                                objectAddress =         null,
                                filesToDecode =         null;
//                        boolean fileFound =             false;

                        for (File cobaFile : listFiles) {

//                            System.out.println("myLog cobaFile:" + cobaFile);

                            if (cobaFile.isFile()) {

                                String checkObjectNumber =  objectEquals(listIncomingSms, cobaFile.getName());

//                                System.out.println("myLog 1: " + objectNumber);

                                if (checkObjectNumber != null) {

//                                    System.out.println("myLog 2: " + objectNumber);

                                    objectNumber =          checkObjectNumber;
                                    objectAddress =         getAddressFromSms(smsBody);
//                                    fileFound =             true;

                                    if (filesToDecode != null) {
                                        filesToDecode +=    "#";
                                    } else {
                                        filesToDecode =     "";
                                    }

                                    filesToDecode +=        cobaFile.getName();

                                }

                            }

                        }

//                        System.out.println("myLog: " + objectNumber + "_" + filesToDecode);

                        if (objectNumber != null) {

                            listPassportItem.put("img", R.mipmap.ic_passports_item_ico);
                            listPassportItem.put("objectNumber", objectNumber);
                            listPassportItem.put("objectType", "(" + numberAndType[0] + ")");
                            listPassportItem.put("objectAddress", objectAddress);
                            listPassportItem.put("fileName", filesToDecode);

                            listPassports.add(listPassportItem);

                        }

                    }

                }

            }

            RootActivity.passportsListAdapter =                                 new SimpleAdapter(
                                                                                    getActivity(),
                                                                                    listPassports,
                                                                                    R.layout.passports_list_item,
                                                                                    new String[]{
                                                                                            "img",
                                                                                            "objectNumber",
                                                                                            "objectType",
                                                                                            "objectAddress",
                                                                                            "fileName"
                                                                                    },
                                                                                    new int[]{
                                                                                            R.id.passports_item_img,
                                                                                            R.id.passports_item_object_number,
                                                                                            R.id.passports_item_object_type,
                                                                                            R.id.passports_item_object_address,
                                                                                            R.id.passports_item_hide
                                                                                    });

        }









        /**
         *  Функция возвращает номер объекта из строки смс сообщения, который попадает под маску файла
         * @param listIncomingSms           - список с смс
         * @param fileName                  - файл, номер объекта из имени которого сравниваем с
         *                                    номером из смс
         * @return                          - возвращается номер объекта или null в случае, если
         *                                      совпадения нет
         */
        private String objectEquals(ArrayList<String> listIncomingSms, String fileName) {

            for (String smsBody : listIncomingSms) {

                final String[] numberAndType =              Functions.getNumberAndTypeFromString(
                                                                Functions.getFirstWordInString(smsBody));

                if(fileName.contains(Settings.OBJECT_PART_DIVIDER)) {

                    String fileNameSplit[] =                (!fileName.contains("_")) ?
                                                                fileName.substring(0, fileName.lastIndexOf(
                                                                    Settings.OBJECT_PART_DIVIDER)).split(",") :
                                                                fileName.substring(fileName.indexOf("_") + 1, fileName.lastIndexOf(
                                                                        Settings.OBJECT_PART_DIVIDER)).split(",");

                    for (String fn : fileNameSplit) {

//                        System.out.println("myLog fileNameSplit:" + fn);

                        if (Functions.isInteger(fn) && fn.equals(numberAndType[1])) {
//                            System.out.println("myLog " + smsBody + "%" + fn + "%" + fileName);
                            return numberAndType[1];

                        }

                    }

                }

            }

            return null;
        }









        /**
         *  Функция выдергивает адрес объекта из смс сообщения
         * @param smsBody               - строка смс сообщения
         * @return                      - возвращает адрес в случае успеха или "-" в случае неудачи
         */
        private String getAddressFromSms(String smsBody) {

            if(smsBody.contains(Settings.ADDRESS_SYMBOL_START) & smsBody.contains(Settings.ADDRESS_SYMBOL_FINISH)) {
                int startAddress =                                              smsBody.indexOf(Settings.ADDRESS_SYMBOL_START) + 1;
                int finishAddress =                                             smsBody.lastIndexOf(Settings.ADDRESS_SYMBOL_FINISH);

                return smsBody.substring(startAddress, finishAddress);
            }

            return "-";

        }








        @Override
        protected void onPreExecute() {

            this.pd.show();

        }

        @Override
        protected Void doInBackground(Void... params) {

            getSms();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            setAdapter();

            this.pd.dismiss();
        }

    }




}