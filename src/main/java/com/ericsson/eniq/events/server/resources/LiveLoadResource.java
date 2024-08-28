/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2015
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.eniq.events.server.resources;

import static com.ericsson.eniq.events.server.common.ApplicationConfigConstants.*;
import static com.ericsson.eniq.events.server.common.ApplicationConstants.*;
import static com.ericsson.eniq.events.server.common.LiveLoadConstants.*;
import static com.ericsson.eniq.events.server.common.TechPackData.DIM_E_LTE;
import static com.ericsson.eniq.events.server.common.TechPackData.DIM_E_SGEH;

import java.util.*;

import javax.ejb.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.ericsson.eniq.events.server.query.QueryParameter;
import com.ericsson.eniq.events.server.services.impl.TechPackCXCMappingService;
import com.ericsson.eniq.events.server.utils.*;
import com.ericsson.eniq.events.server.utils.json.JSONUtils;
import com.ericsson.eniq.events.server.utils.parameterchecking.ParameterChecker;

/**
 * The Class LiveLoadResource.
 * Sub-root resource of RESTful service.
 * @since May 2010
 */
@Stateless
//@TransactionManagement(TransactionManagementType.BEAN)
@LocalBean
public class LiveLoadResource extends BaseResource {

    @EJB
    protected ParameterChecker parameterChecker;

    @EJB
    protected HashUtilities hashUtilities;

    @EJB
    private TechPackCXCMappingService techPackCXCMappingService;

    /**
     * Get the LiveLoad handet makes
     * If the parameter &id is set, a list of Handset Manufactures is returned.
     * If the &id parameter is not set, the UI Metadata is generated for each handset manufacturer
     * The generated ui metadata will contain links back to this REST path with the &id param set, callback nor enything else is set, UI must append therse.
     * If the query value has a trailing comma (e.g 1208,), the comma is removed to enable correct liveloading.
     *
     * @return if defined(&id) --> LiveLoad/Search results for &id
     *         else --> Handset MetaDataUI to LiveLoad/Search
     *         <p/>
     *         http://localhost:8080/EniqEventsServices/LIVELOAD/HANDSET_MAKES?callback=transId1&limit=5
     *         then with id parameter
     *         http://localhost:8080/EniqEventsServices/LIVELOAD/HANDSET_MAKES?id=Nokia&callback=transId1&limit=5
     */
    @Path(LIVELOAD_HANDSET_MAKES)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLiveLoadHandsetMakes() {
        final MultivaluedMap<String, String> urlParams = getDecodedQueryParameters();

        trimQueryParameterComma(urlParams);

        final Map<String, String> templateParams = new HashMap<String, String>(4);

        setLimitFactor(templateParams);
        setFilter(templateParams, urlParams);

        final String  makeId = urlParams.getFirst(ID);
        final boolean getLiveLoad = makeId != null && makeId.length() > 0;
        final String  escapedMakeID = StringEscapeUtils.escapeSql(makeId);
        if (getLiveLoad) {
            templateParams.put(ID, escapedMakeID);
        }
        final String pathName = new StringBuffer().append(LIVELOAD).append(PATH_SEPARATOR)
                .append(LIVELOAD_HANDSET_MAKES).toString();
        final String query = templateUtils.getQueryFromTemplate(getTemplate(pathName, urlParams, DRILLTYPE),
                templateParams);
        final String json;
        if (getLiveLoad) {
            final String callbackId = urlParams.getFirst(LIVELOAD_CALLBACK_PARAM);
            if (callbackId == null) {
                return JSONUtils.createJSONErrorResult("No 'callback' parameter specified.");
            }
            json = extractPagingParametersAndCallGetLiveLoad(query, makeId, callbackId, urlParams);
        } else {
            json = dataService.getHandsetMakesUIMetadata(query, uriInfo.getBaseUri().toString(), uriInfo.getPath());
        }
        return json;
    }

    private String extractPagingParametersAndCallGetLiveLoad(final String query, final String liveLoadType,
            final String callbackId, final MultivaluedMap<String, String> urlParams) {
        final String pagingLimit = urlParams.getFirst(PAGING_LIMIT_KEY);
        final String pagingIndex = urlParams.getFirst(PAGING_START_KEY);
        return dataService.getLiveLoad(query, liveLoadType, callbackId, pagingLimit, pagingIndex);
    }

    private String extractPagingParametersAndCallGetLiveLoadForAPN(final String query, final String liveLoadType,
            final String callbackId, final MultivaluedMap<String, String> urlParams,
            final Map<String, QueryParameter> queryParams) {
        final String pagingLimit = urlParams.getFirst(PAGING_LIMIT_KEY);
        final String pagingIndex = urlParams.getFirst(PAGING_START_KEY);
        return dataService.getLiveLoadForAPN(query, queryParams, liveLoadType, callbackId, pagingLimit, pagingIndex);
    }

    /**
     * load_type, BSC or CELL
     * Here BSC means Controller - ie a query requesting BSCs will return all BSCs, RNCs and eRBSs
     * Likewise, a CELL query will return all 2G, 3G and 4G cells
     * http://localhost:8080/EniqEventsServices/LIVELOAD/BSC?callback=transId0&limit=20&query=bsc1
     * http://localhost:8080/EniqEventsServices/LIVELOAD/CALL?callback=transId0&limit=20&query=bsc1
     *
     * @param liveload_type load type
     * @return LiveLoad JSON for either BSC or CELL
     */
    @Path("{" + LIVELOAD_TYPE_NODE + "}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLiveLoadNodes(@PathParam(LIVELOAD_TYPE_NODE)
    final String liveload_type) {
        final String YES = "Y";
        final String NO = "N";
        final int MAP_SIZE = 6;
        final String DOES_LTE_TECHPACK_EXIST = "doesLteTechPackExist";
        final String DOES_SGEH_TECHPACK_EXIST = "doesSgehTechPackExist";
        final MultivaluedMap<String, String> urlParams = getDecodedQueryParameters();
        final String callbackId = urlParams.getFirst(LIVELOAD_CALLBACK_PARAM);
        if (callbackId == null) {
            return JSONUtils.createJSONErrorResult("No '" + LIVELOAD_CALLBACK_PARAM + "' parameter specified.");
        }
        if (isNotValidNodeTypeForLiveLoad(liveload_type)) {
            return JSONUtils.createJSONErrorResult("LiveLoad type '" + liveload_type + "' not supported.");
        }
        final Map<String, String> templateParams = new HashMap<String, String>(MAP_SIZE);
        setLimitFactor(templateParams);
        setFilter(templateParams, urlParams);
        final String pathName = putTogetherLiveLoadPathName(liveload_type);
        String template = getTemplate(pathName, urlParams, DRILLTYPE);
        final List<String> licensedTechPacksLTE = getTechPackCXCNumbers(DIM_E_LTE);
        if(licensedTechPacksLTE!=null && licensedTechPacksLTE.size()>0){
            templateParams.put(DOES_LTE_TECHPACK_EXIST,YES);
        }else{
            templateParams.put(DOES_LTE_TECHPACK_EXIST,NO);
        }
        final List<String> licensedTechPacksSGEH = getTechPackCXCNumbers(DIM_E_SGEH);
        if(licensedTechPacksSGEH!=null && licensedTechPacksSGEH.size()>0){
            templateParams.put(DOES_SGEH_TECHPACK_EXIST,YES);
        }else{
            templateParams.put(DOES_SGEH_TECHPACK_EXIST,NO);
        }
       final String query = templateUtils.getQueryFromTemplate(template,templateParams);
       return extractPagingParametersAndCallGetLiveLoad(query, liveload_type.toUpperCase(), callbackId, urlParams);
    }

    protected List<String> getTechPackCXCNumbers(String techpackName){
        return techPackCXCMappingService.getTechPackCXCNumbers(techpackName);
    }

    @Path(LIVELOAD_KPI_RNC)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLiveLoadRncs() {
        final MultivaluedMap<String, String> urlParams = getDecodedQueryParameters();

        final String callbackId = urlParams.getFirst(LIVELOAD_CALLBACK_PARAM);
        if (callbackId == null) {
            return JSONUtils.createJSONErrorResult("No '" + LIVELOAD_CALLBACK_PARAM + "' parameter specified.");
        }

        final Map<String, String> templateParams = new HashMap<String, String>();

        setLimitFactor(templateParams);
        setFilter(templateParams, urlParams);
        final String pathName = new StringBuffer().append(LIVELOAD).append("/").append(LIVELOAD_KPI_RNC).toString();

        final String query = templateUtils.getQueryFromTemplate(getTemplate(pathName, urlParams, DRILLTYPE),
                templateParams);
        return extractPagingParametersAndCallGetLiveLoad(query, LIVELOAD_KPI_RNC, callbackId, urlParams);
    }

    private String putTogetherLiveLoadPathName(final String liveload_type) {
        return new StringBuffer().append(LIVELOAD).append("/").append(liveload_type).toString();
    }

    private boolean isNotValidNodeTypeForLiveLoad(final String liveload_type) {
        return !(LIVELOAD_TYPE_BSC.equalsIgnoreCase(liveload_type) || LIVELOAD_TYPE_CELL
                .equalsIgnoreCase(liveload_type));
    }

    /**
     * load_type for SGSN nodes
     * http://localhost:8080/EniqEventsServices/LIVELOAD/SGSN?callback=transId0&limit=20&query=sgsn
     *
     * @return JSON will all groups of a particular type and all the values in each group.
     */
    @Path(LIVELOAD_SGSN)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLiveLoadSgsn() {
        final MultivaluedMap<String, String> urlParams = getDecodedQueryParameters();
        final String pathName = new StringBuffer().append(LIVELOAD).append("/").append(LIVELOAD_SGSN).toString();
        return liveLoad(LIVELOAD_SGSN, urlParams, pathName);
    }

    /**
     * load_type for MSC nodes
     * http://localhost:8080/EniqEventsServices/LIVELOAD/MSC?callback=transId0&limit=20&query=msc
     *
     * @return JSON will all groups of a particular type and all the values in each group.
     */
    @Path(LIVELOAD_MSC)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLiveLoadMsc() {
        final MultivaluedMap<String, String> urlParams = getDecodedQueryParameters();
        final String pathName = new StringBuffer().append(LIVELOAD).append("/").append(LIVELOAD_MSC).toString();
        final String techPack = "DIM_E_MSS";
        final List<String> cxcLicensesForTechPack = getTechPackCXCNumbers(techPack);
        if (cxcLicensesForTechPack.isEmpty()) {
            return JSONUtils.createJSONErrorResult("MSS feature has not been installed.");
        }
        return liveLoad(LIVELOAD_MSC, urlParams, pathName);
    }

    /**
     * REST to get the LiveLoad/Search ExtGWT results for APN's
     * http://localhost:8080/EniqEventsServices/LIVELOAD/APN?callback=transId0&limit=20&query=apn
     * e.g. {transId2({"totalCount":"1","APN" : [{"id" : "blackberry.net"}]})}
     *
     * @return JSON for ExtGWT LiveSearch
     */
    @Path(LIVELOAD_APN)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLiveLoadApns() {
        final MultivaluedMap<String, String> urlParams = getDecodedQueryParameters();
        final String tzOffset = urlParams.getFirst(TZ_OFFSET);
        final String apnRetentionInDays = Integer.toString(applicationConfigManager.getAPNRetention());
        final FormattedDateTimeRange dateTimeRange = DateTimeRange.getFormattedTimeRangeInDays(tzOffset,
                apnRetentionInDays);
        final Map<String, QueryParameter> queryParams = queryUtils.mapDateParametersForApnRetention(dateTimeRange);
        final String pathName = new StringBuffer().append(LIVELOAD).append("/").append(LIVELOAD_APN).toString();
        return liveLoadForAPN(LIVELOAD_APN, urlParams, queryParams, pathName);
    }

    /**
     * load_type for TRAC nodes
     * http://localhost:8080/EniqEventsServices/LIVELOAD/TRAC?callback=transId0&limit=20&query=trac
     *
     * @return JSON will all groups of a particular type and all the values in each group.
     */
    @Path(LIVELOAD_TRAC)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLiveLoadTrac() {
        final MultivaluedMap<String, String> urlParams = getDecodedQueryParameters();
        final String pathName = new StringBuffer().append(LIVELOAD).append("/").append(LIVELOAD_TRAC).toString();
        return liveLoad(LIVELOAD_TRAC, urlParams, pathName);
    }

    @Path(LIVELOAD_SBR_CELL)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLiveLoadSbrCell() {
        final MultivaluedMap<String, String> urlParams = getDecodedQueryParameters();

        final String callbackId = urlParams.getFirst(LIVELOAD_CALLBACK_PARAM);
        if (callbackId == null) {
            return JSONUtils.createJSONErrorResult("No '" + LIVELOAD_CALLBACK_PARAM + "' parameter specified.");
        }

        final Map<String, String> templateParams = new HashMap<String, String>();

        setLimitFactor(templateParams);
        setFilter(templateParams, urlParams);
        updateNodeforHashID(templateParams, urlParams);
        final String pathName = new StringBuffer().append(LIVELOAD).append("/").append(LIVELOAD_SBR_CELL).toString();

        final String query = templateUtils.getQueryFromTemplate(getTemplate(pathName, urlParams, DRILLTYPE),
                templateParams);
        return extractPagingParametersAndCallGetLiveLoad(query, LIVELOAD_SBR_CELL, callbackId, urlParams);
    }

    private void updateNodeforHashID(final Map<String, String> templateParams,
            final MultivaluedMap<String, String> requestParameters) {
        final String trimmedNode = requestParameters.getFirst(NODE_PARAM).trim();
        final String[] value = trimmedNode.split(DELIMITER);
        final String bsc = value[0];
        final String vendor = value[1];
        requestParameters.putSingle(TYPE_PARAM, TYPE_BSC);
        final String rat = queryUtils.getRATValueAsInteger(requestParameters);
        templateParams.put(CONTROLLER_SQL_ID, hashUtilities.createHashIDForController(rat, bsc, vendor));
    }

    /**
     *  load_type for list of COUNTRY NAME
     */

    @Path(LIVELOAD_COUNTRY)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLiveLoadCountry() {
        final MultivaluedMap<String, String> urlParams = getDecodedQueryParameters();
        urlParams.add(PAGING_START_KEY, "0");
        urlParams.add(PAGING_LIMIT_KEY, Integer.toString(MAXIMUM_POSSIBLE_CONFIGURABLE_MAX_JSON_RESULT_SIZE));
        final String pathName = new StringBuffer().append(LIVELOAD).append("/").append(LIVELOAD_COUNTRY).toString();
        return liveLoadWithoutLimit(LIVE_LOAD_TYPE_COUNTRY, urlParams, pathName);

    }

    /**
    *  load_type for list of OPERATOR NAME using MCC
    */

    @Path(LIVELOAD_OPERATOR)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLiveLoadOperator() {

        final MultivaluedMap<String, String> urlParams = getDecodedQueryParameters();

        trimQueryParameterComma(urlParams);
        urlParams.add(PAGING_START_KEY, "0");
        urlParams.add(PAGING_LIMIT_KEY, Integer.toString(MAXIMUM_POSSIBLE_CONFIGURABLE_MAX_JSON_RESULT_SIZE));

        final Map<String, String> templateParams = new HashMap<String, String>();

        final String mcc = urlParams.getFirst(MCC_PARAM);
        if (null == mcc || mcc.equals(EMPTY_STRING)) {
            return JSONUtils.createJSONErrorResult("No MCC parameter specified.");
        }
        templateParams.put(MCC_PARAM, mcc);
        final String pathName = new StringBuffer().append(LIVELOAD).append(PATH_SEPARATOR).append(LIVELOAD_OPERATOR)
                .toString();
        final String query = templateUtils.getQueryFromTemplate(getTemplate(pathName, urlParams, DRILLTYPE),
                templateParams);
        final String json;
        final String callbackId = urlParams.getFirst(LIVELOAD_CALLBACK_PARAM);
        if (callbackId == null) {
            return JSONUtils.createJSONErrorResult("No 'callback' parameter specified.");
        }
        json = extractPagingParametersAndCallGetLiveLoad(query, LIVE_LOAD_TYPE_OPERATOR, callbackId, urlParams);
        return json;

    }

    /**
     * Generic live load method. Used for SGSN, BSC, CELL and APN.
     *
     * @param liveLoadType The live load type
     * @param urlParams    The client URL parameters
     * @return JSON for ExtGWT LiveSearch
     */
    private String liveLoadForAPN(final String liveLoadType, final MultivaluedMap<String, String> urlParams,
            final Map<String, QueryParameter> queryParam, final String pathName) {
        final String callbackId = urlParams.getFirst(LIVELOAD_CALLBACK_PARAM);
        if (callbackId == null) {
            return JSONUtils.createJSONErrorResult("No '" + LIVELOAD_CALLBACK_PARAM + "' parameter specified.");
        }
        final Map<String, String> templateParams = new HashMap<String, String>(4);
        setLimitFactor(templateParams);
        setFilter(templateParams, urlParams);
        final String query = templateUtils.getQueryFromTemplate(getTemplate(pathName, urlParams, DRILLTYPE),
                templateParams);
        return extractPagingParametersAndCallGetLiveLoadForAPN(query, liveLoadType, callbackId, urlParams, queryParam);
    }

    /**
     * Generic live load method. Used for SGSN, BSC, CELL and APN.
     *
     * @param liveLoadType The live load type
     * @param urlParams    The client URL parameters
     * @return JSON for ExtGWT LiveSearch
     */
    private String liveLoad(final String liveLoadType, final MultivaluedMap<String, String> urlParams,
            final String pathName) {
        final String callbackId = urlParams.getFirst(LIVELOAD_CALLBACK_PARAM);
        if (callbackId == null) {
            return JSONUtils.createJSONErrorResult("No '" + LIVELOAD_CALLBACK_PARAM + "' parameter specified.");
        }

        final Map<String, String> templateParams = new HashMap<String, String>(4);
        setLimitFactor(templateParams);
        setFilter(templateParams, urlParams);
        final String query = templateUtils.getQueryFromTemplate(getTemplate(pathName, urlParams, DRILLTYPE),
                templateParams);
        return extractPagingParametersAndCallGetLiveLoad(query, liveLoadType, callbackId, urlParams);
    }

    /**
     * Generic live load method without max number limit. Used by Country(MCC) liveload
     *
     * @param liveLoadType The live load type
     * @param urlParams    The client URL parameters
     * @return JSON for ExtGWT LiveSearch
     */
    private String liveLoadWithoutLimit(final String liveLoadType, final MultivaluedMap<String, String> urlParams,
            final String pathName) {
        final String callbackId = urlParams.getFirst(LIVELOAD_CALLBACK_PARAM);
        if (callbackId == null) {
            return JSONUtils.createJSONErrorResult("No '" + LIVELOAD_CALLBACK_PARAM + "' parameter specified.");
        }
        final Map<String, String> templateParams = new HashMap<String, String>(4);
        setFilter(templateParams, urlParams);
        final String query = templateUtils.getQueryFromTemplate(getTemplate(pathName, urlParams, DRILLTYPE),
                templateParams);
        return extractPagingParametersAndCallGetLiveLoad(query, liveLoadType, callbackId, urlParams);
    }

    @Override
    protected String getData(final String requestId, final MultivaluedMap<String, String> requestParameters)
            throws WebApplicationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Add the idFilter param to the velocity template parameters if its set on the URI
     *
     * @param templateParams The parameter map to update
     * @param urlParams      Inout URI parameters
     */
    private void setFilter(final Map<String, String> templateParams, final MultivaluedMap<String, String> urlParams) {
        String idFilter = urlParams.getFirst(LIVELOAD_QUERY_PARAM);
        if (idFilter != null) {
            if (!idFilter.endsWith("%")) {
                idFilter += "%";
            }
            if (!idFilter.startsWith("%")) {
                idFilter = "%" + idFilter;
            }
            idFilter= StringEscapeUtils.escapeSql(idFilter);
            templateParams.put(LIVELOAD_QUERY_PARAM, idFilter);
        }
    }

    /**
     * Add the limit param to the velocity template parameters if its set on the URI
     *
     * @param templateParams The parameter map to update
     */
    private void setLimitFactor(final Map<String, String> templateParams) {
        final int limit = applicationConfigManager.getLiveLoadCount();
        final StringBuffer sb = new StringBuffer();
        sb.append(limit);
        templateParams.put(LIVELOAD_LIMIT_PARAM, sb.toString());

    }

    /* (non-Javadoc)
     * @see com.ericsson.eniq.events.server.resources.BaseResource#checkParameters(javax.ws.rs.core.MultivaluedMap)
     */
    @Override
    protected List<String> checkParameters(final MultivaluedMap<String, String> requestParameters) {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see com.ericsson.eniq.events.server.resources.BaseResource#isValidValue(javax.ws.rs.core.MultivaluedMap)
     */
    @Override
    protected boolean isValidValue(final MultivaluedMap<String, String> requestParameters) {
        final String node = requestParameters.getFirst(NODE_PARAM);
        requestParameters.putSingle(TYPE_PARAM, TYPE_BSC);
        if (StringUtils.isBlank(node) || !parameterChecker.checkValidValueOfParameter(requestParameters)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the urlParams map contains a query key, and if so, checks if the query key value is non-null
     *
     * @param urlParams The MultivaluedMap containing the request parameters
     * @return true if the map contains a query key, and if the keys value is non-null
     */
    private boolean isRequestAQueryRequest(final MultivaluedMap<String, String> urlParams) {
        return (urlParams.containsKey(LIVELOAD_QUERY_PARAM) && urlParams.getFirst(LIVELOAD_QUERY_PARAM) != null);
    }

    /**
     * Checks if query value String ends with a comma (e.g 1208,)
     *
     * @param queryValue
     * @return true if the string ends with a comma
     */
    boolean doesStringEndWithComma(final String queryValue) {
        return (queryValue.length() != 0 && queryValue.charAt(queryValue.length() - 1) == COMMA.charAt(0));
    }

    /**
     * Removes the trailing comma from the passed String
     *
     * @param queryValue
     * @return String with trailing comma removed
     */
    String removeTrailingComma(final String queryValue) {
        return queryValue.substring(0, queryValue.length() - 1);
    }

    /**
     * Trims the query parameter contained in the urlParams map, removing a trailing comma if present
     *
     * @param urlParams The MultivaluedMap containing the request parameters
     */
    void trimQueryParameterComma(final MultivaluedMap<String, String> urlParams) {
        if (isRequestAQueryRequest(urlParams)) {
            final String queryValue = urlParams.getFirst(LIVELOAD_QUERY_PARAM);
            if (doesStringEndWithComma(queryValue)) {
                urlParams.putSingle(LIVELOAD_QUERY_PARAM, removeTrailingComma(queryValue));
            }
        }
    }

    public void setHashUtilities(final HashUtilities hashUtilities) {
        this.hashUtilities = hashUtilities;
    }
}
