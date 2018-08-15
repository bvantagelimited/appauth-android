package com.bvantage.ipfication.demoapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class UserActivity extends AppCompatActivity {

    private TextView tv;
    private String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        tv = findViewById(R.id.message);
        updateinfo();
        if(getIntent() != null && getIntent().getExtras() != null){
            userID = getIntent().getExtras().getString("preferred_username");
            updateinfo();
        }
        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }


    private void updateinfo(){
        if(tv != null){
            tv.setText("Connected! " + userID);
        }
    }
}
