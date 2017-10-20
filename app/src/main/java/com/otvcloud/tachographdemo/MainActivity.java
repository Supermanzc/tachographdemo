package com.otvcloud.tachographdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnRecorder = (Button) findViewById(R.id.btnRecorder);
        btnRecorder.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnRecorder:
                Intent intent = new Intent(this, RecorderActivity.class);
                startActivity(intent);
                break;
        }
    }
}
