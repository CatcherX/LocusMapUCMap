package com.hty.LocusMapUCMap;

import java.io.File;
import java.util.Arrays;
import java.util.Vector;

import org.jeo.vector.BasicFeature;
import org.jeo.vector.Feature;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import cn.creable.ucmap.openGIS.UCCoordinateFilter;
import cn.creable.ucmap.openGIS.UCFeatureLayer;
import cn.creable.ucmap.openGIS.UCFeatureLayerListener;
import cn.creable.ucmap.openGIS.UCMapView;
import cn.creable.ucmap.openGIS.UCMarkerLayer;
import cn.creable.ucmap.openGIS.UCRasterLayer;
import cn.creable.ucmap.openGIS.UCStyle;
import cn.creable.ucmap.openGIS.UCVectorLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class HistoryLocus extends Activity implements UCFeatureLayerListener, LocationListener {

	TextView textView_history;
	CheckBox checkBoxFollow;

	UCMapView mapView;
	private MeasureTool mTool = null;
	private UCCoordinateFilter filter_WGS_to_GCJ, filter_BD_to_GCJ;
	private LocationManager locationManager;
	PathAnalysisTool paTool;
	UCRasterLayer gLayer;
	UCMarkerLayer mlayer;
	UCVectorLayer vlayer;
	int type;
	BitmapDrawable BD_start, BD_end;
	GeometryFactory gf = new GeometryFactory();
	Coordinate coord0;
	double lgt, ltt, lgt0, ltt0;
	boolean isFirst = true;
	String filename = "";
	ImageButton imageButton_location;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MainApplication.getInstance().addActivity(this);
		UCMapView.setTileScale(0.5f);
		setContentView(R.layout.activity_history);
		textView_history = (TextView) findViewById(R.id.textView_history);
		checkBoxFollow = (CheckBox) findViewById(R.id.checkBoxFollow);
		imageButton_location = (ImageButton) findViewById(R.id.imageButton_location);
		imageButton_location.setOnClickListener(new ButtonListener());

		mapView = (UCMapView) findViewById(R.id.mapView_history);
		mapView.setBackgroundColor(0xFFFFFFFF);
		mapView.addScaleBar();
		mapView.rotation(false);

		filter_WGS_to_GCJ = new UCCoordinateFilter() {
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

		filter_BD_to_GCJ = new UCCoordinateFilter() {
			@Override
			public double[] to(double x, double y) {
				// 百度坐标系(BD-09)转火星坐标系(GCJ-02)
				double[] result = new double[2];
				double x_pi = 3.14159265358979324 * 3000.0 / 180.0;
				x = x - 0.0065;
				y = y - 0.006;
				double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi);
				double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi);
				result[0] = z * Math.cos(theta);
				result[1] = z * Math.sin(theta);
				return result;
			}

			@Override
			public double[] from(double x, double y) {
				double[] result = new double[2];
				result[0] = x;
				result[1] = y;
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

		mapView.moveTo(116.383333, 39.9, 5000);
		mapView.postDelayed(new Runnable() {
			@Override
			public void run() {
				mapView.refresh();
			}
		}, 0);

		BD_start = (BitmapDrawable) getResources().getDrawable(R.drawable.marker_start);
		BD_end = (BitmapDrawable) getResources().getDrawable(R.drawable.marker_end);
	}

	class ButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.imageButton_location:
				mapView.moveTo(coord0.x, coord0.y, mapView.getScale());
				break;
			}
		}
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
		menu.add(0, 8, 8, "BD09转GCJ02");
		menu.add(0, 9, 9, "还原坐标");
		menu.add(0, 10, 10, "删除");
		menu.add(0, 11, 11, "分享");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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
			mapView.setCoordinateFilter(filter_BD_to_GCJ);
			mapView.refresh();
		} else if (id == 9) {
			mapView.setCoordinateFilter(filter_WGS_to_GCJ);
			mapView.refresh();
		} else if (id == 10) {
			new AlertDialog.Builder(HistoryLocus.this).setIcon(R.drawable.warn).setTitle("删除操作").setMessage("此步骤不可还原，确定删除\n" + filename + " ？")
					.setPositiveButton("是", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String ttext = null;
							Log.e(" MainApplication.getfn()", MainApplication.getfn());
							Log.e("MainApplication.getrfn()", MainApplication.getrfn());
							if (!filename.equals(MainApplication.getrfn())) {
								ttext = filename + " 已删除！";
								RWXML.del(filename);
								startActivity(new Intent(HistoryLocus.this, GPXListActivity.class));
							} else {
								ttext = "此文件正在写入数据，请先结束行程！";
							}
							Toast.makeText(getApplicationContext(), ttext, Toast.LENGTH_SHORT).show();
						}
					}).setNegativeButton("否", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					}).show();
		} else if (id == 11) {
			String filepath = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/" + filename;
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filepath)));
			intent.setType("*/*");
			startActivity(Intent.createChooser(intent, "分享 " + filename));
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
		if (checkBoxFollow.isChecked())
			mapView.moveTo(lgt, ltt, mapView.getScale());
		mapView.setLocationPosition(lgt, ltt, location.getAccuracy());
		mapView.moveLayer(gLayer, 0);
		mapView.refresh();
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
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.e("onNewIntent", "onNewIntent");
		setIntent(intent); // must store the new intent unless getIntent() will return the old one
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.e("onResume", "onResume");
		mapView.setCoordinateFilter(filter_WGS_to_GCJ);
		enableAvailableProviders();
		Intent intent = getIntent();
		Log.e("intent", intent.toString());
		String filename1 = intent.getStringExtra("filename");
		Log.e("filename1", filename1);
		// if (!MainApplication.getfn().equals("")) {
		if (!filename.equals(filename1) || filename1.equals(MainApplication.getrfn())) {
			filename = filename1;
			if (vlayer != null) {
				mapView.deleteLayer(vlayer);
			}
			vlayer = mapView.addVectorLayer();
			if (mlayer != null) {
				mapView.deleteLayer(mlayer);
			}
			mlayer = mapView.addMarkerLayer(null);
			filename = MainApplication.getfn();
			Coordinate[] coords = RWXML.read(filename);
			textView_history.setText(MainApplication.getmsg() + "\n" + coords.length + " 个点");
			Geometry geo = gf.createLineString(coords);
			vlayer.addLine(geo, 2, 0xFF0000FF);
			mlayer.addBitmapItem(BD_start.getBitmap(), coords[0].x, coords[0].y, "", "");
			mlayer.addBitmapItem(BD_end.getBitmap(), coords[coords.length - 1].x, coords[coords.length - 1].y, "", "");
			coord0 = coords[coords.length / 2];
			mapView.moveTo(coords[coords.length / 2].x, coords[coords.length / 2].y, mapView.getScale());
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.e("onPause", "onPause");
		if (locationManager != null)
			locationManager.removeUpdates(this);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			startActivity(new Intent(HistoryLocus.this, GPXListActivity.class));
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

}
