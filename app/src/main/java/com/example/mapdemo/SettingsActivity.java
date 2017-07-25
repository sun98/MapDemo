package com.example.mapdemo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

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
