package com.pirhoalpha.awairhome;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.view.CardListView;
import it.gmariotti.cardslib.library.view.CardView;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
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
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

public class MainActivity extends Activity implements Serializable {

	public BluetoothAdapter mBluetoothAdapter;
	public ArrayAdapter<String> mArrayAdapter;
	public ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
	public static ArrayList<Integer> dataValues;
	private ActionBar actionBar;
	private MenuItem mnuScan;
	private IntentFilter filter;
	private BroadcastReceiver mReceiver;
	private int colorCounter = 0;
	Boolean sent = false;

	ArrayList<String> deviceNames = new ArrayList<String>();
	ArrayList<Card> cards = new ArrayList<Card>();
	CardArrayAdapter mCardArrayAdapter;
	CardListView listView;

	ArrayList<ColorDrawable> bkgDrawables = new ArrayList<ColorDrawable>();

	private static final int REQUEST_ENABLE_BT = 1;
	private static final UUID UBIPLUG_SERVICE = UUID
			.fromString("a6322521-eb79-4b9f-9152-19daa4870418");
	private static final UUID UBIPLUG_DATA_CHAR = UUID
			.fromString("f90ea017-f673-45b8-b00b-16a088a2ed61");
	private static final UUID CONFIG_DESCRIPTOR = UUID
			.fromString("00002902-0000-1000-8000-00805f9b34fb");
	private static final UUID SERVICE1 = UUID
			.fromString("0000180a-0000-1000-8000-00805f9b34fb");
	private static final UUID SERVICE2 = UUID
			.fromString("00001800-0000-1000-8000-00805f9b34fb");

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// getActionBar().setDisplayShowHomeEnabled(false);
		getActionBar().hide();
		// Flat UI
		// FlatUI.initDefaultValues(this);
		// FlatUI.setDefaultTheme(FlatUI.DEEP);

		// KitKat tint effect
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
		BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		this.mBluetoothAdapter = manager.getAdapter();
		Typeface tf = Typeface.createFromAsset(getApplicationContext()
				.getAssets(), "fonts/open-sans-regular.ttf");

		// UI elements
		this.mCardArrayAdapter = new CardArrayAdapter(this, this.cards);
		this.listView = (CardListView) findViewById(R.id.list_cards);
		this.listView.setAdapter(this.mCardArrayAdapter);
		this.listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				ConnectThread connection = new ConnectThread(
						MainActivity.this.deviceList.get(arg2));
				Log.v("Connection", connection.toString());

			}

		});

		// Pick all background drawables in an ArrayList
		this.bkgDrawables.add(new ColorDrawable(getResources().getColor(
				R.color.gk_green)));
		this.bkgDrawables.add(new ColorDrawable(getResources().getColor(
				R.color.gk_blue)));
		this.bkgDrawables.add(new ColorDrawable(getResources().getColor(
				R.color.gk_orange)));
		this.bkgDrawables.add(new ColorDrawable(getResources().getColor(
				R.color.gk_yellow)));
		this.bkgDrawables.add(new ColorDrawable(getResources().getColor(
				R.color.gk_cyan)));
		this.bkgDrawables.add(new ColorDrawable(getResources().getColor(
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
		this.mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				// When discovery finds a device
				if (BluetoothDevice.ACTION_FOUND.equals(action)) {
					// Get the BluetoothDevice object from the Intent
					BluetoothDevice device = intent
							.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					MainActivity.this.deviceList.add(device);
					Log.v("Device",
							device.getName() + " " + device.getAddress());

					// Show only if it is not there
					if (!MainActivity.this.deviceNames.contains(device
							.getAddress())) {
						MainActivity.this.deviceNames.add(device.getAddress());

						// Create a Card
						Card card = addStuff(device.getName(),
								device.getAddress());

						MainActivity.this.mCardArrayAdapter.add(card);
						MainActivity.this.mCardArrayAdapter
								.notifyDataSetChanged();
						if (!MainActivity.this.sent) {
							ConnectThread connection = new ConnectThread(device);
							Log.v("Bluetooth", connection.toString());
						}
						MainActivity.this.sent = true;

					}

				}
				if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
					Log.v("Bluetooth", "Discovery finished");
					MainActivity.this.mCardArrayAdapter.setEnableUndo(true);
				}
			}
		};

		// Register the BroadcastReceiver
		this.filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(this.mReceiver, this.filter);
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
		this.unregisterReceiver(this.mReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		this.mnuScan = menu.findItem(R.id.mnuScan);
		this.mnuScan.setActionView(R.layout.refresh_menuitem);
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
				this.mnuScan = item;
				item.setActionView(R.layout.refresh_menuitem);

				UUID[] serviceUUIDList;
				serviceUUIDList = new UUID[]{UBIPLUG_SERVICE, SERVICE1,
						SERVICE2};
				return true;
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_OK) {
				// User enabled Bluetooth
				this.mBluetoothAdapter.startDiscovery();
			}
			if (resultCode == RESULT_CANCELED) {
				// User denied to enable Bluetooth
				Toast.makeText(this, "You can't proceed without Bluetooth.",
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	private Card addStuff(String name, String mac) {
		Card card = new Card(this);
		card.setId(mac);

		// View header =
		// getLayoutInflater().inflate(R.layout.custom_card_layout,
		// null);
		// TextView txtName = (TextView) MainActivity.this
		// .findViewById(R.id.txt_name);
		// card.setInnerLayout(header);
		// CardHeader cardHeader = new CardHeader(this);
		// card.addCardHeader(cardHeader);

		card.setBackgroundResource(this.bkgDrawables.get(this.colorCounter % 6)); // (int)
		// (Math.random()
		// * (5 + 1))
		this.colorCounter++;
		card.setCardView((CardView) MainActivity.this.findViewById(R.id.cardid));
		card.setTitle("\n  " + name + "\n\n" + "  " + mac);
		card.setSwipeable(true);
		// MainActivity.this.mCardArrayAdapter.setEnableUndo(true);
		return card;
	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;

		public ConnectThread(BluetoothDevice device) {
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server
				// code
				tmp = device.createRfcommSocketToServiceRecord(UBIPLUG_SERVICE);
			} catch (IOException e) {
			}
			this.mmSocket = tmp;
		}

		@Override
		public void run() {
			// Cancel discovery because it will slow down the connection
			MainActivity.this.mBluetoothAdapter.cancelDiscovery();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				this.mmSocket.connect();
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						Log.v("Bluetooth", String
								.valueOf(ConnectThread.this.mmSocket
										.isConnected()));
					}

				});
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				Log.v("Bluetooth", connectException.toString());
				try {
					this.mmSocket.close();
				} catch (IOException closeException) {
					Log.v("Bluetooth", closeException.toString());
				}
				return;
			}

			// Do work to manage the connection (in a separate thread)
			// manageConnectedSocket(this.mmSocket);
		}
	}
}
