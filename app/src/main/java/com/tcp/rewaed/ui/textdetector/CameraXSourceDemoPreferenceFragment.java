

package com.tcp.rewaed.ui.textdetector;

import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import com.tcp.rewaed.R;


/** Configures CameraXSource live preview demo settings. */
public class CameraXSourceDemoPreferenceFragment extends CameraXLivePreviewPreferenceFragment {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    PreferenceScreen preferenceScreen =
        (PreferenceScreen) findPreference(getResources().getString(R.string.pref_screen));

    PreferenceCategory preferenceCategory =
        (PreferenceCategory) findPreference(getString(R.string.pref_category_key_camera));

    preferenceCategory.removePreference(
        findPreference(getString(R.string.pref_key_camera_live_viewport)));
    // Remove the PreferenceCategories for hiding camera detection info.
    preferenceScreen.removePreference(preferenceScreen.getPreference(1));

    // Remove the last 3 PreferenceCategories
    preferenceScreen.removePreference(preferenceScreen.getPreference(2));
    preferenceScreen.removePreference(preferenceScreen.getPreference(2));
    preferenceScreen.removePreference(preferenceScreen.getPreference(2));
  }
}
