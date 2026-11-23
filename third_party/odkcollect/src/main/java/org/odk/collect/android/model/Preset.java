package org.odk.collect.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.joda.time.DateTime;
import org.odk.collect.android.utilities.Parcels;

import java.util.HashMap;
import java.util.Map;

/** A Parcelable data object that specifies preset values for certain form fields. */
public class Preset implements Parcelable {

    public static final int UNSPECIFIED = -1;
    public static final int UNKNOWN = 1;
    public static final int YES = 2;
    public static final int NO = 3;

    public DateTime encounterDatetime;  // the preset value for encounter.encounter_datetime
    public String locationUuid;  // UUID of the preset value for encounter.location_id
    public String providerUuid;  // UUID of the preset value for encounter.provider_id
    public int pregnancy = UNSPECIFIED;
    public int ivAccess = UNSPECIFIED;
    public String targetGroup;  // text of a section heading in the form to scroll to
    // Values for date and datetime questions, keyed by the concept code of the question's
    // openmrs_concept attribute ("8001640" in "8001640^Label^99DCT"), as epoch millis.
    public HashMap<String, Long> conceptDates = new HashMap<>();

    public Preset() {}

    public Preset(Parcel in) {
        encounterDatetime = Parcels.readNullableDateTime(in);
        locationUuid = Parcels.readNullableString(in);
        providerUuid = Parcels.readNullableString(in);
        pregnancy = in.readInt();
        ivAccess = in.readInt();
        targetGroup = Parcels.readNullableString(in);
        int n = in.readInt();
        for (int i = 0; i < n; i++) {
            String code = in.readString();
            conceptDates.put(code, in.readLong());
        }
    }

    /** Returns the concept code of an openmrs_concept attribute ("8001640^Label^99DCT" → "8001640"), or null. */
    public static String conceptCode(String encoded) {
        if (encoded == null) return null;
        int caret = encoded.indexOf('^');
        return caret > 0 ? encoded.substring(0, caret) : null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Parcels.writeNullableDateTime(dest, encounterDatetime);
        Parcels.writeNullableString(dest, locationUuid);
        Parcels.writeNullableString(dest, providerUuid);
        dest.writeInt(pregnancy);
        dest.writeInt(ivAccess);
        Parcels.writeNullableString(dest, targetGroup);
        dest.writeInt(conceptDates.size());
        for (Map.Entry<String, Long> entry : conceptDates.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeLong(entry.getValue());
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Preset> CREATOR =
            new Parcelable.Creator<Preset>() {

        @Override
        public Preset createFromParcel(Parcel in) {
            return new Preset(in);
        }

        @Override
        public Preset[] newArray(int size) {
            return new Preset[size];
        }
    };
}
