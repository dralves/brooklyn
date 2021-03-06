package brooklyn.entity.basic;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.config.ConfigKey;
import brooklyn.location.Location;
import brooklyn.util.ResourceUtils;

/**
 * An abstract implementation of the {@link SoftwareProcessDriver}.
 */
public abstract class AbstractSoftwareProcessDriver implements SoftwareProcessDriver {

	private static final Logger log = LoggerFactory.getLogger(AbstractSoftwareProcessDriver.class);
	
    protected final EntityLocal entity;
    private final Location location;
    
    public AbstractSoftwareProcessDriver(EntityLocal entity, Location location) {
        this.entity = checkNotNull(entity, "entity");
        this.location = checkNotNull(location, "location");
    }
	
    /*
     * (non-Javadoc)
     * @see brooklyn.entity.basic.SoftwareProcessDriver#rebind()
     */
    @Override
    public void rebind() {
        // no-op
    }

    /**
     * Start the entity.
     *
     * this installs, configures and launches the application process. However,
     * users can also call the {@link #install()}, {@link #customize()} and
     * {@link #launch()} steps independently. The {@link #postLaunch()} will
     * be called after the {@link #launch()} metheod is executed, but the
     * process may not be completely initialised at this stage, so care is
     * required when implementing these stages.
     *
     * @see #stop()
     */
	@Override
	public void start() {
        waitForConfigKey(ConfigKeys.INSTALL_LATCH);
		install();
        
        waitForConfigKey(ConfigKeys.CUSTOMIZE_LATCH);
		customize();
        
        waitForConfigKey(ConfigKeys.LAUNCH_LATCH);
		launch();
        
        postLaunch();  
	}

	@Override
	public abstract void stop();
	
	public abstract void install();
	public abstract void customize();
	public abstract void launch();
    
    @Override
    public void kill() {
        stop();
    }
    
    /**
     * Implement this method in child classes to add some post-launch behavior
     */
	public void postLaunch() {}
    
	@Override
	public void restart() {
		stop();
		start();
	}
	
	public EntityLocal getEntity() { return entity; } 

	public Location getLocation() { return location; } 
	
    public InputStream getResource(String url) {
        return new ResourceUtils(entity).getResourceFromUrl(url);
    }
		
    protected void waitForConfigKey(ConfigKey<?> configKey) {
        Object val = entity.getConfig(configKey);
        if (val != null) log.debug("{} finished waiting for {} (value {}); continuing...", new Object[] {this, configKey, val});
    }
}
