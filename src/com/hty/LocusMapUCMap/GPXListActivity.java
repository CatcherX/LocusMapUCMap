package com.hty.LocusMapUCMap;

import java.io.File;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class GPXListActivity extends ListActivity {
	ListView listView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MainApplication.getInstance().addActivity(this);
		this.setTitle("轨迹列表");
		getList();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		String title = ((TextView) info.targetView.findViewById(android.R.id.text1)).getText().toString();
		menu.setHeaderTitle(title);
		menu.add(0, 0, 0, "分享");
		menu.add(0, 1, 1, "删除");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case 0:
			String filename = listView.getItemAtPosition(info.position).toString();
			if (filename.toLowerCase().endsWith(".gpx")) {
				String filepath = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/" + filename;
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_SEND);
				intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filepath)));
				intent.setType("*/*");
				startActivity(Intent.createChooser(intent, "分享 " + filename));
			}
			break;
		case 1:
			final String file = listView.getItemAtPosition(info.position).toString();
			if (file.toLowerCase().endsWith(".gpx")) {
				new AlertDialog.Builder(GPXListActivity.this).setIcon(R.drawable.warn).setTitle("删除操作").setMessage("此步骤不可还原，确定删除\n" + file + " ？")
						.setPositiveButton("是", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								String ttext = null;
								if (!file.equals(MainApplication.getrfn())) {
									RWXML.del(file);
									Intent intent = getIntent();
									// intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
									finish();
									overridePendingTransition(0, 0);
									startActivity(intent);
									ttext = file + " 已删除";
								} else {
									ttext = file + " 正在写入数据，禁止删除！";
								}
								Toast.makeText(getApplicationContext(), ttext, Toast.LENGTH_SHORT).show();
							}
						}).setNegativeButton("否", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
							}
						}).show();
			}
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			startActivity(new Intent(GPXListActivity.this, MenuActivity.class));
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getList();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	void getList() {
		String[] s = RWXML.gpxlist();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, s);
		setListAdapter(adapter);
		listView = getListView();
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				String filename = listView.getItemAtPosition(arg2).toString();
				// Log.e("filename", filename);
				// Log.e("MainApplication.getrfn()", MainApplication.getrfn());
				if (filename.toLowerCase().endsWith(".gpx")) {
					if (RWXML.read(filename) != null) {
						Intent intent = new Intent(GPXListActivity.this, HistoryLocus.class);
						intent.putExtra("filename", filename);
						MainApplication.setfn(filename);
						startActivity(intent);
					} else {
						String ttext = null;
						if (!filename.equals(MainApplication.getrfn())) {
							RWXML.del(filename);
							ttext = filename + "是空文档，自动删除！";
							Intent intent = getIntent();
							finish();
							overridePendingTransition(0, 0);
							startActivity(intent);
						} else {
							ttext = filename + "刚创建，即将写入数据！";
						}
						Toast.makeText(getApplicationContext(), ttext, Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
		registerForContextMenu(getListView());
	}
}
