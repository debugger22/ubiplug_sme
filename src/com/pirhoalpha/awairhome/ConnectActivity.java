package com.pirhoalpha.awairhome;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;

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
		mDevice = getIntent().getExtras().getParcelable("device");

		device_text = (TextView) findViewById(R.id.device_text);
		mTxtReceive = (TextView) findViewById(R.id.mTxtReceive);
		device_text.setText(mDevice.getName() + " " + mDevice.getAddress());
		Log.v("GOT Device", mDevice.getName());

		txtCO = (TextView) findViewById(R.id.txtCO);
		txtVOC = (TextView) findViewById(R.id.txtVOC);
		txtDust = (TextView) findViewById(R.id.txtDust);
		txtLight = (TextView) findViewById(R.id.txtLight);
		txtHumidity = (TextView) findViewById(R.id.txtHumidity);
		txtTemperature = (TextView) findViewById(R.id.txtTemperature);

	}

	@Override
	protected void onResume() {
		if (mBTSocket == null || !mIsBluetoothConnected) {
			new ConnectBT().execute();
		}
		super.onResume();
	}

	private class ReadInput implements Runnable {

		private boolean bStop = false;
		private final Thread t;

		public ReadInput() {
			t = new Thread(this, "Input Thread");
			t.start();
		}

		public boolean isRunning() {
			return t.isAlive();
		}

		@Override
		public void run() {
			InputStream inputStream;

			try {
				inputStream = mBTSocket.getInputStream();
				while (!bStop) {
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
							// JSONObject jsonObj = new JSONObject(strInput);
							JSONParser parser = new JSONParser();
							ContainerFactory containerFactory = new ContainerFactory() {
								@Override
								public List creatArrayContainer() {
									return new LinkedList();
								}

								@Override
								public Map createObjectContainer() {
									return new LinkedHashMap();
								}
							};
							@SuppressWarnings("unchecked")
							HashMap<String, String> data = (HashMap<String, String>) parser
									.parse(strInput, containerFactory);
							txtCO.setText(data.get("co"));
							txtVOC.setText(data.get("voc"));
							txtDust.setText(data.get("dust"));
							txtHumidity.setText(data.get("hum"));
							txtLight.setText(data.get("light"));
							txtTemperature.setText(data.get("temp"));

						} catch (Exception e) {
							// TODO Auto-generated catch block
							Log.v("Parse error", e.toString());
						}

						mTxtReceive.post(new Runnable() {
							@Override
							public void run() {
								mTxtReceive.append(strInput);
								// Uncomment below for testing
								// mTxtReceive.append("\n");
								// mTxtReceive.append("Chars: " +
								// strInput.length() + " Lines: " +
								// mTxtReceive.getLineCount() + "\n");

								int txtLength = mTxtReceive.getEditableText()
										.length();
								if (txtLength > mMaxChars) {
									mTxtReceive.getEditableText().delete(0,
											txtLength - mMaxChars);
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
			bStop = true;
		}

	}

	private class ConnectBT extends AsyncTask<Void, Void, Void> {
		private boolean mConnectSuccessful = true;

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(ConnectActivity.this,
					"Hold on", "Connecting");
		}

		@Override
		protected Void doInBackground(Void... devices) {

			try {
				if (mBTSocket == null || !mIsBluetoothConnected) {
					mBTSocket = mDevice
							.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
					BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
					mBTSocket.connect();
					progressDialog.dismiss();
					new ReadInput();
				}
			} catch (IOException e) {
				// Unable to connect to device
				e.printStackTrace();
				mConnectSuccessful = false;
			}
			return null;
		}
	}

}
