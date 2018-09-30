package com.hty.LocusMapUCMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.os.Environment;
import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;

public class RWXML {
	static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
	static SimpleDateFormat timeformat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
	static Date duration;
	static double distance;
	static int seconds, h, m, s;
	static DecimalFormat DF = new DecimalFormat("0.0");
	static DecimalFormat DF1 = new DecimalFormat("0");

	static void create(String time) {
		String fp = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/";
		String fn = time + "UC.gpx";
		String fpn = fp + fn;
		// Log.e("xmlFile", fpn);
		File filepath = new File(fp);
		if (!filepath.exists()) {
			filepath.mkdirs();
		}
		File file = new File(fpn);
		if (!file.exists()) {
			try {
				file.createNewFile();
				Log.e("RWXML", "createFile");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e("RWXML:create", "IOException");
			}
		}
		StringBuilder sb = new StringBuilder(time);
		sb.insert(4, "-");
		sb.insert(7, "-");
		sb.insert(10, " ");
		sb.insert(13, ":");
		sb.insert(16, ":");
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			fw = new FileWriter(fpn, true);
			bw = new BufferedWriter(fw);
			String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\"><metadata><starttime>"
					+ sb
					+ "</starttime><endtime>"
					+ time
					+ "</endtime><distance>0</distance><duration>00:00:00</duration><maxspeed>0</maxspeed></metadata><trk><trkseg></trkseg></trk></gpx>";
			bw.write(content);
			bw.flush();
			bw.close();
			fw.close();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}

	static void add(String filename, String time, String latitude, String longitude, String distance, String duration) {
		// String fp = Environment.getExternalStorageDirectory().getPath() +
		// "/LocusMap/";
		// String fn = filename + ".gpx";
		String fpn = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/" + filename;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Document doc = null;
		try {
			doc = db.parse(new FileInputStream(fpn));
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		doc.getDocumentElement().getElementsByTagName("endtime").item(0).setTextContent(time);
		doc.getDocumentElement().getElementsByTagName("distance").item(0).setTextContent(distance);
		doc.getDocumentElement().getElementsByTagName("duration").item(0).setTextContent(duration);
		Element Etrkpt = doc.createElement("trkpt");
		Attr attr = doc.createAttribute("lat");
		attr.setValue(latitude);
		Attr attr2 = doc.createAttribute("lon");
		attr2.setValue(longitude);
		Etrkpt.setAttributeNode(attr);
		Etrkpt.setAttributeNode(attr2);
		Element Etime = doc.createElement("time");
		Etime.setTextContent(time);
		Etrkpt.appendChild(Etime);
		doc.getDocumentElement().getElementsByTagName("trkseg").item(0).appendChild(Etrkpt);
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = null;
		try {
			transformer = tFactory.newTransformer();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(fpn));
		try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static String[] gpxlist() {
		String fp = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/";
		File f = new File(fp);
		if (!f.exists()) {
			String[] m = { "目录不存在" };
			return m;
		} else {
			String[] names = f.list(new FilenameFilter() {
				@Override
				public boolean accept(File f, String name) {
					return name.endsWith(".gpx");
				}
			});
			if (names.length == 0) {
				String[] m = { "没有轨迹文件" };
				return m;
			} else {
				java.util.Arrays.sort(names, Collections.reverseOrder());
				return names;
			}
		}
	}

	static Coordinate[] read(String filename) {
		String starttime = "", endtime = "", sdistance = "", sduration = "", info;
		String fp = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/" + filename;
		DocumentBuilderFactory DBF = DocumentBuilderFactory.newInstance();
		DocumentBuilder DB = null;
		try {
			DB = DBF.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Document doc = null;
		try {
			doc = DB.parse(new FileInputStream(fp));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			starttime = doc.getDocumentElement().getElementsByTagName("starttime").item(0).getFirstChild().getTextContent();
		} catch (Exception e) {
		}
		try {
			endtime = doc.getDocumentElement().getElementsByTagName("endtime").item(0).getFirstChild().getTextContent();
		} catch (Exception e) {
		}
		try {
			sdistance = doc.getDocumentElement().getElementsByTagName("distance").item(0).getFirstChild().getTextContent();
			distance = Double.parseDouble(sdistance);
			sdistance = sdistance.substring(0, sdistance.indexOf("."));
		} catch (Exception e) {
		}
		try {
			sduration = doc.getDocumentElement().getElementsByTagName("duration").item(0).getFirstChild().getTextContent();
			h = Integer.parseInt(sduration.substring(0, 2));
			m = Integer.parseInt(sduration.substring(3, 5));
			s = Integer.parseInt(sduration.substring(6));
			seconds = h * 3600 + m * 60 + s;
		} catch (Exception e) {
		}
		String v = "?";
		if (seconds != 0)
			v = DF.format(distance / seconds) + " 米/秒";
		info = filename + "\n开始时间：" + starttime + "\n结束时间：" + endtime + "\n路程：" + sdistance + " 米\n时长：" + sduration + " (" + seconds + " 秒)\n平均速度：" + v;
		MainApplication.setmsg(info);
		Element root = doc.getDocumentElement();
		NodeList trkpt = root.getElementsByTagName("trkpt");
		int trkpt_length = trkpt.getLength();
		// Log.e("trkpt_length", trkpt_length + "");
		if (trkpt_length > 1) {
			Coordinate[] coords = new Coordinate[trkpt_length];
			for (int i = 0; i < trkpt_length; i++) {
				Element elemt = (Element) trkpt.item(i);
				coords[i] = new Coordinate(Double.parseDouble(elemt.getAttribute("lon")), Double.parseDouble(elemt.getAttribute("lat")));
			}
			return coords;
		} else {
			return null;
		}
	}

	static void del(String filename) {
		String fp = Environment.getExternalStorageDirectory().getPath() + "/LocusMap/" + filename;
		File file = new File(fp);
		file.delete();
		MainApplication.setfn("");
		Log.e("del", fp);
	}

	static void append(String file, String conent) {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
			Date date = new Date();
			out.write(SDF.format(date) + ": " + conent + "\n");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}