/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.weatherara;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.example.android.weatherara.data.SunshinePreferences;
import com.example.android.weatherara.data.WeatherContract;
import com.example.android.weatherara.sync.SunshineSyncUtils;

/**
 * The SettingsFragment serves as the display for all of the user's settings. In Sunshine, the
 * user will be able to change their preference for units of measurement from metric to imperial,
 * set their preferred weather location, and indicate whether or not they'd like to see
 * notifications.
 *
 * Please note: If you are using our dummy weather services, the location returned will always be
 * Mountain View, California.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private static GpsTracker gpsTracker;
    private void setPreferenceSummary(Preference preference, Object value) {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(stringValue);
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        gpsTracker = new GpsTracker(getContext());
        // Add 'general' preferences, defined in the XML file
        addPreferencesFromResource(R.xml.pref_general);

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen prefScreen = getPreferenceScreen();
        int count = prefScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference p = prefScreen.getPreference(i);
            if (!(p instanceof CheckBoxPreference)&&!(p instanceof SwitchPreferenceCompat)) {

                String value = sharedPreferences.getString(p.getKey(), "");
                setPreferenceSummary(p, value);
            }
        }
        SwitchPreferenceCompat current_location_preference=findPreference(getString(R.string.Use_Location_Key));
        if (current_location_preference.isChecked()) {
            gpsTracker.getLocation();
            getLocation();
            EditTextPreference locationPreference = findPreference(getString(R.string.pref_location_key));
            assert locationPreference != null;
            locationPreference.setEnabled(false);
        } else if (!current_location_preference.isChecked()) {
            Log.i(TAG + " ###", "SwitchPreference disabled: true");
            EditTextPreference locationPreference = findPreference(getString(R.string.pref_location_key));
//            assert locationPreference != null;
            locationPreference.setEnabled(true);
            Log.i(TAG + " ###", "is edit text preference enabled: " + locationPreference.isEnabled());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // unregister the preference change listener
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        // register the preference change listener
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        Activity activity;
        activity = getActivity();
        SwitchPreferenceCompat current_location_preference = findPreference(getString(R.string.Use_Location_Key));
        assert current_location_preference != null;
        Log.i(TAG + " ###", "current location preference: " + current_location_preference.isChecked());
        if (key.equals(getString(R.string.Use_Location_Key)))
            if (current_location_preference.isChecked()) {
                gpsTracker.getLocation();
                getLocation();
                EditTextPreference locationPreference = findPreference(getString(R.string.pref_location_key));
                assert locationPreference != null;
                locationPreference.setEnabled(false);

                //  TODO (14) Sync the weather if the location changes
                SunshineSyncUtils.startImmediateSync(activity);
            } else if (!current_location_preference.isChecked()) {
                Log.i(TAG + " ###", "SwitchPreference disabled: true");
                EditTextPreference locationPreference = findPreference(getString(R.string.pref_location_key));
//            assert locationPreference != null;
                locationPreference.setEnabled(true);
                Log.i(TAG + " ###", "is edit text preference enabled: " + locationPreference.isEnabled());
                SunshinePreferences.resetLocationCoordinates(activity);
                //  TODO (14) Sync the weather if the location changes
                SunshineSyncUtils.startImmediateSync(activity);

            }
        if (key.equals(getString(R.string.pref_location_key))) {
            // we've changed the location
            // Wipe out any potential PlacePicker latlng values so that we can use this text entry.
            SunshinePreferences.resetLocationCoordinates(activity);
            //  TODO (14) Sync the weather if the location changes
            SunshineSyncUtils.startImmediateSync(activity);
        } else if (key.equals(getString(R.string.pref_units_key))) {
            // units have changed. update lists of weather entries accordingly
            activity.getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
        }
        Preference preference = findPreference(key);
        if (null != preference) {
            if (!(preference instanceof CheckBoxPreference)&&!(preference instanceof SwitchPreferenceCompat)) {
                setPreferenceSummary(preference, sharedPreferences.getString(key, ""));
            }
        }

    }
    public void getLocation()
    {

        if(gpsTracker.canGetLocation()){
            Log.i(TAG+"Coordinates","Can get Location="+true);
            double latitude = gpsTracker.getLatitude();
            double longitude = gpsTracker.getLongitude();
            SunshinePreferences.setLocationDetails(getContext(), latitude, longitude);
            Log.i(TAG + " ###", "latitude: " + latitude + " longituede: " + longitude);
            Toast.makeText(getContext(), "Latitude: " + latitude + " Longitude: " + longitude, Toast.LENGTH_LONG).show();
        }else{
            gpsTracker.showSettingsAlert();
        }
//        Log.i(TAG+" Coordinates","Latitude and Longitude are "+latitude+" "+longitude)
    }
}
