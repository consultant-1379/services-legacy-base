/**
 * -----------------------------------------------------------------------
 *     Copyright (C) 2011 LM Ericsson Limited.  All rights reserved.
 * -----------------------------------------------------------------------
 */
package com.ericsson.eniq.events.server.resources.piechart;

import static com.ericsson.eniq.events.server.common.ApplicationConstants.*;
import static com.ericsson.eniq.events.server.common.MessageConstants.*;
import static com.ericsson.eniq.events.server.common.TechPackData.*;
import static com.ericsson.eniq.events.server.resources.piechart.CauseCodeAnalysisPieChartConstants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;

import com.ericsson.eniq.events.server.common.EventDataSourceType;
import com.ericsson.eniq.events.server.common.Group;
import com.ericsson.eniq.events.server.common.TechPackData;
import com.ericsson.eniq.events.server.common.tablesandviews.AggregationTableInfo;
import com.ericsson.eniq.events.server.common.tablesandviews.TechPackTables;
import com.ericsson.eniq.events.server.resources.BaseResource;
import com.ericsson.eniq.events.server.utils.FormattedDateTimeRange;
import com.ericsson.eniq.events.server.utils.json.JSONUtils;

/**
 * This is the main resource class for cause code pie chart analysis 
 * which is used by the API class #CauseCodeAnalysisPieChartAPI
 * 
 * @author eavidat
 * @since 2011
 *
 */
@Stateless
//@TransactionManagement(TransactionManagementType.BEAN)
@LocalBean
public class CauseCodeAnalysisPieChartResource extends BaseResource {

    private static final List<String> listOfTechPacks = new ArrayList<String>();

    private FormattedDateTimeRange dateTimeRange = null;

    private String timerange = null;

    private boolean useAggregations = false;

    static {
        listOfTechPacks.add(TechPackData.EVENT_E_SGEH);
        listOfTechPacks.add(TechPackData.EVENT_E_LTE);
    }

    /**
     * Constructor : it initialises the aggregationViews with possible aggregations for different types
     */
    public CauseCodeAnalysisPieChartResource() {
        aggregationViews = new HashMap<String, AggregationTableInfo>();
        aggregationViews.put(TYPE_APN, new AggregationTableInfo(APN_CC_SCC, EventDataSourceType.AGGREGATED_DAY));
        aggregationViews.put(TYPE_BSC, new AggregationTableInfo(VEND_HIER3_CC_SCC,
                EventDataSourceType.AGGREGATED_15MIN, EventDataSourceType.AGGREGATED_DAY));
        aggregationViews.put(TYPE_CELL, new AggregationTableInfo(VEND_HIER321_CC_SCC));
        aggregationViews.put(TYPE_SGSN, new AggregationTableInfo(EVNTSRC_CC_SCC, EventDataSourceType.AGGREGATED_1MIN,
                EventDataSourceType.AGGREGATED_15MIN, EventDataSourceType.AGGREGATED_DAY));
    }

    /**
     * Only public interface to the outside world.
     * This method returns the cause code pie chart analysis data
     * 
     * @param path the URL path
     * 
     * @return the result of cause code pie chart analysis
     */
    public String getResults(final String path) {
        final MultivaluedMap<String, String> requestParameters = getDecodedQueryParameters();
        if (!isValidValue(requestParameters)) {
            return JSONUtils.jsonErrorInputMsg();
        }
        final List<String> errors = checkParameters(requestParameters);
        if (!errors.isEmpty()) {
            return getErrorResponse(E_INVALID_OR_MISSING_PARAMS, errors);
        }

        final String requestId = httpHeaders.getRequestHeaders().getFirst(REQUEST_ID);
        final String type = requestParameters.getFirst(TYPE_PARAM);
        return getPieResults(requestId, requestParameters, path, type);
    }

    /**
     * This is the main method that does the main work : the input (from URL) -> output (as JSON)
     * This is a facade that uses other utility methods to perform the end-to-end work
     * 
     * @param requestId the ID that comes with each request header with the URL call
     * @param requestParameters the parameters from the URL
     * @param path the relative URL
     * @param type the input from URL
     * 
     * @return the query result in JSON format
     * 
     * @throws WebApplicationException
     */
    private String getPieResults(final String requestId, final MultivaluedMap<String, String> requestParameters,
            final String path, final String type) throws WebApplicationException {
        final Map<String, Object> templateParameters = new HashMap<String, Object>();

        updateTemplateWithTimeRangeInfo(templateParameters, requestParameters);
        updateTemplateWithGroupOrColumnInfo(templateParameters, requestParameters, type);
        updateTemplateWithPathSpecificInfo(templateParameters, requestParameters, path);
        if (updateTemplateWithTechPackInfo(templateParameters, type)) {
            return JSONUtils.JSONEmptySuccessResult();
        }
        final String query = getQuery(requestParameters, templateParameters, path);

        return getData(requestParameters, requestId, query);
    }

    /**
     * This private utility method maps the input type parameter to the group type
     * 
     * @param type the input from URL
     * 
     * @return the mapped group type
     */
    private String getGroupType(final String type) {
        if (TYPE_APN.equalsIgnoreCase(type)) {
            return GROUP_TYPE_APN;
        } else if (TYPE_BSC.equalsIgnoreCase(type)) {
            return GROUP_TYPE_HIER3;
        } else if (TYPE_CELL.equalsIgnoreCase(type)) {
            return GROUP_TYPE_HIER1;
        } else if (TYPE_SGSN.equalsIgnoreCase(type)) {
            return GROUP_TYPE_SGSN;
        }
        return null;
    }

    /**
     * This private utility method updates the template parameters with group or single node specific info
     *  
     * @param templateParameters the parameters used in template
     * @param requestParameters the parameters from the URL
     * @param type the input type parameter from URL
     */
    private void updateTemplateWithGroupOrColumnInfo(final Map<String, Object> templateParameters,
            final MultivaluedMap<String, String> requestParameters, final String type) {
        if (requestParameters.containsKey(GROUP_NAME_PARAM)) {
            final Map<String, Group> groupDefs = dataService.getGroupsForTemplates();
            final Group groupDef = groupDefs.get(getGroupType(type));
            templateParameters.put(GROUP_TABLE_NAME, groupDef.getTableName());
            templateParameters.put(GROUP_COLUMN_NAME, groupDef.getGroupNameColumn());
            templateParameters.put(JOIN_KEYS, groupDef.getGroupKeys());
            templateParameters.put(IS_GROUP, true);
        } else {
            templateParameters.put(COLUMNS, aggregationColumns.get(type));
            templateParameters.put(IS_GROUP, false);
        }
    }

    /**
     * This private utility method updates the template parameters with URL path specific info
     * 
     * @param templateParameters the parameters used in template
     * @param requestParameters the parameters from the URL
     * @param path the relative URL
     */
    private void updateTemplateWithPathSpecificInfo(final Map<String, Object> templateParameters,
            final MultivaluedMap<String, String> requestParameters, final String path) {
        templateParameters.put(SGEH_CAUSE_CODE, causecodeTableSGEH);
        templateParameters.put(LTE_CAUSE_CODE, causecodeTableLTE);

        if (path.endsWith(SUB_CAUSE_CODE_ANALYSIS)) {
            templateParameters.put(SGEH_CAUSE_PROT_TYPE, causeProtoTypeTableSGEH);
            templateParameters.put(LTE_CAUSE_PROT_TYPE, causeProtoTypeTableLTE);
            templateParameters.put(SGEH_SUB_CAUSE_CODE, subCausecodeTableSGEH);
            templateParameters.put(LTE_SUB_CAUSE_CODE, subCausecodeTableLTE);
        } else if (path.endsWith(CAUSE_CODE_ANALYSIS)) {
            templateParameters.put(CAUSE_CODE_IDS, prepareCCProtList(requestParameters.getFirst(CAUSE_CODE_IDS)));
        }
    }

    private String prepareCCProtList(String ccList) {
        String[] ccProt = ccList.split(",");
        String result="";
        for(int i=0;i<ccProt.length;i++){
            result=result+"'"+ccProt[i]+"'";
            if(i<ccProt.length-1)
                result=result+",";
        }
        return result;

    }
    
    /**
     * This private utility method updates the template parameters with techpack specific info
     *  
     * @param templateParameters the parameters used in template
     * @param type the input type parameter from URL
     * 
     * @return false if list of tables is empty
     */
    private boolean updateTemplateWithTechPackInfo(final Map<String, Object> templateParameters, final String type) {
        final TechPackTables techPackTables = getTechPackTablesOrViews(dateTimeRange, timerange, type, listOfTechPacks);
        useAggregations = shouldQueryUseAggregationView(type, timerange);
        templateParameters.put(USE_AGGREGATION_TABLES, useAggregations);
        templateParameters.put(RAW_ERR_TABLES, techPackTables.getErrTables());
        boolean hasRawTables = true;
        if (useAggregations) {
            hasRawTables = updateTemplateWithRAWTables(templateParameters, dateTimeRange, KEY_TYPE_ERR,
                    RAW_ALL_ERR_TABLES);
        }
        return techPackTables.shouldReportErrorAboutEmptyRawTables() || !hasRawTables;
    }

    /**
     * This private utility method updates the template parameters with time range specific info
     * 
     * @param templateParameters the parameters used in template
     * @param requestParameters the parameters from the URL
     */
    private void updateTemplateWithTimeRangeInfo(final Map<String, Object> templateParameters,
            final MultivaluedMap<String, String> requestParameters) {
        dateTimeRange = getAndCheckFormattedDateTimeRangeForDailyAggregation(requestParameters, listOfTechPacks);
        timerange = queryUtils.getEventDataSourceType(dateTimeRange).getValue();
        templateParameters.put(TIMERANGE_PARAM, timerange);
    }

    /**
     * This private utility method gets the actual query using the path and time range specific logic
     * 
     * @param templateParameters the parameters used in template
     * @param requestParameters the parameters from the URL
     * @param path the relative URL
     * 
     * @return the query that will be run in the DB
     */
    private String getQuery(final MultivaluedMap<String, String> requestParameters,
            final Map<String, Object> templateParameters, final String path) {
        if (useAggregations && (path.endsWith(CAUSE_CODE_ANALYSIS) || path.endsWith(SUB_CAUSE_CODE_ANALYSIS))) {

            final String queryEvents = templateUtils.getQueryFromTemplate(
                    getTemplate(path + PATH_SEPARATOR + EVENT_ANALYSIS, requestParameters, null), templateParameters);

            final String querySubscribers = templateUtils.getQueryFromTemplate(
                    getTemplate(path + PATH_SEPARATOR + IMPACTED_SUBSCRIBERS.toUpperCase(), requestParameters, null),
                    templateParameters);

            templateParameters.put(templateParameterEventsQuery, queryEvents);
            templateParameters.put(templateParameterSubscribersQuery, querySubscribers);

            return templateUtils.getQueryFromTemplate(
                    getTemplate(path + PATH_SEPARATOR + AGGREGATION_TABLES, requestParameters, null),
                    templateParameters);
        }

        return templateUtils.getQueryFromTemplate(getTemplate(path, requestParameters, null), templateParameters);
    }

    /**
     * This private utility method gets the result from the database and returns it in JSON format 
     * 
     * @param requestParameters the parameters from the URL
     * @param requestId the ID that comes with each request header with the URL call
     * @param query the SQL to be run to get the result
     * 
     * @return the result in JSON format
     */
    private String getData(final MultivaluedMap<String, String> requestParameters, final String requestId,
            final String query) {
        final FormattedDateTimeRange newDateTimeRange = getDateTimeRangeOfChartAndSummaryGrid(dateTimeRange, timerange,
                listOfTechPacks);
        final String tzOffset = requestParameters.getFirst(TZ_OFFSET);
        final String timeColumn = null;
        return this.dataService.getGridData(requestId, query,
                this.queryUtils.mapRequestParameters(requestParameters, newDateTimeRange), timeColumn, tzOffset,
                getLoadBalancingPolicy(requestParameters));
    }

    /* (non-Javadoc)
     * @see com.ericsson.eniq.events.server.resources.BaseResource#isValidValue(javax.ws.rs.core.MultivaluedMap)
     */
    @Override
    protected boolean isValidValue(final MultivaluedMap<String, String> requestParameters) {
        if (requestParameters.containsKey(NODE_PARAM) || requestParameters.containsKey(GROUP_NAME_PARAM)) {
            if (!queryUtils.checkValidValue(requestParameters)) {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see com.ericsson.eniq.events.server.resources.BaseResource#getData(java.lang.String, javax.ws.rs.core.MultivaluedMap)
     */
    @Override
    protected String getData(final String requestId, final MultivaluedMap<String, String> requestParameters)
            throws WebApplicationException {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see com.ericsson.eniq.events.server.resources.BaseResource#checkParameters(javax.ws.rs.core.MultivaluedMap)
     */
    @Override
    protected List<String> checkParameters(final MultivaluedMap<String, String> requestParameters) {
        final List<String> errors = new ArrayList<String>();

        if (getGroupType(requestParameters.getFirst(TYPE_PARAM)) == null) {
            errors.add(TYPE_PARAM);
        }
        return errors;
    }
}