package com.pirhoalpha.awairhome;

import it.gmariotti.cardslib.library.internal.Card;

import java.io.Serializable;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity implements Serializable {

	public BluetoothAdapter mBluetoothAdapter;
	public ArrayAdapter<String> mArrayAdapter;
	public ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
	public static ArrayList<Integer> dataValues;
	private ActionBar actionBar;
	private MenuItem mnuScan;
	private IntentFilter filter;
	private BroadcastReceiver mReceiver;
	private final int colorCounter = 0;
	Boolean sent = false;

	ArrayList<String> deviceNames = new ArrayList<String>();
	ArrayList<Card> cards = new ArrayList<Card>();
	ArrayAdapter<String> deviceArrayAdapter;
	ListView listView;

	ArrayList<ColorDrawable> bkgDrawables = new ArrayList<ColorDrawable>();

	private static final int REQUEST_ENABLE_BT = 1;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// getActionBar().setDisplayShowHomeEnabled(false);
		getActionBar().hide();

		deviceArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);

		// KitKat tint effect
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			Window w = getWindow(); // in Activity's onCreate() for instance
			w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
			w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

			int actualColor = getResources().getColor(
					android.R.color.holo_blue_dark);
		}
		try {
			BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
			mBluetoothAdapter = manager.getAdapter();
		} catch (NoClassDefFoundError e) {
			Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT)
					.show();
			finish();
		}

		Typeface tf = Typeface.createFromAsset(getApplicationContext()
				.getAssets(), "fonts/open-sans-regular.ttf");

		// UI elements
		listView = (ListView) findViewById(R.id.listView);
		listView.setAdapter(deviceArrayAdapter);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				mBluetoothAdapter.cancelDiscovery();
				Log.v("Device", deviceList.get(arg2).getName());
				Intent i = new Intent(MainActivity.this, ConnectActivity.class);
				i.putExtra("device", deviceList.get(arg2));
				startActivity(i);
			}

		});

		// Pick all background drawables in an ArrayList
		bkgDrawables.add(new ColorDrawable(getResources().getColor(
				R.color.gk_green)));
		bkgDrawables.add(new ColorDrawable(getResources().getColor(
				R.color.gk_blue)));
		bkgDrawables.add(new ColorDrawable(getResources().getColor(
				R.color.gk_orange)));
		bkgDrawables.add(new ColorDrawable(getResources().getColor(
				R.color.gk_yellow)));
		bkgDrawables.add(new ColorDrawable(getResources().getColor(
				R.color.gk_cyan)));
		bkgDrawables.add(new ColorDrawable(getResources().getColor(
				R.color.gk_red)));

		// Get handle of Bluetooth adapter
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();

		// Check if device supports Bluetooth
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Your device does not support Bluetooth.",
					Toast.LENGTH_SHORT);
			finish();
		}

		// Enable Bluetooth if disabled
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else {
			// Bluetooth was already enabled so start discovery
			this.mBluetoothAdapter.startDiscovery();
		}

		// Already paired devices
		// Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
		// .getBondedDevices();
		// If there are paired devices
		// if (pairedDevices.size() > 0) {
		// Loop through paired devices
		// for (BluetoothDevice device : pairedDevices) {
		// Add the name and address to an array adapter to show in a
		// ListView
		// MainActivity.this.devices.add(device.getAddress());

		// Create a Card
		// Card card = addStuff(device.getName(), device.getAddress());

		// MainActivity.this.mCardArrayAdapter.add(card);
		// MainActivity.this.mCardArrayAdapter.notifyDataSetChanged();
		// }
		// }
		// Create a BroadcastReceiver for ACTION_FOUND
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				// When discovery finds a device
				if (BluetoothDevice.ACTION_FOUND.equals(action)) {
					// Get the BluetoothDevice object from the Intent
					BluetoothDevice device = intent
							.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					deviceList.add(device);
					Log.v("Device",
							device.getName() + " " + device.getAddress());

					// Show only if it is not there
					if (!deviceNames.contains(device.getAddress())) {
						deviceNames.add(device.getAddress());

						deviceArrayAdapter.add(device.getAddress() + " "
								+ device.getName());
						deviceArrayAdapter.notifyDataSetChanged();
						if (!sent) {
							// ConnectThread connection = new
							// ConnectThread(device);
							// Log.v("Bluetooth", connection.toString());
						}
						sent = true;

					}

				}
				if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
					Log.v("Bluetooth", "Discovery finished");
				}
			}
		};

		// Register the BroadcastReceiver
		filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);

	}
	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();

	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	/**
	 * This method is called when a user terminates the app Here we are
	 * disconnecting from any active tag connection
	 */
	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(mReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		mnuScan = menu.findItem(R.id.mnuScan);
		mnuScan.setActionView(R.layout.refresh_menuitem);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_settings :
				Toast.makeText(this, "No settings available",
						Toast.LENGTH_SHORT).show();
				return true;
			case R.id.mnuScan :
				mnuScan = item;
				item.setActionView(R.layout.refresh_menuitem);
				return true;
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_OK) {
				// User enabled Bluetooth
				mBluetoothAdapter.startDiscovery();
			}
			if (resultCode == RESULT_CANCELED) {
				// User denied to enable Bluetooth
				Toast.makeText(this, "You can't proceed without Bluetooth.",
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}
}
