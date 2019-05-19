package com.hty.LocusMapUCMap;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import org.jeo.vector.BasicFeature;
import org.jeo.vector.Feature;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import cn.creable.ucmap.openGIS.UCCoordinateFilter;
import cn.creable.ucmap.openGIS.UCFeatureLayer;
import cn.creable.ucmap.openGIS.UCFeatureLayerListener;
import cn.creable.ucmap.openGIS.UCMapView;
import cn.creable.ucmap.openGIS.UCRasterLayer;
import cn.creable.ucmap.openGIS.UCStyle;
import cn.creable.ucmap.openGIS.UCVectorLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class CurrentLocus extends Activity implements UCFeatureLayerListener, LocationListener {

	TextView textView_current, textView_upload;
	CheckBox checkBoxFollow;

	UCMapView mapView;
	private MeasureTool mTool = null;
	private UCCoordinateFilter filter;
	private LocationManager locationManager;
	PathAnalysisTool paTool;
	UCRasterLayer gLayer;
	int type;
	BitmapDrawable BD_start, BD_end;
	UCVectorLayer vlayer;
	GeometryFactory GF = new GeometryFactory();
	double lgt, ltt, lgt0, ltt0, lc = 0, dist, speed;
	boolean isFirst = true;
	SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
	SimpleDateFormat SDF1 = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
	SimpleDateFormat SDF_time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
	SimpleDateFormat SDF_date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
	Date time_start;
	SharedPreferences sharedPreferences;
	String upload_server = "", SR = "", filename_gpx = "";
	DecimalFormat DF1 = new DecimalFormat("0.0");
	DecimalFormat DF2 = new DecimalFormat("0.00");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_current);
		MainApplication.getInstance().addActivity(this);
		MainApplication.setMode("map");
		SDF_time.setTimeZone(TimeZone.getTimeZone("GMT+0"));
		UCMapView.setTileScale(0.5f);
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		upload_server = sharedPreferences.getString("uploadServer", "http://sonichy.gearhostpreview.com/locusmap");
		textView_current = (TextView) findViewById(R.id.textView_current);
		textView_upload = (TextView) findViewById(R.id.textView_upload);
		checkBoxFollow = (CheckBox) findViewById(R.id.checkBoxFollow);

		mapView = (UCMapView) findViewById(R.id.mapView_current);
		mapView.setBackgroundColor(0xFFFFFFFF);
		mapView.addScaleBar();
		mapView.rotation(false);
		// mapView.refresh();
		if (vlayer == null)
			vlayer = mapView.addVectorLayer();

		filter = new UCCoordinateFilter() {
			@Override
			public double[] to(double x, double y) {
				double[] result = new double[2];
				Gps gps = PositionUtil.gps84_To_Gcj02(y, x);
				result[0] = gps.getWgLon();
				result[1] = gps.getWgLat();
				return result;
			}

			@Override
			public double[] from(double x, double y) {
				double[] result = new double[2];
				Gps gps = PositionUtil.gcj_To_Gps84(y, x);
				result[0] = gps.getWgLon();
				result[1] = gps.getWgLat();
				return result;
			}
		};

		String dir = Environment.getExternalStorageDirectory().getPath();
		gLayer = mapView.addGoogleMapLayer("http://mt0.google.cn/vt/lyrs=m&hl=zh-CN&gl=cn&scale=2&x={X}&y={Y}&z={Z}", 0, 20, dir + "/cacheGoogleMapM.db");
		// mapView.addGoogleMapLayer("http://mt0.google.cn/vt/lyrs=p&hl=zh-CN&gl=cn&scale=2&x={X}&y={Y}&z={Z}",
		// 0, 20, dir+"/cacheGoogleMapP.db");
		// mapView.addGoogleMapLayer("http://mt0.google.cn/vt/lyrs=y&hl=zh-CN&gl=cn&scale=2&x={X}&y={Y}&z={Z}",
		// 0, 20, dir+"/cacheGoogleMapY.db");
		// mapView.setLayerVisible(1, false);
		// mapView.setLayerVisible(2, false);

		// 以下三行的效果是添加一个矢量图层叠加到google图层上
		if (new File(dir + "/bjshp/_polyline.shp").exists()) {
			UCFeatureLayer layer = mapView.addFeatureLayer(this);
			layer.loadShapefile(dir + "/bjshp/铁路_polyline.shp");
			// , 30, 2,
			// "#FFF1EAB5",
			// "#00000000");
			layer.setStyle(new UCStyle(null, null, 0, 0, 3, 4, "#FF000000", 8, "#FFFFFFFF"));
		}

		mapView.addLocationLayer();
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		mapView.moveTo(116.383333, 39.9, 2000);
		mapView.postDelayed(new Runnable() {
			@Override
			public void run() {
				mapView.refresh();
			}
		}, 0);

		BD_start = (BitmapDrawable) getResources().getDrawable(R.drawable.marker_start);
		BD_end = (BitmapDrawable) getResources().getDrawable(R.drawable.marker_end);

		time_start = new Date();
		filename_gpx = SDF1.format(time_start) + "UC.gpx";
		MainApplication.setrfn(filename_gpx);
		RWXML.create(SDF1.format(time_start));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "线路图");
		menu.add(0, 1, 1, "地形图");
		menu.add(0, 2, 2, "卫星图");
		menu.add(0, 3, 3, "开始路径分析");
		menu.add(0, 4, 4, "结束路径分析");
		menu.add(0, 5, 5, "测距离");
		menu.add(0, 6, 6, "测面积");
		menu.add(0, 7, 7, "停止测量");
		menu.add(0, 8, 8, "结束");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == 0) {
			if (type != 0) {
				mapView.deleteLayer(gLayer);
				String dir = Environment.getExternalStorageDirectory().getPath();
				gLayer = mapView.addGoogleMapLayer("http://mt0.google.cn/vt/lyrs=m&hl=zh-CN&gl=cn&scale=2&x={X}&y={Y}&z={Z}", 0, 20, dir
						+ "/cacheGoogleMapM.db");
				mapView.moveLayer(gLayer, 0);
				mapView.refresh();
				type = 0;
			}
			// mapView.setLayerVisible(0, true);
			// mapView.setLayerVisible(1, false);
			// mapView.setLayerVisible(2, false);
			// mapView.refresh();
		} else if (id == 1) {
			if (type != 1) {
				mapView.deleteLayer(gLayer);
				String dir = Environment.getExternalStorageDirectory().getPath();
				gLayer = mapView.addGoogleMapLayer("http://mt0.google.cn/vt/lyrs=p&hl=zh-CN&gl=cn&scale=2&x={X}&y={Y}&z={Z}", 0, 20, dir
						+ "/cacheGoogleMapP.db");
				mapView.moveLayer(gLayer, 0);
				mapView.refresh();
				type = 1;
			}
			// mapView.setLayerVisible(0, false);
			// mapView.setLayerVisible(1, true);
			// mapView.setLayerVisible(2, false);
			// mapView.refresh();
		} else if (id == 2) {
			if (type != 2) {
				mapView.deleteLayer(gLayer);
				String dir = Environment.getExternalStorageDirectory().getPath();
				gLayer = mapView.addGoogleMapLayer("http://mt0.google.cn/vt/lyrs=y&hl=zh-CN&gl=cn&scale=2&x={X}&y={Y}&z={Z}", 0, 20, dir
						+ "/cacheGoogleMapY.db");
				mapView.moveLayer(gLayer, 0);
				mapView.refresh();
				type = 2;
			}
			// mapView.setLayerVisible(0, false);
			// mapView.setLayerVisible(1, false);
			// mapView.setLayerVisible(2, true);
			// mapView.refresh();
		} else if (id == 3) {
			if (mTool != null) {
				mTool.stop();
				mTool = null;
			}
			if (paTool == null) {
				paTool = new PathAnalysisTool(mapView, BD_start.getBitmap(), BD_end.getBitmap());
				paTool.start();
			}
			return true;
		} else if (id == 4) {
			if (paTool != null) {
				paTool.end();
				paTool = null;
			}
			return true;
		} else if (id == 5) {
			if (paTool != null) {
				paTool.end();
				paTool = null;
			}
			if (mTool == null) {
				BitmapDrawable bd = (BitmapDrawable) getResources().getDrawable(R.drawable.marker_poi);
				mTool = new MeasureTool(mapView, bd.getBitmap(), 0);
				mTool.start();
			}
		} else if (id == 6) {
			if (paTool != null) {
				paTool.end();
				paTool = null;
			}
			if (mTool == null) {
				BitmapDrawable bd = (BitmapDrawable) getResources().getDrawable(R.drawable.marker_poi);
				mTool = new MeasureTool(mapView, bd.getBitmap(), 1);
				mTool.start();
			}
		} else if (id == 7) {
			if (mTool != null)
				mTool.stop();
			mTool = null;
			mapView.refresh();
		} else if (id == 8) {
			MainApplication.setrfn("");
			MainApplication.setMode("");
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

	static Feature feature(String id, Object... values) {
		Feature current = new BasicFeature(id, Arrays.asList(values));
		return current;
	}

	@Override
	public boolean onItemLongPress(UCFeatureLayer layer, Feature feature, double distance) {
		if (mTool != null)
			return true;
		if (distance > 30)
			return false;
		Toast.makeText(getBaseContext(), "长按了\n" + feature + " distance=" + distance, Toast.LENGTH_SHORT).show();
		Vector<Feature> features = new Vector<Feature>();
		features.add(feature);
		mapView.getMaskLayer().setData(features, 30, 2, "#88FF0000", "#88FF0000");
		mapView.refresh();
		return true;
	}

	@Override
	public boolean onItemSingleTapUp(UCFeatureLayer layer, Feature feature, double distance) {
		if (mTool != null)
			return true;
		if (distance > 30)
			return false;
		Toast.makeText(getBaseContext(), "点击了\n" + feature + " distance=" + distance, Toast.LENGTH_SHORT).show();
		Vector<Feature> features = new Vector<Feature>();
		features.add(feature);
		mapView.getMaskLayer().setData(features, 30, 2, "#88FF0000", "#88FF0000");
		mapView.refresh();
		return true;
	}

	@Override
	public void onLocationChanged(Location location) {
		lgt = location.getLongitude();
		ltt = location.getLatitude();
		speed = location.getSpeed();
		if (checkBoxFollow.isChecked())
			mapView.moveTo(lgt, ltt, mapView.getScale());
		mapView.setLocationPosition(lgt, ltt, location.getAccuracy());
		// mapView.refresh();

		if (isFirst) {
			isFirst = false;
			lgt0 = lgt;
			ltt0 = ltt;
		}

		Coordinate[] coords = new Coordinate[2];
		coords[0] = new Coordinate(lgt, ltt);
		coords[1] = new Coordinate(lgt0, ltt0);
		Geometry geo = GF.createLineString(coords);
		vlayer.addLine(geo, 2, 0xFF00FF00);
		// mapView.refresh();
		// dist =
		// cn.creable.ucmap.openGIS.Arithmetic.Distance(GF.createPoint(new
		// Coordinate(lgt0, ltt0)), GF.createPoint(new Coordinate(lgt, ltt)));
		float[] results = new float[1];
		Location.distanceBetween(ltt, lgt, ltt0, lgt0, results);
		dist = results[0];
		lc += dist;
		lgt0 = lgt;
		ltt0 = ltt;
		Date date = new Date();
		long duration = date.getTime() - time_start.getTime();
		RWXML.add(filename_gpx, SDF.format(date), String.valueOf(ltt), String.valueOf(lgt), String.valueOf(lc), SDF_time.format(duration));
		new Thread(t).start();
		textView_current
				.setText(SDF.format(time_start) + "\n经度：" + lgt + "\n纬度：" + ltt + "\n高度：" + location.getAltitude() + " 米\n速度：" + DF1.format(speed)
						+ " 米/秒\n精度：" + location.getAccuracy() + " 米\n位移：" + DF2.format(dist) + " 米\n时长：" + SDF_time.format(duration) + "\n路程："
						+ DF2.format(lc) + " 米");
		textView_upload.setText("上传：" + SR);
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		enableAvailableProviders();
		mapView.setCoordinateFilter(filter);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			startActivity(new Intent(CurrentLocus.this, MenuActivity.class));
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void enableAvailableProviders() {
		if (locationManager == null)
			return;
		locationManager.removeUpdates(this);
		for (String provider : locationManager.getProviders(true)) {
			if (LocationManager.GPS_PROVIDER.equals(provider) || LocationManager.NETWORK_PROVIDER.equals(provider)) {
				locationManager.requestLocationUpdates(provider, 0, 0, this);
			}
		}
	}

	Thread t = new Thread() {
		@Override
		public void run() {
			String dateu = "";
			String timeu = "";
			Date date = new Date();
			try {
				dateu = URLEncoder.encode(SDF_date.format(date), "utf-8");
				timeu = URLEncoder.encode(SDF_time.format(date), "utf-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			String SU = upload_server + "/add.php?date=" + dateu + "&time=" + timeu + "&longitude=" + lgt + "&latitude=" + ltt + "&speed=" + DF1.format(speed)
					+ "&distance=" + DF2.format(dist);
			SR = Utils.sendURLResponse(SU);
			RWXML.append(Environment.getExternalStorageDirectory().getPath() + "/LocusMap/UCMap.log", "CurrentLocus.upload:" + SU);
		}
	};

}
