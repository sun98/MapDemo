package com.example.mapdemo.activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.example.mapdemo.R;

// TODO: not used yet

public class SettingsActivity extends Activity {
    private static String APP_CONFIG = "app_config";

    private SharedPreferences config;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        config = getSharedPreferences(APP_CONFIG, MODE_APPEND);
        editor = config.edit();

    }
}
