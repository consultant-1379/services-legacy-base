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
import static com.ericsson.eniq.events.server.test.common.ApplicationTestConstants.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import java.util.*;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import com.ericsson.eniq.events.server.common.EventDataSourceType;
import com.ericsson.eniq.events.server.common.TechPackData;
import com.ericsson.eniq.events.server.common.tablesandviews.AggregationTableInfo;
import com.ericsson.eniq.events.server.query.TimeRangeSelector;
import com.ericsson.eniq.events.server.services.exclusivetacs.ExclusiveTACHandler;
import com.ericsson.eniq.events.server.templates.mappingengine.TemplateMappingEngine;
import com.ericsson.eniq.events.server.test.common.BaseJMockUnitTest;
import com.ericsson.eniq.events.server.test.util.JSONAssertUtils;
import com.ericsson.eniq.events.server.utils.*;
import com.ericsson.eniq.events.server.utils.config.ApplicationConfigManager;
import com.ericsson.eniq.events.server.utils.techpacks.RawTableFetcher;
import com.ericsson.eniq.events.server.utils.techpacks.TechPackListFactory;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class BaseResourceMockedTest extends BaseJMockUnitTest {

    private static final MediaType APPLICATION_CSV_MEDIA_TYPE = new MediaType(
            "application", "csv");

    private StubbedBaseResource objToTest;

    FormattedDateTimeRange mockedFormattedDateTimeRange;

    final String startDateTime = "2010-10-11 13:00";

    final String endDateTime = "2010-10-11 14:00";

    TechPackListFactory mockedTechPackTableFactory;

    TimeRangeSelector mockedTimeRangeSelector;

    TemplateMappingEngine mockedTemplateMappingEngine;

    RawTableFetcher mockedRawTableFetcher;

    private static final int MINUTES_IN_2_HOURS = 120;

    private static final int MINUTES_IN_3_HOURS = 180;

    private static final int MINUTES_IN_4_HOURS = 240;

    private static final int MINUTES_IN_12_HOURS = 720;

    private static final int MINUTES_IN_2_DAY = 2880;

    private static final int MINUTES_IN_5_DAY = 7200;

    private static final int MINUTES_IN_A_WEEK = 10080;

    private final JSONAssertUtils jsonAssertUtils = new JSONAssertUtils();

    @Before
    public void setup() {
        objToTest = new StubbedBaseResource();
        mockedRawTableFetcher = mockery.mock(RawTableFetcher.class);
        objToTest.setRawTableFetcher(mockedRawTableFetcher);

        objToTest.setMediaTypeHandler(new MediaTypeHandler());
        mockedTechPackTableFactory = mockery.mock(TechPackListFactory.class);
        objToTest.setTechPackListFactory(mockedTechPackTableFactory);
        mockedFormattedDateTimeRange = mockery
                .mock(FormattedDateTimeRange.class);
        mockedTimeRangeSelector = mockery.mock(TimeRangeSelector.class);
        mockedTemplateMappingEngine = mockery.mock(TemplateMappingEngine.class);
        allowGetStartAndEndDateTimeOnDateTimeRange();
        final ExclusiveTACHandler exclusiveTACHandler = setUpMockedExclusiveTACHandler();
        objToTest.setExclusiveTACHandler(exclusiveTACHandler);
    }

    private ExclusiveTACHandler setUpMockedExclusiveTACHandler() {
        final ExclusiveTACHandler exclusiveTACHandler = mockery
                .mock(ExclusiveTACHandler.class);
        mockery.checking(new Expectations() {
            {
                allowing(exclusiveTACHandler).queryIsExclusiveTacRelated(null,
                        null);
                will(returnValue(false));
                allowing(exclusiveTACHandler).queryIsExclusiveTacRelated(
                        EXCLUSIVE_TAC_GROUP, null);
                will(returnValue(true));
                allowing(exclusiveTACHandler).queryIsExclusiveTacRelated(
                        SAMPLE_IMSI_GROUP, EXCLUSIVE_TAC_GROUP);
                will(returnValue(true));
            }

        });
        return exclusiveTACHandler;
    }

    @Test
    public void testupdateTemplateWithErrRAWTablesForLTEAndNONLTEQueriesRMIEEngine() {
        final Map<String, Object> templateParameters = new HashMap<String, Object>();

        final String key = KEY_TYPE_ERR;
        final List<String> listOfTables = new ArrayList<String>();
        listOfTables.add("rawLteTable1");
        listOfTables.add("rawTable2");
        final Map<String, String> parameterMapForOrdinaryTables = new HashMap<String, String>();
        parameterMapForOrdinaryTables.put(KEY_PARAM, KEY_TYPE_ERR);

        final Map<String, String> parameterMapForLTETables = new HashMap<String, String>();
        parameterMapForLTETables.put(KEY_PARAM, KEY_TYPE_ERR);
        parameterMapForLTETables.put(IS_LTE_VIEW, "true");

        mockery.checking(new Expectations() {
            {

                one(mockedRawTableFetcher).getRAWTables(
                        mockedFormattedDateTimeRange, ERR, RAW_ALL_ERR_TABLES);
                will(returnValue(listOfTables));
            }
        });
        final FormattedDateTimeRange dateTimeRange = mockedFormattedDateTimeRange;
        final boolean result = objToTest.updateTemplateWithRAWTables(
                templateParameters, dateTimeRange, key, RAW_ALL_ERR_TABLES);
        assertThat(result, is(true));
        assertThat((List<String>) templateParameters.get(RAW_ALL_ERR_TABLES),
                is(listOfTables));

    }

    @Test
    public void testshouldQueryUseAggregationViewWithNullGroupNameAndNoAgg() {
        objToTest.aggregationViews.put(TYPE_APN, new AggregationTableInfo(
                "some agg table", EventDataSourceType.AGGREGATED_DAY));
        final String timerange = EventDataSourceType.AGGREGATED_15MIN
                .toString();
        expectsOnTechPackTableFactory(TYPE_APN, timerange,
                objToTest.aggregationViews, false);
        assertThat(objToTest.shouldQueryUseAggregationView(TYPE_APN, timerange,
                null), is(false));
    }

    private void expectsOnTechPackTableFactory(final String type,
            final String timerange,
            final Map<String, AggregationTableInfo> aggregationViews,
            final boolean valueToReturn) {
        mockery.checking(new Expectations() {
            {
                one(mockedTechPackTableFactory).shouldQueryUseAggregationView(
                        type, timerange, aggregationViews);
                will(returnValue(valueToReturn));
            }
        });

    }

    @Test
    public void testshouldQueryUseAggregationViewWithNullGroupNameAndAggExists() {
        objToTest.aggregationViews.put(TYPE_APN, new AggregationTableInfo(
                "some agg table", EventDataSourceType.AGGREGATED_DAY));
        final String timerange = EventDataSourceType.AGGREGATED_DAY.toString();
        expectsOnTechPackTableFactory(TYPE_APN, timerange,
                objToTest.aggregationViews, true);
        assertThat(objToTest.shouldQueryUseAggregationView(TYPE_APN, timerange,
                null), is(true));
    }

    @Test
    public void testshouldQueryUseAggregationViewForExclusiveTacGroupIsFalse() {
        assertThat(objToTest.shouldQueryUseAggregationView(null, null,
                EXCLUSIVE_TAC_GROUP), is(false));
    }

    @Test
    public void testisImsiOrImsiGroupQueryForIMSIGroupQuery() {
        final MultivaluedMap<String, String> requestParameters = new MultivaluedMapImpl();
        requestParameters.putSingle(TYPE_PARAM, TYPE_IMSI);
        requestParameters.putSingle(GROUP_NAME_PARAM, SAMPLE_IMSI_GROUP);
        assertThat(objToTest.isImsiOrImsiGroupQuery(requestParameters),
                is(true));
    }

    @Test
    public void testisImsiOrImsiGroupQueryForIMSIQuery() {
        final MultivaluedMap<String, String> requestParameters = new MultivaluedMapImpl();
        requestParameters.putSingle(TYPE_PARAM, TYPE_IMSI);
        requestParameters.putSingle(IMSI_PARAM, String.valueOf(SAMPLE_IMSI));
        assertThat(objToTest.isImsiOrImsiGroupQuery(requestParameters),
                is(true));
    }

    @Test
    public void testisImsiOrImsiGroupQueryForPTMSIQuery() {
        final MultivaluedMap<String, String> requestParameters = new MultivaluedMapImpl();
        requestParameters.putSingle(TYPE_PARAM, TYPE_PTMSI);
        requestParameters.putSingle(PTMSI_PARAM, "98989");
        assertThat(objToTest.isImsiOrImsiGroupQuery(requestParameters),
                is(false));
    }

    @Test
    public void testupdateTemplateWithErrRAWTablesForLTEAndNONLTE() {
        final Map<String, Object> templateParameters = new HashMap<String, Object>();

        final String key = KEY_TYPE_ERR;
        final List<String> listOfTables = new ArrayList<String>();
        listOfTables.add("rawLteTable1");
        listOfTables.add("rawTable2");
        final Map<String, String> parameterMapForOrdinaryTables = new HashMap<String, String>();
        parameterMapForOrdinaryTables.put(KEY_PARAM, KEY_TYPE_ERR);

        final Map<String, String> parameterMapForLTETables = new HashMap<String, String>();
        parameterMapForLTETables.put(KEY_PARAM, KEY_TYPE_ERR);
        parameterMapForLTETables.put(IS_LTE_VIEW, "true");

        mockery.checking(new Expectations() {
            {

                one(mockedRawTableFetcher).getRAWTables(
                        mockedFormattedDateTimeRange, key, RAW_ALL_ERR_TABLES);
                will(returnValue(listOfTables));
            }
        });
        final FormattedDateTimeRange dateTimeRange = mockedFormattedDateTimeRange;
        final boolean result = objToTest.updateTemplateWithRAWTables(
                templateParameters, dateTimeRange, key, RAW_ALL_ERR_TABLES);
        assertThat(result, is(true));
        assertThat((List<String>) templateParameters.get(RAW_ALL_ERR_TABLES),
                is(listOfTables));

    }

    @Test
    public void testupdateTemplateWithErrRAWTablesIncludingLTEReturnsTrueWhenOrdinaryTablesFoundButNoLTETablesFound() {
        final Map<String, Object> templateParameters = new HashMap<String, Object>();

        final String key = KEY_TYPE_ERR;
        final List<String> emptyList = new ArrayList<String>();
        final Map<String, String> parameterMapForOrdinaryTables = new HashMap<String, String>();
        parameterMapForOrdinaryTables.put(KEY_PARAM, KEY_TYPE_ERR);

        final Map<String, String> parameterMapForLTETables = new HashMap<String, String>();
        parameterMapForLTETables.put(KEY_PARAM, KEY_TYPE_ERR);
        parameterMapForLTETables.put(IS_LTE_VIEW, "true");
        parameterMapForLTETables.put(IS_COMBINED_VIEW, "false");
        parameterMapForLTETables.put(IS_DT_VIEW, "false");

        final List<String> listOfNonLteTables = new ArrayList<String>();
        listOfNonLteTables.add("rawTable2");
        mockery.checking(new Expectations() {
            {
                one(mockedRawTableFetcher).getRAWTables(
                        mockedFormattedDateTimeRange, key, RAW_LTE_TABLES);

                one(mockedRawTableFetcher).getRAWTables(
                        mockedFormattedDateTimeRange, key, RAW_NON_LTE_TABLES);
                will(returnValue(listOfNonLteTables));
            }
        });
        final FormattedDateTimeRange dateTimeRange = mockedFormattedDateTimeRange;
        final boolean result = objToTest.updateTemplateWithRAWTables(
                templateParameters, dateTimeRange, key, RAW_LTE_TABLES,
                RAW_NON_LTE_TABLES);
        assertThat(result, is(true));
        assertThat((List<String>) templateParameters.get(RAW_LTE_TABLES),
                is(emptyList));
        assertThat((List<String>) templateParameters.get(RAW_NON_LTE_TABLES),
                is(listOfNonLteTables));

    }

    @Test
    public void testupdateTemplateWithErrRAWTablesIncludingLTEReturnsFalseWhenNoTablesFound() {
        final Map<String, Object> templateParameters = new HashMap<String, Object>();

        final String key = KEY_TYPE_ERR;
        final Map<String, String> parameterMapForOrdinaryTables = new HashMap<String, String>();
        parameterMapForOrdinaryTables.put(KEY_PARAM, KEY_TYPE_ERR);
        parameterMapForOrdinaryTables.put(IS_LTE_VIEW, "false");
        parameterMapForOrdinaryTables.put(IS_COMBINED_VIEW, "false");
        parameterMapForOrdinaryTables.put(IS_DT_VIEW, "false");

        final Map<String, String> parameterMapForLTETables = new HashMap<String, String>();
        parameterMapForLTETables.put(KEY_PARAM, KEY_TYPE_ERR);
        parameterMapForLTETables.put(IS_LTE_VIEW, "true");
        parameterMapForLTETables.put(IS_COMBINED_VIEW, "false");
        parameterMapForLTETables.put(IS_DT_VIEW, "false");

        mockery.checking(new Expectations() {
            {
                one(mockedRawTableFetcher).getRAWTables(
                        mockedFormattedDateTimeRange, key, RAW_LTE_TABLES);
                one(mockedRawTableFetcher).getRAWTables(
                        mockedFormattedDateTimeRange, key, RAW_NON_LTE_TABLES);
            }
        });
        final FormattedDateTimeRange dateTimeRange = mockedFormattedDateTimeRange;
        final boolean result = objToTest.updateTemplateWithRAWTables(
                templateParameters, dateTimeRange, key, RAW_LTE_TABLES,
                RAW_NON_LTE_TABLES);
        assertThat(result, is(false));

    }

    @Test
    public void testupdateTemplateWithErrRAWTablesForLTE() {
        final Map<String, Object> templateParameters = new HashMap<String, Object>();

        final String key = KEY_TYPE_ERR;
        final List<String> rawNonLteTableNames = new ArrayList<String>();
        rawNonLteTableNames.add("rawErrTable1");
        final List<String> rawLTETableNames = new ArrayList<String>();
        rawLTETableNames.add("rawLteErrTable2");
        mockery.checking(new Expectations() {
            {
                one(mockedRawTableFetcher).getRAWTables(
                        mockedFormattedDateTimeRange, key, RAW_LTE_TABLES);
                will(returnValue(rawLTETableNames));
                one(mockedRawTableFetcher).getRAWTables(
                        mockedFormattedDateTimeRange, key, RAW_NON_LTE_TABLES);
                will(returnValue(rawNonLteTableNames));
            }
        });
        final FormattedDateTimeRange dateTimeRange = mockedFormattedDateTimeRange;
        final boolean result = objToTest.updateTemplateWithRAWTables(
                templateParameters, dateTimeRange, key, RAW_LTE_TABLES,
                RAW_NON_LTE_TABLES);
        assertThat(result, is(true));
        assertThat((List<String>) templateParameters.get(RAW_LTE_TABLES),
                is(rawLTETableNames));
        assertThat((List<String>) templateParameters.get(RAW_NON_LTE_TABLES),
                is(rawNonLteTableNames));

    }

    @Test
    public void testupdateTemplateWithErrRAWTablesReturnsFalseWhenNoTablesFound() {
        final Map<String, Object> templateParameters = new HashMap<String, Object>();
        final String key = KEY_TYPE_ERR;
        final Map<String, Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put(KEY_PARAM, KEY_TYPE_ERR);
        parameterMap.put(IS_LTE_VIEW, "false");
        parameterMap.put(IS_COMBINED_VIEW, "false");
        parameterMap.put(IS_DT_VIEW, "false");

        mockery.checking(new Expectations() {
            {
                one(mockedRawTableFetcher).getRAWTables(
                        mockedFormattedDateTimeRange, key, RAW_ERR_TABLES);
            }
        });
        final FormattedDateTimeRange dateTimeRange = mockedFormattedDateTimeRange;
        final boolean result = objToTest.updateTemplateWithRAWTables(
                templateParameters, dateTimeRange, key, RAW_ERR_TABLES);
        assertThat(result, is(false));

    }

    @Test
    public void testupdateTemplateWithErrRAWTables() {
        final Map<String, Object> templateParameters = new HashMap<String, Object>();

        final String key = KEY_TYPE_ERR;
        final List<String> rawTableNames = new ArrayList<String>();
        rawTableNames.add("rawErrTable1");
        mockery.checking(new Expectations() {
            {
                one(mockedRawTableFetcher).getRAWTables(
                        mockedFormattedDateTimeRange, key, RAW_ERR_TABLES);
                will(returnValue(rawTableNames));
            }
        });
        final FormattedDateTimeRange dateTimeRange = mockedFormattedDateTimeRange;
        final boolean result = objToTest.updateTemplateWithRAWTables(
                templateParameters, dateTimeRange, key, RAW_ERR_TABLES);
        assertThat(result, is(true));
        assertThat((List<String>) templateParameters.get(RAW_ERR_TABLES),
                is(rawTableNames));

    }

    @Test
    public void testisMediaTypeApplicationCSVWhenItsJson() {
        final List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
        acceptableMediaTypes.add(MediaType.APPLICATION_JSON_TYPE);
        final HttpHeaders mockedHttpHeaders = mockHttpHeadersAndExpectCall(acceptableMediaTypes);
        objToTest.setHttpHeaders(mockedHttpHeaders);
        assertThat(objToTest.isMediaTypeApplicationCSV(), is(false));
    }

    @Test
    public void testisMediaTypeApplicationCSVIsFalseWhenListIsNull() {
        final List<MediaType> acceptableMediaTypes = null;
        final HttpHeaders mockedHttpHeaders = mockHttpHeadersAndExpectCall(acceptableMediaTypes);
        objToTest.setHttpHeaders(mockedHttpHeaders);
        assertThat(objToTest.isMediaTypeApplicationCSV(), is(false));
    }

    @Test
    public void testisMediaTypeApplicationCSVWhenMoreThanOneAcceptableMediaTypeAndContainsApplicationCSV() {
        final List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
        acceptableMediaTypes.add(MediaType.APPLICATION_ATOM_XML_TYPE);
        addMediaCSV(acceptableMediaTypes);
        acceptableMediaTypes.add(MediaType.APPLICATION_SVG_XML_TYPE);
        final HttpHeaders mockedHttpHeaders = mockHttpHeadersAndExpectCall(acceptableMediaTypes);
        objToTest.setHttpHeaders(mockedHttpHeaders);
        assertThat(objToTest.isMediaTypeApplicationCSV(), is(true));
    }

    @Test
    public void testisMediaTypeApplicationCSVWhenMoreThanOneAcceptableMediaTypeAndDoesNotContainApplicationCSV() {
        final List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
        acceptableMediaTypes.add(MediaType.APPLICATION_ATOM_XML_TYPE);
        acceptableMediaTypes.add(MediaType.APPLICATION_SVG_XML_TYPE);
        final HttpHeaders mockedHttpHeaders = mockHttpHeadersAndExpectCall(acceptableMediaTypes);
        objToTest.setHttpHeaders(mockedHttpHeaders);
        assertThat(objToTest.isMediaTypeApplicationCSV(), is(false));
    }

    @Test
    public void testisMediaTypeApplicationCSVWhenItIs() {
        final List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
        addMediaCSV(acceptableMediaTypes);
        final HttpHeaders mockedHttpHeaders = mockHttpHeadersAndExpectCall(acceptableMediaTypes);
        objToTest.setHttpHeaders(mockedHttpHeaders);
        assertThat(objToTest.isMediaTypeApplicationCSV(), is(true));
    }

    private void addMediaCSV(final List<MediaType> acceptableMediaTypes) {
        acceptableMediaTypes.add(APPLICATION_CSV_MEDIA_TYPE);
    }

    private HttpHeaders mockHttpHeadersAndExpectCall(
            final List<MediaType> acceptableMediaTypes) {
        final HttpHeaders mockedHttpHeaders = mockery.mock(HttpHeaders.class);
        mockery.checking(new Expectations() {
            {
                one(mockedHttpHeaders).getAcceptableMediaTypes();
                will(returnValue(acceptableMediaTypes));

            }
        });

        return mockedHttpHeaders;
    }

    @Test
    public void testCheckRequiredParametersExistWhenTheyDo() {
        final MultivaluedMap<String, String> requestParameters = new MultivaluedMapImpl();
        requestParameters.putSingle(BSC_PARAM, "BSC1");
        requestParameters.putSingle(CAUSE_CODE_PARAM, "3");
        requestParameters.putSingle(TIME_QUERY_PARAM, "30");
        final String[] requiredParameters = new String[] { BSC_PARAM,
                CAUSE_CODE_PARAM };
        assertThat(objToTest.checkRequiredParametersExistAndReturnErrorMessage(
                requestParameters, requiredParameters), is((String) null));
    }
    @Ignore
    @Test
    public void testCheckRequiredParametersExistWhenOneMissing() {
        final MultivaluedMap<String, String> requestParameters = new MultivaluedMapImpl();
        requestParameters.putSingle(BSC_PARAM, "BSC1");
        requestParameters.putSingle(TIME_QUERY_PARAM, "30");
        final String[] requiredParameters = new String[] { BSC_PARAM,
                CAUSE_CODE_PARAM };
        final String result = objToTest
                .checkRequiredParametersExistAndReturnErrorMessage(
                        requestParameters, requiredParameters);
        jsonAssertUtils.assertJSONErrorResult(result, getClass().getName());
        jsonAssertUtils.assertResultContains(result,
                E_INVALID_OR_MISSING_PARAMS, getClass().getName());
        jsonAssertUtils.assertResultContains(result, CAUSE_CODE_PARAM,
                getClass().getName());
    }

    @Ignore
    @Test
    public void testCheckRequiredParametersExistWhenTwoMissing() {
        final MultivaluedMap<String, String> requestParameters = new MultivaluedMapImpl();
        requestParameters.putSingle(TIME_QUERY_PARAM, "30");
        final String[] requiredParameters = new String[] { BSC_PARAM,
                CAUSE_CODE_PARAM };
        final String result = objToTest
                .checkRequiredParametersExistAndReturnErrorMessage(
                        requestParameters, requiredParameters);
        jsonAssertUtils.assertJSONErrorResult(result, getClass().getName());
        jsonAssertUtils.assertResultContains(result,
                E_INVALID_OR_MISSING_PARAMS, getClass().getName());
        jsonAssertUtils.assertResultContains(result, CAUSE_CODE_PARAM,
                getClass().getName());
        jsonAssertUtils.assertResultContains(result, BSC_PARAM, getClass()
                .getName());
    }

    @Test
    public void testGetCountValueValidInput() {

        final MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.putSingle(MAX_ROWS, "300");
        final int count = objToTest.getCountValue(map, 500);
        assertEquals(300, count);
    }

    @Test
    public void testGetCountValueInputGreaterThanMax() {
        final MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.putSingle(MAX_ROWS, "700");
        final int count = objToTest.getCountValue(map, 500);
        assertEquals(500, count);
    }

    @Test
    public void testGetCountValueInvalidInput() {
        final MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.putSingle(MAX_ROWS, "FAIL");
        final int count = objToTest.getCountValue(map, 500);
        assertEquals(500, count);
    }

    private void allowGetStartAndEndDateTimeOnDateTimeRange() {
        mockery.checking(new Expectations() {
            {
                allowing(mockedFormattedDateTimeRange).getStartDateTime();
                will(returnValue(startDateTime));
                allowing(mockedFormattedDateTimeRange).getEndDateTime();
                will(returnValue(endDateTime));
            }
        });

    }

    private void testGetDVTPIntervalChart(final long timerange,
            final int expectedInterval) {
        final int interval = objToTest.getDVTPIntervalForChart(timerange);
        assertEquals(expectedInterval, interval);
    }

    private void testGetDVTPIntervalGrid(final long timerange,
            final int expectedInterval) {
        final int interval = objToTest.getDVTPIntervalForGrid(timerange);
        assertEquals(expectedInterval, interval);
    }

    @Test
    public void testGetDVTPChartInterval_30min() {
        testGetDVTPIntervalChart(THIRTY_MINUTES_VALUE, FIVE_MINUTE_VALUE);
    }

    @Test
    public void testGetDVTPChartInterval_2hr() {
        testGetDVTPIntervalChart(MINUTES_IN_2_HOURS, TEN_MINUTE_VALUE);
    }

    @Test
    public void testGetDVTPChartInterval_3hr() {
        testGetDVTPIntervalChart(MINUTES_IN_3_HOURS, FIFTEEN_MINUTE_VALUE);
    }

    @Test
    public void testGetDVTPChartInterval_4hr() {
        testGetDVTPIntervalChart(MINUTES_IN_4_HOURS, THIRTY_MINUTES_VALUE);
    }

    @Test
    public void testGetDVTPChartInterval_6hr() {
        testGetDVTPIntervalChart(MINUTES_IN_6_HOURS, THIRTY_MINUTES_VALUE);
    }

    @Test
    public void testGetDVTPChartInterval_12hr() {
        testGetDVTPIntervalChart(MINUTES_IN_12_HOURS, SIXTY_MINUTE_VALUE);
    }

    @Test
    public void testGetDVTPChartInterval_1day() {
        testGetDVTPIntervalChart(MINUTES_IN_A_DAY, MINUTES_IN_2_HOURS);
    }

    @Test
    public void testGetDVTPChartInterval_2day() {
        testGetDVTPIntervalChart(MINUTES_IN_2_DAY, MINUTES_IN_4_HOURS);
    }

    @Test
    public void testGetDVTPChartInterval_5day() {
        testGetDVTPIntervalChart(MINUTES_IN_5_DAY, MINUTES_IN_6_HOURS);
    }

    @Test
    public void testGetDVTPChartInterval_1week() {
        testGetDVTPIntervalChart(MINUTES_IN_A_WEEK, MINUTES_IN_A_DAY);
    }

    @Test
    public void testGetDVTPGridInterval_2hr() {
        testGetDVTPIntervalGrid(MINUTES_IN_2_HOURS, FIVE_MINUTE_VALUE);
    }

    @Test
    public void testGetDVTPGridInterval_1day() {
        testGetDVTPIntervalGrid(MINUTES_IN_A_DAY, FIFTEEN_MINUTE_VALUE);
    }

    @Test
    public void testGetDVTPGridInterval_1week() {
        testGetDVTPIntervalGrid(MINUTES_IN_A_WEEK, MINUTES_IN_A_DAY);
    }

    @Test
    public void testGetDateTimeRangeOfChartAndSummaryGrid_DAY() {
        final List<String> listOfTechPacks = new ArrayList<String>();
        listOfTechPacks.add(TechPackData.EVENT_E_SGEH);
        listOfTechPacks.add(TechPackData.EVENT_E_LTE);
        FormattedDateTimeRange newDateTimeRange1, newDateTimeRange2;
        FormattedDateTimeRange expectedDateTimeRange = createTimeRange(
                "2011-06-21 00:00", "2011-06-29 00:00");
        newDateTimeRange1 = objToTest.getDateTimeRangeOfChartAndSummaryGrid(
                expectedDateTimeRange, "TR_4", listOfTechPacks);
        newDateTimeRange2 = objToTest.getDateTimeRangeOfChartAndSummaryGrid(
                expectedDateTimeRange, "TEST", listOfTechPacks);
    }

    private FormattedDateTimeRange createTimeRange(final String startDateTime,
            final String endDateTime) {
        return DateTimeRange
                .getFormattedDateTimeRange(
                        startDateTime,
                        endDateTime,
                        ApplicationConfigManager.ENIQ_EVENTS_DT_TIME_DELAY_1MIN_DATA_DEFAULT_MINUTES,
                        ApplicationConfigManager.ENIQ_EVENTS_DT_TIME_DELAY_15MIN_DATA_DEFAULT_MINUTES,
                        ApplicationConfigManager.ENIQ_EVENTS_DT_TIME_DELAY_DAY_DATA_DEFAULT_MINUTES);

    }

    @Test
    public void testGetTzOffsetForCSV() {
        String tzOffsetQuery = objToTest.getTzOffsetForCSV("00530");
        assertEquals("TZ Offset for CSV Queries", "0330", tzOffsetQuery);
    }

    @Test
    public void testGetTemplate() {
        final MultivaluedMap<String, String> requestParameters = new MultivaluedMapImpl();
        requestParameters.putSingle(BSC_PARAM, "BSC1");
        requestParameters.putSingle(TIME_QUERY_PARAM, "30");
        requestParameters.putSingle(GROUP_NAME_PARAM, SAMPLE_IMSI_GROUP);
        requestParameters.putSingle(TAC_PARAM, EXCLUSIVE_TAC_GROUP);

        mockery.checking(new Expectations() {
            {
                allowing(mockedTimeRangeSelector).getTimeRangeType("TR_4",
                        true, true);
                will(returnValue("VIEW_NAME"));
                allowing(mockedTemplateMappingEngine).getTemplate(EVENT_VOLUME,
                        requestParameters, null, "VIEW_NAME");
                will(returnValue("TEMPLATE_NAME"));
                allowing(mockedTemplateMappingEngine).getTemplate(EVENT_VOLUME,
                        requestParameters, null);
                will(returnValue("TEMPLATE_NAME"));
            }
        });

        objToTest.setTimeRangeSelector(mockedTimeRangeSelector);
        objToTest.setTemplateMappingEngine(mockedTemplateMappingEngine);
        objToTest.getTemplate(EVENT_VOLUME, requestParameters, null, "TR_4",
                true);
        objToTest.getTemplate(EVENT_VOLUME, requestParameters, null);

    }

    @Test
    public void testGetNoSuchDisplayErrorResponse() {
        String response = objToTest.getNoSuchDisplayErrorResponse("TEST");
        String responseContains = response.substring(62, 66);
        assertEquals("TEST", responseContains);
    }

    class StubbedBaseResource extends BaseResource {

        @Override
        protected String getData(final String requestID,
                final MultivaluedMap<String, String> requestParameters)
                throws WebApplicationException {
            return null;
        }

        @Override
        protected List<String> checkParameters(
                final MultivaluedMap<String, String> requestParameters) {
            return null;
        }

        @Override
        protected boolean isValidValue(
                final MultivaluedMap<String, String> requestParameters) {
            return false;
        }

    }

}
