package ru.korshun.cobaguardidea.app.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ru.korshun.cobaguardidea.app.R;

public class FragmentPassports
        extends Fragment {




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }




    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v =                                                inflater.inflate(R.layout.fragment_passports, container, false);

        FloatingActionButton fabPassportsRefresh =              (FloatingActionButton) v.findViewById(R.id.fab_passports_refresh);
        FloatingActionButton fabPassportsUpdate =               (FloatingActionButton) v.findViewById(R.id.fab_passports_update);

//        fabPassportsRefresh.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar
//                        .make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null)
//                        .show();
//            }
//        });


        return v;
    }





    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }


}