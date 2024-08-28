/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2014
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.eniq.events.server.utils;

import static com.ericsson.eniq.events.server.common.ApplicationConstants.*;
import static com.ericsson.eniq.events.server.common.utils.JSONUtilsConstants.*;

import com.ericsson.eniq.events.server.json.JSONArray;
import com.ericsson.eniq.events.server.json.JSONException;
import com.ericsson.eniq.events.server.json.JSONObject;

public final class SubsessionBIUtils {
    
    private SubsessionBIUtils() {
    }

    /**
     * @param busyKey
     * @return
     */
    public static boolean isBusyType(final String busyKey) {
        return busyKey != null;
    }

    /**
     * @param displayType
     * @return
     */
    public static boolean isGrid(final String displayType) {
        return GRID_PARAM.equals(displayType);
    }

    /**
     * @param displayType
     * @return
     */
    public static boolean isChart(final String displayType) {
        return CHART_PARAM.equals(displayType);
    }

    /**
     * This method is implemented to intercept the JSON result in order to
     * display two time columns of subscriber details response in local time format.
     *
     * @param result the JSON result intercepted by this function
     * @param tzOffset the timezone offset
     * @param timeColumns the time columns
     * @return the replaced JSON with proper time display
     * @throws JSONException the JSON exception
     */
    public static String updateTimeWithTimeZoneOffset(final String result, final String tzOffset,
            final String[] timeColumns) throws JSONException {

        if (result == null) {
            return result;
        }

        final JSONObject metaDataJsonObject = new JSONObject(result);
        if (metaDataJsonObject.has(DATA)) {
            final JSONArray tabElements = (JSONArray) metaDataJsonObject.get(DATA);
            for (int index = 0; index < tabElements.length(); index++) {
                final JSONObject tabElement = (JSONObject) tabElements.get(index);

                for (final String timeColumn : timeColumns) {
                    final String oldValue = (String) tabElement.get(timeColumn);
                    final String newValue = DateTimeUtils.getLocalTime(oldValue, tzOffset, RECEIVED_DATE_FORMAT);
                    tabElement.put(timeColumn, newValue);
                }
            }
        }
        return metaDataJsonObject.toString();
    }

    /**
     * This method is used during SUBBI Hour drilldown to
     * update the HOUR_PARAM with correct time offset.
     *
     * @param requestParameters the request parameters
     */
    public static String getHourParameterWithTZOffset(final String hourString, final String tzOffset) {
        final String tzSign;
        if (tzOffset.substring(0, 1).equals("-")) {
            tzSign = "-";
        } else {
            tzSign = "";
        }
        final int hour = (Integer.parseInt(hourString) + 24 - Integer.parseInt(tzSign + tzOffset.substring(1, 3))) % 24;
        return "" + hour;
    }

    /**
     * This method is used during SUBBI Busy offset calculation.
     *
     * @param tzOffset the time zone offset
     * @return the TZ offset minutes
     */
    public static int getTZOffsetMinutes(final String tzOffset) {
        final String tzSign;
        if (tzOffset.substring(0, 1).equals("-")) {
            tzSign = "-";
        } else {
            tzSign = "";
        }

        final int minutes = 60 * Integer.parseInt(tzOffset.substring(1, 3))
                + Integer.parseInt(tzOffset.substring(3, 5));

        if (tzSign.equalsIgnoreCase("-")) {
            return -minutes;
        }
        return minutes;
    }
}
