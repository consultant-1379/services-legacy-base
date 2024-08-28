/**
 * -----------------------------------------------------------------------
 *     Copyright (C) 2011 LM Ericsson Limited.  All rights reserved.
 * -----------------------------------------------------------------------
 */
package com.ericsson.eniq.events.server.resources;

import static com.ericsson.eniq.events.server.common.ApplicationConstants.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to hold some of the meta data for QOSStatistics
 * @author eemecoy
 *
 */
public class QOSStatisticsResourceConstants {

    final static List<String> columnsRequiredFromAllRawTables = new ArrayList<String>();

    final static List<String> columnsRequiredFromRawSucTablesForAllQueries = new ArrayList<String>();

    final static List<String> columnsRequiredFromRawErrTablesForAllQueries = new ArrayList<String>();

    static final String QCI_ERR_1 = "QCI_ERR_1";

    static final String QCI_ERR_2 = "QCI_ERR_2";

    static final String QCI_ERR_3 = "QCI_ERR_3";

    static final String QCI_ERR_4 = "QCI_ERR_4";

    static final String QCI_ERR_5 = "QCI_ERR_5";

    static final String QCI_ERR_6 = "QCI_ERR_6";

    static final String QCI_ERR_7 = "QCI_ERR_7";

    static final String QCI_ERR_8 = "QCI_ERR_8";

    static final String QCI_ERR_9 = "QCI_ERR_9";

    static final String QCI_ERR_10 = "QCI_ERR_10";

    static final String QCI_SUC_1 = "QCI_SUC_1";

    static final String QCI_SUC_2 = "QCI_SUC_2";

    static final String QCI_SUC_3 = "QCI_SUC_3";

    static final String QCI_SUC_4 = "QCI_SUC_4";

    static final String QCI_SUC_5 = "QCI_SUC_5";

    static final String QCI_SUC_6 = "QCI_SUC_6";

    static final String QCI_SUC_7 = "QCI_SUC_7";

    static final String QCI_SUC_8 = "QCI_SUC_8";

    static final String QCI_SUC_9 = "QCI_SUC_9";

    static final String QCI_SUC_10 = "QCI_SUC_10";

    static {
        columnsRequiredFromRawErrTablesForAllQueries.add(QCI_ERR_1);
        columnsRequiredFromRawErrTablesForAllQueries.add(QCI_ERR_2);
        columnsRequiredFromRawErrTablesForAllQueries.add(QCI_ERR_3);
        columnsRequiredFromRawErrTablesForAllQueries.add(QCI_ERR_4);
        columnsRequiredFromRawErrTablesForAllQueries.add(QCI_ERR_5);
        columnsRequiredFromRawErrTablesForAllQueries.add(QCI_ERR_6);
        columnsRequiredFromRawErrTablesForAllQueries.add(QCI_ERR_7);
        columnsRequiredFromRawErrTablesForAllQueries.add(QCI_ERR_8);
        columnsRequiredFromRawErrTablesForAllQueries.add(QCI_ERR_9);
        columnsRequiredFromRawErrTablesForAllQueries.add(QCI_ERR_10);
        columnsRequiredFromRawSucTablesForAllQueries.add(QCI_SUC_1);
        columnsRequiredFromRawSucTablesForAllQueries.add(QCI_SUC_2);
        columnsRequiredFromRawSucTablesForAllQueries.add(QCI_SUC_3);
        columnsRequiredFromRawSucTablesForAllQueries.add(QCI_SUC_4);
        columnsRequiredFromRawSucTablesForAllQueries.add(QCI_SUC_5);
        columnsRequiredFromRawSucTablesForAllQueries.add(QCI_SUC_6);
        columnsRequiredFromRawSucTablesForAllQueries.add(QCI_SUC_7);
        columnsRequiredFromRawSucTablesForAllQueries.add(QCI_SUC_8);
        columnsRequiredFromRawSucTablesForAllQueries.add(QCI_SUC_9);
        columnsRequiredFromRawSucTablesForAllQueries.add(QCI_SUC_10);
        columnsRequiredFromAllRawTables.add(DATETIME_ID);
        columnsRequiredFromAllRawTables.add(IMSI);
    }

}
