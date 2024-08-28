/**
 * -----------------------------------------------------------------------
 *     Copyright (C) 2011 LM Ericsson Limited.  All rights reserved.
 * -----------------------------------------------------------------------
 */
package com.ericsson.eniq.events.server.resources;

import static com.ericsson.eniq.events.server.common.ApplicationConstants.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.ericsson.eniq.events.server.utils.json.JSONUtils;

/**
 * The Class ImsiMsisdnMappingResource.
 * This REST resource shall be placed under ENIQ EVENTS Services layer root path.
 * There is no query parameter for imsi-msisdn mapping URL path.
 *
 * @author ehaoswa
 * @since 2011
 */
@Stateless
//@TransactionManagement(TransactionManagementType.BEAN)
@LocalBean
public class ImsiMsisdnMappingResource extends BaseResource {

    /** The Constant TYPE for URL path. */
    private static final String TYPE = "subscriberType";

    /** The Constant INPUT_VALUE for URL path. */
    private static final String INPUT_VALUE = "inputValue";

    /** The Constant FORWARD_SLASH for URL path. */
    private static final String FORWARD_SLASH = "/";

    /* (non-Javadoc)
     * @see com.ericsson.eniq.events.server.resources.BaseResource#getData(java.lang.String, javax.ws.rs.core.MultivaluedMap)
     */
    @Override
    protected String getData(final String requestId, final MultivaluedMap<String, String> requestParameters)
            throws WebApplicationException {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see com.ericsson.eniq.events.server.resources.BaseResource#isValidValue(javax.ws.rs.core.MultivaluedMap)
     */
    @Override
    protected boolean isValidValue(final MultivaluedMap<String, String> requestParameters) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get mapped IMSI/MSISDN for a particular type e.g. IMSI and all the imsi values.
     * This method will put <code>subscriberType</code> and <code>inputValue</code> into Velocity template to generate query.
     *
     * @param subscriberType the subscriber type
     * @param inputValue the input value
     * @return JSON the mapped imsi/msisdn value in JSON format.
     */
    @Path("{" + TYPE + FORWARD_SLASH + INPUT_VALUE + "}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getMappingResults(@PathParam(TYPE)
    final String subscriberType, @PathParam(INPUT_VALUE)
    final String inputValue) {
        try {
            final String drillType = null;
            final MultivaluedMap<String, String> requestParameters = getDecodedQueryParameters();

            final Map<String, Object> templateParams = new HashMap<String, Object>();
            templateParams.put(TYPE_PARAM, subscriberType);
            templateParams.put(INPUT_VALUE, inputValue);
            final String query = templateUtils.getQueryFromTemplate(
                    getTemplate(IMSI_MSISDN_MAPPING, requestParameters, drillType), templateParams);
            return dataService.getJSONDataWithoutTimeInfo(query);
        } catch (final EJBException e) {
            if (e.getCause() == null) {
                return JSONUtils.createJSONErrorResult(e.getMessage());
            }
            return JSONUtils.createJSONErrorResult(e.getCause().getMessage());
        } catch (final Throwable t) { //NOPMD - ehaoswa 07/11/2011, required to translate exception into json error
            return JSONUtils.createJSONErrorResult(t.toString());
        }
    }

    /* (non-Javadoc)
     * @see com.ericsson.eniq.events.server.resources.BaseResource#checkParameters(javax.ws.rs.core.MultivaluedMap)
     */
    @Override
    protected List<String> checkParameters(final MultivaluedMap<String, String> requestParameters) {
        throw new UnsupportedOperationException();
    }

}
