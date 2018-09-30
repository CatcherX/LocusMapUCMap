package com.hty.LocusMapUCMap;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Setting extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefFragment()).commit();
	}

}