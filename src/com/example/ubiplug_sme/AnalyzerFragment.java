package com.example.ubiplug_sme;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;


import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.loopj.android.http.*;

public class AnalyzerFragment extends Fragment implements OnClickListener{
	
	public TextView lblStatus;
	private Button cmdAnalyze;
	private ConnectDevice act;
	private ArrayList<Integer> transientData;
	private ProgressDialog dialog;
	private Boolean activityCanceled;
	private Button cmdCorrect;
	private Button cmdIncorrect;
	private TextView lblIsCorrect;
	private String curr_vals;
	private TextView lblTransientPeak;
	private TextView lblSteadyCurrent;
	private TextView lblEfficiency;
	private TextView lblPowerConsumption;
	private Animation fadeIn;
	private Animation fadeOut;
	public final String VADC = "3";
	public final String BITS = "11";
	
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		transientData = new ArrayList<Integer>();
		activityCanceled = false;
		fadeIn = new AlphaAnimation(0, 1);
		fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
		fadeIn.setDuration(2000);
		
		fadeOut = new AlphaAnimation(1, 0);
		fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
		fadeOut.setStartOffset(500);
		fadeOut.setDuration(2000);
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
    	View rootView = (RelativeLayout)inflater.inflate(R.layout.fragment_analyzer, container, false); 
    	
    	return rootView;
    }
	
	@SuppressLint("ResourceAsColor")
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        act = (ConnectDevice)getActivity();
        dialog = new ProgressDialog(getActivity());
        Typeface tf = Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(),"fonts/roboto-regular.ttf");
        lblStatus = (TextView)getActivity().findViewById(R.id.lblStatusAnalyzer);
        lblIsCorrect = (TextView)getActivity().findViewById(R.id.lblWasCorrectInfo);
        lblTransientPeak = (TextView)getActivity().findViewById(R.id.lblTransientPeak);
        lblSteadyCurrent = (TextView)getActivity().findViewById(R.id.lblSteadyCurrent);
        lblPowerConsumption = (TextView)getActivity().findViewById(R.id.lblPowerConsumption);
        lblEfficiency = (TextView)getActivity().findViewById(R.id.lblEfficiency);
        
        lblPowerConsumption.setTypeface(tf);
        lblEfficiency.setTypeface(tf);
        
        //Handling cmdAnalyze
        cmdAnalyze = (Button)getActivity().findViewById(R.id.cmdAnalyze);
        cmdAnalyze.setTypeface(tf);
        cmdAnalyze.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
            	act.dataPoints.clear();
            	updateStatus("");
            	lblTransientPeak.setText("");
            	lblSteadyCurrent.setText("");
            	activityCanceled = false;
            	dialog.setMessage("Collecting data");
            	dialog.setCancelable(true);
            	dialog.setCanceledOnTouchOutside(false);
            	dialog.setTitle("Hang On");
            	dialog.setIcon(R.drawable.data);
            	dialog.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						activityCanceled = true;
						lblStatus.setTextColor(R.color.silver);
						//lblStatus.setTextSize(R.dimen.smallText);
						updateStatus("Analysis canceled");
						lblTransientPeak.setText("");
		            	lblSteadyCurrent.setText("");
					}       		
            	});
            	dialog.show();
            	final Handler handler = new Handler();
            	Runnable runnable = new Runnable(){
					@Override
					public void run() {
						Boolean done = false;
						if(activityCanceled)return;
						getActivity().runOnUiThread(new Runnable(){
							@Override
							public void run() {
								dialog.setMessage("Collecting data ("+String.valueOf(act.dataPoints.size()/10.0)+"%)");			
							}});
						  
						try{							
							if(act.dataPoints.size()>=1000){
								//for(int i=0;i<1000;i++){
									//transientData.add(act.dataPoints.get(i));
									transientData.addAll(0, act.dataPoints);
								//}
								classify(transientData);
								done=true;
							}
						}catch(Exception e){		
						}finally{
							if(!done)handler.postDelayed(this, 10);
						}
					}           		
            	};
            	handler.postDelayed(runnable, 10);
            }
        });
        
        //handling cmdCorrect
        cmdCorrect = (Button)getActivity().findViewById(R.id.cmdCorrect);
        cmdCorrect.setTypeface(tf);
        cmdCorrect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				
				NotificationCompat.Builder mBuilder =
					    new NotificationCompat.Builder(getActivity())
					    .setSmallIcon(R.drawable.ic_launcher)
					    .setContentTitle("ubiplug")
					    .setContentText("Thanks for your feedback.\nWe use your feedback to improve user experience.");
				// Sets an ID for the notification
				int mNotificationId = 001;
				// Gets an instance of the NotificationManager service
				NotificationManager mNotifyMgr = 
				        (NotificationManager) getActivity().getSystemService(getActivity().getApplicationContext().NOTIFICATION_SERVICE);
				// Builds the notification and issues it.
				Uri uri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				mBuilder.setSound(uri);
				mBuilder.setTicker("Thanks for your feedback.\n We use the data to improve user experience.");
				mNotifyMgr.notify(mNotificationId, mBuilder.build());
				//cmdCorrect.setVisibility(0);
				//cmdIncorrect.setVisibility(0);
				cmdCorrect.setAnimation(fadeOut);
				cmdIncorrect.setAnimation(fadeOut);
				cmdCorrect.setVisibility(View.GONE);
        	    cmdIncorrect.setVisibility(View.GONE);

			}
        	
        });
        
        //handling cmdIncorrect
        cmdIncorrect = (Button)getActivity().findViewById(R.id.cmdNotCorrect);
        cmdIncorrect.setTypeface(tf);
        cmdIncorrect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(),FeedbackActivity.class);
				intent.putExtra("transient", curr_vals);
				cmdCorrect.setAnimation(fadeOut);
        	    cmdCorrect.setVisibility(View.GONE);
        	    cmdIncorrect.setAnimation(fadeOut);
        	    cmdIncorrect.setVisibility(View.GONE);
				getActivity().startActivity(intent);
			}
        });
        
        lblStatus.setTypeface(tf);
	}

	@Override
	public void onStart(){
		super.onStart();
		 
	}
	
	
	public void updateStatus(String text){
		lblStatus.setText(text);
	}
	
	public void classify(ArrayList<Integer> transientData){
		curr_vals = String.valueOf(transientData.get(0));
		for(int i=1;i<transientData.size();i++){
			curr_vals += String.valueOf(" "+transientData.get(i));
		}
		
		String url = new String("http://www.ubiplug.com:8080/classify/");
		final int DEFAULT_TIMEOUT = 20 * 1000;
    	RequestParams params = new RequestParams();
    	params.put("curr_vals", curr_vals);
    	params.put("vadc", VADC);
    	params.put("bits", BITS);
    	dialog.setMessage("Uploading to ubiplug server");
    	dialog.setIcon(R.drawable.upload);
    	try {
        	AsyncHttpClient client = new AsyncHttpClient();
        	client.setTimeout(DEFAULT_TIMEOUT);
        	client.post(getActivity(),url,params,new AsyncHttpResponseHandler() 
        	{
        	    @Override
        	    public void onSuccess(String response) {
        	    	lblStatus.setTextColor(Color.GRAY);
        	    	//lblStatus.setTextSize(R.dimen.normalText);
        	    	String deviceName;
        	    	int health;
        	    	float peakCurrent;
        	    	JSONParser parser=new JSONParser();
        	    	ContainerFactory containerFactory = new ContainerFactory(){
					    public List creatArrayContainer() {
					      return new LinkedList();
					    }

					    public Map createObjectContainer() {
					      return new LinkedHashMap();
					    }                
				  };
        	    	try{
        	    		Map data = (Map)parser.parse(response, containerFactory);
        	    		
        	    		
        	    		updateStatus(String.valueOf((String)data.get("app_name")));
        	    		//updateStatus(response);
        	    		lblTransientPeak.setText("Peak Current: "+String.valueOf(Integer.valueOf((int) ((Double)data.get("peak_current")*1000)))+" mA");
        	    		lblSteadyCurrent.setText("Steady Current: "+String.valueOf(Integer.valueOf((int) ((Double)data.get("steady_current")*1000)))+" mA");
        	    		Double powerConsuming = 230.0*(Double)data.get("steady_current");
        	    		lblPowerConsumption.setText("Power Consuming: "+String.valueOf(Math.round(powerConsuming))+" W");
        	    		
        	    		if(powerConsuming<=(Long)data.get("power_rating")){
            	    		lblEfficiency.setText("Efficiency: "+String.valueOf(100)+" %");
            	    		lblEfficiency.setTextColor(Color.rgb(0, 100, 0));
            	    		
        	    		}else{
        	    			Double efficiency = 100 - (powerConsuming - (Long)data.get("power_rating"))/(Long)data.get("power_rating");
        	    			lblEfficiency.setTextColor(Color.RED);
        	    			lblEfficiency.setText(String.valueOf(efficiency)+" %");
        	    		}
        	    		
        	    	}catch(ParseException pe){
        	    		updateStatus(pe.toString());
        	    	}
	        	    
	        	    //lblIsCorrect.setVisibility(1);
	        	    //cmdCorrect.setVisibility(1);
	        	    //cmdIncorrect.setVisibility(1);
        	    	cmdCorrect.setAnimation(fadeIn);
            	    cmdCorrect.setVisibility(View.VISIBLE);
            	    cmdIncorrect.setAnimation(fadeIn);
            	    cmdIncorrect.setVisibility(View.VISIBLE);
        	    	
        	    	dialog.dismiss();
        	    }
        	    
        	    @SuppressLint("ResourceAsColor")
				@Override
        	    public void onFailure(Throwable error, String response){
        	    	//lblStatus.setTextSize(R.dimen.smallText);
        	    	try{
	        	    	if(response.intern()=="can't resolve host" || response.intern()=="socket time out"){
	        	    		updateStatus("Could not connect, please try again.");
	        	    	}
	        	    	updateStatus(response);	
        	    	}catch(NullPointerException e){
        	    		//updateStatus("Something went wrong.");
        	    		updateStatus(response);
        	    	}
        	    	lblStatus.setTextColor(R.color.silver);
	        	    //lblIsCorrect.setVisibility(1);
	        	    //cmdCorrect.setVisibility(1);
	        	    //cmdIncorrect.setVisibility(1);
	        	    dialog.dismiss();
        	    	
        	    }
        	    
        	});
    	}
    	catch(Exception e){
    		Log.v("INTERNET", "Unable to connect"+e.toString());
    		dialog.dismiss();
    		//status.setText(e.toString());
    	}
	}
	

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v.getId()==cmdAnalyze.getId()){
			updateStatus("csa");
		}
	}

}
