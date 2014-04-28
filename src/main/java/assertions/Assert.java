package assertions;

import static test.utilities.Utilities.isEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import test.TestFixture;

/**
 * helper class that defines counters that can be used to track impact of defensive programming.
 * 
 * @author patrick
 *
 */
public final class Assert implements TestFixture {

	// executor
	private static final ExecutorService executor = Executors.newSingleThreadExecutor();
	
	// convenient definition of action to increment anonymous counter
	private static final Runnable incrementGlobal = new Runnable() {			
		@Override public final void run() {
			// update global counter
			global.incrementAndGet();				
		}
	};

	// convenient definition of action to increment anonymous counter
	private static final Runnable incrementAnonymous = new Runnable() {			
		@Override public final void run() {
			// update anonymous counter
			anonymous.incrementAndGet();				
		}
	};

			
	// hide constructor
	private Assert(){}

	// create a counter that can be used to track any assert called.
	public static final AtomicLong global = new AtomicLong();

	// create a counter that can be used to track anonymous assert calls only.
	public static final AtomicLong anonymous = new AtomicLong();

	// add named counters
	static final Map<String,AtomicLong> counters = Collections.synchronizedMap( new HashMap<String,AtomicLong>() );

	
	/**
	 * anonymous wrapper around 
	 * @param condition
	 */
	public static void _assert( boolean condition ){
		// TODO get use case name from stack trace?		
		// increment global counters in background thread
		evaluate( incrementAnonymous, condition);
	}

	/**
	 * evaluate the condition
	 * @param incrementanonymous2 
	 * @param condition
	 */
	private static void evaluate(Runnable incrementCounter, boolean condition) {
		// increment the counter specified and the global counter
		executor.execute( incrementGlobal );
		executor.execute( incrementCounter );
		
		// evaluate the assertion
		assert condition;
	}
	
	/**
	 * named counter
	 * 
	 * @param name counter name
	 * @param condition
	 */
	public static void _assert(String name, boolean condition) {
		// determine which counter to increment and evaluate the condition
		evaluate( isEmpty(name) ? incrementAnonymous : getIncrementCounterAction( name ), condition);
	}

	/**
	 * create an increment action for the counter associated with the given counter name.
	 * @param name of counter
	 * @return action to increment the specified counter
	 */
	private static Runnable getIncrementCounterAction(String name) {
		assert !isEmpty( name );
		AtomicLong counter = counters.get( name );
		if( counter == null ) {
			counter = new AtomicLong();
			counters.put( name, counter );
		}
		final AtomicLong c = counter;
		return new Runnable() {			
			@Override public void run() {
				c.incrementAndGet();
			}
		};
	}

	/**
	 * convenience method to reset counters. reserved for internal use only.
	 */
	public static final void reset() {
		global.set( 0l );
		anonymous.set( 0l );
		counters.clear();		
	}
}
