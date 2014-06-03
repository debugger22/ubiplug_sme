package com.pirhoalpha.awairhome;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHealth;
import android.bluetooth.BluetoothHealthAppConfiguration;
import android.bluetooth.BluetoothHealthCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.Settings.Secure;
import android.util.Log;
// for getting system-id

/**
 * This Service encapsulates Bluetooth Health API to establish, manage, and
 * disconnect communication between the Android device and a Bluetooth
 * HDP-enabled device. Possible HDP device type includes blood pressure monitor,
 * glucose meter, thermometer, etc.
 * 
 * As outlined in the <a href=
 * "http://developer.android.com/reference/android/bluetooth/BluetoothHealth.html"
 * >BluetoothHealth</a> documentation, the steps involve: 1. Get a reference to
 * the BluetoothHealth proxy object. 2. Create a BluetoothHealth callback and
 * register an application configuration that acts as a Health SINK. 3.
 * Establish connection to a health device. Some devices will initiate the
 * connection. It is unnecessary to carry out this step for those devices. 4.
 * When connected successfully, read / write to the health device using the file
 * descriptor. The received data needs to be interpreted using a health manager
 * which implements the IEEE 11073-xxxxx specifications. 5. When done, close the
 * health channel and unregister the application. The channel will also close
 * when there is extended inactivity.
 */
public class BluetoothHDPService extends Service {
	private static final String TAG = "bp";

	byte[] sysId;
	public static String result = "No measurement data to display now.";
	public static String[] results = new String[8];

	public static final int RESULT_OK = 0;
	public static final int RESULT_FAIL = -1;

	// Status codes sent back to the UI client.
	// Application registration complete.
	public static final int STATUS_HEALTH_APP_REG = 100;
	// Application unregistration complete.
	public static final int STATUS_HEALTH_APP_UNREG = 101;
	// Channel creation complete.
	public static final int STATUS_CREATE_CHANNEL = 102;
	// Channel destroy complete.
	public static final int STATUS_DESTROY_CHANNEL = 103;
	// Reading data from Bluetooth HDP device.
	public static final int STATUS_READ_DATA = 104;
	// Done with reading data.
	public static final int STATUS_READ_DATA_DONE = 105;

	// Message codes received from the UI client.
	// Register client with this service.
	public static final int MSG_REG_CLIENT = 200;
	// Unregister client from this service.
	public static final int MSG_UNREG_CLIENT = 201;
	// Register health application.
	public static final int MSG_REG_HEALTH_APP = 300;
	// Unregister health application.
	public static final int MSG_UNREG_HEALTH_APP = 301;
	// Connect channel.
	public static final int MSG_CONNECT_CHANNEL = 400;
	// Disconnect channel.
	public static final int MSG_DISCONNECT_CHANNEL = 401;
	// show measurement data result in UI
	public static final int SHOW_RESULT = 999;

	private BluetoothHealthAppConfiguration mHealthAppConfig;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothHealth mBluetoothHealth;
	private BluetoothDevice mDevice;
	private int mChannelId;

	private Messenger mClient;

	// Handles events sent by {@link HealthHDPActivity}.
	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			// Register UI client to this service so the client can receive
			// messages.
				case MSG_REG_CLIENT :
					Log.d(TAG, "Activity client registered");
					BluetoothHDPService.this.mClient = msg.replyTo;
					break;
				// Unregister UI client from this service.
				case MSG_UNREG_CLIENT :
					BluetoothHDPService.this.mClient = null;
					break;
				// Register health application.
				case MSG_REG_HEALTH_APP :
					Log.d(TAG, "Register health application!!!");
					registerApp(msg.arg1);
					break;
				// Unregister health application.
				case MSG_UNREG_HEALTH_APP :
					Log.d(TAG, "Unregister health application");
					unregisterApp();
					break;
				// Connect channel.
				case MSG_CONNECT_CHANNEL :
					BluetoothHDPService.this.mDevice = (BluetoothDevice) msg.obj;
					connectChannel();
					break;
				// Disconnect channel.
				case MSG_DISCONNECT_CHANNEL :
					BluetoothHDPService.this.mDevice = (BluetoothDevice) msg.obj;
					disconnectChannel();
					break;
				default :
					super.handleMessage(msg);
			}
		}
	}

	final Messenger mMessenger = new Messenger(new IncomingHandler());

	/**
	 * Make sure Bluetooth and health profile are available on the Android
	 * device. Stop service if they are not available.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (this.mBluetoothAdapter == null
				|| !this.mBluetoothAdapter.isEnabled()) {
			// Bluetooth adapter isn't available. The client of the service is
			// supposed to
			// verify that it is available and activate before invoking this
			// service.
			Log.d(TAG, "Device not available, adapter null.");
			stopSelf();
			return;
		}
		if (!this.mBluetoothAdapter.getProfileProxy(this,
				this.mBluetoothServiceListener, BluetoothProfile.HEALTH)) {
			// Toast.makeText(this,
			// R.string.bluetooth_health_profile_not_available,
			// Toast.LENGTH_LONG);
			stopSelf();
			return;
		}
		Log.d(TAG, "Bluetooth Profile (health) available");

		String android_id = Secure.getString(getBaseContext()
				.getContentResolver(), Secure.ANDROID_ID);
		Log.d(TAG, "@@@@@System-id = " + android_id);
		this.sysId = new byte[8];
		// this.convertSystemId(sysId, android_id);
		// String systemId2 = bytesToHex(sysId);
		// Log.d(TAG, "@@@@@System-id2 = "+systemId2);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "BluetoothHDPService is running.");
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return this.mMessenger.getBinder();
	};

	// Register health application through the Bluetooth Health API.
	private void registerApp(int dataType) {
		this.mBluetoothHealth.registerSinkAppConfiguration(TAG, dataType,
				this.mHealthCallback);
	}

	// Unregister health application through the Bluetooth Health API.
	private void unregisterApp() {
		this.mBluetoothHealth.unregisterAppConfiguration(this.mHealthAppConfig);
		// ######
		this.mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEALTH,
				this.mBluetoothHealth);
	}

	// Connect channel through the Bluetooth Health API.
	private void connectChannel() {
		Log.i(TAG, "connectChannel()");
		this.mBluetoothHealth.connectChannelToSource(this.mDevice,
				this.mHealthAppConfig);
	}

	// Disconnect channel through the Bluetooth Health API.
	private void disconnectChannel() {
		Log.i(TAG, "disconnectChannel()");
		this.mBluetoothHealth.disconnectChannel(this.mDevice,
				this.mHealthAppConfig, this.mChannelId);
	}

	// Callbacks to handle connection set up and disconnection clean up.
	private final BluetoothProfile.ServiceListener mBluetoothServiceListener = new BluetoothProfile.ServiceListener() {
		@Override
		public void onServiceConnected(int profile, BluetoothProfile proxy) {
			Log.d(TAG, "in onServiceConnected() of listener.");
			if (profile == BluetoothProfile.HEALTH) {
				BluetoothHDPService.this.mBluetoothHealth = (BluetoothHealth) proxy;
				if (Log.isLoggable(TAG, Log.INFO)) {
					Log.d(TAG, "onServiceConnected to profile: " + profile);
				}
			}
		}

		@Override
		public void onServiceDisconnected(int profile) {
			Log.d(TAG, "onServiceDisconnected to profile: " + profile);
			if (profile == BluetoothProfile.HEALTH) {
				BluetoothHDPService.this.mBluetoothHealth = null;
			}
		}
	};

	private final BluetoothHealthCallback mHealthCallback = new BluetoothHealthCallback() {
		// Callback to handle application registration and unregistration
		// events. The service
		// passes the status back to the UI client.
		@Override
		public void onHealthAppConfigurationStatusChange(
				BluetoothHealthAppConfiguration config, int status) {
			if (status == BluetoothHealth.APP_CONFIG_REGISTRATION_FAILURE) {
				BluetoothHDPService.this.mHealthAppConfig = null;
				sendMessage(STATUS_HEALTH_APP_REG, RESULT_FAIL);
			} else if (status == BluetoothHealth.APP_CONFIG_REGISTRATION_SUCCESS) {
				BluetoothHDPService.this.mHealthAppConfig = config;
				sendMessage(STATUS_HEALTH_APP_REG, RESULT_OK);
			} else if (status == BluetoothHealth.APP_CONFIG_UNREGISTRATION_FAILURE
					|| status == BluetoothHealth.APP_CONFIG_UNREGISTRATION_SUCCESS) {
				sendMessage(
						STATUS_HEALTH_APP_UNREG,
						status == BluetoothHealth.APP_CONFIG_UNREGISTRATION_SUCCESS
								? RESULT_OK
								: RESULT_FAIL);
			}
		}

		// Callback to handle channel connection state changes.
		// Note that the logic of the state machine may need to be modified
		// based on the HDP device.
		// When the HDP device is connected, the received file descriptor is
		// passed to the
		// ReadThread to read the content.
		@Override
		public void onHealthChannelStateChange(
				BluetoothHealthAppConfiguration config, BluetoothDevice device,
				int prevState, int newState, ParcelFileDescriptor fd,
				int channelId) {
			if (Log.isLoggable(TAG, Log.INFO)) {
				Log.d(TAG, String.format(
						"prevState\t%d ----------> newState\t%d", prevState,
						newState));
			}
			if (prevState == BluetoothHealth.STATE_CHANNEL_DISCONNECTED
					&& newState == BluetoothHealth.STATE_CHANNEL_CONNECTED) {
				Log.d(TAG, "+++++ state: disconnected -> connected.");
				if (config.equals(BluetoothHDPService.this.mHealthAppConfig)) {
					BluetoothHDPService.this.mChannelId = channelId;
					sendMessage(STATUS_CREATE_CHANNEL, RESULT_OK);
					new ReadThread(fd).start();
				} else {
					sendMessage(STATUS_CREATE_CHANNEL, RESULT_FAIL);
				}
			} else if (prevState == BluetoothHealth.STATE_CHANNEL_CONNECTING
					&& newState == BluetoothHealth.STATE_CHANNEL_CONNECTED) {
				Log.d(TAG, "+++++ state: connecting -> connected.");
				if (config.equals(BluetoothHDPService.this.mHealthAppConfig)) {
					BluetoothHDPService.this.mChannelId = channelId;
					sendMessage(STATUS_CREATE_CHANNEL, RESULT_OK);
					new ReadThread(fd).start();
				} else {
					sendMessage(STATUS_CREATE_CHANNEL, RESULT_FAIL);
				} // ############ modified case
			} else if (prevState == BluetoothHealth.STATE_CHANNEL_CONNECTING
					&& newState == BluetoothHealth.STATE_CHANNEL_DISCONNECTED) {
				Log.d(TAG, "+++++ state: connecting -> disconnected.");
				sendMessage(STATUS_CREATE_CHANNEL, RESULT_FAIL);
			} else if (prevState == BluetoothHealth.STATE_CHANNEL_DISCONNECTING
					&& newState == BluetoothHealth.STATE_CHANNEL_DISCONNECTED) {
				Log.d(TAG, "+++++ state: disconnecting -> disconnected.");
				stopSelf();// #######################
			} else if (newState == BluetoothHealth.STATE_CHANNEL_DISCONNECTED) {
				Log.d(TAG, "+++++ state: whatever -> disconnected."
						+ " whatever = " + prevState);
				if (config.equals(BluetoothHDPService.this.mHealthAppConfig)) {
					sendMessage(STATUS_DESTROY_CHANNEL, RESULT_OK);
				} else {
					sendMessage(STATUS_DESTROY_CHANNEL, RESULT_FAIL);
				}
			}
		}
	};

	// Sends an update message to registered UI client.
	private void sendMessage(int what, int value) {
		if (this.mClient == null) {
			Log.d(TAG, "No clients registered.");
			return;
		}

		try {
			this.mClient.send(Message.obtain(null, what, value, 0));
		} catch (RemoteException e) {
			// Unable to reach client.
			e.printStackTrace();
		}
	}

	int count; // #####
	byte[] invoke = new byte[2];
	public static final int RECEIVED_SYS = 901;
	public static final int RECEIVED_DIA = 902;
	public static final int RECEIVED_PUL = 903;

	// Thread to read incoming data received from the HDP device. This sample
	// application merely
	// reads the raw byte from the incoming file descriptor. The data should be
	// interpreted using
	// a health manager which implements the IEEE 11073-xxxxx specifications.
	private class ReadThread extends Thread {
		private final ParcelFileDescriptor mFd;

		public ReadThread(ParcelFileDescriptor fd) {
			super();
			this.mFd = fd;
		}

		@Override
		public void run() {
			FileInputStream fis = new FileInputStream(
					this.mFd.getFileDescriptor());
			final byte data[] = new byte[1000];
			try {
				while (fis.read(data) > -1) {
					// At this point, the application can pass the raw data to a
					// parser that
					// has implemented1 the IEEE 11073-xxxxx specifications.
					// Instead, this sample
					// simply indicates1 that some data has been received.
					/*
					 * Log.d(TAG, "addr: "+data.toString()); String str =
					 * bytesTo132Hex(data); Log.d(TAG, "Content: "+str);
					 */// ############

					if (data[0] != (byte) 0x00) {
						String test = byte2hex(data);
						Log.i(TAG, test);
						if (data[0] == (byte) 0xE2) {
							Log.i(TAG, "E2");
							BluetoothHDPService.this.count = 1;
							new WriteThread(this.mFd).start();
							try {
								sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							BluetoothHDPService.this.count = 2;
							new WriteThread(this.mFd).start();
						} else if (data[0] == (byte) 0xE7) {
							Log.i(TAG, "E7");

							if (data[18] == (byte) 0x0d
									&& data[19] == (byte) 0x1f) // fixed report
							{
								BluetoothHDPService.this.count = 3;
								// set invoke id so get correct response
								BluetoothHDPService.this.invoke = new byte[]{
										data[6], data[7]};
								// write back response
								new WriteThread(this.mFd).start();
								// parse data!!
								int systolic, diastolic, pulse;
								String year, month, day, hour, minute;
								byte byte2[] = new byte[2];
								byte byte1[] = new byte[1];
								systolic = data[45] >= 0
										? data[45]
										: 256 + data[45]; // in case overflow >
															// 127
								diastolic = data[47] >= 0
										? data[47]
										: 256 + data[47];
								pulse = data[63];
								byte2[0] = data[50];
								byte2[1] = data[51];
								year = bytesToHex(byte2);
								byte1[0] = data[52];
								month = bytesToHex(byte1);
								byte1[0] = data[53];
								day = bytesToHex(byte1);
								byte1[0] = data[54];
								hour = bytesToHex(byte1);
								byte1[0] = data[55];
								minute = bytesToHex(byte1);
								result = "*****the measured data are: Systolic = "
										+ systolic
										+ ", Diastolic = "
										+ diastolic
										+ ", pulse = "
										+ pulse
										+ " at "
										+ year
										+ "-"
										+ month
										+ "-"
										+ day
										+ " "
										+ hour
										+ ":"
										+ minute
										+ ". *****";
								results[0] = systolic + "";
								results[1] = diastolic + "";
								results[2] = pulse + "";
								results[3] = year;
								results[4] = month;
								results[5] = day;
								results[6] = hour;
								results[7] = minute;
								Log.d(TAG, result);
								sendMessage(SHOW_RESULT, 0);

							} else {
								for (int i = 0; i < data.length; i++) {
									data[i] = (byte) 0x00;
								}
								BluetoothHDPService.this.count = 2;
								fis.read(data);
								String test1 = byte2hex(data);
								Log.i(TAG, "ALL_ZERO:" + test1);

								try {
									sleep(300);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								/*
								 * //########################### count = 3;
								 * //set invoke id so get correct response
								 * invoke = new byte[] { data[6], data[7] };
								 * //write back response (new
								 * WriteThread(mFd)).start();
								 */// comment out on 20 March, early morning
							}
						} else if (data[0] == (byte) 0xE4) {
							BluetoothHDPService.this.count = 4;
							new WriteThread(this.mFd).start();
							// sendMessage();
						}
						// zero out the data
						for (int i = 0; i < data.length; i++) {
							data[i] = (byte) 0x00;
						}
					}

					sendMessage(STATUS_READ_DATA, 0);
				}

			} catch (IOException ioe) {
			}
			if (this.mFd != null) {
				try {
					this.mFd.close();
				} catch (IOException e) { /* Do nothing. */
				}
			}
			sendMessage(STATUS_READ_DATA_DONE, 0);

			// close stream
			try {
				fis.close();
				fis = null;
			} catch (IOException e) {
				Log.d(TAG,
						"unable to close file stream (fis): " + e.getMessage());
			}
		}

	}

	// method to convert byte array to hex string
	public static String bytesToHex(byte[] bytes) {
		final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
				'9', 'A', 'B', 'C', 'D', 'E', 'F'};
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	public static String bytesTo132Hex(byte[] bytes) {
		final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
				'9', 'A', 'B', 'C', 'D', 'E', 'F'};
		char[] hexChars = new char[66 * 2];
		int v;
		for (int j = 0; j < 65; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public String byte2hex(byte[] b) {
		// String Buffer can be used instead
		String hs = "";
		String stmp = "";

		for (int n = 0; n < b.length; n++) {
			stmp = java.lang.Integer.toHexString(b[n] & 0XFF);

			if (stmp.length() == 1) {
				hs = hs + "0" + stmp;
			} else {
				hs = hs + stmp;
			}

			if (n < b.length - 1) {
				hs = hs + "";
			}
		}

		return hs;
	}

	public static int byteToUnsignedInt(byte b) {
		return 0x00 << 24 | b & 0xff;
	}

	private class WriteThread extends Thread {
		private final ParcelFileDescriptor mFd;

		public WriteThread(ParcelFileDescriptor fd) {
			super();
			this.mFd = fd;
		}

		@Override
		public void run() {
			FileOutputStream fos = new FileOutputStream(
					this.mFd.getFileDescriptor());
			final byte data_AR[] = new byte[]{(byte) 0xE3, (byte) 0x00,
					(byte) 0x00, (byte) 0x2C, (byte) 0x00, (byte) 0x00,
					(byte) 0x50, (byte) 0x79, (byte) 0x00, (byte) 0x26,
					(byte) 0x80,
					(byte) 0x00,
					(byte) 0x00,
					(byte) 0x00,
					(byte) 0x80,
					(byte) 0x00,
					(byte) 0x80,
					(byte) 0x00,
					(byte) 0x00,
					(byte) 0x00,
					(byte) 0x00,
					(byte) 0x00,
					(byte) 0x00,
					(byte) 0x00,
					(byte) 0x80,
					(byte) 0x00,
					(byte) 0x00,
					(byte) 0x00,
					(byte) 0x00,
					(byte) 0x08, // bt add for phone, can be automate in the
									// future
					// sysId[0], sysId[1], sysId[2], sysId[3],
					// sysId[4], sysId[5], sysId[6], sysId[7],
					(byte) 0x01, (byte) 0x04, (byte) 0x01, (byte) 0x04,
					(byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x04,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00};
			final byte data_DR[] = new byte[]{(byte) 0xE7, (byte) 0x00,
					(byte) 0x00, (byte) 0x12, (byte) 0x00, (byte) 0x10,
					BluetoothHDPService.this.invoke[0],
					BluetoothHDPService.this.invoke[1], (byte) 0x02,
					(byte) 0x01, (byte) 0x00, (byte) 0x0A, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x0D, (byte) 0x1F, (byte) 0x00,
					(byte) 0x00};

			final byte get_MDS[] = new byte[]{(byte) 0xE7, (byte) 0x00,
					(byte) 0x00, (byte) 0x0E, (byte) 0x00,
					(byte) 0x0C,
					(byte) 0x4A,
					(byte) 0x05,// #####
					(byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x06,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00};

			final byte data_RR[] = new byte[]{(byte) 0xE5, (byte) 0x00,
					(byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00};

			final byte data_RRQ[] = new byte[]{(byte) 0xE4, (byte) 0x00,
					(byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00};

			final byte data_ABORT[] = new byte[]{(byte) 0xE6, (byte) 0x00,
					(byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00};
			try {
				Log.i(TAG, String.valueOf(BluetoothHDPService.this.count));
				if (BluetoothHDPService.this.count == 1) {
					fos.write(data_AR);
					Log.i(TAG, "Association Responsed!");
				} else if (BluetoothHDPService.this.count == 2) {
					fos.write(get_MDS);
					Log.i(TAG, "Get MDS object attributes!");
					// fos.write(data_ABORT);
				} else if (BluetoothHDPService.this.count == 3) {
					fos.write(data_DR);
					Log.i(TAG, "Data Responsed!");
				} else if (BluetoothHDPService.this.count == 4) {
					fos.write(data_RR);
					Log.i(TAG, "Association Released!");
				}
			} catch (IOException ioe) {
			}

			try {
				fos.close();
				fos = null;
			} catch (IOException e) {
				Log.d(TAG,
						"unable to close file stream (fos): " + e.getMessage());
			}
		}
	}

	// utitlity method: convert system-id to byte array of 8
	void convertSystemId(byte[] systemId, String sId) {
		for (int i = 0; i < 8; i++) {
			systemId[i] = (byte) Integer.parseInt(
					sId.substring(2 * i, 2 * i + 2), 16);
		}
	}
}
