package brooklyn.util.internal

import java.io.IOException
import java.util.Map

import javax.management.AttributeList
import javax.management.MBeanInfo
import javax.management.MBeanServerConnection
import javax.management.ObjectInstance
import javax.management.ObjectName
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL

public class JmxSensorEffectorTool {
	public final String jmxUrl;
	JMXConnector jmxc;
	MBeanServerConnection mbsc = null;
	
	long connectPollPeriodMillis = 500;
	
	public JmxSensorEffectorTool(String jmxUrl) {
		this.jmxUrl = jmxUrl;
	}
	public JmxSensorEffectorTool(String host, int port) {
		this.jmxUrl = "service:jmx:rmi:///jndi/rmi://"+host+":"+port+"/jmxrmi";
	}

	public boolean isConnected() {
		return (jmxc && mbsc);
	}
	/** attempts to connect immediately */
	public void connect() throws IOException {
		if (jmxc) jmxc.close()
		JMXServiceURL url = new JMXServiceURL(jmxUrl)
		jmxc = JMXConnectorFactory.connect(url, null);
		mbsc = jmxc.getMBeanServerConnection();
	}
	/** continuously attempts to connect (blocking), for at least the indicated amount of time; or indefinitely if -1 */
	public boolean connect(int timeoutMillis) {
		println "invoking connect to "+jmxUrl
		long thisStartTime = System.currentTimeMillis()
		long endTime = thisStartTime + timeoutMillis
		if (timeoutMillis==-1) endTime = Long.MAX_VALUE
		while (thisStartTime <= endTime) {
			thisStartTime = System.currentTimeMillis()
			println "$thisStartTime trying connection to "+jmxUrl
			try {
				connect()
				return true
			} catch (IOException e) {
				println ""+System.currentTimeMillis()+" failed connection to "+jmxUrl+" ("+e+")"
			}
			Thread.sleep connectPollPeriodMillis
		}
		false
	}
	
	public void disconnect() {
		if (jmxc) {
			jmxc.close()
			jmxc = null
			mbsc = null
		}
	}
	
	public void checkConnected() {
		if (!isConnected()) throw new IllegalStateException("JmxTool must be connected")
	}
	
	/** returns all attributes on a specific named object */
	public Map getAttributes(String objectName) {
		checkConnected()
		ObjectName mxbeanName = new ObjectName(objectName);
		ObjectInstance bean = mbsc.getObjectInstance(mxbeanName)
		Map r = [:]
//		println "bean $it";
		MBeanInfo info = mbsc.getMBeanInfo(bean.getObjectName())
		info.getAttributes().each { r[it.getName()] = null }
		AttributeList attrs = mbsc.getAttributes bean.getObjectName(), r.keySet() as String[]
		attrs.asList().each {
//				println "  attr value "+it.getName()+" = "+it.getValue()+"  ("+it.getValue().getClass()+")"
			r[it.getName()] = it.getValue();
//			info.getNotifications().each { println "  notf $it" }
		}
		r
	}
	
	/** returns all attributes for all children matching a given objectName string;
	 * structure:
	 * 
	 *  children {
	 *    MBean1[objName] {
	 *      attributes {
	 *        size: 1
	 *      }
	 *      notifications {
	 *        desiredSize {
	 *          value: 4
	 *          seqNo: 8
	 *          oldValue: 1
	 *        }
	 *      }
	 *    }
	 *    MBean2[objName2] {
	 *      attributes {
	 *        size: 0
	 *      }
	 *  }
	 *  totals {
	 *    size: 1
	 *  }
	 *  
	 **/
	//TODO allow overrides for attributes which should take min, max, rather than sum
	public Map getChildrenAttributesWithTotal(String objectName) {
		checkConnected()
		ObjectName mxbeanName = new ObjectName(objectName);
		Set<ObjectInstance> matchingBeans = mbsc.queryMBeans mxbeanName, null
		Map r = [:]
		r.totals = [:]
		matchingBeans.each {
			ObjectInstance bean = it
//			println "bean $it";
			if (!r.children) r.children=[:]
			def c = r.children[it.toString()] = [:]
			MBeanInfo info = mbsc.getMBeanInfo(it.getObjectName())
			c.attributes = [:]
			info.getAttributes().each {
//				println "  attr $it"
				c.attributes[it.getName()] = null
			}
			AttributeList attrs = mbsc.getAttributes it.getObjectName(), c.attributes.keySet() as String[]
			attrs.asList().each {
//				println "  attr value "+it.getName()+" = "+it.getValue()+"  ("+it.getValue().getClass()+")"
				c.attributes[it.getName()] = it.getValue();
				if (it.getValue() in Number)
					r.totals[it.getName()] = (r.totals[it.getName()]?:0) + it.getValue()
			}
//			info.getNotifications().each { println "  notf $it" }
		}
		r
	}
	
}