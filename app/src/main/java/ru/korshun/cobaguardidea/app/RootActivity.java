package ru.korshun.cobaguardidea.app;



import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;




/**
 *  Главный класс
 *  Основное Activity, в котором создаем drawer, фрагменты и т.п.
 */
public class RootActivity
        extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {



    DrawerLayout drawer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root);

        Toolbar toolbar =                                   (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer =                                            (DrawerLayout) findViewById(R.id.drawer_layout);

        FloatingActionButton fab =                          (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar
                        .make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show();
            }
        });

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
        // Handle navigation view item clicks here.
        int id =                                            item.getItemId();

        switch (id) {

            case R.id.nav_drawer_passports_item:
                break;

            case R.id.nav_drawer_signals_item:
                break;

            case R.id.nav_drawer_objects_item:
                break;

            case R.id.nav_drawer_settings_item:
                break;

            case R.id.nav_drawer_exit_item:
                finish();
                break;

            default:
                break;
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }









}
