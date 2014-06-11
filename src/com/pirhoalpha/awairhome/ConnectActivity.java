package com.pirhoalpha.awairhome;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class ConnectActivity extends Activity {

	private BluetoothDevice mDevice;
	private BluetoothSocket mBTSocket;
	public UUID mDeviceUUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private final int mMaxChars = 50000; // Default

	private TextView device_text;
	private final ReadInput mReadThread = null;
	private TextView mTxtReceive;
	private final boolean mIsUserInitiatedDisconnect = false;
	private final boolean mIsBluetoothConnected = false;
	private ProgressDialog progressDialog;
	private TextView txtCO;
	private TextView txtVOC;
	private TextView txtDust;
	private TextView txtHumidity;
	private TextView txtLight;
	private TextView txtTemperature;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connect);
		this.mDevice = getIntent().getExtras().getParcelable("device");

		this.device_text = (TextView) findViewById(R.id.device_text);
		this.mTxtReceive = (TextView) findViewById(R.id.mTxtReceive);
		this.device_text.setText(this.mDevice.getName() + " "
				+ this.mDevice.getAddress());
		Log.v("GOT Device", this.mDevice.getName());

		this.txtCO = (TextView) findViewById(R.id.txtCO);
		this.txtVOC = (TextView) findViewById(R.id.txtVOC);
		this.txtDust = (TextView) findViewById(R.id.txtDust);
		this.txtLight = (TextView) findViewById(R.id.txtLight);
		this.txtHumidity = (TextView) findViewById(R.id.txtHumidity);
		this.txtTemperature = (TextView) findViewById(R.id.txtTemperature);

	}

	@Override
	protected void onResume() {
		if (this.mBTSocket == null || !this.mIsBluetoothConnected) {
			new ConnectBT().execute();
		}
		super.onResume();
	}

	private class ReadInput implements Runnable {

		private boolean bStop = false;
		private final Thread t;

		public ReadInput() {
			this.t = new Thread(this, "Input Thread");
			this.t.start();
		}

		public boolean isRunning() {
			return this.t.isAlive();
		}

		@Override
		public void run() {
			InputStream inputStream;

			try {
				inputStream = ConnectActivity.this.mBTSocket.getInputStream();
				while (!this.bStop) {
					byte[] buffer = new byte[256];
					if (inputStream.available() > 0) {
						inputStream.read(buffer);
						int i = 0;
						/*
						 * This is needed because new String(buffer) is taking
						 * the entire buffer i.e. 256 chars on Android 2.3.4
						 * http://stackoverflow.com/a/8843462/1287554
						 */
						for (i = 0; i < buffer.length && buffer[i] != 0; i++) {
						}
						final String strInput = new String(buffer, 0, i);

						try {
							JSONObject jsonObj = new JSONObject(strInput);
							ConnectActivity.this.txtCO.setText(jsonObj
									.getString("co"));
							ConnectActivity.this.txtVOC.setText(jsonObj
									.getString("voc"));
							ConnectActivity.this.txtDust.setText(jsonObj
									.getString("dust"));
							ConnectActivity.this.txtHumidity.setText(jsonObj
									.getString("hum"));
							ConnectActivity.this.txtLight.setText(jsonObj
									.getString("light"));
							ConnectActivity.this.txtTemperature.setText(jsonObj
									.getString("temp"));

						} catch (Exception e) {
							// TODO Auto-generated catch block
							Log.v("Parse error", e.toString());
						}

						ConnectActivity.this.mTxtReceive.post(new Runnable() {
							@Override
							public void run() {
								ConnectActivity.this.mTxtReceive
										.append(strInput);
								// Uncomment below for testing
								// mTxtReceive.append("\n");
								// mTxtReceive.append("Chars: " +
								// strInput.length() + " Lines: " +
								// mTxtReceive.getLineCount() + "\n");

								int txtLength = ConnectActivity.this.mTxtReceive
										.getEditableText().length();
								if (txtLength > ConnectActivity.this.mMaxChars) {
									ConnectActivity.this.mTxtReceive
											.getEditableText()
											.delete(0,
													txtLength
															- ConnectActivity.this.mMaxChars);
								}
							}
						});

					}
					Thread.sleep(500);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		public void stop() {
			this.bStop = true;
		}

	}

	private class ConnectBT extends AsyncTask<Void, Void, Void> {
		private boolean mConnectSuccessful = true;

		@Override
		protected void onPreExecute() {
			ConnectActivity.this.progressDialog = ProgressDialog.show(
					ConnectActivity.this, "Hold on", "Connecting");
		}

		@Override
		protected Void doInBackground(Void... devices) {

			try {
				if (ConnectActivity.this.mBTSocket == null
						|| !ConnectActivity.this.mIsBluetoothConnected) {
					ConnectActivity.this.mBTSocket = ConnectActivity.this.mDevice
							.createInsecureRfcommSocketToServiceRecord(ConnectActivity.this.mDeviceUUID);
					BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
					ConnectActivity.this.mBTSocket.connect();
					ConnectActivity.this.progressDialog.dismiss();
					new ReadInput();
				}
			} catch (IOException e) {
				// Unable to connect to device
				e.printStackTrace();
				this.mConnectSuccessful = false;
			}
			return null;
		}
	}

}
