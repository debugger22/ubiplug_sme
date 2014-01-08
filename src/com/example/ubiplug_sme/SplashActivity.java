package com.example.ubiplug_sme;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

/**
 * This class creates Splash Screen of the application
 */
public class SplashActivity extends Activity {
 
    // Splash screen timer
    private static int SPLASH_TIME_OUT = 2000;
    private TextView textView;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        textView = (TextView)findViewById(R.id.lblLogo);
        Typeface tf = Typeface.createFromAsset(getApplicationContext().getAssets(),"fonts/roboto-condensed-bold.ttf");
        textView.setTypeface(tf);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
            	Intent i = new Intent(SplashActivity.this, MainActivity.class);
            	startActivity(i);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
 
    @Override
    public void onBackPressed() {
       return;
    }	
}