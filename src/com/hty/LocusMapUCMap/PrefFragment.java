package com.hty.LocusMapUCMap;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;

public class PrefFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	EditTextPreference EDP_upload_server;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		EDP_upload_server = (EditTextPreference) findPreference("upload_server");
	}

	void initTextSummary() {
		if (EDP_upload_server.getText().equals("")) {
			EDP_upload_server.setSummary("http://sonichy.gearhostpreview.com/locusmap");
		} else {
			EDP_upload_server.setSummary(EDP_upload_server.getText());
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		initTextSummary();
	}

	@Override
	public void onResume() {
		super.onResume();
		initTextSummary();
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}
}