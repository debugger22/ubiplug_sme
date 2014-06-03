package com.pirhoalpha.awairhome;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

import com.mattkula.secrettextview.SecretTextView;
import com.readystatesoftware.systembartint.SystemBarTintManager;

/**
 * This class creates Splash Screen of the application
 */
public class SplashActivity extends Activity {

	// Splash screen timer
	private static int SPLASH_TIME_OUT = 3000;
	private SecretTextView textView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			Window w = getWindow(); // in Activity's onCreate() for instance
			w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
			w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			SystemBarTintManager tintManager = new SystemBarTintManager(this);
			SystemBarTintManager.SystemBarConfig config = tintManager
					.getConfig();

			int actualColor = getResources().getColor(
					android.R.color.holo_blue_dark);
			tintManager.setTintColor(actualColor);
			tintManager
					.setStatusBarTintDrawable(new ColorDrawable(actualColor));
			// getActionBar()
			// .setBackgroundDrawable(new ColorDrawable(actualColor));
		}

		this.textView = (SecretTextView) findViewById(R.id.lblLogo);
		Typeface tf = Typeface.createFromAsset(getApplicationContext()
				.getAssets(), "fonts/roboto-condensed-bold.ttf");
		this.textView.setTypeface(tf);
		this.textView.setmDuration(3000);
		this.textView.setIsVisible(false);
		this.textView.toggle();
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