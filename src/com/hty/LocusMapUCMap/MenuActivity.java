package com.hty.LocusMapUCMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MenuActivity extends Activity {
	Button btn1, btn2, btn3, btn4, btn5, btn6, btn7;
	int i = 0;
	String upload_server = "";
	SharedPreferences sharedPreferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MainApplication.getInstance().addActivity(this);
		MainApplication.setrfn("");
		setContentView(R.layout.activity_menu);
		btn1 = (Button) findViewById(R.id.button_new);
		btn2 = (Button) findViewById(R.id.button_history);
		btn3 = (Button) findViewById(R.id.button_server);
		btn4 = (Button) findViewById(R.id.button_set);
		btn5 = (Button) findViewById(R.id.button_about);
		btn6 = (Button) findViewById(R.id.button_quit);
		btn7 = (Button) findViewById(R.id.button_nomap);
		btn1.setOnClickListener(new ButtonListener());
		btn2.setOnClickListener(new ButtonListener());
		btn3.setOnClickListener(new ButtonListener());
		btn4.setOnClickListener(new ButtonListener());
		btn5.setOnClickListener(new ButtonListener());
		btn6.setOnClickListener(new ButtonListener());
		btn7.setOnClickListener(new ButtonListener());
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		upload_server = sharedPreferences.getString("uploadServer", "http://sonichy.gearhostpreview.com/locusmap");
		// Log.e("upload_server", upload_server);
		// Toast.makeText(getApplicationContext(), "服务器：" + upload_server,
		// Toast.LENGTH_SHORT).show();
	}

	class ButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.button_new:
				startActivity(new Intent(MenuActivity.this, CurrentLocus.class));
				break;
			case R.id.button_history:
				startActivity(new Intent(MenuActivity.this, GPXList.class));
				break;
			case R.id.button_server:
				Intent intent = new Intent();
				intent.setData(Uri.parse(upload_server));
				intent.setAction(Intent.ACTION_VIEW);
				MenuActivity.this.startActivity(intent);
				break;
			case R.id.button_set:
				startActivity(new Intent(MenuActivity.this, Setting.class));
				break;
			case R.id.button_about:
				new AlertDialog.Builder(MenuActivity.this).setIcon(R.drawable.ic_launcher).setTitle("轨迹地图UCMap版  V1.0")
						.setMessage("利用UCMap提供的地图、定位、绘图和手机的GPS功能绘制、记录位移轨迹，查看记录的轨迹，上传GPS数据到服务器。\n作者：黄颖\nE-mail：sonichy@163.com\nQQ：84429027\n\n更新历史\n1.0 (2018-09-19)\n在地图上定位当前位置")
						.setPositiveButton("确定", null).show();
				break;
			case R.id.button_quit:
				MainApplication.getInstance().exit();
				break;
			case R.id.button_nomap:
				startActivity(new Intent(MenuActivity.this, NoMapActivity.class));
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "退出");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int item_id = item.getItemId();
		switch (item_id) {
		case 0:
			Log.e("MainApplication", "exit()");
			MainApplication.getInstance().exit();
			break;
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
}
