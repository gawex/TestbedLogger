package cz.vsb.cbe.testbed.sql;

import android.content.Context;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SmallTest
public class TestbedDatabaseInstrumentationTest {

    TestbedDatabase mTestbedDatabase;
    TestbedDevice mTestbedDevice1, mTestbedDevice2, mTestbedDevice3, mTestbedDevice4;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mTestbedDatabase = new TestbedDatabase(context);
        mTestbedDevice1 = new TestbedDevice(8228, "Testbed", 7, "88:6B:0F:D3:0E:CF", 0, 1614943832203L);
        mTestbedDevice2 = new TestbedDevice(8228, "Testbed", 4, "88:6B:0F:D3:0E:CF", 1, 1614943832203L);
        mTestbedDevice3 = new TestbedDevice(8228, "Testbed", 7, "88:6B:0F:D3:0E:CE", 0, 1614943832203L);
        mTestbedDevice4 = new TestbedDevice(1234, "Testbed v2", 4, "AA:BB:CC:DD:EE:FF", 0, 1234567890000L);
    }

    @Test
    public void testTestbedDevice1_STORED_CONSISTENTLY() {
        long result = mTestbedDatabase.getStoredStatusOfTestbedDevice(mTestbedDevice1);
        assertEquals((long) TestbedDevice.STORED_CONSISTENTLY, result);
    }

    @Test
    public void testTestbedDevice2_STORED_BUT_MODIFIED() {
        long result = mTestbedDatabase.getStoredStatusOfTestbedDevice(mTestbedDevice2);
        assertEquals((long) TestbedDevice.STORED_BUT_MODIFIED, result);
    }

    @Test
    public void testTestbedDevice3_STORED_BUT_MODIFIED() {
        long result = mTestbedDatabase.getStoredStatusOfTestbedDevice(mTestbedDevice3);
        assertEquals((long) TestbedDevice.STORED_BUT_MODIFIED, result);
    }

    @Test
    public void testTestbedDevice4_NOT_STORED() {
        long result = mTestbedDatabase.getStoredStatusOfTestbedDevice(mTestbedDevice4);
        assertEquals((long) TestbedDevice.NOT_STORED, result);
    }
}
