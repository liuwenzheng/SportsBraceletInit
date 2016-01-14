package com.sportsbracelet.init;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.sportsbracelet.init.BTService.LocalBinder;

public class InitActivity extends Activity implements OnClickListener,
		OnItemClickListener {
	private ListView lv_devices;
	private EditText et_sn;
	private TextView tv_sn, tv_device_size;
	private DeviceAdapter mAdapter;
	private ArrayList<Device> devices;
	private BTService mBtService;
	private ProgressDialog mDialog;
	private int mPosition = -1;
	private Device mSelectDevice = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.init);
		// 启动蓝牙服务
		// startService(new Intent(this, BTService.class));
		// 初始化蓝牙适配器
		BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext()
				.getSystemService(Context.BLUETOOTH_SERVICE);
		BTModule.mBluetoothAdapter = bluetoothManager.getAdapter();
		lv_devices = (ListView) findViewById(R.id.lv_devices);
		et_sn = (EditText) findViewById(R.id.et_sn);
		tv_sn = (TextView) findViewById(R.id.tv_sn);
		tv_device_size = (TextView) findViewById(R.id.tv_device_size);
		findViewById(R.id.btn_scan).setOnClickListener(this);
		findViewById(R.id.btn_sleep).setOnClickListener(this);
		findViewById(R.id.btn_clear).setOnClickListener(this);
		findViewById(R.id.btn_write_sn).setOnClickListener(this);
		findViewById(R.id.btn_read_sn).setOnClickListener(this);
		devices = new ArrayList<Device>();
		mAdapter = new DeviceAdapter(this, devices);
		lv_devices.setAdapter(mAdapter);
		lv_devices.setOnItemClickListener(this);
		tv_device_size.setText(getString(R.string.device_size, devices.size()));
		bindService(new Intent(this, BTService.class), mServiceConnection,
				BIND_AUTO_CREATE);
		isDeviceConnected();
	}

	private void isDeviceConnected() {
		if (mSelectDevice == null) {
			findViewById(R.id.btn_sleep).setEnabled(false);
			findViewById(R.id.btn_write_sn).setEnabled(false);
			findViewById(R.id.btn_read_sn).setEnabled(false);
		} else {
			findViewById(R.id.btn_sleep).setEnabled(true);
			findViewById(R.id.btn_write_sn).setEnabled(true);
			findViewById(R.id.btn_read_sn).setEnabled(true);
		}
	}

	@Override
	protected void onResume() {
		// 注册广播接收器
		IntentFilter filter = new IntentFilter();
		filter.addAction(BTConstants.ACTION_BLE_DEVICES_DATA);
		filter.addAction(BTConstants.ACTION_BLE_DEVICES_DATA_END);
		filter.addAction(BTConstants.ACTION_CONN_STATUS_TIMEOUT);
		filter.addAction(BTConstants.ACTION_CONN_STATUS_DISCONNECTED);
		filter.addAction(BTConstants.ACTION_DISCOVER_SUCCESS);
		filter.addAction(BTConstants.ACTION_DISCOVER_FAILURE);
		filter.addAction(BTConstants.ACTION_REFRESH_DATA);
		filter.addAction(BTConstants.ACTION_ACK);
		filter.addAction(BTConstants.ACTION_REFRESH_SN);
		registerReceiver(mReceiver, filter);
		super.onResume();
	}

	@Override
	protected void onPause() {
		// 注销广播接收器
		unregisterReceiver(mReceiver);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		mBtService.disConnectBle();
		unbindService(mServiceConnection);
		mBtService = null;
		// stopService(new Intent(this, BTService.class));
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_scan:
			// 开启蓝牙
			if (!BTModule.isBluetoothOpen()) {
				BTModule.openBluetooth(InitActivity.this);
				return;
			}
			devices.clear();
			mPosition = -1;
			mSelectDevice = null;
			LogModule.d("开始扫描...");
			mBtService.scanDevice();
			mDialog = ProgressDialog.show(InitActivity.this, null,
					getString(R.string.setting_device_search), false, false);
			isDeviceConnected();
			break;
		case R.id.btn_sleep:
			if (mSelectDevice != null) {
				mBtService.synSleep();
			}
			break;
		case R.id.btn_clear:
			devices.clear();
			mPosition = -1;
			mSelectDevice = null;
			tv_device_size.setText(getString(R.string.device_size,
					devices.size()));
			mAdapter.notifyDataSetChanged();
			isDeviceConnected();
			break;
		case R.id.btn_write_sn:
			if (TextUtils.isEmpty(et_sn.getText().toString())) {
				return;
			}
			mBtService.setSNData(et_sn.getText().toString());
			break;
		case R.id.btn_read_sn:
			if (mSelectDevice != null) {
				mBtService.getSNData();
			}
			break;

		default:
			break;
		}

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (mSelectDevice != null) {
			return;
		}
		LogModule.i("选中设备mac地址:" + devices.get(position).address);
		if (!devices.get(position).isConnected) {
			mPosition = position;
			mBtService.connectBle(devices.get(position).address);
			mDialog = ProgressDialog.show(InitActivity.this, null,
					getString(R.string.setting_device), false, false);
		}
	}

	/**
	 * 同步数据
	 */
	private void synData() {
		// 5.0偶尔会出现获取不到数据的情况，这时候延迟发送命令，解决问题
		BTService.mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				mBtService.synTimeData();
			}
		}, 200);
	}

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null) {
				if (BTConstants.ACTION_BLE_DEVICES_DATA.equals(intent
						.getAction())) {
					Device bleDevice = (Device) intent.getExtras()
							.getSerializable("device");
					for (Device device : devices) {
						if (device.address.equals(bleDevice.address)) {
							return;
						}
					}
					devices.add(bleDevice);
					mAdapter.setDevices(devices);
					tv_device_size.setText(getString(R.string.device_size,
							devices.size()));
					mAdapter.notifyDataSetChanged();
					if (devices.size() >= 30) {
						mBtService.stopLeScan();
					}
				}
				if (BTConstants.ACTION_BLE_DEVICES_DATA_END.equals(intent
						.getAction())) {
					if (mDialog != null) {
						mDialog.dismiss();
					}
				}
				if (BTConstants.ACTION_CONN_STATUS_TIMEOUT.equals(intent
						.getAction())
						|| BTConstants.ACTION_CONN_STATUS_DISCONNECTED
								.equals(intent.getAction())
						|| BTConstants.ACTION_DISCOVER_FAILURE.equals(intent
								.getAction())) {
					LogModule.d("配对失败...");
					ToastUtils.showToast(InitActivity.this,
							R.string.setting_device_conn_failure);
					if (mDialog != null) {
						mDialog.dismiss();
					}
				}
				if (BTConstants.ACTION_DISCOVER_SUCCESS.equals(intent
						.getAction())) {
					LogModule.d("配对成功...");
					ToastUtils.showToast(InitActivity.this,
							R.string.setting_device_conn_success);
					mSelectDevice = devices.get(mPosition);
					if (mSelectDevice != null) {
						mSelectDevice.isConnected = true;
						mAdapter.notifyDataSetChanged();
						synData();
					}
					if (mDialog != null) {
						mDialog.dismiss();
					}
					isDeviceConnected();
				}
				if (BTConstants.ACTION_ACK.equals(intent.getAction())) {
					int ack = intent.getIntExtra(
							BTConstants.EXTRA_KEY_ACK_VALUE, 0);
					if (ack == 0) {
						return;
					}
					if (ack == BTConstants.HEADER_SYNTIMEDATA) {
						ToastUtils.showToast(InitActivity.this,
								R.string.sync_time_success);
						mBtService.synTouchButton();
					} else if (ack == BTConstants.HEADER_SYNTOUCHBUTTON) {
						ToastUtils.showToast(InitActivity.this,
								R.string.sync_touch_success);
					} else if (ack == BTConstants.HEADER_SLEEP) {
						ToastUtils.showToast(InitActivity.this,
								R.string.sync_sleep_success);
						devices.remove(mSelectDevice);
						mBtService.disConnectBle();
						mPosition = -1;
						mSelectDevice = null;
						tv_device_size.setText(getString(R.string.device_size,
								devices.size()));
						mAdapter.notifyDataSetChanged();
						isDeviceConnected();
					} else if (ack == BTConstants.HEADER_SETSN) {
						ToastUtils.showToast(InitActivity.this,
								R.string.sync_sn_success);
					}
				}
				if (BTConstants.ACTION_REFRESH_SN.equals(intent.getAction())) {
					String sn = intent.getStringExtra(BTConstants.EXTRA_KEY_SN);
					tv_sn.setText(getString(R.string.read_sn_back, sn));
				}
			}

		}
	};
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			LogModule.d("连接服务onServiceConnected...");
			mBtService = ((LocalBinder) service).getService();
			if (mBtService.mBluetoothGatt != null) {
				mBtService.disConnectBle();
			}
			// 开启蓝牙
			if (!BTModule.isBluetoothOpen()) {
				BTModule.openBluetooth(InitActivity.this);
			} else {
				// LogModule.d("开始扫描..." + mScanTimes);
				// mBtService.scanDevice();
				// mDialog = ProgressDialog
				// .show(SettingDeviceActivity.this, null,
				// getString(R.string.setting_device_search),
				// false, false);
				// mScanTimes++;
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			LogModule.d("断开服务onServiceDisconnected...");
			mBtService = null;
		}
	};
}
