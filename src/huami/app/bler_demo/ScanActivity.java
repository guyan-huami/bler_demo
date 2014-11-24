package huami.app.bler_demo;

import huami.dev.bler.core.IDeviceFoundCallback;
import huami.dev.bler.core.ScanCallback;
import huami.dev.bler.gatt.service.IAuthService;

import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public final class ScanActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scan);
		final ListView lv = (ListView) findViewById(R.id.lv_devices);
		final ArrayAdapter<BluetoothDevice> adapter = new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final BluetoothDevice device = adapter.getItem(position);
				final Intent intent = new Intent(ScanActivity.this, DeviceActivity.class);
				intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
				startActivity(intent);
			}

		});
		// filter用来对发现的设备进行过滤
		final ScanCallback.DeviceFilter filter = new ScanCallback.DeviceFilter();
		filter.setService(IAuthService.UUID_SERVICE);
		// 第一个参数设置为null则默认报告所有BLE设备
		final ScanCallback scan = new ScanCallback(new ScanCallback.DeviceFilter[] { filter }, new IDeviceFoundCallback() {

			@Override
			public void onDeviceFound(final BluetoothDevice device, int rssi, byte[] scanRecord) {
				// rssi可以用来粗略的判断设备的远近，例如rssi>-40可以视为极近，rssi>-60可以视为较近，rssi<-80可以视为较远。
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						adapter.add(device);
						adapter.notifyDataSetChanged();
					}

				});
			}

		});
		findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				adapter.clear();
				adapter.notifyDataSetChanged();
				// 参数为扫描时间，单位毫秒
				scan.startScan(5000);
			}

		});
		findViewById(R.id.btn_get_connected_devices).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// 查询已经连接的设备。应用在启动时应该优先查找系统已经连接着的设备，这些设备通常不会出现在scan返回的设备中。
				final List<BluetoothDevice> connectedDevices = ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getConnectedDevices(BluetoothProfile.GATT);
				adapter.clear();
				if (!connectedDevices.isEmpty()) adapter.addAll(connectedDevices);
				adapter.notifyDataSetChanged();
			}

		});
	}

}
