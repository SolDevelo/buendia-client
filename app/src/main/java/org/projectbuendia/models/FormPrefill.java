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

import org.joda.time.LocalDate;
import org.projectbuendia.client.utils.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * The dates a form opens pre-filled with: the admission date and the date of onset of
 * symptoms, as the chart shows them.  Without them, a second Admission form submitted
 * untouched would record today in both, and its admission date would win (AdmissionObs).
 */
public class FormPrefill {
    /** Concept codes as the xforms carry them ("8001640^Label^99DCT"); see Utils.expandUuid. */
    static final String ADMISSION_CODE = "8001640";
    static final String ONSET_CODE = "6001730";

    /**
     * Returns the pre-fill values for a form, keyed by concept code, as epoch millis.
     * @param latest the chart's latest observation per concept (ChartDataHelper.getLatestObservations,
     *               whose admission date is the one AdmissionObs chooses)
     */
    public static Map<String, Long> dates(Map<String, Obs> latest) {
        Map<String, Long> result = new HashMap<>();
        Obs admission = latest.get(ConceptUuids.ADMISSION_DATETIME_UUID);
        if (admission != null && admission.value != null) {
            try {
                result.put(ADMISSION_CODE, Long.parseLong(admission.value));
            } catch (NumberFormatException e) { /* not pre-filled */ }
        }
        Obs onset = latest.get(ConceptUuids.SYMPTOM_ONSET_DATE_UUID);
        LocalDate onsetDate = onset != null ? Utils.toLocalDate(onset.value) : null;
        if (onsetDate != null) {
            result.put(ONSET_CODE, onsetDate.toDateTimeAtStartOfDay().getMillis());
        }
        return result;
    }
}
