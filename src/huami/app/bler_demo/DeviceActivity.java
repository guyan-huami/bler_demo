package huami.app.bler_demo;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import dev.bler.core.IConnectionStateChangeCallback;
import dev.bler.gatt.profile.ImmediateAlertProfile;

public final class DeviceActivity extends Activity {

	private ImmediateAlertProfile m_Peripheral = null;
	private static final int APPID = 1000;
	private static final byte[] KEY = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device);
		findViewById(R.id.btn_authorize).setEnabled(false);
		findViewById(R.id.btn_authenticate).setEnabled(false);
		findViewById(R.id.btn_verify).setEnabled(false);
		final BluetoothDevice device = getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		m_Peripheral = new ImmediateAlertProfile(this, device, new IConnectionStateChangeCallback() {

			@Override
			public void onConnected(BluetoothDevice device) {
				m_Peripheral.discoverServices();
				m_Peripheral.init();
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(DeviceActivity.this, "CONNECTED", Toast.LENGTH_SHORT).show();
						findViewById(R.id.btn_authorize).setEnabled(true);
						findViewById(R.id.btn_authenticate).setEnabled(true);
						findViewById(R.id.btn_verify).setEnabled(true);
					}

				});
			}

			@Override
			public void onDisconnected(BluetoothDevice device) {}

			@Override
			public void onConnectionFailed(BluetoothDevice device) {}

		});
		m_Peripheral.connect(true);
		findViewById(R.id.btn_authorize).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				final boolean ret = m_Peripheral.getAuthService().authroize(APPID, KEY);
				Toast.makeText(DeviceActivity.this, "ret: " + ret, Toast.LENGTH_SHORT).show();
			}

		});
		findViewById(R.id.btn_authenticate).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				final boolean ret = m_Peripheral.getAuthService().authenticate(APPID, KEY);
				Toast.makeText(DeviceActivity.this, "ret: " + ret, Toast.LENGTH_SHORT).show();
			}

		});
		findViewById(R.id.btn_verify).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				final boolean ret = m_Peripheral.getAuthService().verify(APPID);
				Toast.makeText(DeviceActivity.this, "ret: " + ret, Toast.LENGTH_SHORT).show();
			}

		});
	}

	@Override
	protected void onDestroy() {
		if (m_Peripheral != null) m_Peripheral.close();
		super.onDestroy();
	}

}
