// https://blog.csdn.net/jason0539/article/details/12047963
package com.hty.LocusMapUCMap;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class NoMapActivity extends Activity {
	boolean isFirstLoc = true;
	TextView textView_location, textView_timer, textView_gpsStatus, textView_upload;
	double lc = 0, lc0 = 0, speed = 0, speedmax = 0, pi = 3.14, ltt, lgt, alt, ltt1, lgt1, distance = 0, distancesend = 0, lc1 = 0, distance1, speed1,
			speedmax1 = 0;
	float direction;
	Date date, time_start, datel;
	long duration;
	SimpleDateFormat timeformat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
	SimpleDateFormat timeformatd = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
	SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
	SimpleDateFormat dateformat1 = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
	SimpleDateFormat dateformat2 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
	String fp, fn, RC, sduration;
	int ER, d = 0, lci, c = 0;
	private LocationManager LM;
	SharedPreferences sharedPreferences;
	String uploadServer = "", filename_gpx = "";
	GeometryFactory GF = new GeometryFactory();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nomap);
		MainApplication.getInstance().addActivity(this);
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		uploadServer = sharedPreferences.getString("uploadServer", "http://sonichy.gearhostpreview.com/locusmap");
		timeformat.setTimeZone(TimeZone.getDefault());
		timeformatd.setTimeZone(TimeZone.getTimeZone("GMT+0"));
		textView_location = (TextView) findViewById(R.id.textView_location);
		textView_gpsStatus = (TextView) findViewById(R.id.textView_gpsStatus);
		textView_upload = (TextView) findViewById(R.id.textView_upload);
		textView_timer = (TextView) findViewById(R.id.textView2);
		time_start = new Date();
		filename_gpx = dateformat1.format(time_start) + "UC.gpx";
		MainApplication.setrfn(filename_gpx);
		RWXML.create(dateformat1.format(time_start));

		LM = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (!LM.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Toast.makeText(this, "请开启GPS导航", Toast.LENGTH_SHORT).show();
			// 打开GPS设置界面
			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivityForResult(intent, 0);
			// return;
		}
		// 为获取地理位置信息时设置查询条件
		String bestProvider = LM.getBestProvider(getCriteria(), true);
		// 获取位置信息，如果不设置查询要求，getLastKnownLocation方法传入的参数为LocationManager.GPS_PROVIDER
		Location location = LM.getLastKnownLocation(bestProvider);
		updateView(location);
		// 监听状态
		LM.addGpsStatusListener(listener);
		// 绑定监听，有4个参数
		// 参数1，设备：有GPS_PROVIDER和NETWORK_PROVIDER两种
		// 参数2，位置信息更新周期，单位毫秒
		// 参数3，位置变化最小距离：当位置距离变化超过此值时，将更新位置信息
		// 参数4，监听
		// 备注：参数2和3，如果参数3不为0，则以参数3为准；参数3为0，则通过时间来定时更新；两者为0，则随时刷新
		// 1秒更新一次，或最小位移变化超过1米更新一次；
		// 注意：此处更新准确度非常低，推荐在service里面启动一个Thread，在run中sleep(10000);然后执行handler.sendMessage(),更新位置
		LM.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
	}

	// 位置监听
	private final LocationListener locationListener = new LocationListener() {

		// 位置信息变化时触发
		@Override
		public void onLocationChanged(Location location) {
			updateView(location);
		}

		// GPS状态变化时触发
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			switch (status) {
			case LocationProvider.AVAILABLE:
				textView_gpsStatus.setText("GPS找到");
				// Log.e("GPS", "GPS找到");
				break;
			case LocationProvider.OUT_OF_SERVICE:
				textView_gpsStatus.setText("GPS服务区外");
				// Log.e("GPS", "GPS服务区外");
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				textView_gpsStatus.setText("GPS暂时找不到");
				// Log.e("GPS", "GPS暂时找不到");
				break;
			}
		}

		// GPS开启时触发
		@Override
		public void onProviderEnabled(String provider) {
			Location location = LM.getLastKnownLocation(provider);
			updateView(location);
		}

		// GPS关闭时触发
		@Override
		public void onProviderDisabled(String provider) {
			updateView(null);
		}
	};

	// 状态监听
	GpsStatus.Listener listener = new GpsStatus.Listener() {
		@Override
		public void onGpsStatusChanged(int event) {
			switch (event) {
			case GpsStatus.GPS_EVENT_FIRST_FIX:
				textView_gpsStatus.setText("第一次定位");
				// Log.e("GpsStatus", "第一次定位");
				break;
			case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
				// Log.e("GpsStatus", "卫星状态改变");
				// 获取当前状态
				GpsStatus gpsStatus = LM.getGpsStatus(null);
				// 获取卫星颗数的默认最大值
				int maxSatellites = gpsStatus.getMaxSatellites();
				// 创建一个迭代器保存所有卫星
				Iterator<GpsSatellite> iters = gpsStatus.getSatellites().iterator();
				int count = 0;
				while (iters.hasNext() && count <= maxSatellites) {
					GpsSatellite s = iters.next();
					count++;
				}
				textView_gpsStatus.setText("搜到 " + count + " 颗卫星");
				// Log.e("GpsStatus", "搜到 " + count + " 颗卫星");
				break;
			case GpsStatus.GPS_EVENT_STARTED:
				textView_gpsStatus.setText("定位启动");
				// Log.e("GpsStatus", "定位启动");
				break;
			case GpsStatus.GPS_EVENT_STOPPED:
				textView_gpsStatus.setText("定位结束");
				// Log.e("GpsStatus", "定位结束");
				break;
			}
		}
	};

	private void updateView(Location location) {
		if (location != null) {
			lgt = location.getLongitude();
			ltt = location.getLatitude();
			alt = location.getAltitude();
			speed = location.getSpeed();
			String slgt = String.valueOf(location.getLongitude());
			String sltt = String.valueOf(location.getLatitude());
			datel = new Date(location.getTime());
			textView_location.setText(dateformat.format(datel));
			textView_location.append("\n经度：" + slgt);
			textView_location.append("\n纬度：" + sltt);
			textView_location.append("\n海拔：" + alt + "米");
			textView_location.append("\n速度：" + speed + "米/秒");
			textView_location.append("\n" + location.toString());
			if (isFirstLoc) {
				isFirstLoc = false;
			} else {
				// distance = Utils.getDistance(lgt, ltt, lgt1, ltt1);
				double distance = cn.creable.ucmap.openGIS.Arithmetic.Distance(GF.createPoint(new Coordinate(lgt, ltt)),
						GF.createPoint(new Coordinate(lgt1, ltt1)));
				lc += distance;
				date = new Date();
				duration = date.getTime() - time_start.getTime();
				sduration = timeformatd.format(duration);
				textView_timer.setText(sduration);
				textView_location.append("\n位移：" + String.valueOf(distance) + "米");
			}
			lgt1 = lgt;
			ltt1 = ltt;
			textView_location.append("\n路程：" + lc + "米");
			RWXML.add(filename_gpx, dateformat.format(datel), sltt, slgt, String.valueOf(lc), sduration);
			if (c > 10) {
				new Thread(t).start();
				c = 0;
			}
			c++;
		} else {
			textView_location.setText("无法定位");
		}
	}

	private Criteria getCriteria() {
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE); // 精度
		criteria.setSpeedRequired(true);
		criteria.setCostAllowed(false); // 收费
		criteria.setBearingRequired(true); // 方向
		criteria.setAltitudeRequired(true); // 海拔
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		return criteria;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		LM.removeUpdates(locationListener);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
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
			MainApplication.setrfn("");
			finish();
			break;
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			startActivity(new Intent(NoMapActivity.this, MenuActivity.class));
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private String m(int i) {
		return i < 10 ? "0" + i : "" + i;
	}

	public static final boolean isGPSOpen(final Context context) {
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		if (gps) {
			return true;
		}
		return false;
	}

	public static final void openGPS(Context context) {
		if (!isGPSOpen(context)) {
			Intent GPSIntent = new Intent();
			GPSIntent.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
			GPSIntent.addCategory("android.intent.category.ALTERNATIVE");
			GPSIntent.setData(Uri.parse("custom:3"));
			try {
				PendingIntent.getBroadcast(context, 0, GPSIntent, 0).send();
			} catch (CanceledException e) {
				e.printStackTrace();
			}
		}
	}

	Thread t = new Thread(new Runnable() {
		@Override
		public void run() {
			String dateu = "";
			String timeu = "";
			try {
				dateu = URLEncoder.encode(dateformat2.format(datel), "utf-8");
				timeu = URLEncoder.encode(timeformat.format(datel), "utf-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			final String SU = uploadServer + "?date=" + dateu + "&time=" + timeu + "&longitude=" + lgt + "&latitude=" + ltt + "&speed=" + speed + "&distance="
					+ distance;
			RC = Utils.sendURLResponse(SU);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					textView_upload.setText(SU + "\nResponseCode: " + RC);
				}
			});
			// distancesend = 0;
		}
	});

}
