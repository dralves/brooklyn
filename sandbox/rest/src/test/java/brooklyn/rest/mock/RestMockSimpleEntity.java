package brooklyn.rest.mock;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Effector;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.basic.Description;
import brooklyn.entity.basic.MethodEffector;
import brooklyn.entity.basic.NamedParameter;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensor;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;

public class RestMockSimpleEntity extends AbstractEntity {

    private static final Logger log = LoggerFactory.getLogger(RestMockSimpleEntity.class);
    
    public RestMockSimpleEntity() {
        super();
    }

    public RestMockSimpleEntity(Entity owner) {
        super(owner);
    }

    public RestMockSimpleEntity(Map flags, Entity owner) {
        super(flags, owner);
    }

    public RestMockSimpleEntity(Map flags) {
        super(flags);
    }

    @SetFromFlag("sampleConfig")
    public static final ConfigKey<String> SAMPLE_CONFIG = new BasicConfigKey<String>(
            String.class, "brooklyn.rest.mock.sample.config", "Mock sample config", "DEFAULT_VALUE");

    public static final AttributeSensor<String> SAMPLE_SENSOR = new BasicAttributeSensor<String>(
            String.class, "brooklyn.rest.mock.sample.sensor", "Mock sample sensor");

    public static final Effector<String> SAMPLE_EFFECTOR = new MethodEffector<String>(RestMockSimpleEntity.class, "sampleEffector");
    
    public String sampleEffector(@NamedParameter("param1") @Description("param one") String param1, 
            @NamedParameter("param2") Integer param2) {
        log.info("Invoked sampleEffector("+param1+","+param2+")");
        String result = ""+param1+param2;
        setAttribute(SAMPLE_SENSOR, result);
        return result;
    }
    
}
