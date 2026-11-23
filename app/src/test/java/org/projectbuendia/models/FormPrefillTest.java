// Copyright 2026 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.models;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.odk.collect.android.model.Preset;
import org.projectbuendia.client.json.Datatype;
import org.projectbuendia.client.utils.Utils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FormPrefillTest {
    private static final DateTime NOW = new DateTime(2026, 11, 23, 14, 0);
    private DateTimeZone savedZone;

    @Before public void setUp() {
        savedZone = DateTimeZone.getDefault();
        DateTimeZone.setDefault(DateTimeZone.forID("Africa/Kinshasa"));
    }

    @After public void tearDown() {
        DateTimeZone.setDefault(savedZone);
    }

    private static Obs obs(String conceptUuid, Datatype type, String value) {
        return new Obs("o-" + conceptUuid, "e", "patient", "provider", conceptUuid, type, NOW, null, value, null);
    }

    private static Map<String, Obs> latest(Obs... observations) {
        Map<String, Obs> map = new HashMap<>();
        for (Obs o : observations) map.put(o.conceptUuid, o);
        return map;
    }

    @Test public void noObs_empty() {
        assertTrue(FormPrefill.dates(latest()).isEmpty());
    }

    @Test public void admissionMillisKept() {
        long admitted = new DateTime(2026, 11, 15, 8, 30).getMillis();
        Map<String, Long> dates = FormPrefill.dates(latest(
            obs(ConceptUuids.ADMISSION_DATETIME_UUID, Datatype.DATETIME, "" + admitted)));
        assertEquals(Long.valueOf(admitted), dates.get("8001640"));
        assertEquals(1, dates.size());
    }

    @Test public void onsetDateToStartOfDayMillis() {
        Map<String, Long> dates = FormPrefill.dates(latest(
            obs(ConceptUuids.SYMPTOM_ONSET_DATE_UUID, Datatype.DATE, "2026-11-12")));
        long kinshasaMidnight = new DateTime(2026, 11, 12, 0, 0, DateTimeZone.forID("Africa/Kinshasa")).getMillis();
        assertEquals(Long.valueOf(kinshasaMidnight), dates.get("6001730"));
        assertEquals(new LocalDate(2026, 11, 12), new DateTime(dates.get("6001730")).toLocalDate());
    }

    @Test public void bothDates() {
        long admitted = new DateTime(2026, 11, 15, 8, 30).getMillis();
        Map<String, Long> dates = FormPrefill.dates(latest(
            obs(ConceptUuids.ADMISSION_DATETIME_UUID, Datatype.DATETIME, "" + admitted),
            obs(ConceptUuids.SYMPTOM_ONSET_DATE_UUID, Datatype.DATE, "2026-11-12")));
        assertEquals(2, dates.size());
        assertEquals(Long.valueOf(admitted), dates.get("8001640"));
    }

    @Test public void missingOrBadValuesSkipped() {
        assertTrue(FormPrefill.dates(latest(
            obs(ConceptUuids.ADMISSION_DATETIME_UUID, Datatype.DATETIME, "not-a-number"),
            obs(ConceptUuids.SYMPTOM_ONSET_DATE_UUID, Datatype.DATE, null))).isEmpty());
        assertTrue(FormPrefill.dates(latest(
            obs(ConceptUuids.ADMISSION_DATETIME_UUID, Datatype.DATETIME, null))).isEmpty());
    }

    @Test public void unparseableOnsetSkipped() {
        assertTrue(FormPrefill.dates(latest(
            obs(ConceptUuids.SYMPTOM_ONSET_DATE_UUID, Datatype.DATE, "Mon Nov 23 00:00:00 WAT 2026"))).isEmpty());
    }

    @Test public void conceptCodesMatchUuids() {
        assertEquals(ConceptUuids.ADMISSION_DATETIME_UUID, Utils.expandUuid(FormPrefill.ADMISSION_CODE));
        assertEquals(ConceptUuids.SYMPTOM_ONSET_DATE_UUID, Utils.expandUuid(FormPrefill.ONSET_CODE));
    }

    @Test public void presetConceptCode() {
        assertEquals("8001640", Preset.conceptCode("8001640^Date and time of admission^99DCT"));
        assertNull(Preset.conceptCode(null));
        assertNull(Preset.conceptCode(""));
        assertNull(Preset.conceptCode("8001640"));
    }
}
