package brooklyn.entity;

import brooklyn.management.ManagementContext;


/**
 * TODO javadoc
 */
public interface Application extends Entity {
//    void registerEntity(Entity entity);
//    Collection<Entity> getEntities();
//    void addEntityChangeListener(PropertyChangeListener listener);
    
    ManagementContext getManagementContext();
}
