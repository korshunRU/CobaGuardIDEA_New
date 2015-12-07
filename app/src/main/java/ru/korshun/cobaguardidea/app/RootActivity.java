package ru.korshun.cobaguardidea.app;



import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import ru.korshun.cobaguardidea.app.fragments.FragmentObjects;
import ru.korshun.cobaguardidea.app.fragments.FragmentPassports;
import ru.korshun.cobaguardidea.app.fragments.FragmentPassportsUpdate;
import ru.korshun.cobaguardidea.app.fragments.FragmentSettings;
import ru.korshun.cobaguardidea.app.fragments.FragmentSignals;


/**
 *  Главный класс
 *  Основное Activity, в котором создаем drawer, фрагменты и т.п.
 */
public class RootActivity
        extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    private android.support.v4.app.Fragment     fragment =                          null;
    private android.app.Fragment                nativeFragment =                    null;


    private DrawerLayout                        drawer;
    private Toolbar                             toolbar;
    public NavigationView                       navigationView;

//    public static final String                  PI_REQUEST =                        "piRequest";
//
//    public static final int                     CODE_REQUEST_PASSPORTS_UPDATE =     0;
//    public static final int                     CODE_REQUEST_SIGNALS =              1;
//    public static final int                     CODE_REQUEST_OBJECTS =              2;

    public static SimpleAdapter                 passportsListAdapter =              null;
    public static SimpleAdapter                 signalsListAdapter =                null;
    public static SimpleAdapter                 objectsListAdapter =                null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root);

        toolbar =                                           (Toolbar) findViewById(R.id.toolbar);
        drawer =                                            (DrawerLayout) findViewById(R.id.drawer_layout);

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle =                      new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView =                                    (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);



        if(savedInstanceState == null) {
            MenuItem firstItem =                            navigationView.getMenu().getItem(0);

            firstItem.setChecked(true);
            onNavigationItemSelected(firstItem);
        }

//        Toast
//                .makeText(this, StartActivity.sharedPreferences.getString(FragmentSettings.PASSPORTS_PATH_KEY, "-") + "", Toast.LENGTH_LONG)
//                .show();

    }



    /**
     *  Здесь, значит, делается следующая херня: всем фрагментам отсылается то, что пришло от
     *  сервиса. Любого. А в самом фрагменте уже будем ловить данные и смотреть, что это и для
     *  чего прилетело
     */
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//
//        List<android.support.v4.app.Fragment> fragments = getSupportFragmentManager().getFragments();
//
//
//        for(android.support.v4.app.Fragment f : fragments) {
//            if (f != null) {
//                f.onActivityResult(requestCode, resultCode, data);
//            }
//        }
//
//    }





    @Override
    public void onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }

        else {
            createConfirmDialog();
        }

    }






    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        boolean isSettingsFragment =                        false;
        Bundle bundle =                                     new Bundle();

        // Handle navigation view item clicks here.
        int id =                                            item.getItemId();

        switch (id) {

            case R.id.nav_drawer_passports_item:

                fragment =                                  new FragmentPassports();
                break;

            case R.id.nav_drawer_signals_item:

                fragment =                                  new FragmentSignals();
                break;

            case R.id.nav_drawer_objects_item:

                fragment =                                  new FragmentObjects();
                break;

            case R.id.nav_drawer_settings_item:

                isSettingsFragment =                        true;
                nativeFragment =                            new FragmentSettings();
                break;

            case R.id.nav_drawer_passports_update_item:

                fragment =                                  new FragmentPassportsUpdate();
                break;

            case R.id.nav_drawer_exit_item:
                createConfirmDialog();
                return false;

            default:
                Toast
                        .makeText(this, getResources().getString(R.string.err_data), Toast.LENGTH_LONG)
                        .show();
                return false;
        }

        toolbar.setTitle(item.getTitle());

        if(isSettingsFragment) {

            if(fragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .remove(fragment)
                        .commit();
            }

            nativeFragment.setArguments(bundle);
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, nativeFragment)
                    .commit();

        }

        else {

            if (nativeFragment != null) {
                getFragmentManager()
                        .beginTransaction()
                        .remove(nativeFragment)
                        .commit();
                nativeFragment =                            null;
            }

            fragment.setArguments(bundle);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();

        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }





    /**
     *  СОздание диалогового окна, в котором запрашивается подтверждение выхода
     */
    private void createConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getResources().getText(R.string.confirm_question))
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create()
                .show();
    }





    /**
     *  Функция ищет пункт в меню боковой панели по его заголовку
     * @param title                     - заголовок для поиска
     * @return                          - в случае успеха возвращается MenuItem
     */
    public MenuItem getMenuItemFromTitle(String title) {

        for(int x = 0; x < navigationView.getMenu().size(); x++) {

            if(navigationView.getMenu().getItem(x).getTitle().toString().equals(title)) {

                return navigationView.getMenu().getItem(x);

            }

        }

        return null;

    }






}
