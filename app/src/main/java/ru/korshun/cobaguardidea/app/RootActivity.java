package ru.korshun.cobaguardidea.app;



import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import ru.korshun.cobaguardidea.app.fragments.FragmentObjects;
import ru.korshun.cobaguardidea.app.fragments.FragmentPassports;
import ru.korshun.cobaguardidea.app.fragments.FragmentSettings;
import ru.korshun.cobaguardidea.app.fragments.FragmentSignals;


/**
 *  Главный класс
 *  Основное Activity, в котором создаем drawer, фрагменты и т.п.
 */
public class RootActivity
        extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {



    private DrawerLayout drawer;
    public static SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root);

        Toolbar toolbar =                                   (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer =                                            (DrawerLayout) findViewById(R.id.drawer_layout);

        sharedPreferences =                                 PreferenceManager.getDefaultSharedPreferences(this);

//        FloatingActionButton fab =                          (FloatingActionButton) findViewById(R.id.fab_root);
//
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar
//                        .make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null)
//                        .show();
//            }
//        });

        ActionBarDrawerToggle toggle =                      new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView =                     (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);



        if(savedInstanceState == null) {
            MenuItem firstItem =                            navigationView.getMenu().getItem(0);

            firstItem.setChecked(true);
            onNavigationItemSelected(firstItem);
        }

        Toast
                .makeText(this, sharedPreferences.getBoolean(FragmentSettings.AUTO_UPDATE_KEY, true) + "", Toast.LENGTH_LONG)
                .show();

    }







    @Override
    public void onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }

        else {
            super.onBackPressed();
        }

    }






    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        Fragment fragment =                                 null;
        Bundle bundle =                                     new Bundle();
        FragmentManager fragmentManager =                   getFragmentManager();

        // Handle navigation view item clicks here.
        int id =                                            item.getItemId();

        switch (id) {

            case R.id.nav_drawer_passports_item:

                fragment =                         new FragmentPassports();
                break;

            case R.id.nav_drawer_signals_item:

                fragment =                           new FragmentSignals();
                break;

            case R.id.nav_drawer_objects_item:

                fragment =                           new FragmentObjects();
                break;

            case R.id.nav_drawer_settings_item:

                fragment =                          new FragmentSettings();
                break;

            case R.id.nav_drawer_exit_item:
                finish();
                return false;

            default:
                Toast
                        .makeText(this, getResources().getString(R.string.err_data), Toast.LENGTH_LONG)
                        .show();
                return false;
        }



        fragment.setArguments(bundle);
        fragmentManager
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit();


        drawer.closeDrawer(GravityCompat.START);
        return true;
    }









}
