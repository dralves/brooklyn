package org.overpaas.entities;

import groovy.util.ObservableMap

import java.util.Collection
import java.util.Map
import java.util.concurrent.ConcurrentHashMap

import org.overpaas.decorators.Startable
import org.overpaas.util.EntityStartUtils
import org.overpaas.util.SerializableObservables.SerializableObservableMap

public interface Application extends Entity, Startable {
	public void registerEntity(Entity entity);
	Collection<Entity> getEntities();
}

public abstract class AbstractApplication extends AbstractGroup implements Application {
    public AbstractApplication(Map properties=[:]) {
        super(properties, null)
    }
    
    // --------------- application records all entities in use ----------------------
    final ObservableMap entities = new SerializableObservableMap(new ConcurrentHashMap<String,Entity>());
 
    public void registerEntity(Entity entity) {
        entities.put entity.id, entity
    }
    
    Collection<Entity> getEntities() { entities }

    protected void initApplicationRegistrant() { /* do nothing; we register ourself later */ }
    // record ourself as an entity in the entity list
    { registerWithApplication this }
    
    // ---------------- lifecycle ---------------------
    
    /** default start will start all Startable children */
    public void start(Map addlProperties=[:]) {
        EntityStartUtils.startGroup addlProperties, this
    }
}