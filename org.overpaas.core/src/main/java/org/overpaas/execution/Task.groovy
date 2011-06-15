package org.overpaas.execution

import java.util.Collection;
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.overpaas.util.LanguageUtils;


class TaskStub {
	final String id = LanguageUtils.newUid()
//	Object jvm = null //pointer to jvm where something is running, for distributed tasks 
}

class Task<T> implements Future<T> {
	Closure job
	public final String displayName
	public final String description
	
	public Task(Map flags=[:], Runnable job) { this(flags, { job.run() } ) }
	public Task(Map flags=[:], Callable job) { this(flags, { job.call() } ) }
	public Task(Map flags=[:], Closure job) {
		this.job = job
		description = flags.remove("description")
		displayName = flags.remove("displayName")
		if (flags) throw new IllegalArgumentException("Unsupported flags passed to task: "+flags)
	}
	
	boolean cancelled = false
	Future result

	public synchronized boolean cancel(boolean mayInterruptIfRunning) {
		if (result) result.cancel(mayInterruptIfRunning)
		cancelled = true
	}

	public boolean isCancelled() {
		cancelled || result?.isCancelled()
	}

	/** true if all children are done */
	public boolean isDone() {
		result?.isDone()
	}

	public T get() throws InterruptedException, ExecutionException {
		blockUntilStarted()
		result.get()
	}

	public synchronized void blockUntilStarted() {
		while (true) {
			if (cancelled) throw new CancellationException()
			if (result==null) wait()
			if (result!=null) return
		}
	}
	
	public Collection get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		//FIXME add support for timeouts
		get()
//		
//		def v = Futures.run(collect { Future f -> { -> f.get(timeout, unit) } } )
//		v.collect { Future f -> f.get() }
	}

}