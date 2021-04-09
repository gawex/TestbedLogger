package cz.vsb.cbe.testbed;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import cz.vsb.cbe.testbed.activitiesAndServices.BluetoothLeService;
import cz.vsb.cbe.testbed.sql.Record;
import cz.vsb.cbe.testbed.utils.StatisticalData;

import static org.junit.Assert.assertEquals;


public class StatisticalDataUnitTest {

    private static final String DATA_KEY = BluetoothLeService.TEMPERATURE_DATA;
    // "cz.vsb.cbe.testbed.TEMPERATURE_DATA"

    private StatisticalData mStatisticalData;

    @Before
    public void setUp() {
        List<Record> testRecords = new ArrayList<>();
        testRecords.add(new Record(1234, 555, DATA_KEY, -4.24f, 1617524535000L));
        testRecords.add(new Record(1236, 555, DATA_KEY, 18.25f, 1617524536000L));
        testRecords.add(new Record(1237, 555, DATA_KEY, 20.12f, 1617524537000L));
        testRecords.add(new Record(1238, 555, DATA_KEY, 21.34f, 1617524538000L));
        testRecords.add(new Record(1242, 555, DATA_KEY, 22.55f, 1617524539000L));
        testRecords.add(new Record(1245, 555, DATA_KEY, 23.78f, 1617524540000L));
        testRecords.add(new Record(1246, 555, DATA_KEY, 24.98f, 1617524541000L));
        testRecords.add(new Record(1247, 555, DATA_KEY, 27.45f, 1617524542000L));
        testRecords.add(new Record(1248, 555, DATA_KEY, 36.54f, 1617524543000L));
        mStatisticalData = new StatisticalData(testRecords);
    }

    @Test
    public void testGetDataSetSize_isCorrect() {
        assertEquals(9, mStatisticalData.getDataSetSize());
    }

    @Test
    public void testGetDataSetSum_isCorrect() {
        assertEquals(190.77f, mStatisticalData.getDataSetSum(), 0.01f);
    }

    @Test
    public void testGetMeanValue_isCorrect() {
        assertEquals(21.19, mStatisticalData.getMeanValue(), 0.01f);
    }

    @Test
    public void testGetMinValue_isCorrect() {
        assertEquals(-4.24f, mStatisticalData.getMinValue().getValue(), 0f);
    }

    @Test
    public void testGetFirtsQuartile_isCorrect() {
        assertEquals(21.34f, mStatisticalData.getFirstQuartile().getValue(), 0f);
    }

    @Test
    public void testGetMedian_isCorrect() {
        assertEquals(23.78f, mStatisticalData.getMedian().getValue(), 0f);
    }

    @Test
    public void testGetThirdQuartile_isCorrect() {
        assertEquals(27.45f, mStatisticalData.getThirdQuartile().getValue(), 0f);
    }

    @Test
    public void testGetMaxValue_isCorrect() {
        assertEquals(36.54f, mStatisticalData.getMaxValue().getValue(), 0f);
    }

    @Test
    public void testStandardDeviation_isCorrect() {
        assertEquals(2.24f, mStatisticalData.getStandardDeviation(), 0.01f);
    }
}