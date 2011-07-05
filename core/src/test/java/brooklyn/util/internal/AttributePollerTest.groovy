package brooklyn.util.internal

import static org.testng.Assert.*

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.annotations.Test

import brooklyn.entity.LocallyManagedEntity
import brooklyn.entity.basic.AbstractEntity
import brooklyn.event.adapter.AttributePoller
import brooklyn.event.adapter.ValueProvider
import brooklyn.event.basic.BasicAttributeSensor
import brooklyn.test.TestUtils

/**
 * Test the operation of the {@link AttributePoller} class.
 */
public class AttributePollerTest {
    private static final Logger log = LoggerFactory.getLogger(AttributePollerTest.class)

    @Test
    public void sensorUpdatedPeriodically() {
        AbstractEntity entity = new LocallyManagedEntity()
        AttributePoller attributePoller = new AttributePoller(entity, [period:50])
        
        final AtomicInteger desiredVal = new AtomicInteger(1)
        BasicAttributeSensor<Integer> FOO = [ Integer, "foo", "My foo" ]
        attributePoller.addSensor(FOO, { return desiredVal.get() } as ValueProvider)

        TestUtils.executeUntilSucceeds( { assertEquals(entity.getAttribute(FOO), 1) } as Runnable )

        desiredVal.set(2)
        TestUtils.executeUntilSucceeds( { assertEquals(entity.getAttribute(FOO), 2) } as Runnable )
    }
    
    @Test
    public void sensorUpdateDefaultPeriodIsUsed() {
        final int PERIOD = 250
        AbstractEntity entity = new LocallyManagedEntity()
        AttributePoller attributePoller = new AttributePoller(entity, [period:PERIOD, connectDelay:0])
        
        List<Long> callTimes = [] as CopyOnWriteArrayList
        
        BasicAttributeSensor<Integer> FOO = [ Integer, "foo", "My foo" ]
        attributePoller.addSensor(FOO, { callTimes.add(System.currentTimeMillis()); return 1 } as ValueProvider)
        
        Thread.sleep(500)
        assertApproxPeriod(callTimes, PERIOD, 500)
    }

    @Test
    public void sensorUpdatePeriodOverrideIsUsed() {
        final int PERIOD = 250
        // Create an entity and configure it with the above JMX service
        AbstractEntity entity = new LocallyManagedEntity()
        AttributePoller attributePoller = new AttributePoller(entity, [period:1000, connectDelay:0])
        
        List<Long> callTimes = [] as CopyOnWriteArrayList
        
        BasicAttributeSensor<Integer> FOO = [ Integer, "foo", "My foo" ]
        attributePoller.addSensor(FOO, { callTimes.add(System.currentTimeMillis()); return 1 } as ValueProvider, PERIOD)
        
        Thread.sleep(500)
        assertApproxPeriod(callTimes, PERIOD, 500)
    }
    

    private void assertApproxPeriod(List<Long> actual, int expectedInterval, long expectedDuration) {
        final long ACCEPTABLE_VARIANCE = 200
        long minNextExpected = actual.get(0);
        actual.each {
            assertTrue it >= minNextExpected && it <= (minNextExpected+ACCEPTABLE_VARIANCE), 
                    "expected=$minNextExpected, actual=$it, interval=$expectedInterval, series=$actual, duration=$expectedDuration"
            minNextExpected += expectedInterval
        }
        int expectedSize = expectedDuration/expectedInterval
        assertTrue Math.abs(actual.size()-expectedSize) <= 1, "actualSize=${actual.size()}, series=$actual, duration=$expectedDuration, interval=$expectedInterval"
    }
}