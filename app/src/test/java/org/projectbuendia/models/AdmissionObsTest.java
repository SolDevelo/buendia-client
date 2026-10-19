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
import org.junit.Test;
import org.projectbuendia.client.json.Datatype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class AdmissionObsTest {
    private static final DateTime REGISTERED = new DateTime(2026, 10, 19, 10, 0);
    private static final DateTime ADMITTED = new DateTime(2026, 10, 16, 9, 0);

    private static Obs admission(String uuid, String encounterUuid, DateTime time, DateTime value) {
        return admission(uuid, encounterUuid, time, value == null ? null : "" + value.getMillis());
    }

    private static Obs admission(String uuid, String encounterUuid, DateTime time, String value) {
        return new Obs(uuid, encounterUuid, "patient", "provider",
            ConceptUuids.ADMISSION_DATETIME_UUID, Datatype.DATETIME, time, null, value, null);
    }

    private static Obs placement(String encounterUuid, DateTime time) {
        return new Obs("p-" + encounterUuid, encounterUuid, "patient", "provider",
            ConceptUuids.PLACEMENT_UUID, Datatype.TEXT, time, null, "zone/1", null);
    }

    /** The two obs registration records, in one encounter. */
    private static List<Obs> registration() {
        return new ArrayList<>(Arrays.asList(
            admission("reg", "e-reg", REGISTERED, REGISTERED),
            placement("e-reg", REGISTERED)));
    }

    @Test public void registrationOnly_choosesRegistration() {
        List<Obs> obs = registration();
        assertSame(obs.get(0), AdmissionObs.choose(obs));
    }

    @Test public void noAdmissionTime_choosesNothing() {
        assertNull(AdmissionObs.choose(Collections.singletonList(placement("e1", REGISTERED))));
    }

    @Test public void formAfterRegistration_choosesForm() {
        List<Obs> obs = registration();
        Obs form = admission("f1", "e-f1", REGISTERED.plusMinutes(5), ADMITTED);
        obs.add(form);
        assertSame(form, AdmissionObs.choose(obs));
    }

    @Test public void formBackdatedBeforeRegistration_choosesForm() {
        List<Obs> obs = registration();
        Obs form = admission("f1", "e-f1", ADMITTED, ADMITTED);
        obs.add(0, form);
        assertSame(form, AdmissionObs.choose(obs));
    }

    @Test public void twoForms_choosesTheLaterForm() {
        List<Obs> obs = registration();
        Obs first = admission("f1", "e-f1", ADMITTED, ADMITTED);
        Obs correction = admission("f2", "e-f2", REGISTERED.minusMinutes(1), ADMITTED.plusDays(1));
        obs.add(correction);
        obs.add(0, first);
        assertSame(correction, AdmissionObs.choose(obs));
    }

    @Test public void formNotYetSynced_isNotTakenForRegistration() {
        // A form saved on the tablet has no encounter UUID until the next sync;
        // a placement without one must not pair with it.
        List<Obs> obs = registration();
        Obs form = admission(null, null, ADMITTED, ADMITTED);
        obs.add(form);
        obs.add(placement(null, ADMITTED));
        assertSame(form, AdmissionObs.choose(obs));
    }

    @Test public void tieOnTime_choosesTheLaterAdmissionTime() {
        Obs early = admission("f2", "e-f2", ADMITTED, ADMITTED);
        Obs late = admission("f1", "e-f1", ADMITTED, ADMITTED.plusDays(1));
        assertSame(late, AdmissionObs.choose(Arrays.asList(early, late)));
        assertSame(late, AdmissionObs.choose(Arrays.asList(late, early)));
    }

    @Test public void tieOnTimeAndValue_choosesTheHigherUuid() {
        Obs a = admission("a", "e-a", ADMITTED, ADMITTED);
        Obs b = admission("b", "e-b", ADMITTED, ADMITTED);
        assertSame(b, AdmissionObs.choose(Arrays.asList(a, b)));
        assertSame(b, AdmissionObs.choose(Arrays.asList(b, a)));
    }

    @Test public void tieBetweenTwoUnsyncedForms_doesNotFail() {
        // Forms saved on the tablet carry no UUID until the next sync; Model stores it as "".
        Obs first = admission(null, null, ADMITTED, ADMITTED);
        Obs second = admission(null, null, ADMITTED, ADMITTED);
        assertSame(first, AdmissionObs.choose(Arrays.asList(first, second)));
    }

    @Test public void missingValue_ranksLowestWithoutFailing() {
        Obs empty = admission("b", "e-b", ADMITTED, (String) null);
        Obs filled = admission("a", "e-a", ADMITTED, ADMITTED);
        assertSame(filled, AdmissionObs.choose(Arrays.asList(empty, filled)));
    }
}
