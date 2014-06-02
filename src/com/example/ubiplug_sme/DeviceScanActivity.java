package com.example.ubiplug_sme;

import java.util.ArrayList;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class DeviceScanActivity extends ListActivity{

	private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private Handler mHandler1;
	ArrayList<String> deviceArray=new ArrayList<String>();
	ArrayAdapter<String> listAdapter;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private void scanLeDevice(final boolean enable) {
		listAdapter=new ArrayAdapter<String>(this,
	            android.R.layout.simple_list_item_1,
	            deviceArray);
		setListAdapter(listAdapter);
		
    	if (enable){
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }
    
    
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			// TODO Auto-generated method stub
		    runOnUiThread(new Runnable() {
               @Override
               public void run(){
                   deviceArray.add(device.getName());
                   listAdapter.notifyDataSetChanged();
               }
           });
		}
		
    };
    
	
    
}
