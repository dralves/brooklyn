package brooklyn.util.pool;

import com.google.common.base.Function;

/**
 * See discussion at http://code.google.com/p/guava-libraries/issues/detail?id=683.
 * This API is inspired by that proposed by kevinb@google.com
 * 
 * There are two ways to use the pool.
 * 
 * Passive:
 * 
 * <code>
 *   Pool<Expensive> pool = ...
 *   Lease<Expensive> lease = pool.leaseObject();
 *   try {
 *     Expensive o = lease.leasedObject();
 *     doSomethingWith(o);
 *   } finally {
 *     lease.close();
 *   }
 * </code>
 *
 * Or active:
 * 
 * <code>
 *   Pool<Expensive> pool = ...
 *   pool.exec(
 *       new Function<Expensive,Void>() {
 *         public Void apply(Expensive o) {
 *           doSomethingWith(o);
 *           return null;
 *         }
 *       });
 * </code>
 * 
 * @see BasicPool
 * 
 * @author aled
 */
public interface Pool<T> {

    Lease<T> leaseObject();
    
    <R> R exec(Function<T,R> receiver);
    
    void closePool();
}
