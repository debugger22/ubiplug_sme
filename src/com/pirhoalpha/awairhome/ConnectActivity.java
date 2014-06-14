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
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.fima.cardsui.objects.CardStack;
import com.fima.cardsui.views.CardUI;

public class ConnectActivity extends Activity {

	private BluetoothDevice mDevice;
	private BluetoothSocket mBTSocket;
	public UUID mDeviceUUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private final int mMaxChars = 50000; // Default

	private final ReadInput mReadThread = null;
	private TextView mTxtReceive;
	private final boolean mIsUserInitiatedDisconnect = false;
	private final boolean mIsBluetoothConnected = false;
	private ProgressDialog progressDialog;

	private CardUI mCardView;
	private ImageCard co_card;
	private ImageCard voc_card;
	private ImageCard dust_card;
	private ImageCard light_card;
	private ImageCard hum_card;
	private ImageCard temp_card;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connect);
		mDevice = getIntent().getExtras().getParcelable("device");

		mTxtReceive = (TextView) findViewById(R.id.mTxtReceive);
		Log.v("GOT Device", mDevice.getName());

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// init CardView
		mCardView = (CardUI) findViewById(R.id.cardsview);
		mCardView.setSwipeable(false);
		CardStack stack2 = new CardStack();
		stack2.setTitle(mDevice.getName());
		mCardView.addStack(stack2);

		if (mBTSocket == null || !mIsBluetoothConnected) {
			new ConnectBT().execute();
		}

	}
	@Override
	protected void onResume() {
		super.onResume();
	}

	private class ReadInput implements Runnable {

		private boolean bStop = false;
		private final Thread t;
		private StringBuilder strBuilder;

		public ReadInput() {
			strBuilder = new StringBuilder();
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

						int sindex;
						int eindex;
						final String str = null;
						Byte backUp;
						/*
						 * This is needed because new String(buffer) is taking
						 * the entire buffer i.e. 256 chars on Android 2.3.4
						 * http://stackoverflow.com/a/8843462/1287554
						 */
						for (i = 0; i < buffer.length && buffer[i] != 0; i++) {

							backUp = new Byte(buffer[i]);

							// System.out.println(backUp.toString());
						}
						final String strInput = new String(buffer, 0, i);

						// System.out.println(strInput);
						strBuilder = strBuilder.append(strInput);

						String compStr = new String(strBuilder.toString());
						// System.out.println(compStr);

						if (compStr.contains("{") && compStr.contains("}")) {

							// System.out.println("complete" +compStr);
						}

						// Major change form HEre

						mTxtReceive.post(new Runnable() {
							@Override
							public void run() {
								progressDialog.dismiss();
								strBuilder = strBuilder.append(strInput);

								String compStr = new String(strBuilder
										.toString());

								if (compStr.contains("{")
										&& compStr.contains("}")) {

									// //mTxtReceive.append(compStr);
									// int txtLength =
									// mTxtReceive.getEditableText()
									// .length();
									// if (txtLength > mMaxChars) {
									// mTxtReceive.getEditableText().delete(0,
									// txtLength - mMaxChars);
									// }

									try {
										// JSONObject jsonObj = new
										// JSONObject(strInput);
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
												.parse(strInput,
														containerFactory);

										mCardView.clearCards();

										PlayCard co_play_card = new PlayCard(
												"CO", data.get("co"),
												"#FFC94E", "#AAAAAA", false,
												false);
										PlayCard voc_play_card = new PlayCard(
												"VOC", data.get("voc"),
												"#FFC94E", "#AAAAAA", false,
												false);
										PlayCard dust_play_card = new PlayCard(
												"DUST", data.get("dust"),
												"#FFC94E", "#AAAAAA", false,
												false);
										PlayCard light_play_card = new PlayCard(
												"LIGHT", data.get("light"),
												"#FFC94E", "#AAAAAA", false,
												false);
										PlayCard hum_play_card = new PlayCard(
												"HUMIDITY", data.get("hum"),
												"#FFC94E", "#AAAAAA", false,
												false);
										PlayCard temp_play_card = new PlayCard(
												"TEMPERATURE",
												data.get("temp"), "#FFC94E",
												"#AAAAAA", false, false);
										mCardView.addCard(co_play_card);
										mCardView.addCard(voc_play_card);
										mCardView.addCard(dust_play_card);
										mCardView.addCard(light_play_card);
										mCardView.addCard(hum_play_card);
										mCardView.addCard(temp_play_card);
										mCardView.refresh();

									} catch (Exception e) {
										// TODO Auto-generated catch block
										Log.v("Parse error", e.toString());
									}
								}

								// Uncomment below for testing
								// mTxtReceive.append("\n");
								// mTxtReceive.append("Chars: " +
								// strInput.length() + " Lines: " +
								// mTxtReceive.getLineCount() + "\n");

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
