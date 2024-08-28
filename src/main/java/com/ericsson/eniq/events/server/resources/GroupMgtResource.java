package com.ericsson.eniq.events.server.resources;

import static com.ericsson.eniq.events.server.common.ApplicationConstants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import org.apache.commons.lang.StringUtils;

import com.distocraft.dc5000.repository.cache.GroupTypeDef;
import com.distocraft.dc5000.repository.cache.GroupTypeKeyDef;
import com.ericsson.eniq.events.server.utils.json.JSONUtils;

@Stateless
//@TransactionManagement(TransactionManagementType.BEAN)
@LocalBean
public class GroupMgtResource extends BaseResource {

    /**
     * These are the list of columns that we do not want to show to the user when they are viewing Group Management
     * details. This is necessary because the Platform class GroupTypeDef will give all the defined columns, without
     * the ability to exclude the HashingID's for example, so we need to exclude them here.
     * <p/>
     * If there are other columns that need to be hidden, add them to this set.
     */
    static final Set<String> ignoredDataKeyNames = new HashSet<String>();

    static {
        ignoredDataKeyNames.add(HIER3_ID);
        ignoredDataKeyNames.add(CELL_SQL_ID);
        ignoredDataKeyNames.add(HIER32_ID);
        ignoredDataKeyNames.add(EVENT_SOURCE_SQL_ID);
        ignoredDataKeyNames.add(HIER3_CELL_HASHID);
    }

    @Override
    protected String getData(final String requestId, final MultivaluedMap<String, String> requestParameters)
            throws WebApplicationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Group Mgt Path variable
     */
    private static final String GROUP_TYPE = "group_type";

    /**
     * Get all the groups for a particular type e.g. IMSI and all the group values.
     * This method looks up dwhrep.Grouptypes for <code>groupType</code> to get the data keys, then goes to the
     * group table in dwhdb and extracts and converts all entries for the group to a json form.
     *
     * @param groupType The group type e.g. APN, IMSI or TAC
     * @return JSON will all groups of a particular type and all the values in each group.
     */
    @Path("{" + GROUP_TYPE + "}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getGroupDetails(@PathParam(GROUP_TYPE)
    final String groupType) {
        try {
            final String drillType = null;
            final MultivaluedMap<String, String> requestParameters = getDecodedQueryParameters();

            // throws an unchecked exception is there are no groups in the techpack.
            final GroupTypeDef groupDefinition = dataService.getGroupDefinition(groupType);
            if (groupDefinition == null) {
                return JSONUtils.JSONEmptySuccessResult();
            }
            // Get all the data keys (start_time and stop_time arnt data keys)
            final List<String> keys = new ArrayList<String>();
            keys.add(GroupTypeDef.KEY_NAME_GROUP_NAME);

            for (final GroupTypeKeyDef dataKey : groupDefinition.getDataKeys()) {

                if (!isIgnoredColumnName(dataKey.getKeyName())) {
                    keys.add(dataKey.getKeyName());
                }
            }
            final Map<String, Object> templateParams = new HashMap<String, Object>();
            templateParams.put(GROUP_KEYS, keys);
            templateParams.put(GROUP_TABLE, groupDefinition.getTableName());
            addGroupNameToTemplateParameters(requestParameters, templateParams);

            final String query = templateUtils.getQueryFromTemplate(getTemplate(GROUP, requestParameters, drillType),
                    templateParams);

            if (MCC_MNC_WITH_NAMES.equalsIgnoreCase(requestParameters.getFirst("type"))
                    && MCC_MNC.equalsIgnoreCase(groupType)) {
                return dataService.getGroupDataMultipleValues(query);
            }

            return dataService.getGroupData(query);
        } catch (final EJBException e) {
            if (e.getCause() == null) {
                return JSONUtils.createJSONErrorResult(e.getMessage());
            }
            return JSONUtils.createJSONErrorResult(e.getCause().getMessage());
        } catch (final Throwable t) { //NOPMD - eemecoy 2/6/10, required to translate exception into json error
            return JSONUtils.createJSONErrorResult(t.toString());
        }
    }

    private void addGroupNameToTemplateParameters(final MultivaluedMap<String, String> requestParameters,
            final Map<String, Object> templateParams) {
        final String groupName = requestParameters.getFirst(GROUP_NAME_PARAM);
        if (StringUtils.isNotBlank(groupName)) {
            templateParams.put(GROUP_NAME_PARAM, groupName);
        }
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
        throw new UnsupportedOperationException();
    }

    private boolean isIgnoredColumnName(final String columnName) {
        return ignoredDataKeyNames.contains(columnName.toUpperCase());
    }
}
