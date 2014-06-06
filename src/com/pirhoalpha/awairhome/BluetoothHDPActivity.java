package com.pirhoalpha.awairhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main user interface for the Sample application. All Bluetooth health-related
 * operations happen in {@link BluetoothHDPService}. This activity passes
 * messages to and from the service.
 */
public class BluetoothHDPActivity extends Activity {
	private static final String TAG = "BluetoothHealthActivity";

	// Use the appropriate IEEE 11073 data types based on the devices used.
	// Below are some examples. Refer to relevant Bluetooth HDP specifications
	// for detail.
	// 0x1007 - blood pressure meter
	// 0x1008 - body thermometer
	// 0x100F - body weight scale
	private static final int HEALTH_PROFILE_SOURCE_DATA_TYPE = 0x1007;

	private static final int REQUEST_ENABLE_BT = 1;

	private TextView mConnectIndicator;
	private ImageView mDataIndicator;
	private TextView mStatusMessage;

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice[] mAllBondedDevices;
	private BluetoothDevice mDevice;
	private int mDeviceIndex = 0;
	private Resources mRes;
	private Messenger mHealthService;
	private boolean mHealthServiceBound;

	// myturnnow
	private TextView mSys;
	private TextView mDia;
	private TextView mPul;

	// version number
	private TextView mVersion;
	// Handles events sent by {@link HealthHDPService}.
	private final Handler mIncomingHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			// Application registration complete.
				case BluetoothHDPService.STATUS_HEALTH_APP_REG :
					BluetoothHDPActivity.this.mStatusMessage.setText(String
							.format(BluetoothHDPActivity.this.mRes
									.getString(R.string.status_reg), msg.arg1));
					break;
				// Application unregistration complete.
				case BluetoothHDPService.STATUS_HEALTH_APP_UNREG :
					BluetoothHDPActivity.this.mStatusMessage
							.setText(String.format(
									BluetoothHDPActivity.this.mRes
											.getString(R.string.status_unreg),
									msg.arg1));
					break;
				// Reading data from HDP device.
				case BluetoothHDPService.STATUS_READ_DATA :
					BluetoothHDPActivity.this.mStatusMessage
							.setText(BluetoothHDPActivity.this.mRes
									.getString(R.string.read_data));
					BluetoothHDPActivity.this.mDataIndicator.setImageLevel(1);
					break;
				// Finish reading data from HDP device.
				case BluetoothHDPService.STATUS_READ_DATA_DONE :
					BluetoothHDPActivity.this.mStatusMessage
							.setText(BluetoothHDPActivity.this.mRes
									.getString(R.string.read_data_done));
					BluetoothHDPActivity.this.mDataIndicator.setImageLevel(0);
					break;
				// Channel creation complete. Some devices will automatically
				// establish
				// connection.
				case BluetoothHDPService.STATUS_CREATE_CHANNEL :
					Log.d(TAG, "STATUS_CREATE_CHANNEl enabled");
					BluetoothHDPActivity.this.mStatusMessage.setText(String
							.format(BluetoothHDPActivity.this.mRes
									.getString(R.string.status_create_channel),
									msg.arg1));
					BluetoothHDPActivity.this.mConnectIndicator
							.setText(R.string.connected);
					break;
				// Channel destroy complete. This happens when either the device
				// disconnects or
				// there is extended inactivity.
				case BluetoothHDPService.STATUS_DESTROY_CHANNEL :
					BluetoothHDPActivity.this.mStatusMessage
							.setText(String.format(
									BluetoothHDPActivity.this.mRes
											.getString(R.string.status_destroy_channel),
									msg.arg1));
					BluetoothHDPActivity.this.mConnectIndicator
							.setText(R.string.disconnected);
					break;
				case BluetoothHDPService.RECEIVED_SYS :
					int sys = msg.arg1;
					Log.i(TAG, "msg.arg1 @ sys is " + sys);
					BluetoothHDPActivity.this.mSys.setText("" + sys);
					break;
				case BluetoothHDPService.RECEIVED_DIA :
					int dia = msg.arg1;
					BluetoothHDPActivity.this.mDia.setText("" + dia);
					Log.i(TAG, "msg.arg1 @ dia is " + dia);
					break;
				case BluetoothHDPService.RECEIVED_PUL :
					int pul = msg.arg1;
					Log.i(TAG, "msg.arg1 @ pulse is " + pul);
					BluetoothHDPActivity.this.mPul.setText("" + pul);
					break;
				default :
					super.handleMessage(msg);
			}
		}
	};

	private final Messenger mMessenger = new Messenger(this.mIncomingHandler);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Check for Bluetooth availability on the Android platform.
		this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (this.mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.bluetooth_not_available,
					Toast.LENGTH_LONG);
			finish();
			return;
		}
		setContentView(R.layout.console);
		this.mConnectIndicator = (TextView) findViewById(R.id.connect_ind);
		this.mStatusMessage = (TextView) findViewById(R.id.status_msg);
		this.mDataIndicator = (ImageView) findViewById(R.id.data_ind);
		this.mRes = getResources();
		this.mHealthServiceBound = false;

		this.mSys = (TextView) findViewById(R.id.Systolic);
		this.mDia = (TextView) findViewById(R.id.Diastolic);
		this.mPul = (TextView) findViewById(R.id.Pulse);
		// mSys.setText("blah");
		try {
			String versionName = getPackageManager().getPackageInfo(
					getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Initiates application registration through {@link
		// BluetoothHDPService}.
		Button registerAppButton = (Button) findViewById(R.id.button_register_app);
		registerAppButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sendMessage(BluetoothHDPService.MSG_REG_HEALTH_APP,
						HEALTH_PROFILE_SOURCE_DATA_TYPE);
			}
		});

		// Initiates application unregistration through {@link
		// BluetoothHDPService}.
		Button unregisterAppButton = (Button) findViewById(R.id.button_unregister_app);
		unregisterAppButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sendMessage(BluetoothHDPService.MSG_UNREG_HEALTH_APP, 0);
			}
		});

		// Initiates channel creation through {@link BluetoothHDPService}. Some
		// devices will
		// initiate the channel connection, in which case, it is not necessary
		// to do this in the
		// application. When pressed, the user is asked to select from one of
		// the bonded devices
		// to connect to.
		// Button connectButton = (Button)
		// findViewById(R.id.button_connect_channel);
		// connectButton.setOnClickListener(new View.OnClickListener() {
		// public void onClick(View v) {
		// mAllBondedDevices =
		// (BluetoothDevice[]) mBluetoothAdapter.getBondedDevices().toArray(
		// new BluetoothDevice[0]);
		//
		// if (mAllBondedDevices.length > 0) {
		// int deviceCount = mAllBondedDevices.length;
		// if (mDeviceIndex < deviceCount) mDevice =
		// mAllBondedDevices[mDeviceIndex];
		// else {
		// mDeviceIndex = 0;
		// mDevice = mAllBondedDevices[0];
		// }
		// String[] deviceNames = new String[deviceCount];
		// int i = 0;
		// for (BluetoothDevice device : mAllBondedDevices) {
		// deviceNames[i++] = device.getName();
		// }
		// SelectDeviceDialogFragment deviceDialog =
		// SelectDeviceDialogFragment.newInstance(deviceNames, mDeviceIndex);
		// deviceDialog.show(getFragmentManager(), "deviceDialog");
		// }
		// }
		// });

		// Initiates channel disconnect through {@link BluetoothHDPService}.
		// Button disconnectButton = (Button)
		// findViewById(R.id.button_disconnect_channel);
		// disconnectButton.setOnClickListener(new View.OnClickListener() {
		// public void onClick(View v) {
		// disconnectChannel();
		// }
		// });
		// registerReceiver(mReceiver, initIntentFilter());
	}

	// Sets up communication with {@link BluetoothHDPService}.
	private final ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			BluetoothHDPActivity.this.mHealthServiceBound = true;
			Message msg = Message.obtain(null,
					BluetoothHDPService.MSG_REG_CLIENT);
			msg.replyTo = BluetoothHDPActivity.this.mMessenger;
			BluetoothHDPActivity.this.mHealthService = new Messenger(service);
			try {
				BluetoothHDPActivity.this.mHealthService.send(msg);
			} catch (RemoteException e) {
				Log.w(TAG, "Unable to register client to service.");
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			BluetoothHDPActivity.this.mHealthService = null;
			BluetoothHDPActivity.this.mHealthServiceBound = false;
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (this.mHealthServiceBound) {
			unbindService(this.mConnection);
		}
		unregisterReceiver(this.mReceiver);
	}

	@Override
	protected void onStart() {
		super.onStart();
		// If Bluetooth is not on, request that it be enabled.
		if (!this.mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			initialize();
		}
	}

	/**
	 * Ensures user has turned on Bluetooth on the Android device.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_ENABLE_BT :
				if (resultCode == Activity.RESULT_OK) {
					initialize();
				} else {
					finish();
					return;
				}
		}
	}

	/**
	 * Used by {@link SelectDeviceDialogFragment} to record the bonded Bluetooth
	 * device selected by the user.
	 * 
	 * @param position
	 *            Position of the bonded Bluetooth device in the array.
	 */
	public void setDevice(int position) {
		this.mDevice = this.mAllBondedDevices[position];
		this.mDeviceIndex = position;
	}

	private void connectChannel() {
		sendMessageWithDevice(BluetoothHDPService.MSG_CONNECT_CHANNEL);
	}

	private void disconnectChannel() {
		sendMessageWithDevice(BluetoothHDPService.MSG_DISCONNECT_CHANNEL);
	}

	private void initialize() {
		// Starts health service.
		Intent intent = new Intent(this, BluetoothHDPService.class);
		startService(intent);
		bindService(intent, this.mConnection, Context.BIND_AUTO_CREATE);
	}

	// Intent filter and broadcast receive to handle Bluetooth on event.
	private IntentFilter initIntentFilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		return filter;
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
						BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_ON) {
					initialize();
				}
			}
		}
	};

	// Sends a message to {@link BluetoothHDPService}.
	private void sendMessage(int what, int value) {
		if (this.mHealthService == null) {
			Log.d(TAG, "Health Service not connected.");
			return;
		}

		try {
			this.mHealthService.send(Message.obtain(null, what, value, 0));
		} catch (RemoteException e) {
			Log.w(TAG, "Unable to reach service.");
			e.printStackTrace();
		}
	}

	// Sends an update message, along with an HDP BluetoothDevice object, to
	// {@link BluetoothHDPService}. The BluetoothDevice object is needed by the
	// channel creation
	// method.
	private void sendMessageWithDevice(int what) {
		if (this.mHealthService == null) {
			Log.d(TAG, "Health Service not connected.");
			return;
		}

		try {
			this.mHealthService.send(Message.obtain(null, what, this.mDevice));
		} catch (RemoteException e) {
			Log.w(TAG, "Unable to reach service.");
			e.printStackTrace();
		}
	}

	/**
	 * Dialog to display a list of bonded Bluetooth devices for user to select
	 * from. This is needed only for channel connection initiated from the
	 * application.
	 */
	public static class SelectDeviceDialogFragment extends DialogFragment {

		public static SelectDeviceDialogFragment newInstance(String[] names,
				int position) {
			SelectDeviceDialogFragment frag = new SelectDeviceDialogFragment();
			Bundle args = new Bundle();
			args.putStringArray("names", names);
			args.putInt("position", position);
			frag.setArguments(args);
			return frag;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			String[] deviceNames = getArguments().getStringArray("names");
			int position = getArguments().getInt("position", -1);
			if (position == -1) {
				position = 0;
			}
			return new AlertDialog.Builder(getActivity())
					.setTitle(R.string.select_device)
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									((BluetoothHDPActivity) getActivity())
											.connectChannel();
								}
							})
					.setSingleChoiceItems(deviceNames, position,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									((BluetoothHDPActivity) getActivity())
											.setDevice(which);
								}
							}).create();
		}
	}
}