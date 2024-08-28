/**
 * -----------------------------------------------------------------------
 *     Copyright (C) 2011 LM Ericsson Limited.  All rights reserved.
 * -----------------------------------------------------------------------
 */
package com.ericsson.eniq.events.server.resources.piechart;

import static com.ericsson.eniq.events.server.common.ApplicationConstants.*;

import com.ericsson.eniq.events.server.common.TechPackData;

/**
 * A Utility class that holds the constants used in #CauseCodeAnalysisPieChartResource
 * 
 * @author eavidat
 * @since 2011
 *
 */
public class CauseCodeAnalysisPieChartConstants {

    static final String SGEH_CAUSE_CODE = "ccRefForSgeh";

    static final String LTE_CAUSE_CODE = "ccRefForLte";

    static final String SGEH_SUB_CAUSE_CODE = "sccRefForSgeh";

    static final String LTE_SUB_CAUSE_CODE = "sccRefForLte";

    static final String SGEH_CAUSE_PROT_TYPE = "cptRefForSgeh";

    static final String LTE_CAUSE_PROT_TYPE = "cptRefForLte";

    static final String CAUSE_CODE_IDS = "causeCodeIds";

    static final String IS_GROUP = "isgroup";

    static final String GROUP_TABLE_NAME = "groupTable";

    static final String GROUP_COLUMN_NAME = "groupnameColumn";

    static final String JOIN_KEYS = "joinKeys";

    static final String tableSGEH = TechPackData.DIM_E_SGEH + UNDERSCORE;

    static final String tableLTE = TechPackData.DIM_E_LTE + UNDERSCORE;

    static final String causecodeTableSGEH = tableSGEH + CAUSE_CODE.toUpperCase();

    static final String causecodeTableLTE = tableLTE + CAUSE_CODE.toUpperCase();

    static final String subCausecodeTableSGEH = tableSGEH + SUB_CAUSE_CODE.toUpperCase();

    static final String subCausecodeTableLTE = tableLTE + SUB_CAUSE_CODE.toUpperCase();

    static final String causeProtoTypeTableSGEH = tableSGEH + TYPE_CAUSE_PROT_TYPE;

    static final String causeProtoTypeTableLTE = tableLTE + TYPE_CAUSE_PROT_TYPE;

    static final String templateParameterEventsQuery = "QUERY_PART_EVENTS";

    static final String templateParameterSubscribersQuery = "QUERY_PART_SUBSCRIBERS";
}