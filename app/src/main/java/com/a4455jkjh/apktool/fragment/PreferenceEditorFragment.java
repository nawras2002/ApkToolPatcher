package com.a4455jkjh.apktool.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import apk.tool.patcher.R;

public class PreferenceEditorFragment extends PreferenceFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference_editor);
	}
}
