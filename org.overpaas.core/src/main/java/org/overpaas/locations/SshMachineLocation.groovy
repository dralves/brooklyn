package org.overpaas.locations;

import java.io.File
import java.util.Map

import org.overpaas.entities.Entity
import org.overpaas.types.Location
import org.overpaas.util.SshJschTool
import org.slf4j.Logger
import org.slf4j.LoggerFactory

public class SshMachineLocation implements Location {
	String name
	String user = null
	String host
 
	Map properties=[:]
		
	/**
	 * These properties are separate to the entity hierarchy properties,
	 * used by certain types of entities as documented in their setup
	 * (e.g. JMX port) 
	 */
	public Map getProperties() { properties }

	/** convenience for running a script, returning the result code */
	public int run(Map props=[:], String command) {
		assert host : "host must be specified for $this"
		
		if (!user) user=System.getProperty "user.name"
		def t = new SshJschTool(user:user, host:host);
		t.connect()
		int result = t.execShell props, command
		t.disconnect()
		result
		
//		ExecUtils.execBlocking "ssh", (user?user+"@":"")+host, command
	}
    
    public int copyTo(File src, String destination) {
        def conn = new SshJschTool(user:user, host:host)
        conn.connect()
        int result = conn.copyToServer [:], src, destination
        conn.disconnect()
        result
    }

    @Override
    public String toString() {
        return name?name+"["+host+"]":host;
    }
}
	
// TODO OS-X failure, if no recent command line ssh
// ssh_askpass: exec(/usr/libexec/ssh-askpass): No such file or directory
// Permission denied, please try again.
// ssh_askpass: exec(/usr/libexec/ssh-askpass): No such file or directory
// Received disconnect from ::1: 2: Too many authentication failures for alex 

public abstract class SshBasedJavaAppSetup {
    private static final Logger logger = LoggerFactory.getLogger(SshBasedJavaAppSetup.class);;
        
	String overpaasBaseDir = "/tmp/overpaas"
	String installsBaseDir = overpaasBaseDir+"/installs"

	/** convenience to generate string -Dprop1=val1 -Dprop2=val2 for use with java */		
	public static String toJavaDefinesString(Map m) {
		StringBuffer sb = []
		m.each { sb.append("-D"); sb.append(it.key); if (sb.value!='') { sb.append('=\''); sb.append(it.value); sb.append('\' ') } }
		return sb.toString().trim()
	}
 
	/** convenience to record a value on the location to ensure each instance gets a unique value */
	protected int getNextValue(String field, int initial) {
		def v = entity.properties[field]
		if (!v) {
			println "retrieving "+field+", "+entity.location.properties
			synchronized (entity.location) {
				v = entity.location.properties["next_"+field] ?: initial
				entity.location.properties["next_"+field] = (v+1)
			}
			println "retrieved "+field+", "+entity.location.properties
			entity.properties[field] = v
		}
		v
	}
 
	public Map getJvmStartupProperties() {
		[:]+getJmxConfigOptions()
	}
 
	public int getJmxPort() {
		println "setting jmxHost on $entity as "+entity.location.host
		entity.properties.jmxHost = entity.location.host
		getNextValue("jmxPort", 10100)
	}
 
	public Map getJmxConfigOptions() {
		//TODO security!
		[ 'com.sun.management.jmxremote':'',
		  'com.sun.management.jmxremote.port':getJmxPort(),
		  'com.sun.management.jmxremote.ssl':false,
		  'com.sun.management.jmxremote.authenticate':false
		]
	}
	protected String makeInstallScript(String ...lines) { 
		String result = """\
if [ -f $installDir/../INSTALL_COMPLETION_DATE ] ; then echo software is already installed ; exit ; fi
mkdir -p $installDir && \\
cd $installDir/.. && \\
""";
		lines.each { result += it + "&& \\\n" }
		result += """\
date > INSTALL_COMPLETION_DATE
exit
""" 
	}

	public String getInstallScript() { null }
	public abstract String getRunScript();
    public abstract String getDeployScript(String filename);
	
	public void start(SshMachineLocation loc) {
		synchronized (getClass()) {
			String s = getInstallScript()
			if (s) {
				int result = loc.run(out: System.out, s)
				if (result) throw new IllegalStateException("failed to start $entity (exit code $result)")
			}
		}

		def result = loc.run(out: System.out, getRunScript())
		if (result) throw new IllegalStateException("failed to start $entity (exit code $result)")
	}
        
    /**
     * Copies f to loc:$installsBaseDir and invokes this.getDeployScript
     * for further processing on server.
     */
    public void deploy(File f, SshMachineLocation loc) {
        def target = new File(new File(installsBaseDir), f.getName()).toString()
        int copySuccess = loc.copyTo f, target
        String deployScript = getDeployScript(target)
        if (deployScript) {
            int result = loc.run(out:System.out, deployScript)
            if (result) {
                logger.error "Failed to deploy $f to $loc"
            } else {
                logger.trace "Deployed $f to $loc"
            }
        }
    }
        
	Entity entity
	String appBaseDir

	public SshBasedJavaAppSetup(Entity entity) {
		this.entity = entity
		appBaseDir = overpaasBaseDir + "/" + "app-"+entity.getApplication()?.id
	}			
}
	