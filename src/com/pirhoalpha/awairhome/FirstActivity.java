package com.pirhoalpha.awairhome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import app.akexorcist.bluetoothspp.BluetoothSPP;
import app.akexorcist.bluetoothspp.BluetoothSPP.AutoConnectionListener;
import app.akexorcist.bluetoothspp.BluetoothSPP.BluetoothConnectionListener;
import app.akexorcist.bluetoothspp.BluetoothSPP.BluetoothStateListener;
import app.akexorcist.bluetoothspp.BluetoothSPP.OnDataReceivedListener;
import app.akexorcist.bluetoothspp.BluetoothState;
import app.akexorcist.bluetoothspp.DeviceList;

public class FirstActivity extends Activity {
	BluetoothSPP bt;
	TextView lblData;
	TextView lblMessage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.first_activity);
		this.lblData = (TextView) findViewById(R.id.lblData);
		this.lblData.setText("");
		this.lblMessage = (TextView) findViewById(R.id.lblMessage);
		this.lblMessage.setText("");

		this.bt = new BluetoothSPP(this);

		if (!this.bt.isBluetoothAvailable()) {
			Toast.makeText(getApplicationContext(),
					"Bluetooth is not available", Toast.LENGTH_SHORT).show();
			finish();
		}

		this.bt.setBluetoothStateListener(new BluetoothStateListener() {
			@Override
			public void onServiceStateChanged(int state) {
				if (state == BluetoothState.STATE_CONNECTED) {
					Log.i("Check", "State : Connected");
				} else if (state == BluetoothState.STATE_CONNECTING) {
					Log.i("Check", "State : Connecting");
				} else if (state == BluetoothState.STATE_LISTEN) {
					Log.i("Check", "State : Listen");
				} else if (state == BluetoothState.STATE_NONE) {
					Log.i("Check", "State : None");
				}
			}
		});

		this.bt.setOnDataReceivedListener(new OnDataReceivedListener() {
			@Override
			public void onDataReceived(byte[] data, String message) {
				Log.i("Check", "Message : " + message);
				FirstActivity.this.lblMessage.setText(message);
				FirstActivity.this.lblData.setText(String.valueOf(data));
			}
		});

		this.bt.setBluetoothConnectionListener(new BluetoothConnectionListener() {
			@Override
			public void onDeviceConnected(String name, String address) {
				Toast.makeText(FirstActivity.this, "Device Connected.",
						Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onDeviceDisconnected() {
				Toast.makeText(FirstActivity.this, "Device Disconnected.",
						Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onDeviceConnectionFailed() {
				Toast.makeText(FirstActivity.this, "Unable to Connect.",
						Toast.LENGTH_SHORT).show();
			}
		});

		this.bt.setAutoConnectionListener(new AutoConnectionListener() {
			@Override
			public void onNewConnection(String name, String address) {
				Log.i("Check", "New Connection - " + name + " - " + address);
			}

			@Override
			public void onAutoConnectionStarted() {
				Log.i("Check", "Auto connection started");
			}
		});

		Button btnConnect = (Button) findViewById(R.id.btnConnect);
		btnConnect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (FirstActivity.this.bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
					FirstActivity.this.bt.disconnect();
				} else {
					Intent intent = new Intent(FirstActivity.this,
							DeviceList.class);
					startActivityForResult(intent,
							BluetoothState.REQUEST_CONNECT_DEVICE);
				}
			}
		});
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.bt.stopService();
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!this.bt.isBluetoothEnabled()) {
			this.bt.enable();
		} else {
			if (!this.bt.isServiceAvailable()) {
				this.bt.setupService();
				this.bt.startService(BluetoothState.DEVICE_ANDROID);
				setup();
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
			if (resultCode == Activity.RESULT_OK) {
				this.bt.connect(data);
			}
		} else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
			if (resultCode == Activity.RESULT_OK) {
				this.bt.setupService();
			} else {
				Toast.makeText(getApplicationContext(),
						"Bluetooth was not enabled.", Toast.LENGTH_SHORT)
						.show();
				finish();
			}
		}
	}

	public void setup() {
	}
}