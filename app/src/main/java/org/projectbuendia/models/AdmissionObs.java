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

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import static org.projectbuendia.client.utils.Utils.eq;

/**
 * Chooses which admission time observation (concept 1640) counts for a patient.
 * Registration records one, stamped with the registration time; the Admission form
 * records the date the clinician entered.  The form's date wins even when the form's
 * time is earlier than the registration (a backdated form, or a tablet clock behind
 * the server's).  The server's DataHelper.chooseAdmissionObs applies the same rule
 * to the printout; keep the two in step.
 */
public class AdmissionObs {
    /**
     * Returns the latest admission time recorded outside registration, or the
     * registration's own when there is no other.  Registration is the only writer that
     * puts an admission time and a placement in one encounter.  "Latest" orders by
     * observation time, then by the admission time entered, then by UUID.
     */
    public static @Nullable Obs choose(Iterable<Obs> observations) {
        Set<String> placementEncounters = new HashSet<>();
        for (Obs obs : observations) {
            if (eq(obs.conceptUuid, ConceptUuids.PLACEMENT_UUID) && obs.encounterUuid != null) {
                placementEncounters.add(obs.encounterUuid);
            }
        }
        Obs registration = null;
        Obs form = null;
        for (Obs obs : observations) {
            if (!eq(obs.conceptUuid, ConceptUuids.ADMISSION_DATETIME_UUID)) continue;
            if (obs.encounterUuid != null && placementEncounters.contains(obs.encounterUuid)) {
                registration = later(registration, obs);
            } else {
                form = later(form, obs);
            }
        }
        return form != null ? form : registration;
    }

    private static Obs later(@Nullable Obs a, Obs b) {
        return a == null || compare(b, a) > 0 ? b : a;
    }

    private static int compare(Obs a, Obs b) {
        int result = a.time.compareTo(b.time);
        if (result == 0) result = Long.compare(valueMillis(a), valueMillis(b));
        if (result == 0) result = a.uuid.compareTo(b.uuid);
        return result;
    }

    /** The admission time entered, in millis; a missing or unreadable value ranks lowest. */
    private static long valueMillis(Obs obs) {
        try {
            return Long.parseLong(obs.value);
        } catch (NumberFormatException e) {
            return Long.MIN_VALUE;
        }
    }
}
