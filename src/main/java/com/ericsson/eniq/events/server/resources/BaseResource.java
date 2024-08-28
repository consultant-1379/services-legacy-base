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
package com.ericsson.eniq.events.server.resources;

import static com.ericsson.eniq.events.server.common.ApplicationConstants.*;
import static com.ericsson.eniq.events.server.common.MessageConstants.*;
import static com.ericsson.eniq.events.server.logging.performance.ServicesPerformanceThreadLocalHolder.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import com.ericsson.eniq.events.server.common.*;
import com.ericsson.eniq.events.server.common.exception.ServiceException;
import com.ericsson.eniq.events.server.common.tablesandviews.*;
import com.ericsson.eniq.events.server.datasource.loadbalancing.LoadBalancingPolicy;
import com.ericsson.eniq.events.server.logging.ServicesLogger;
import com.ericsson.eniq.events.server.logging.performance.ServicePerformanceTraceLogger;
import com.ericsson.eniq.events.server.logging.performance.ServicesPerformanceThreadLocalHolder;
import com.ericsson.eniq.events.server.query.QueryParameter;
import com.ericsson.eniq.events.server.query.TimeRangeSelector;
import com.ericsson.eniq.events.server.services.DataService;
import com.ericsson.eniq.events.server.services.StreamingDataService;
import com.ericsson.eniq.events.server.services.datatiering.DataTieringHandler;
import com.ericsson.eniq.events.server.services.exclusivetacs.ExclusiveTACHandler;
import com.ericsson.eniq.events.server.templates.mappingengine.TemplateMappingEngine;
import com.ericsson.eniq.events.server.templates.utils.TemplateUtils;
import com.ericsson.eniq.events.server.utils.*;
import com.ericsson.eniq.events.server.utils.config.ApplicationConfigManager;
import com.ericsson.eniq.events.server.utils.datetime.DateTimeHelper;
import com.ericsson.eniq.events.server.utils.json.JSONUtils;
import com.ericsson.eniq.events.server.utils.techpacks.*;

/**
 * @deprecated There is a new Framework for Services. Resources should now
 *             extend
 *             com.ericsson.eniq.events.server.resources.AbstractResource.
 *             Example can be seen in
 *             com.ericsson.eniq.events.server.resources.MultipleRankingResource
 *             . Please also see the wiki for more details.
 *             http://atrclin2.athtem.eei.ericsson.se/wiki/index.php/
 *             ENIQ_Events_WCDMA_Services_Rework
 */
@Deprecated
public abstract class BaseResource {

    protected Map<String, String> typesRestrictedToOneTechPack = new HashMap<String, String>();

    protected Map<String, AggregationTableInfo> aggregationViews = new HashMap<String, AggregationTableInfo>();

    @Context
    protected UriInfo uriInfo;

    @EJB
    protected ServicePerformanceTraceLogger performanceTrace;

    @EJB
    protected DataService dataService;

    @EJB
    protected StreamingDataService streamingDataService;

    @EJB
    protected ApplicationConfigManager applicationConfigManager;

    @Context
    protected HttpServletResponse response;

    @EJB
    protected QueryUtils queryUtils;

    @EJB
    protected TemplateUtils templateUtils;

    @EJB
    protected TemplateMappingEngine templateMappingEngine;

    @Context
    protected HttpHeaders httpHeaders;

    @EJB
    private AuditService auditService;

    @EJB
    private MediaTypeHandler mediaTypeHandler;

    @EJB
    private CSVResponseBuilder csvResponseBuilder;

    @EJB
    private LoadBalancingPolicyService loadBalancingPolicyService;

    @EJB
    private TechPackListFactory techPackListFactory;

    @EJB
    protected RawTableFetcher rawTableFetcher;

    @EJB
    private ExclusiveTACHandler exclusiveTACHandler;

    @EJB
    protected TimeRangeSelector timeRangeSelector;

    @EJB
    protected DateTimeHelper dateTimeHelper;

    @EJB
    protected DataTieringHandler dataTieringHandler;

    private static final int TEN_MINUTES_VALUE = 10;

    private static final int MINUTES_IN_1_HOURS = 60;

    private static final int MINUTES_IN_1_HOUR_30_MIN = 90;

    private static final int MINUTES_IN_2_HOURS = MINUTES_IN_1_HOURS * 2;

    private static final int MINUTES_IN_4_HOURS = MINUTES_IN_1_HOURS * 4;

    private static final int MINUTES_IN_6_HOURS = MINUTES_IN_1_HOURS * 6;

    private static final int MINUTES_IN_9_HOURS = MINUTES_IN_1_HOURS * 9;

    private static final int MINUTES_IN_18_HOURS = MINUTES_IN_1_HOURS * 18;

    private static final int MINUTES_IN_1_DAY = MINUTES_IN_12_HOURS * 2;

    private static final int MINUTES_IN_2_DAY = MINUTES_IN_1_DAY * 2;

    private static final int MINUTES_IN_4_DAY = MINUTES_IN_1_DAY * 4;

    private static final int MINUTES_IN_6_DAY = MINUTES_IN_1_DAY * 6;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getData() throws WebApplicationException {
        try {
            setRequestStartTime(Calendar.getInstance().getTimeInMillis());
            ServicesPerformanceThreadLocalHolder.setUriInfo(uriInfo
                    .getRequestUri().toString());
            final String requestId = httpHeaders.getRequestHeaders().getFirst(
                    REQUEST_ID);
            return getData(requestId, getDecodedQueryParameters());
        } finally {
            setRequestEndTime(Calendar.getInstance().getTimeInMillis());
            performanceTrace.detailed(Level.INFO, getContextInfo());
            releaseAllResources();
        }
    }

    protected MultivaluedMap<String, String> getDecodedQueryParameters() {
        return uriInfo.getQueryParameters(true);
    }

    @GET
    @Produces(MediaTypeConstants.APPLICATION_CSV)
    public Response getDataAsCSV() throws WebApplicationException {
        try {
            setRequestStartTime(Calendar.getInstance().getTimeInMillis());
            ServicesPerformanceThreadLocalHolder.setUriInfo(uriInfo
                    .getRequestUri().toString());
            final String requestId = httpHeaders.getRequestHeaders().getFirst(
                    REQUEST_ID);
            getData(requestId, getDecodedQueryParameters());
            return buildHttpResponseForCSVData();
        } finally {
            setRequestEndTime(Calendar.getInstance().getTimeInMillis());
            performanceTrace.detailed(Level.INFO, getContextInfo());
            releaseAllResources();
        }
    }

    protected abstract boolean isValidValue(
            MultivaluedMap<String, String> requestParameters);

    protected String getTemplate(final String pathName,
            final MultivaluedMap<String, String> requestParameters,
            final String drillType) {
        return templateMappingEngine.getTemplate(pathName, requestParameters,
                drillType);
    }

    protected String getTemplate(final String pathName,
            final MultivaluedMap<String, String> requestParameters,
            final String drillType, final String timerange,
            final boolean isDataTiered) {
        String groupName = null;
        if (requestParameters.containsKey(GROUP_NAME_PARAM)) {
            groupName = requestParameters.getFirst(GROUP_NAME_PARAM);
        }

        String tacParam = null;
        if (requestParameters.containsKey(TAC_PARAM)) {
            tacParam = requestParameters.getFirst(TAC_PARAM);
        }

        final String view = timeRangeSelector.getTimeRangeType(timerange,
                queryIsExclusiveTacRelated(groupName, tacParam), isDataTiered);
        return templateMappingEngine.getTemplate(pathName, requestParameters,
                drillType, view);
    }

    protected Response buildHttpResponseForCSVData() {
        return csvResponseBuilder.buildHttpResponseForCSVData();
    }

    protected abstract String getData(final String requestId,
            final MultivaluedMap<String, String> requestParameters)
            throws WebApplicationException;

    protected abstract List<String> checkParameters(
            final MultivaluedMap<String, String> requestParameters);

    protected String getNoSuchDisplayErrorResponse(final String displayType) {
        return getErrorResponse(E_NO_SUCH_DISPLAY_TYPE + " : " + displayType);
    }

    protected TechPackList createTechPackListWithMeasurementType(
            final List<String> techPackList,
            final FormattedDateTimeRange dateTimeRange,
            final List<String> measurementTypes) {
        return techPackListFactory.createTechPackListWithMeasuermentType(
                techPackList, measurementTypes, dateTimeRange,
                new AggregationTableInfo(NO_TABLE), SUC);
    }

    protected String getErrorResponse(final String message,
            final List<String> errors) {
        final StringBuilder sb = new StringBuilder();
        if (errors != null) {
            for (final String error : errors) {
                sb.append(error);
            }
        }
        return JSONUtils.createJSONErrorResult(message
                + (errors == null || errors.isEmpty() ? "" : " : "
                        + sb.toString()));
    }

    protected String getErrorResponse(final String message) {
        return JSONUtils.createJSONErrorResult(message);
    }

    /**
     * Pull out the time/date parameters from the URL parameters in
     * requestParameters, and pass these along to DateTimeRange to be converted
     * into a FormattedDateTimeRange.
     */
    protected FormattedDateTimeRange getFormattedDateTimeRange(
            final MultivaluedMap<String, String> requestParameters,
            final List<String> techPacks) {
        boolean isCSV = false;
        if (isMediaTypeApplicationCSV()) {
            isCSV = true;
        }
        /**
         * @param isCSV
         *            isCSV param is true when user click on export to CSV
         */
        final FormattedDateTimeRange dateTimeRange = DateTimeRange
                .getFormattedDateTimeRange(
                        requestParameters.getFirst(KEY_PARAM),
                        requestParameters.getFirst(TIME_QUERY_PARAM),
                        requestParameters.getFirst(TIME_FROM_QUERY_PARAM),
                        requestParameters.getFirst(TIME_TO_QUERY_PARAM),
                        requestParameters.getFirst(DATE_FROM_QUERY_PARAM),
                        requestParameters.getFirst(DATE_TO_QUERY_PARAM),
                        requestParameters.getFirst(DATA_TIME_FROM_QUERY_PARAM),
                        requestParameters.getFirst(DATA_TIME_TO_QUERY_PARAM),
                        requestParameters.getFirst(TZ_OFFSET),
                        applicationConfigManager
                                .getTimeDelayOneMinuteData(techPacks),
                        applicationConfigManager
                                .getTimeDelayFifteenMinuteData(techPacks),
                        applicationConfigManager.getTimeDelayDayData(techPacks),
                        isCSV);
        return dateTimeRange;
    }

    protected FormattedDateTimeRange getAndCheckFormattedDateTimeRangeForDailyAggregation(
            final MultivaluedMap<String, String> requestParameters,
            final List<String> techPacks) {
        FormattedDateTimeRange timerange = getFormattedDateTimeRange(
                requestParameters, techPacks);
        if (requestParameters.containsKey(TIME_QUERY_PARAM)
                && queryUtils.getEventDataSourceType(timerange).equals(
                        EventDataSourceType.AGGREGATED_DAY.getValue())) {
            timerange = DateTimeRange.getDailyAggregationTimeRangebyLocalTime(
                    requestParameters.getFirst(TIME_QUERY_PARAM),
                    applicationConfigManager
                            .getTimeDelayOneMinuteData(techPacks),
                    applicationConfigManager
                            .getTimeDelayFifteenMinuteData(techPacks),
                    applicationConfigManager.getTimeDelayDayData(techPacks));

        }
        return timerange;
    }

    public FormattedDateTimeRange getDateTimeRangeOfChartAndSummaryGrid(
            final FormattedDateTimeRange dateTimeRange, final String viewName,
            final List<String> techPacks) throws WebApplicationException {
        FormattedDateTimeRange newDateTimeRange = null;
        if (viewName.equals(EventDataSourceType.AGGREGATED_15MIN.getValue())) {
            newDateTimeRange = DateTimeRange.getFormattedDateTimeRange(
                    DateTimeRange.formattedDateTimeAgainst15MinsTable(
                            dateTimeRange.getStartDateTime(),
                            dateTimeRange.getMinutesOfStartDateTime()),
                    DateTimeRange.formattedDateTimeAgainst15MinsTable(
                            dateTimeRange.getEndDateTime(),
                            dateTimeRange.getMinutesOfEndDateTime()),
                    applicationConfigManager
                            .getTimeDelayOneMinuteData(techPacks),
                    applicationConfigManager
                            .getTimeDelayFifteenMinuteData(techPacks),
                    applicationConfigManager.getTimeDelayDayData(techPacks));
        } else if (viewName.equals(EventDataSourceType.AGGREGATED_DAY
                .getValue())) {
            newDateTimeRange = DateTimeRange.getFormattedDateTimeRange(
                    DateTimeRange.formattedDateTimeAgainstDayTable(
                            dateTimeRange.getStartDateTime(),
                            dateTimeRange.getMinutesOfStartDateTime()),
                    DateTimeRange.formattedDateTimeAgainstDayTable(
                            dateTimeRange.getEndDateTime(),
                            dateTimeRange.getMinutesOfEndDateTime()), 0, 0, 0);
        } else {
            newDateTimeRange = dateTimeRange;
        }
        return newDateTimeRange;
    }

    protected LoadBalancingPolicy getLoadBalancingPolicy(
            final MultivaluedMap<String, String> requestParameters) {
        return loadBalancingPolicyService
                .getLoadBalancingPolicy(requestParameters);
    }

    protected boolean isMediaTypeApplicationCSV() {
        return mediaTypeHandler.isMediaTypeApplicationCSV(httpHeaders);
    }

    public void setHttpHeaders(final HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    @Override
    public String toString() {
        return "BaseResource{" + "uriInfo=" + uriInfo.getRequestUri() + '}';
    }

    protected void updateTemplateParametersWithGroupDefinition(
            final Map<String, Object> templateParameters,
            final MultivaluedMap<String, String> requestParameters) {
        if (requestParameters.containsKey(GROUP_NAME_PARAM)) {
            final Map<String, Group> templateGroupDefs = dataService
                    .getGroupsForTemplates();
            templateParameters.put(GROUP_DEFINITIONS, templateGroupDefs);
            final String groupName = requestParameters
                    .getFirst(GROUP_NAME_PARAM);
            if (groupName == null || groupName.length() == 0) {
                throw new ServiceException(GROUP_NAME_PARAM + " undefined");
            }
        }
    }

    protected void updateTemplateParametersWithGroupDefinitionForHashId(
            final Map<String, Object> templateParameters,
            final MultivaluedMap<String, String> requestParameters) {
        if (requestParameters.containsKey(GROUP_NAME_PARAM)) {
            final Map<String, GroupHashId> templateGroupDefs = dataService
                    .getGroupsForTemplatesForHashId();
            templateParameters.put(GROUP_DEFINITIONS, templateGroupDefs);
            final String groupName = requestParameters
                    .getFirst(GROUP_NAME_PARAM);
            if (groupName == null || groupName.length() == 0) {
                throw new ServiceException(GROUP_NAME_PARAM + " undefined");
            }
        }
    }

    protected String checkRequiredParametersExistAndReturnErrorMessage(
            final MultivaluedMap<String, String> requestParameters,
            final String[] requiredParameters) {
        final List<String> errors = checkRequiredParametersExist(
                requestParameters, requiredParameters);
        if (!errors.isEmpty()) {
            return getErrorResponse(E_INVALID_OR_MISSING_PARAMS, errors);
        }
        return null;
    }

    protected List<String> checkRequiredParametersExist(
            final MultivaluedMap<String, String> requestParameters,
            final String requiredParam) {
        return checkRequiredParametersExist(requestParameters,
                new String[] { requiredParam });
    }

    protected List<String> checkRequiredParametersExist(
            final MultivaluedMap<String, String> requestParameters,
            final String[] requiredParameters) {
        final List<String> errors = new ArrayList<String>();
        for (final String requiredParameter : requiredParameters) {
            if (!requestParameters.containsKey(requiredParameter)) {
                errors.add(requiredParameter);
            }
        }
        return errors;
    }

    protected List<String> getRAWTables(
            final FormattedDateTimeRange newDateTimeRange, final String key,
            final String templateKey) {
        return rawTableFetcher.getRAWTables(newDateTimeRange, key, templateKey);

    }

    protected boolean updateTemplateWithRAWTables(
            final Map<String, Object> templateParameters,
            final FormattedDateTimeRange dateTimeRange, final String key,
            final String... templateKeys) {

        final List<String> totalRawtables = new ArrayList<String>();

        for (final String templateKey : templateKeys) {
            final List<String> rawtables = getRAWTables(dateTimeRange, key,
                    templateKey);
            totalRawtables.addAll(rawtables);
            templateParameters.put(templateKey, rawtables);
        }

        return !totalRawtables.isEmpty();
    }

    protected int getCountValue(
            final MultivaluedMap<String, String> requestParameters,
            final int maxAllowableSize) {
        return new RequestParametersWrapper(requestParameters)
                .getCountValue(maxAllowableSize);
    }

    protected void streamDataAsCSV(
            final MultivaluedMap<String, String> requestParameters,
            final String tzOffset, final String timeColumn, final String query,
            final FormattedDateTimeRange newDateTimeRange) {
        response.setContentType("application/csv");
        response.setHeader("Content-disposition",
                "attachment; filename=export.csv");
        try {
            this.streamingDataService.streamDataAsCsv(query,
                    mapQueryParameters(requestParameters, newDateTimeRange),
                    timeColumn, tzOffset,
                    getLoadBalancingPolicy(requestParameters),
                    response.getOutputStream());
        } catch (final IOException e) {
            ServicesLogger.error(getClass().getName(), "streamDataAsCSV", e);
        }
    }

    protected Map<String, QueryParameter> mapQueryParameters(
            final MultivaluedMap<String, String> requestParameters,
            final FormattedDateTimeRange newDateTimeRange) {
        return this.queryUtils.mapRequestParameters(requestParameters,
                newDateTimeRange);
    }

    protected void checkAndCreateFineAuditLogEntryForQuery(
            final MultivaluedMap<String, String> requestParameters,
            final String query, final FormattedDateTimeRange newDateTimeRange) {
        final Map<String, QueryParameter> queryParameters = this.queryUtils
                .mapRequestParameters(requestParameters, newDateTimeRange);
        auditService.logAuditEntryForQuery(uriInfo, requestParameters, query,
                queryParameters, httpHeaders);
    }

    protected void checkAndCreateFineAuditLogEntryForQuery(
            final MultivaluedMap<String, String> requestParameters,
            final List<String> queries,
            final FormattedDateTimeRange newDateTimeRange) {
        final Map<String, QueryParameter> queryParameters = this.queryUtils
                .mapRequestParameters(requestParameters, newDateTimeRange);
        auditService.logAuditEntryForQuery(uriInfo, requestParameters, queries,
                queryParameters, httpHeaders);
    }

    protected void checkAndCreateINFOAuditLogEntryForURI(
            final MultivaluedMap<String, String> requestParameters) {
        auditService.logAuditEntryForURI(uriInfo, requestParameters,
                httpHeaders);
    }

    public void setDataService(final DataService dataService) {
        this.dataService = dataService;
    }

    public void setApplicationConfigManager(
            final ApplicationConfigManager applicationConfigManager) {
        this.applicationConfigManager = applicationConfigManager;
    }

    public void setQueryUtils(final QueryUtils queryUtils) {
        this.queryUtils = queryUtils;
    }

    public void setTemplateUtils(final TemplateUtils templateUtils) {
        this.templateUtils = templateUtils;
    }

    protected String getMatchingDIMTechpack(final String techPackName) {
        return techPackListFactory.getMatchingDIMTechPack(techPackName);
    }

    protected TechPackTables getAggregationTables(final String type,
            final String timerange) {
        return getAggregationTables(type, timerange,
                TechPackData.completeSGEHTechPackList);
    }

    protected TechPackTables getDTPutAggregationTables(final String type,
            final String timerange) {
        return getDTPutAggregationTables(type, timerange,
                TechPackData.completeDVTPTechPackList);
    }

    protected TechPackTables getAggregationTables(final String type,
            final String timerange, final List<String> listOfTechPacks) {
        final TechPackTables techPackTables = new TechPackTables(
                TableType.AGGREGATION);
        final String errTime = ApplicationConstants
                .returnAggregateViewType(timerange);
        for (final String techPackName : listOfTechPacks) {
            if (isTechPackApplicableForType(type, techPackName)) {
                final TechPack techPack = new TechPack(techPackName,
                        TableType.AGGREGATION,
                        getMatchingDIMTechpack(techPackName));
                techPack.setErrAggregationView(techPackListFactory
                        .getErrorAggregationView(type, errTime, techPackName,
                                aggregationViews));
                final String sucTime = ViewTypeSelector
                        .returnSuccessAggregateViewType(EventDataSourceType
                                .getEventDataSourceType(timerange),
                                techPackName);
                techPack.setSucAggregationView(techPackListFactory
                        .getSuccessAggregationView(type, sucTime, techPackName,
                                aggregationViews));
                techPackTables.addTechPack(techPack);
            }
        }
        return techPackTables;
    }

    protected TechPackTables getDTPutAggregationTables(final String type,
            final String timerange, final List<String> listOfTechPacks) {
        final TechPackTables techPackTables = new TechPackTables(
                TableType.AGGREGATION);

        final String aggregationViewForQueryType = aggregationViews.get(type)
                .getAggregationView();
        final String time = ApplicationConstants
                .returnAggregateViewType(timerange);

        for (final String techPackName : listOfTechPacks) {
            if (isTechPackApplicableForType(type, techPackName)) {
                final TechPack techPack = new TechPack(techPackName,
                        TableType.AGGREGATION,
                        getMatchingDIMTechpack(techPackName));
                if (techPackName.equals(EVENT_E_DVTP_DT_TPNAME)) {
                    techPack.setDtAggregationView(techPackName + UNDERSCORE
                            + aggregationViewForQueryType + time);
                }
                techPackTables.addTechPack(techPack);
            }
        }
        return techPackTables;
    }

    protected String getTemplateKeyMatchingTechPack(final String techPack) {
        if (techPack.equals(EVENT_E_SGEH_TPNAME)) {
            return RAW_NON_LTE_TABLES;
        } else if (techPack.equals(EVENT_E_DVTP_DT_TPNAME)) {
            return RAW_DT_TABLES;
        }

        return RAW_LTE_TABLES;
    }

    protected TechPackTables getRawTables(final String type,
            final FormattedDateTimeRange dateTimeRange) {
        return getRawTables(type, dateTimeRange,
                TechPackData.completeSGEHTechPackList);
    }

    protected TechPackTables getDTPutRawTables(final String type,
            final FormattedDateTimeRange dateTimeRange) {
        return getDTPutRawTables(type, dateTimeRange,
                TechPackData.completeDVTPTechPackList);
    }

    protected TechPackTables getRawTables(final String type,
            final FormattedDateTimeRange dateTimeRange,
            final List<String> listOfTechPacks) {
        final TechPackTables techPackTables = new TechPackTables(TableType.RAW);
        for (final String techPackName : listOfTechPacks) {
            if (isTechPackApplicableForType(type, techPackName)) {
                final TechPack techPack = new TechPack(techPackName,
                        TableType.RAW, getMatchingDIMTechpack(techPackName));
                final List<String> errTables = rawTableFetcher.getRawErrTables(
                        dateTimeRange, techPackName);
                if (errTables.size() > 0) {
                    techPack.setErrRawTables(errTables);
                }
                final List<String> sucTables = rawTableFetcher.getRawSucTables(
                        dateTimeRange, techPackName);
                if (sucTables.size() > 0) {
                    techPack.setSucRawTables(sucTables);
                }
                if (techPack.hasAnyTables()) {
                    techPackTables.addTechPack(techPack);
                }
            }
        }
        return techPackTables;
    }

    public boolean isTechPackApplicableForType(final String type,
            final String techPack) {
        if (typesRestrictedToOneTechPack.containsKey(type)) {
            if (typesRestrictedToOneTechPack.get(type).equals(techPack)) {
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * Get DTPUT raw tables to use in a query. This method will look up the
     * engine or query the time range views to determine which raw tables are
     * applicable for this time range. Depends on the sub resource sub class
     * populating the aggregationViews map with the correct information for the
     * query/resource
     */
    protected TechPackTables getDTPutRawTables(final String type,
            final FormattedDateTimeRange dateTimeRange,
            final List<String> listOfTechPacks) {
        final TechPackTables techPackTables = new TechPackTables(TableType.RAW);
        for (final String techPackName : listOfTechPacks) {
            if (isTechPackApplicableForType(type, techPackName)) {
                final TechPack techPack = new TechPack(techPackName,
                        TableType.RAW, getMatchingDIMTechpack(techPackName));
                if (techPackName.equals(EVENT_E_DVTP_DT_TPNAME)) {
                    final List<String> dtTables = getRAWTables(dateTimeRange,
                            KEY_TYPE_DT,
                            getTemplateKeyMatchingTechPack(techPackName));
                    if (dtTables.size() > 0) {
                        techPack.setDtRawTables(dtTables);
                    }
                }
                if (techPack.hasAnyDTPutTables()) {
                    techPackTables.addTechPack(techPack);
                }
            }
        }
        return techPackTables;
    }

    protected void addColumnsForQueries(final String type,
            final Map<String, Object> templateParameters) {
        templateParameters.put(COLUMNS_FOR_QUERY,
                TechPackData.aggregationColumns.get(type));
        templateParameters.put(COLUMNS_FOR_DTPUT_RANKING_QUERY,
                TechPackData.dtAggregationColumns.get(type));
    }

    protected boolean shouldQueryUseAggregationView(final String type,
            final String timerange) {
        return techPackListFactory.shouldQueryUseAggregationView(type,
                timerange, aggregationViews);
    }

    boolean shouldQueryUseAggregationView(final String type,
            final String timerange, final String groupName) {
        return shouldQueryUseAggregationView(type, timerange, groupName, null);
    }

    boolean shouldQueryUseAggregationView(final String type,
            final String timerange, final String groupName, final String tac) {
        if (queryIsExclusiveTacRelated(groupName, tac)) {
            return false;
        }
        return shouldQueryUseAggregationView(type, timerange);
    }

    protected boolean queryIsExclusiveTacRelated(final String groupName,
            final String tac) {
        return exclusiveTACHandler.queryIsExclusiveTacRelated(groupName, tac);

    }

    public boolean isExclusiveTacGroup(final String groupName) {
        return exclusiveTACHandler.isExclusiveTacGroup(groupName);
    }

    protected boolean isTacInExclusiveTacGroup(final String tac) {
        return exclusiveTACHandler.isTacInExclusiveTacGroup(tac);
    }

    protected TechPackTables getTechPackTablesOrViews(
            final FormattedDateTimeRange dateTimeRange, final String timerange,
            final String type, final List<String> listOfTechPacks) {
        if (shouldQueryUseAggregationView(type, timerange)) {
            return getAggregationTables(type, timerange, listOfTechPacks);
        }
        return getRawTables(type, dateTimeRange, listOfTechPacks);

    }

    /**
     * Returns time interval for different time ranges as required for DVTP
     * feature.
     */
    protected int getDVTPIntervalForChart(final Long timerange) {
        if (timerange < MINUTES_IN_1_HOUR_30_MIN) {
            return FIVE_MINUTE_VALUE;
        } else if (timerange <= MINUTES_IN_2_HOURS) {
            return TEN_MINUTES_VALUE;
        } else if (timerange < MINUTES_IN_4_HOURS) {
            return FIFTEEN_MINUTE_VALUE;
        } else if (timerange < MINUTES_IN_9_HOURS) {
            return THIRTY_MINUTES_VALUE;
        } else if (timerange < MINUTES_IN_18_HOURS) {
            return MINUTES_IN_1_HOURS;
        } else if (timerange < MINUTES_IN_2_DAY) {
            return MINUTES_IN_2_HOURS;
        } else if (timerange < MINUTES_IN_4_DAY) {
            return MINUTES_IN_4_HOURS;
        } else if (timerange < MINUTES_IN_6_DAY) {
            return MINUTES_IN_6_HOURS;
        } else {
            return MINUTES_IN_1_DAY;
        }
    }

    protected int getDVTPIntervalForGrid(final Long timerange) {
        if (timerange <= MINUTES_IN_2_HOURS) {
            return FIVE_MINUTE_VALUE;
        } else if (timerange <= MINUTES_IN_1_DAY) {
            return FIFTEEN_MINUTE_VALUE;
        } else {
            return MINUTES_IN_1_DAY;
        }
    }

    public String getTzOffsetForCSV(final String tzOffset) {
        final char sign = tzOffset.charAt(0);
        final String first = tzOffset.substring(1, 3);
        final String last = tzOffset.substring(3, 5);
        final Integer total = (Integer.parseInt(first) * 60 + Integer
                .parseInt(last));
        final String tzOffsetQuery = sign + total.toString();
        return tzOffsetQuery;
    }

    public void setUriInfo(final UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    public void setTemplateMappingEngine(
            final TemplateMappingEngine templateMappingEngine) {
        this.templateMappingEngine = templateMappingEngine;
    }

    public void setAuditService(final AuditService auditService) {
        this.auditService = auditService;
    }

    protected boolean isImsiOrImsiGroupQuery(
            final MultivaluedMap<String, String> requestParameters) {
        return requestParameters.getFirst(TYPE_PARAM).equals(TYPE_IMSI);
    }

    public void setPerformanceTrace(
            final ServicePerformanceTraceLogger performanceTrace) {
        this.performanceTrace = performanceTrace;
    }

    public void setMediaTypeHandler(final MediaTypeHandler mediaTypeHandler) {
        this.mediaTypeHandler = mediaTypeHandler;
    }

    public void setLoadBalancingPolicyService(
            final LoadBalancingPolicyService loadBalancingPolicyService) {
        this.loadBalancingPolicyService = loadBalancingPolicyService;
    }

    public void setCsvResponseBuilder(
            final CSVResponseBuilder csvResponseBuilder) {
        this.csvResponseBuilder = csvResponseBuilder;
    }

    public void setTechPackListFactory(
            final TechPackListFactory techPackListFactory) {
        this.techPackListFactory = techPackListFactory;
    }

    public void setRawTableFetcher(final RawTableFetcher rawTableFetcher) {
        this.rawTableFetcher = rawTableFetcher;
    }

    public void setExclusiveTACHandler(
            final ExclusiveTACHandler exclusiveTACHandler) {
        this.exclusiveTACHandler = exclusiveTACHandler;
    }

    public void setTimeRangeSelector(final TimeRangeSelector timeRangeSelector) {
        this.timeRangeSelector = timeRangeSelector;
    }

    public void setDateTimeHelper(final DateTimeHelper dateTimeHelper) {
        this.dateTimeHelper = dateTimeHelper;
    }

    public void setDataTieringHandler(
            final DataTieringHandler dataTieringHandler) {
        this.dataTieringHandler = dataTieringHandler;
    }
}
