package ru.korshun.cobaguardidea.app.fragments;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import ru.korshun.cobaguardidea.app.Functions;
import ru.korshun.cobaguardidea.app.R;
import ru.korshun.cobaguardidea.app.RootActivity;
import ru.korshun.cobaguardidea.app.Settings;
import ru.korshun.cobaguardidea.app.StartActivity;
import ru.korshun.cobaguardidea.app.UpdatePassportsService;

public class FragmentPassportsUpdate
        extends Fragment {


    public final static String          LAST_UPDATE_DATE_KEY =      "pref_last_update_date";

    public static final String          DOWNLOAD_TYPE =             "downloadType";

    public static final String          PI_FILES_COUNTER =          "piFilesCounter";
    public static final String          PI_FILES_TOTAL =            "piFilesTotal";

//    private final int                   CODE_REQUEST =              0;

    public static final int             CODE_STATUS_CONNECT =       1;
    public static final int             CODE_STATUS_DISCONNECT =    0;
    public static final int             CODE_STATUS_NO_FILES =      -1;
    public static final int             CODE_STATUS_NO_CONNECT =    -2;
    public static final int             CODE_STATUS_ERROR =         -3;

    public static final int             CODE_FILES_COUNTER =        10;
    public static final int             CODE_FILES_TOTAL =          20;

    double                              counter =                   0;
    double                              total =                     0;

    private LinearLayout                layoutUpdateProccess;

    private ProgressBar                 pbLoad;
    private TextView                    tvProgressStatus, tvProgress, tvProgressProc, tvLastUpdateDate;

    private long                        clickTime =                 0l;
    private int                         clickCount =                0;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }




    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v =                                                    inflater.inflate(R.layout.fragment_passports_update, container, false);

        tvLastUpdateDate =                                          (TextView) v.findViewById(R.id.tv_last_update_date);

        LinearLayout layoutLastUpdate =                             (LinearLayout) v.findViewById(R.id.layout_passports_last_update);
        LinearLayout layoutUpdateAll =                              (LinearLayout) v.findViewById(R.id.layout_passports_update_all);
        LinearLayout layoutUpdateObject =                           (LinearLayout) v.findViewById(R.id.layout_passports_update_object);
        layoutUpdateProccess =                                      (LinearLayout) v.findViewById(R.id.layout_passports_update_process);
        pbLoad =                                                    (ProgressBar) v.findViewById(R.id.pb_files_load);
        tvProgressStatus =                                          (TextView) v.findViewById(R.id.tv_files_load_status);
        tvProgress =                                                (TextView) v.findViewById(R.id.tv_files_load);
        tvProgressProc =                                            (TextView) v.findViewById(R.id.tv_files_load_proc);

        tvLastUpdateDate.setText(getLastUpdateDate());

        if(Functions.isServiceRunning(UpdatePassportsService.class, getActivity())) {
            layoutUpdateProccess.setVisibility(View.VISIBLE);
        }

        // Запуск обновления всех паспортов
        layoutUpdateAll.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                if (!Functions.isServiceRunning(UpdatePassportsService.class, getActivity())) {

                    resetUpdateBlock(0);

                } else {
                    Toast
                            .makeText(getActivity(), getString(R.string.update_double_error), Toast.LENGTH_LONG)
                            .show();
                }

                return false;
            }
        });


        //Запуск обновления паспорта на объект
        layoutUpdateObject.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                if (!Functions.isServiceRunning(UpdatePassportsService.class, getActivity())) {

                    createDownloadFileDialog();

                } else {
                    Toast
                            .makeText(getActivity(), getString(R.string.update_double_error), Toast.LENGTH_LONG)
                            .show();
                }

                return false;
            }
        });




        //  Приблуда для установки времени обновления в текущее время
        layoutLastUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                long thisTime =                                     Calendar.getInstance().getTimeInMillis();

                if (clickCount != 0) {
                    if ((thisTime - clickTime) < 1000) {
                        clickCount++;
                        clickTime =                                 Calendar.getInstance().getTimeInMillis();
                    }
                    else {
                        clickCount =                                0;
                        clickTime =                                 0l;
                    }
                }
                if (clickCount == 10) {
                    StartActivity.sharedPreferences
                            .edit()
                            .putLong(FragmentPassportsUpdate.LAST_UPDATE_DATE_KEY, Calendar.getInstance().getTimeInMillis())
                            .apply();
                    Toast.makeText(getActivity().getApplicationContext(), "DONE!", Toast.LENGTH_LONG).show();
                    clickTime =                                     0l;
                    tvLastUpdateDate.setText(getLastUpdateDate());
                }
                if (clickCount == 0) {
                    clickCount++;
                    clickTime =                                     Calendar.getInstance().getTimeInMillis();
                }
            }
        });


        return v;
    }






    /**
     *  Создание диалогового окна, в котором указывается объект для загрузки файлов
     */
    private void createDownloadFileDialog() {


        AlertDialog.Builder aDialgBuilder =                         new AlertDialog.Builder(getActivity());
        final View aDialogView =                                    LayoutInflater
                                                                        .from(getActivity().getBaseContext())
                                                                        .inflate(R.layout.dialog_with_edittext, new LinearLayout(getActivity()), false);


        final EditText aDialogEditText =                            (EditText) aDialogView.findViewById(R.id.alert_dialog_edittext);

        aDialgBuilder
                .setTitle(getResources().getText(R.string.dialog_object_dwnl_title))
                .setCancelable(false)
                .setView(aDialogView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        resetUpdateBlock(Integer.parseInt(aDialogEditText.getText().toString()));

                        InputMethodManager imm =                    (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(aDialogView.getWindowToken(), 0);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        final AlertDialog aDialog = aDialgBuilder.create();

        aDialog
                .setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        ((AlertDialog) dialog)
                                .getButton(AlertDialog.BUTTON_POSITIVE)
                                .setEnabled(false);
                    }
                });

        aDialogEditText
                .setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {

                        if (aDialogEditText.getText().length() > 3 && aDialogEditText.getText().length() <= 5) {
                            aDialog
                                    .getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setEnabled(true);
                        } else {
                            aDialog
                                    .getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setEnabled(false);
                        }
                        return false;
                    }
                });

        aDialog.show();
    }







    /**
     *  Сброс блока обновления на начальные значения
     * @param dwnlType                  - тип закачки: 0 - все новые паспорта или конкретный номер
     */
    private void resetUpdateBlock(int dwnlType) {
        counter =                                       0;
        total =                                         0;
        createDownloadService(dwnlType);
        tvProgressStatus.setText(getString(R.string.passports_update_connect_title));
        pbLoad.setProgress(0);
        tvProgress.setText(getString(R.string.passports_update_procces_start_count));
        tvProgressProc.setText(getString(R.string.passports_update_procces_start_proc));
    }







    /**
     *  Ловим данные, которые прилетели из Activity
     *  Смотрим на requestCode - это порядковый номер фрагмента:
     *   0 - фрагмент списка паспортов
     *   1 - фрагмент сигналов
     *   2 - фрагмент снятия\постановки объекта и т.д. по порядку в меню
     *
     *   Тут ловим requestCode == 0
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RootActivity.CODE_REQUEST_PASSPORTS_UPDATE) {

            switch (resultCode) {

                case CODE_STATUS_CONNECT:
                    layoutUpdateProccess.setVisibility(View.VISIBLE);
                    tvProgressStatus.setText(getString(R.string.passports_update_connect_title));
                    break;

                case CODE_STATUS_DISCONNECT:
                    tvProgressStatus.setText(getString(R.string.passports_update_complite_title));
                    tvLastUpdateDate.setText(getLastUpdateDate());
                    break;

                case CODE_STATUS_NO_FILES:
                    tvProgressStatus.setText(getString(R.string.update_no_files));
                    break;

                case CODE_STATUS_NO_CONNECT:
                    tvProgressStatus.setText(getString(R.string.no_server_connect));
                    break;

                case CODE_STATUS_ERROR:
                    tvProgressStatus.setText(getString(R.string.err_data));
                    break;

            }

        }


        if(requestCode == CODE_FILES_COUNTER) {
            layoutUpdateProccess.setVisibility(View.VISIBLE);
            tvProgressStatus.setText(getString(R.string.passports_update_proccess_title));
            pbLoad.setProgress(resultCode);
            counter =                                               resultCode;
        }

        if(requestCode == CODE_FILES_TOTAL) {
            tvProgressStatus.setText(getString(R.string.passports_update_proccess_title));
            pbLoad.setMax(resultCode);
            total =                                                 resultCode;
        }

        if(counter > 0 & total > 0) {
            String tvProgressText =                                 String.valueOf((int)counter) + "/" + String.valueOf((int)total);
            String tvProgressProcText =                             (int)((counter / total) * 100) + "%";

            tvProgress.setText(tvProgressText);
            tvProgressProc.setText(tvProgressProcText);
        }

    }





    /**
     *  Создание сервиса для загрузки файлов
     * @param dwnlType      - параметр, указывающий какие именно файлы загружать
     *                      0 - все новые
     *                      <НОМЕР> - выбранный номер объекта
     */
    private void createDownloadService(int dwnlType) {

//        PendingIntent piRequest;
        PendingIntent piRequest, piCounter, piTotal;

        Intent updateDbServiceIntent =                              new Intent(getActivity().getBaseContext(), UpdatePassportsService.class);
        piRequest =                                                 getActivity().createPendingResult(RootActivity.CODE_REQUEST_PASSPORTS_UPDATE, updateDbServiceIntent, 0);
        piCounter =                                                 getActivity().createPendingResult(CODE_FILES_COUNTER, updateDbServiceIntent, 0);
        piTotal =                                                   getActivity().createPendingResult(CODE_FILES_TOTAL, updateDbServiceIntent, 0);
//        piError =                               getActivity().createPendingResult(CODE_ERROR, updateDbServiceIntent, 0);

        updateDbServiceIntent
                .putExtra(DOWNLOAD_TYPE, dwnlType)
                .putExtra(RootActivity.PI_REQUEST, piRequest)
                .putExtra(PI_FILES_COUNTER, piCounter)
                .putExtra(PI_FILES_TOTAL, piTotal);

        getActivity().startService(updateDbServiceIntent);

    }





    /**
     * Функция возвращает дату последнего обновления файлов, которая записана в  sharedPreferences
     * @return              - возвращается дата в формате String
     */
    private String getLastUpdateDate() {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(StartActivity.sharedPreferences.getLong(FragmentPassportsUpdate.LAST_UPDATE_DATE_KEY, 0)));
    }




}