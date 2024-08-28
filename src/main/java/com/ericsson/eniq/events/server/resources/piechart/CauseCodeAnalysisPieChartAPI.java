/**
 * -----------------------------------------------------------------------
 *     Copyright (C) 2011 LM Ericsson Limited.  All rights reserved.
 * -----------------------------------------------------------------------
 */
package com.ericsson.eniq.events.server.resources.piechart;

import static com.ericsson.eniq.events.server.common.ApplicationConstants.*;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import com.ericsson.eniq.events.server.common.MediaTypeConstants;
import com.ericsson.eniq.events.server.json.JSONException;

/**
 * This is the API for cause code pie chart analysis
 * The idea is to hide the actual resource from external world
 * This implementation will be revisited when the services framework will be in place
 * 
 * @author eavidat
 * @since 2011
 *
 */
@Stateless
//@TransactionManagement(TransactionManagementType.BEAN)
@LocalBean
public class CauseCodeAnalysisPieChartAPI {

    @EJB
    protected CauseCodeAnalysisPieChartResource causeCodeAnalysisPieChartResource;

    /**
     * This API returns a list of cause codes 
     * for a particular node type (single or group) and 
     * for a specific time period
     * 
     * @return the list cause codes
     * with the following JSON structure:  
     *      {
                   "1" : "Cause Code ID",
                   "2" : "Cause Code DESC" 
            }
     *  
     * @throws WebApplicationException
     * @throws JSONException
     */
    @Path(CC_LIST)
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaTypeConstants.APPLICATION_CSV })
    public String getCausecodeList() throws WebApplicationException, JSONException {
        return this.causeCodeAnalysisPieChartResource.getResults(CAUSE_CODE_PIE_CHART + PATH_SEPARATOR + CC_LIST);
    }

    /**
     * This API returns the cause code analysis data (e.g. number of errors, impacted subscribers)
     * for the selected cause code (s) and
     * for a particular node type (single or group) and 
     * for a specific time period
     * 
     * @return the cause code analysis
     * with the following JSON structure:  
     *      {
                   "1" : "Cause Code ID",
                   "2" : "Cause Code DESC", 
                   "3" : "Number of failures",
                   "4" : "Impacted Subscribers"
            }
     *   
     * @throws WebApplicationException
     * @throws JSONException
     */
    @Path(CAUSE_CODE_ANALYSIS)
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaTypeConstants.APPLICATION_CSV })
    public String getCauseCodeAnalysis() throws WebApplicationException, JSONException {
        return this.causeCodeAnalysisPieChartResource.getResults(CAUSE_CODE_PIE_CHART + PATH_SEPARATOR
                + CAUSE_CODE_ANALYSIS);
    }

    /**
     * This API returns the sub cause code analysis data (e.g. number of errors, impacted subscribers, what next advice)
     * for a selected cause code and
     * for a particular node type (single or group) and 
     * for a specific time period
     * 
     * @return the sub cause code analysis
     * with the following JSON structure:  
     *      {
                   "1" : "Cause Prototype",
                   "2" : "Cause Code ID",
                   "3" : "Sub Cause Code ID",
                   "4" : "Sub Cause Code DESC (Cause Prototype DESC)",
                   "5" : "What Next Text",
                   "6" : "Number of failures",
                   "7" : "Impacted Subscribers"
            }
     *   
     * @throws WebApplicationException
     * @throws JSONException
     */
    @Path(SUB_CAUSE_CODE_ANALYSIS)
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaTypeConstants.APPLICATION_CSV })
    public String getSubCauseCodeAnalysis() throws WebApplicationException, JSONException {
        return this.causeCodeAnalysisPieChartResource.getResults(CAUSE_CODE_PIE_CHART + PATH_SEPARATOR
                + SUB_CAUSE_CODE_ANALYSIS);
    }
}