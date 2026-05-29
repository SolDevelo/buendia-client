/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.logic;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.javarosa.core.services.IPropertyManager;
import org.javarosa.core.services.properties.IPropertyRules;
import org.odk.collect.android.preferences.PreferencesActivity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

/**
 * Used to return device properties to JavaRosa
 *
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */

public class PropertyManager implements IPropertyManager {

    private String t = "PropertyManager";

    private Context mContext;

    private TelephonyManager mTelephonyManager;
    private HashMap<String, String> mProperties;

    public final static String DEVICE_ID_PROPERTY = "deviceid"; // imei
    private final static String SUBSCRIBER_ID_PROPERTY = "subscriberid"; // imsi
    private final static String SIM_SERIAL_PROPERTY = "simserial";
    private final static String PHONE_NUMBER_PROPERTY = "phonenumber";
    private final static String USERNAME = "username";
    private final static String EMAIL = "email";

    public final static String OR_DEVICE_ID_PROPERTY = "uri:deviceid"; // imei
    public final static String OR_SUBSCRIBER_ID_PROPERTY = "uri:subscriberid"; // imsi
    public final static String OR_SIM_SERIAL_PROPERTY = "uri:simserial";
    public final static String OR_PHONE_NUMBER_PROPERTY = "uri:phonenumber";
    public final static String OR_USERNAME = "uri:username";
    public final static String OR_EMAIL = "uri:email";


    public String getName() {
        return "Property Manager";
    }


    public PropertyManager(Context context) {
        Log.i(t, "calling constructor");

        mContext = context;

        mProperties = new HashMap<String, String>();
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        // On Android 10+ (API 29+), TelephonyManager.getDeviceId() requires
        // READ_PRIVILEGED_PHONE_STATE (only granted to system apps). Calling
        // it from a regular app throws SecurityException, crashing the app
        // on launch. Fall back to a null deviceId on failure — the code
        // below already handles the null case (uses ANDROID_ID / Wi-Fi MAC).
        String deviceId;
        try {
            deviceId = mTelephonyManager.getDeviceId();
        } catch (SecurityException e) {
            Log.w(t, "getDeviceId() denied (Android 10+ restriction); falling back to ANDROID_ID");
            deviceId = null;
        }
        String orDeviceId = null;
        if (deviceId != null ) {
        	if ((deviceId.contains("*") || deviceId.contains("000000000000000"))) {
        		deviceId =
        				Settings.Secure
                        	.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        		orDeviceId = Settings.Secure.ANDROID_ID + ":" + deviceId;
        	} else {
        		orDeviceId = "imei:" + deviceId;
        	}
        }

        if ( deviceId == null ) {
        	// no SIM -- WiFi only
        	// Retrieve WiFiManager
        	WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

    		// Get WiFi status
    		WifiInfo info = wifi.getConnectionInfo();
    		if ( info != null ) {
    			deviceId = info.getMacAddress();
    			orDeviceId = "mac:" + deviceId;
    		}
        }

        // if it is still null, use ANDROID_ID
        if ( deviceId == null ) {
            deviceId =
                    Settings.Secure
                            .getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    		orDeviceId = Settings.Secure.ANDROID_ID + ":" + deviceId;
        }

        mProperties.put(DEVICE_ID_PROPERTY, deviceId);
        mProperties.put(OR_DEVICE_ID_PROPERTY, orDeviceId);

        String value;

        // Android 10+ restricts getSubscriberId / getSimSerialNumber /
        // getLine1Number to privileged apps. Wrap each call so that
        // a denied permission silently leaves the property unset.
        try {
            value = mTelephonyManager.getSubscriberId();
            if ( value != null ) {
                mProperties.put(SUBSCRIBER_ID_PROPERTY, value);
                mProperties.put(OR_SUBSCRIBER_ID_PROPERTY, "imsi:" + value);
            }
        } catch (SecurityException e) {
            Log.w(t, "getSubscriberId() denied; skipping IMSI property");
        }
        try {
            value = mTelephonyManager.getSimSerialNumber();
            if ( value != null ) {
                mProperties.put(SIM_SERIAL_PROPERTY, value);
                mProperties.put(OR_SIM_SERIAL_PROPERTY, "simserial:" + value);
            }
        } catch (SecurityException e) {
            Log.w(t, "getSimSerialNumber() denied; skipping SIM serial property");
        }
        try {
            value = mTelephonyManager.getLine1Number();
            if ( value != null ) {
                mProperties.put(PHONE_NUMBER_PROPERTY, value);
                mProperties.put(OR_PHONE_NUMBER_PROPERTY, "tel:" + value);
            }
        } catch (SecurityException e) {
            Log.w(t, "getLine1Number() denied; skipping phone-number property");
        }

        // Get the username from the settings
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        value = settings.getString(PreferencesActivity.KEY_USERNAME, null);
        if ( value != null ) {
        	mProperties.put(USERNAME, value);
        	mProperties.put(OR_USERNAME, "username:" + value);
        }
        value = settings.getString(PreferencesActivity.KEY_SELECTED_GOOGLE_ACCOUNT, null);
        if ( value != null ) {
        	mProperties.put(EMAIL, value);
        	mProperties.put(OR_EMAIL, "mailto:" + value);
        }
    }

    @Override
    public Vector<String> getProperty(String propertyName) {
        return null;
    }


    @Override
    public String getSingularProperty(String propertyName) {
    	// for now, all property names are in english...
        return mProperties.get(propertyName.toLowerCase(Locale.ENGLISH));
    }


    @Override
    public void setProperty(String propertyName, String propertyValue) {
    }


    @Override
    public void setProperty(String propertyName, @SuppressWarnings("rawtypes") Vector propertyValue) {

    }


    @Override
    public void addRules(IPropertyRules rules) {

    }


    @Override
    public Vector<IPropertyRules> getRules() {
        return null;
    }

}
