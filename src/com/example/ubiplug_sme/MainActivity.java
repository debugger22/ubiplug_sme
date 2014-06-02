package com.example.ubiplug_sme;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.os.Message;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class MainActivity extends Activity implements Serializable{
		
	
	public BluetoothAdapter mBluetoothAdapter;
	public BluetoothGatt mConnectedGatt;
	public BluetoothGattService currentService;
	public BluetoothGattCharacteristic currentChar;
	public ArrayAdapter<String> mArrayAdapter;
	public ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
	private ListView deviceListView;
	public static ArrayList<Integer> dataValues;
	private ActionBar actionBar;
	private MenuItem mnuScan;
	

	private TextView lblStatus;
	private TextView lblValue;
	private static final int REQUEST_ENABLE_BT = 1;
	private static final UUID UBIPLUG_SERVICE = UUID.fromString("a6322521-eb79-4b9f-9152-19daa4870418");
	private static final UUID UBIPLUG_DATA_CHAR = UUID.fromString("f90ea017-f673-45b8-b00b-16a088a2ed61");
	private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	private static final UUID SERVICE1 = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
	private static final UUID SERVICE2 = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		mBluetoothAdapter = manager.getAdapter();
		lblValue = (TextView)findViewById(R.id.lblValue);
		Typeface tf = Typeface.createFromAsset(getApplicationContext().getAssets(),"fonts/open-sans-regular.ttf");
		actionBar = getActionBar();
		actionBar.setIcon(R.drawable.home);
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) 
		{
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) 
		{
		    Toast.makeText(this, "BLE NOT SUPPORTED", Toast.LENGTH_SHORT).show();
		    finish();
		    return;
		}
		
        mArrayAdapter = new ArrayAdapter<String>(this, R.layout.custom_text_view);
        deviceListView = (ListView)findViewById(R.id.deviceListView);
		//deviceListView.
		deviceListView.setAdapter(mArrayAdapter);
		deviceListView.setTextFilterEnabled(true);
		ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
		lblStatus = (TextView)findViewById(R.id.lblStatus);

		deviceListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, final int position, long id) {
				final BluetoothDevice device = (BluetoothDevice)deviceList.get(position);
				runOnUiThread(new Runnable() {
                    @Override
                    public void run(){
                    	mnuScan.setActionView(null);
                    	Intent intent = new Intent(MainActivity.this, ConnectDevice.class);
                    	intent.putExtra("deviceList", deviceList);
                    	intent.putExtra("position", position);
                    	startActivity(intent);
                    	
                    }
                });
				
			}
	    });	
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		

		
	}
	
//-*************************************************GATTCALLBACK******************
	
    private static final String TAG = "BluetoothGatt";

	
//-*******************************************************************************
    BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
		    runOnUiThread(new Runnable() {
               @Override
               public void run(){
            	   
            	   if(mArrayAdapter.getPosition(device.getName()+"     MAC:  "+device.getAddress())==-1){
	                   mArrayAdapter.add(device.getName()+"     MAC:  "+device.getAddress());
	                   deviceList.add(device);
	                   mArrayAdapter.notifyDataSetChanged();
            	   }           	   
	               if(deviceList.size()>0){
	            	   mBluetoothAdapter.stopLeScan(mLeScanCallback);
	            	   mnuScan.setActionView(null);
	               }
               }
           });
		}
    };

	
	@Override
	protected void onResume() {
		super.onResume();
		mArrayAdapter.notifyDataSetChanged();
		
	}
	
    @Override
    protected void onPause() 
    {
        super.onPause();
        //Make sure dialog is hidden
        
    }
    
    /**
     * This method is called when a user terminates the app
     * Here we are disconnecting from any active tag connection
     */
    @Override
    protected void onStop() 
    {
        super.onStop();
        if(mBluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE==1){
        	mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        //finish();
        //return;
    }
	
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
        finish();
        return;
    }	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		mnuScan = menu.findItem(R.id.mnuScan);
		mArrayAdapter.clear();
    	deviceList.clear();
    	mArrayAdapter.notifyDataSetChanged();
       	mnuScan.setActionView(R.layout.refresh_menuitem);
    	mBluetoothAdapter.startLeScan(mLeScanCallback);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.action_settings:
	            Toast.makeText(this, "No settings available", Toast.LENGTH_SHORT).show();
	            return true;
	        case R.id.action_about:
	        	Intent intent = new Intent(MainActivity.this, AboutActivity.class);
	        	startActivity(intent);
	        case R.id.mnuScan:
	        	mnuScan = item;
	        	item.setActionView(R.layout.refresh_menuitem);
	        	
	        	//Toast.makeText(this, "Scanning started", Toast.LENGTH_SHORT).show();
	        	UUID[] serviceUUIDList;
	        	serviceUUIDList = new UUID[] {UBIPLUG_SERVICE, SERVICE1, SERVICE2};
	        	mArrayAdapter.clear();
	        	deviceList.clear();
	        	mArrayAdapter.notifyDataSetChanged();
	        	mBluetoothAdapter.startLeScan(mLeScanCallback);
	        	return true;
	       }
		return true;
	}
	
	@Override 
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if(requestCode == REQUEST_ENABLE_BT){
			if (resultCode == RESULT_OK){
				//Toast.makeText(this, "Bluetooth enabled successfully!", Toast.LENGTH_LONG).show();
				mBluetoothAdapter.startLeScan(mLeScanCallback);
			}
			if(resultCode == RESULT_CANCELED){
				Toast.makeText(this, "You can't proceed without Bluetooth.", Toast.LENGTH_LONG).show();
				finish();
				return;
			}
		}
	 }
 
}
