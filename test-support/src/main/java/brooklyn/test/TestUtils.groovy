package brooklyn.test

import static java.util.concurrent.TimeUnit.*
import static org.testng.Assert.*

import groovy.time.TimeDuration

import java.net.URLConnection
import java.util.Map
import java.util.concurrent.Callable

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import brooklyn.entity.Entity

/**
 * Helper functions for tests of Tomcat, JBoss and others.
 */
public class TestUtils {
    private static final Logger log = LoggerFactory.getLogger(TestUtils.class)
    
    private TestUtils() { }

    /** True if two attempts to connect to the port succeed. */
    public static boolean isPortInUse(int port, long retryAfterMillis=0) {
        try {
            def s = new Socket("localhost", port)
            s.close()
            if (retryAfterMillis>0) {
                log.debug "port {} still open, waiting 1s for it to really close", port
                //give it 1s to close
                Thread.sleep retryAfterMillis
                s = new Socket("localhost", port)
                s.close()
            }
            log.debug "port {} still open (conclusive)", port
            return true
        } catch (ConnectException e) {
            return false
        }
    }

    public static boolean urlRespondsWithStatusCode200(String url) {
        def connection = connectToURL(url)
        int status = ((HttpURLConnection) connection).getResponseCode()
        log.info "connection to {} gives {}", url, status
        if (status == 404)
            throw new Exception("Connection to $url gave 404");
        return status == 200
    }
    
    /** Connects to the given url and returns the connection. */
    public static URLConnection connectToURL(String u) {
        URL url = [u]
        URLConnection connection = url.openConnection()
        connection.connect()
        connection.getContentLength() // Make sure the connection is made.
        return connection
    }

    /** convenience for entities to ensure they shutdown afterwards */
    public static void executeUntilSucceedsWithShutdown(Map flags=[:], Entity entity, Runnable r) {
        executeUntilSucceedsWithFinallyBlock(flags, r, { entity.shutdown() })
    }

    /**
     * Convenience method for cases where we need to test until something is true.
     *
     * The runnable will be invoked periodically until it succesfully concludes.
     * Additionally, a finally block can be supplied.
     * <p>
     * The following flags are supported:
     * <ul>
     * <li>boolean - abortOnError (default true)
     * <li>abortOnException - (default false),
     * <li>useGroovyTruth - (defaults to false; any result code apart from 'false' will be treated as success including null; ignored for Runnables which aren't Callables),
     * <li>timeout - (a TimeDuration, defaults to 30*SECONDS), period (a TimeDuration, defaults to 500*MILLISECONDS),
     * <li>maxAttempts - (integer, Integer.MAX_VALUE)
     * </ul>
     *
     * @param flags, accepts the flags listed above
     * @param r
     * @param finallyBlock
     */
    public static void executeUntilSucceedsWithFinallyBlock(Map flags=[:], Runnable r, Runnable finallyBlock={}) {
        log.debug "abortOnError = {}", flags.abortOnError
        boolean abortOnException = flags.abortOnException ?: false
        boolean abortOnError = flags.abortOnError ?: true
        boolean useGroovyTruth = flags.useGroovyTruth ?: false
        TimeDuration timeout = flags.timeout ?: 30000
        TimeDuration period = flags.period ?: 500
        int maxAttempts = flags.maxAttempts ?: Integer.MAX_VALUE
        try {
            Throwable lastException = null;
            Object result;
            long lastAttemptTime = 0;
            long startTime = System.currentTimeMillis()
            long expireTime = startTime+timeout.toMilliseconds()
            int attempt = 0;
            while (attempt<maxAttempts && lastAttemptTime<expireTime) {
                try {
                    attempt++
                    lastAttemptTime = System.currentTimeMillis()
                    if (r in Callable) {
                        result = r.call();
                        log.trace "Attempt {} after {} ms: {}", attempt, System.currentTimeMillis() - startTime, result
                        if (useGroovyTruth) {
                            if (result) return;
                        } else if (result!=false) return;
                    } else {
                        r.run()
                        return
                    }
                    lastException = null
                } catch(Throwable e) {
                    lastException = e
                    log.trace "Attempt {} after {} ms: {}", attempt, System.currentTimeMillis() - startTime, e.message
                    if (abortOnException) throw e
                    if (abortOnError && e in Error) throw e
                }
                if (period.toMilliseconds()>0) Thread.sleep period.toMilliseconds()
            }
            log.trace "Exceeded max attempts or timeout - {} attempts lasting {} ms", attempt, System.currentTimeMillis()-startTime
            if (lastException != null)
                throw lastException
            fail "invalid result code $result"
        } finally {
            finallyBlock.run()
        }
    }

    public static class BooleanWithMessage {
        boolean value; String message;
        public BooleanWithMessage(boolean value, String message) {
            this.value = value; this.message = message;
        }
        public boolean asBoolean() {
            return value
        }
        public String toString() {
            return message
        }
    }
}