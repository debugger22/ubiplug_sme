package com.example.ubiplug_sme;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.loopj.android.http.*;

public class FeedbackActivity extends Activity{

	private Button cmdSubmit;
	private EditText txtDevName;
	private TextView lblLabel;
	private String curr_vals;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feedback_layout);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		curr_vals = (String)getIntent().getSerializableExtra("transient");
		Typeface tf = Typeface.createFromAsset(getApplicationContext().getAssets(),"fonts/roboto-regular.ttf");
		cmdSubmit = (Button)findViewById(R.id.cmdCorrectDeviceNameSubmit);
		txtDevName = (EditText)findViewById(R.id.txtCorrectDeviceName);
		cmdSubmit.setTypeface(tf);
		txtDevName.setTypeface(tf);
		lblLabel = (TextView)findViewById(R.id.lblPlsWiteCorrectAppName);
		lblLabel.setTypeface(tf);
		cmdSubmit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				String url = new String("http://www.ubiplug.com:8080/wrongdetection/");
				final int DEFAULT_TIMEOUT = 20 * 1000;
		    	RequestParams params = new RequestParams();
		    	params.put("curr_vals", curr_vals);
		    	final NotificationCompat.Builder mBuilder =
					    new NotificationCompat.Builder(FeedbackActivity.this)
					    .setSmallIcon(R.drawable.ic_launcher)
					    .setContentTitle("ubiplug")
					    .setContentText("Sending your response.\nWe use this data to improve user experience.");
				// Sets an ID for the notification
				final int mNotificationId = 002;
				// Gets an instance of the NotificationManager service
				final NotificationManager mNotifyMgr = 
				        (NotificationManager) FeedbackActivity.this
				        	.getSystemService(FeedbackActivity.this.getApplicationContext().NOTIFICATION_SERVICE);
				// Builds the notification and issues it.
				Uri uri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				mBuilder.setSound(uri);
				mBuilder.setTicker("Sending your response.");
				mBuilder.setOngoing(true);
				
				mNotifyMgr.notify(mNotificationId, mBuilder.build());
		    	try {
		        	AsyncHttpClient client = new AsyncHttpClient();
		        	client.setTimeout(DEFAULT_TIMEOUT);
		        	client.post(FeedbackActivity.this,url,params,new AsyncHttpResponseHandler() 
		        	{
		        	    @Override
		        	    public void onSuccess(String response) {
		        	    	mNotifyMgr.cancel(2);
		        	    	mBuilder.setContentText("Response sent succesfully.");
		        	    	mBuilder.setTicker("Response sent succesfully");
		        	    	mBuilder.setAutoCancel(true);
		        	    	mBuilder.setOngoing(false);
		        	    	mNotifyMgr.notify(3, mBuilder.build());
		        	    	finish();
		        	    }
		        	    
		        	    @SuppressLint("ResourceAsColor")
						@Override
		        	    public void onFailure(Throwable error, String response){
		        	    	mNotifyMgr.cancel(2);
		        	    }
		        	    
		        	});
		    	}
		    	catch(Exception e){
		    		Log.v("INTERNET", "Unable to connect"+e.toString());
		    		
		    	}
				
			}
			
		});
	}
	
	@Override
	protected void onStart(){
		super.onStart();
	}
	
	@Override
	protected void onStop(){
		super.onStart();
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
	}
}
