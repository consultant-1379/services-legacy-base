/**
 * -----------------------------------------------------------------------
 *     Copyright (C) 2011 LM Ericsson Limited.  All rights reserved.
 * -----------------------------------------------------------------------
 */
package com.ericsson.eniq.events.server.resources;

import static com.ericsson.eniq.events.server.common.ApplicationConstants.*;
import static com.ericsson.eniq.events.server.common.TechPackData.*;

import java.util.*;

/**
 * @author eavidat
 *
 */
public abstract class EventVolumeResourceConstants {

    final static Map<String, List<String>> columnsToIncludeInRAWView = new HashMap<String, List<String>>();

    final static Map<String, List<String>> columnsToIncludeInIMSICount = new HashMap<String, List<String>>();

    final static Map<String, List<String>> columnsToIncludeInIMSICountOneWeek = new HashMap<String, List<String>>();

    final static Map<String, List<String>> columnsToIncludeInAggViewErr = new HashMap<String, List<String>>();

    final static Map<String, List<String>> columnsToIncludeInAggViewSuc = new HashMap<String, List<String>>();

    final static String RAW_COLUMNS = "columnsToIncludeInRAWView";

    final static String IMSI_COLUMNS = "columnsToIncludeInIMSICount";

    final static String IMSI_COLUMNS_ONE_WEEK = "columnsToIncludeInIMSICountOneWeek";

    final static String AGG_ERR_COLUMNS = "columnsToIncludeInAggViewErr";

    final static String AGG_SUC_COLUMNS = "columnsToIncludeInAggViewSuc";

    private final static String NO_OF_ERRORS = "NO_OF_ERRORS";

    private final static String NO_OF_SUCCESSES = "NO_OF_SUCCESSES";

    final static String LOCAL_DATE_ID = "LOCAL_DATE_ID";

    final static String NO_TYPE = "NO_TYPE";

    final static String IS_NODE_TYPE = "isTypeNode";

    final static String IMSI_COUNT_TABLES = "errTablesToUseInIMSICount";

    static enum QueryType {
        RAW_VIEW_QUERY, IMSI_COUNT_QUERY, AGG_ERR, AGG_SUC
    }

    static {
        columnsToIncludeInRAWView.put(TYPE_APN, getColumns(TYPE_APN, QueryType.RAW_VIEW_QUERY));
        columnsToIncludeInRAWView.put(TYPE_SGSN, getColumns(TYPE_SGSN, QueryType.RAW_VIEW_QUERY));
        columnsToIncludeInRAWView.put(TYPE_BSC, getColumns(TYPE_BSC, QueryType.RAW_VIEW_QUERY));
        columnsToIncludeInRAWView.put(TYPE_CELL, getColumns(TYPE_CELL, QueryType.RAW_VIEW_QUERY));
        columnsToIncludeInRAWView.put(NO_TYPE, getColumns(NO_TYPE, QueryType.RAW_VIEW_QUERY));

        columnsToIncludeInIMSICount.put(TYPE_APN, getColumns(TYPE_APN, QueryType.IMSI_COUNT_QUERY));
        columnsToIncludeInIMSICount.put(TYPE_SGSN, getColumns(TYPE_SGSN, QueryType.IMSI_COUNT_QUERY));
        columnsToIncludeInIMSICount.put(TYPE_BSC, getColumns(TYPE_BSC, QueryType.IMSI_COUNT_QUERY));
        columnsToIncludeInIMSICount.put(TYPE_CELL, getColumns(TYPE_CELL, QueryType.IMSI_COUNT_QUERY));
        columnsToIncludeInIMSICount.put(NO_TYPE, getColumns(NO_TYPE, QueryType.IMSI_COUNT_QUERY));

        columnsToIncludeInAggViewErr.put(TYPE_APN, getColumns(TYPE_APN, QueryType.AGG_ERR));
        columnsToIncludeInAggViewErr.put(TYPE_SGSN, getColumns(TYPE_SGSN, QueryType.AGG_ERR));
        columnsToIncludeInAggViewErr.put(TYPE_BSC, getColumns(TYPE_BSC, QueryType.AGG_ERR));
        columnsToIncludeInAggViewErr.put(TYPE_CELL, getColumns(TYPE_CELL, QueryType.AGG_ERR));
        columnsToIncludeInAggViewErr.put(NO_TYPE, getColumns(NO_TYPE, QueryType.AGG_ERR));

        columnsToIncludeInAggViewSuc.put(TYPE_APN, getColumns(TYPE_APN, QueryType.AGG_SUC));
        columnsToIncludeInAggViewSuc.put(TYPE_SGSN, getColumns(TYPE_SGSN, QueryType.AGG_SUC));
        columnsToIncludeInAggViewSuc.put(TYPE_BSC, getColumns(TYPE_BSC, QueryType.AGG_SUC));
        columnsToIncludeInAggViewSuc.put(TYPE_CELL, getColumns(TYPE_CELL, QueryType.AGG_SUC));
        columnsToIncludeInAggViewSuc.put(NO_TYPE, getColumns(NO_TYPE, QueryType.AGG_SUC));

        columnsToIncludeInIMSICountOneWeek.put(TYPE_APN, getColumnsWeekQuery(TYPE_APN, QueryType.IMSI_COUNT_QUERY));
        columnsToIncludeInIMSICountOneWeek.put(TYPE_SGSN, getColumnsWeekQuery(TYPE_SGSN, QueryType.IMSI_COUNT_QUERY));
        columnsToIncludeInIMSICountOneWeek.put(TYPE_BSC, getColumnsWeekQuery(TYPE_BSC, QueryType.IMSI_COUNT_QUERY));
        columnsToIncludeInIMSICountOneWeek.put(TYPE_CELL, getColumnsWeekQuery(TYPE_CELL, QueryType.IMSI_COUNT_QUERY));
        columnsToIncludeInIMSICountOneWeek.put(NO_TYPE, getColumnsWeekQuery(NO_TYPE, QueryType.IMSI_COUNT_QUERY));

    }

    private static List<String> getColumns(final String type, final QueryType queryType) {
        final List<String> columns = new ArrayList<String>();
        if (!type.equalsIgnoreCase(NO_TYPE)) {
            columns.addAll(aggregationColumns.get(type));
        }
        columns.add(DATETIME_ID);
        if (QueryType.RAW_VIEW_QUERY == queryType) {
            columns.add(EVENT_ID_SQL_PARAM);
        } else if (QueryType.IMSI_COUNT_QUERY == queryType) {
            columns.add(IMSI_PARAM_UPPER_CASE);
        } else if (QueryType.AGG_ERR == queryType) {
            columns.add(EVENT_ID_SQL_PARAM);
            columns.add(NO_OF_ERRORS);
        } else if (QueryType.AGG_SUC == queryType) {
            columns.add(EVENT_ID_SQL_PARAM);
            columns.add(NO_OF_SUCCESSES);
        }
        return columns;
    }

    private static List<String> getColumnsWeekQuery(final String type, final QueryType queryType) {
        final List<String> columns = new ArrayList<String>();
        if (!type.equalsIgnoreCase(NO_TYPE)) {
            columns.addAll(aggregationColumns.get(type));
        }
        columns.add(LOCAL_DATE_ID);
        if (QueryType.RAW_VIEW_QUERY == queryType) {
            columns.add(EVENT_ID_SQL_PARAM);
        } else if (QueryType.IMSI_COUNT_QUERY == queryType) {
            columns.add(IMSI_PARAM_UPPER_CASE);
        } else if (QueryType.AGG_ERR == queryType) {
            columns.add(EVENT_ID_SQL_PARAM);
            columns.add(NO_OF_ERRORS);
        } else if (QueryType.AGG_SUC == queryType) {
            columns.add(EVENT_ID_SQL_PARAM);
            columns.add(NO_OF_SUCCESSES);
        }
        return columns;
    }
}