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

package org.projectbuendia.client.ui.chart;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.projectbuendia.models.ObsValue;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/** The out-of-range colour rules a profile's "css class" column uses (see the Buendia profile guide). */
public class ObsFormatTest {
    // Temperature: normal 36-37.5, non-critical 37.6-39 (the fever band only, so no red on the low side).
    private static final String TEMP = "{1,select,<36:abnormal;>39:critical;>37.5:abnormal}";
    // Potassium: normal 3.8-5, non-critical 2.4-6.6 (critical before abnormal on both sides).
    private static final String K = "{1,select,<2.4:critical;<3.8:abnormal;>6.6:critical;>5:abnormal}";
    // A lab with only a normal range (Hb 10.5-18.5).
    private static final String HB = "{1,select,<10.5:abnormal;>18.5:abnormal}";

    private Locale savedLocale;

    // Decimals follow the app's locale (App.java sets the default): pin it so "3.75" does not become "3,75".
    @Before public void pinLocale() {
        savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After public void restoreLocale() {
        Locale.setDefault(savedLocale);
    }

    private static String formatted(String rule, Double value) {  // applies a format as the chart page does
        List<ObsValue> values = Collections.singletonList(
            value == null ? null : ObsValue.newNumber(value));
        return PebbleExtension.formatValues(values, new ObsFormat(rule));
    }

    @Test public void tempRuleColoursEachBand() {
        assertEquals("abnormal", formatted(TEMP, 35.9));
        assertEquals("", formatted(TEMP, 36.0));
        assertEquals("", formatted(TEMP, 37.5));
        assertEquals("abnormal", formatted(TEMP, 37.55));
        assertEquals("abnormal", formatted(TEMP, 39.0));
        assertEquals("critical", formatted(TEMP, 39.1));
    }

    @Test public void potassiumRuleColoursBothSides() {
        assertEquals("critical", formatted(K, 2.3));
        assertEquals("abnormal", formatted(K, 2.4));
        assertEquals("abnormal", formatted(K, 3.7));
        assertEquals("", formatted(K, 3.8));
        assertEquals("", formatted(K, 5.0));
        assertEquals("abnormal", formatted(K, 5.1));
        assertEquals("abnormal", formatted(K, 6.6));
        assertEquals("critical", formatted(K, 6.7));
    }

    @Test public void labRuleWithoutNonCritical() {
        assertEquals("abnormal", formatted(HB, 10.4));
        assertEquals("", formatted(HB, 10.5));
        assertEquals("", formatted(HB, 18.5));
        assertEquals("abnormal", formatted(HB, 18.6));
    }

    @Test public void unobservedGetsNoClass() {
        assertEquals("", formatted(TEMP, null));
    }

    @Test public void emptyGridCellGetsNoColourClass() {
        // chart.html passes null for a grid cell with no points; the {1} argument is then missing.
        String css = PebbleExtension.formatValues(Collections.<ObsValue>emptyList(), new ObsFormat(TEMP));
        assertEquals("{1}", css);  // no CSS rule matches it, so the cell keeps its default look
    }

    @Test public void decimalFormatShowsTheValueTheColourIsFor() {
        // A number row with no format shows whole numbers (default "0"): K+ 3.75 would read "4" in orange.
        assertEquals("4", formatted("0", 3.75));
        assertEquals("3.75", formatted("0.##", 3.75));
        assertEquals("9", formatted("0.##", 9.0));
        assertEquals("0.45", formatted("0.##", 0.45));
    }

    @Test public void bareDefaultBranchAlsoMatchesUnobserved() {
        // Why a profile rule must not end in a bare default: it colours cells with no observation.
        assertEquals("critical", formatted("{1,select,<=39:;critical}", null));
    }
}
