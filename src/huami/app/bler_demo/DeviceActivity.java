package huami.app.bler_demo;

import huami.dev.bler.core.IConnectionStateChangeCallback;
import huami.dev.bler.gatt.profile.UnlockProfile;
import huami.dev.util.Logdog;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public final class DeviceActivity extends Activity {

	private UnlockProfile m_Peripheral = null;
	private static final byte[] KEY = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }; // 应用自行生成，需妥善保管

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Logdog.ENABLE(); // 打印库调试信息
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device);
		final BluetoothDevice device = getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		m_Peripheral = new UnlockProfile(this, device, new IConnectionStateChangeCallback() {

			@Override
			public void onConnected(BluetoothDevice device) {
				m_Peripheral.discoverServices();
				m_Peripheral.init();
				toast("CONNECTED");
			}

			@Override
			public void onDisconnected(BluetoothDevice device) {
				toast("DISCONNECTED"); // 手环远离或信号不好等异常原因会导致连接断开，主动调用disconnect也会触发该回调，正式产品中请处理该事件
			}

			@Override
			public void onConnectionFailed(BluetoothDevice device) {
				toast("CONNECTION FAILED"); // 连接无法正常建立，通常由于手环不在附近，正式产品中请处理该事件
			}

		});
		m_Peripheral.connect(true); // 参数为true时库在异常断开后会自动重新建立连接，通常这是需要的
		findViewById(R.id.btn_authorize).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				final boolean ret = m_Peripheral.getUnlockService().authorize(KEY);
				toast(ret ? "授权成功" : "授权失败，请检查是否已连接，并确认在亮灯后敲击手环");
			}

		});
		findViewById(R.id.btn_authenticate).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				final boolean ret = m_Peripheral.getUnlockService().authenticate(KEY);
				toast(ret ? "认证成功" : "认证失败，可能原因：未连接或与appid对应的密钥错误");
			}

		});
		findViewById(R.id.btn_read_rssi).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				final int rssi = m_Peripheral.readRemoteRSSI();
				toast("RSSI = " + rssi);
			}

		});
	}

	@Override
	protected void onDestroy() {
		if (m_Peripheral != null) m_Peripheral.close();
		super.onDestroy();
	}

	private void toast(final String msg) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(DeviceActivity.this, msg, Toast.LENGTH_SHORT).show();
			}

		});
	}

}
