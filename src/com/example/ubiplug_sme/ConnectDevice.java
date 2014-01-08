package com.example.ubiplug_sme;

import java.util.ArrayList;


import java.util.UUID;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import ubiplug_sme.adapter.TabsPagerAdapter;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.view.View.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ActionBar;


public class ConnectDevice extends FragmentActivity implements ActionBar.TabListener {
	
	public BluetoothAdapter mBluetoothAdapter;
	public BluetoothGatt mConnectedGatt;
	public BluetoothGattService currentService;
	public BluetoothGattCharacteristic currentChar;
	public ArrayList<Integer> dataPoints;
	private TextView lblStatus;
	private BluetoothDevice device;
	private ArrayList<BluetoothDevice> deviceList;
	private int position;
    private ViewPager viewPager;
    private TabsPagerAdapter mAdapter;
    public String status;
    public TextView lblStatusAnalyzer;
    private AnalyzerFragment analyzer;
    private ActionBar actionBar;
    private MenuItem mnuData;
    private MenuItem mnuConnStatus;
    public TextView lblFirstState;
    private Fragment analyzerFragment;
    private Fragment detailsFragment;
    private Fragment livePlotFragment;
    
    
    // Tab titles
    private String[] tabs = { "Analysis", "Details", "Live Stream" };
	
	private static final UUID UBIPLUG_SERVICE = UUID.fromString("a6322521-eb79-4b9f-9152-19daa4870418");
	private static final UUID UBIPLUG_DATA_CHAR = UUID.fromString("f90ea017-f673-45b8-b00b-16a088a2ed61");
	private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connect_device);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		
		mBluetoothAdapter = manager.getAdapter();
		//lblStatus = (TextView)findViewById(R.id.lblStatusCD);
        Typeface tf = Typeface.createFromAsset(getApplicationContext().getAssets(),"fonts/roboto-regular.ttf");
        //lblStatus.setTypeface(tf);
		position = getIntent().getIntExtra("position", 0);
		deviceList = (ArrayList<BluetoothDevice>)getIntent().getSerializableExtra("deviceList");
		device = deviceList.get(position);
		
		
		
        viewPager = (ViewPager) findViewById(R.id.pager);
        actionBar = getActionBar();
        mAdapter = new TabsPagerAdapter(getSupportFragmentManager());
        
 
        viewPager.setAdapter(mAdapter);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS); 
        actionBar.setDisplayHomeAsUpEnabled(true);
 
        // Adding Tabs
        
        for (String tab_name : tabs) {
        	if (tab_name=="Details")continue;
            actionBar.addTab(actionBar.newTab().setText(tab_name).setTabListener(this));
        }
        
       
        
        dataPoints = new ArrayList<Integer>();
	}
    
	@Override
	protected void onStart() {
		super.onStart();
		viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			 
		    @Override
		    public void onPageSelected(int position) {
		        // on changing the page
		        // make respected tab selected
		        actionBar.setSelectedNavigationItem(position);
		        
		    }
		 
		    @Override
		    public void onPageScrolled(int arg0, float arg1, int arg2) {
		    }
		 
		    @Override
		    public void onPageScrollStateChanged(int arg0) {
		    }
		});
		
		//lblStatusAnalyzer = (TextView)findViewById(R.id.lblStatusAnalyzer);
		analyzer = (AnalyzerFragment)getSupportFragmentManager().findFragmentById(R.layout.fragment_analyzer);
		//dataPoints.add(3);
	}
	
//-*************************************************GATTCALLBACK******************
	
    private static final String TAG = "BluetoothGatt";

	private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        	
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            	if (newState == BluetoothProfile.STATE_CONNECTED) {
            		Log.i(TAG, "Connected to GATT server.");   
            		runOnUiThread(new Runnable() {
                        @Override
                        public void run(){
                        	mnuConnStatus.setTitle("Connected");
                        }
            		});
                    Log.i(TAG, "Discovering Services");
            		gatt.discoverServices();
            		Log.i(TAG, "Service discovery finished");

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from GATT server.");
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run(){
                        	mnuConnStatus.setTitle("Not Connected");
                        }
        		    });
                } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                    Log.i(TAG, "Disconnecting from GATT server.");
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run(){
                        	mnuConnStatus.setTitle("Not Connected");
                        }
        		    });
                }	
            
            }
        	

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                /*
                 * With services discovered, we are going to reset our state machine and start
                 * working through the sensors we need to enable
                 */
                if (status == BluetoothGatt.GATT_SUCCESS) {
                	currentService = gatt.getService(UBIPLUG_SERVICE);
                    currentChar = currentService.getCharacteristic(UBIPLUG_DATA_CHAR);
                	
                    Log.w(TAG, "service discovered-- " + status);
                    Log.w(TAG, "Current service " + currentService.toString());
                    Log.w(TAG, "Current characteristic " + currentChar.toString());


                    gatt.readCharacteristic(currentChar);
		    	    
                } else {
                    Log.w(TAG, "onServicesDiscovered received: " + status);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                //For each read, pass the data up to the UI thread to update the display
                Log.w(TAG, "Characteristic discovered: " + characteristic.getUuid().toString());
            	if (UBIPLUG_DATA_CHAR.equals(characteristic.getUuid())) {
                    mainHandler.sendMessage(Message.obtain(null, MSG_CURRENT, characteristic));
                }

                //After reading the initial value, next we enable notifications
            	gatt.setCharacteristicNotification(characteristic, true);
            	BluetoothGattDescriptor desc = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
	            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
	            gatt.writeDescriptor(desc);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                /*
                 * After notifications are enabled, all updates from the device on characteristic
                 * value changes will be posted here.  Similar to read, we hand these up to the
                 * UI thread to update the display.
                 */
                //Log.w(TAG, "Data Received");
                //mainHandler.sendMessage(Message.obtain(null, MSG_CURRENT, characteristic));
            	
            	if (UBIPLUG_DATA_CHAR.equals(characteristic.getUuid())) {
                    Log.v("Data", String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 1)*16));
            		mainHandler.sendMessage(Message.obtain(null, MSG_CURRENT, characteristic));
                }
            	gatt.readRemoteRssi();
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                //Once notifications are enabled, we move to the next sensor and start over with enable
            	Log.v(TAG,"Descriptor written");
            	if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Callback: Wrote GATT Descriptor successfully.");           
                }           
                else{
                    Log.d(TAG, "Callback: Error writing GATT Descriptor: "+ status);
                }
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, int status) {
            	runOnUiThread(new Runnable(){

					@Override
					public void run() {
						mnuData.setTitle("RSSI: "+String.valueOf(rssi));	
					}
               	});

            }

            private String connectionState(int status) {
                switch (status) {
                    case BluetoothProfile.STATE_CONNECTED:
                        return "Connected";
                    case BluetoothProfile.STATE_DISCONNECTED:
                        return "Disconnected";
                    case BluetoothProfile.STATE_CONNECTING:
                        return "Connecting";
                    case BluetoothProfile.STATE_DISCONNECTING:
                        return "Disconnecting";
                    default:
                        return String.valueOf(status);
                }
            }
 
    };

    

    
    
    BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
		    
		}
    };   
    
	private static final int MSG_CURRENT = 103;
    private static final int MSG_PROGRESS = 201;
    private static final int MSG_DISMISS = 202;
    private static final int MSG_CLEAR = 301;
    private static final int MSG_CONNECTED = 100;
    private Handler mainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            BluetoothGattCharacteristic characteristic;
            switch (msg.what) {
            	case MSG_CONNECTED:
            		break;
            	case MSG_CURRENT:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                    	Log.v(TAG, "NULL VALUE RECEIVED");
                    }else{
                    	
                    	if(dataPoints.size()<=1500){
                    		dataPoints.add(Integer.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 1)*16));
                    	}else{
                    		dataPoints.remove(0);
                    		dataPoints.add(Integer.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 1)*16));
                    	}
                    } 
                    break;
                case MSG_PROGRESS:
                    break;
                case MSG_DISMISS:
                    break;
                case MSG_CLEAR:
                    break;
            
            } 
        }
    };
    
    @Override
    protected void onStop() 
    {
        super.onStop();
        
        //finish();
        //return;
    }
	
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
        if(mBluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE==1){
        	mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        if(mConnectedGatt.STATE_CONNECTED==1)mConnectedGatt.disconnect();
        if(mConnectedGatt!=null) mConnectedGatt.close();
        finish();
        return;
    }	

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_connect_device, menu);
		mnuData = menu.findItem(R.id.mnuData);
		mnuConnStatus = menu.findItem(R.id.mnuConnStatus);
		mnuData.setEnabled(false);
		mConnectedGatt = device.connectGatt(getBaseContext(), true, mGattCallback);		//Connecting to the device
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.action_settings:
	            Toast.makeText(this, "No settings available", Toast.LENGTH_SHORT).show();
	            return true;
	        case R.id.action_about_cd:
	        	startActivity(new Intent(ConnectDevice.this, AboutActivity.class));
	       }
		return true;
	}
	
	

	
	@Override
    protected void onResume() {
		super.onResume();
	}
	
	

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {

	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		viewPager.setCurrentItem(tab.getPosition(), true);
		
		if (tab.getPosition()==0){
			actionBar.setIcon(R.drawable.chip);
			if(mConnectedGatt!=null){
				//TODO cmdAnalyzeEnabled
			}
		}
		//if(tab.getPosition()==1){
			//actionBar.setIcon(R.drawable.details_logo);

		//}
		if(tab.getPosition()==1){
			actionBar.setIcon(R.drawable.graph_logo);

		}
		
		
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
	
	}
	

	
}


