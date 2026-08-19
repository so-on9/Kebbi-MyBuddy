package com.example.assistant;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<PlaceItem> mPlaceList;
    private PlaceAdapter mAdapter;
    //private TextView tvhello;
    private String placeName = "學校";

    private Button p_btn;
    private Button backpage;
    private Button mainpage;
    private LinearLayout fam_btn;
    private LinearLayout sch_btn;
    private LinearLayout com_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //tvhello = findViewById(R.id.hello);
        //p_btn = findViewById(R.id.p_btn);
        backpage = findViewById(R.id.backpage);
        mainpage = findViewById(R.id.mainpage);
        sch_btn = findViewById(R.id.sch_btn);
        fam_btn = findViewById(R.id.fam_btn);
        com_btn = findViewById(R.id.com_btn);

        initList();
     // Spinner spinner = (Spinner) findViewById(R.id.spinner);
      //  mAdapter = new PlaceAdapter(this, mPlaceList);
       // spinner.setAdapter(mAdapter);
       // spinner.setSelection(0, false);

       /* spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PlaceItem clickedItem = (PlaceItem) parent.getItemAtPosition(position);
                String clickedPlaceName = clickedItem.getPlaceName();
                placeName = clickedPlaceName;
                Log.d("Tag", "placeName: "+ placeName);
                //tvhello.setText(clickedPlaceName);
                String sPos=String.valueOf(position);
                //tvhello.setText("選項"+sPos+":"+clickedPlaceName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }*/
        //});

        /*p_btn.setOnClickListener(view -> {
            if(placeName.equals("學校")) {
                //GlobalVariable globalVariable = ((GlobalVariable)getApplicationContext());
                //globalVariable.setAst_num("1");
                Intent intent = new Intent(MainActivity.this, SchoolActivity.class);
                startActivity(intent);
            }
            else if (placeName.equals("家庭")) {

            }
            else if (placeName.equals("社區")) {

            }*/
        //});
        fam_btn.setOnClickListener(view -> {

        });

        sch_btn.setOnClickListener(view -> {
            GlobalVariable globalVariable = ((GlobalVariable)getApplicationContext());
            globalVariable.setAst_num("1");
            Intent intent = new Intent(MainActivity.this, SchoolActivity.class);
            startActivity(intent);
        });

        com_btn.setOnClickListener(view -> {

        });


        backpage.setOnClickListener(view -> {
            finish();
        });

        mainpage.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            startActivity(intent);
        });


        /*sch_btn = findViewById(R.id.btn_school);
        fam_btn = findViewById(R.id.btn_family);
        com_btn = findViewById(R.id.btn_community);*/

        //sch_btn.setOnClickListener(view -> {
            /*GlobalVariable globalVariable = ((GlobalVariable)getApplicationContext());
            globalVariable.setAst_num("1");*/
            //Intent intent = new Intent(MainActivity.this, SchoolActivity.class);

            //startActivity(intent);
        //});

        //fam_btn.setOnClickListener(view -> {});

        //com_btn.setOnClickListener(view -> {});

    }

    private void initList() {
        mPlaceList = new ArrayList<>();
        mPlaceList.add(new PlaceItem("學校", R.drawable.school));
        mPlaceList.add(new PlaceItem("家庭", R.drawable.family));
        mPlaceList.add(new PlaceItem("社區", R.drawable.overpopulation));
    }

}
