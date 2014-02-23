package test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author patrick
 *
 */
public class JunitWrapper implements TestFixture {

	/*
	 * ASSERTION COUNTER WRAPPERS
	 * these methods wrap assertions so that we can count how many assertions were evaluated during the test run.
	 * 
	 * TODO add additional wrappers around rest of junit methods
	 * 
	 */
	private final AtomicInteger assertionCounter = new AtomicInteger();
	
	
	/**
	 * @param string
	 * @param empty
	 */
	public final void assertFalse(String message, boolean condition) {
		assertionCounter.incrementAndGet();
		org.junit.Assert.assertFalse(message, condition);		
	}

	/**
	 * @param string
	 * @param b
	 */
	public final void assertTrue(String message, boolean condition) {
		assertionCounter.incrementAndGet();
		org.junit.Assert.assertTrue(message, condition);		
	}

	/**
	 * @param string
	 * @param result
	 */
	public final void assertNotNull(String message, Object object) {
		assertionCounter.incrementAndGet();
		org.junit.Assert.assertNotNull(message, object);
	}

	/**
	 * @param string
	 * @param result
	 */
	public final void assertNull(String message, Object object) {
		assertionCounter.incrementAndGet();
		org.junit.Assert.assertNull(message, object);
	}

	/**
	 * @param message
	 * @param expected
	 * @param actual
	 * @param tolerance 
	 */
	public final void assertEquals(String message, Object expected, Object actual) {
		assertionCounter.incrementAndGet();
		org.junit.Assert.assertEquals(message, expected, actual);
	}
	/**
	 * @param message
	 * @param expected
	 * @param actual
	 * @param tolerance 
	 */
	public final void assertEquals(String message, double expected, double actual, double tolerance) {
		assertionCounter.incrementAndGet();
		org.junit.Assert.assertEquals(message, expected, actual, tolerance);
	}
	
	/**
	 * @param message
	 * @param expected
	 * @param actual
	 */
	public final void assertSame(String message, Object expected, Object actual) {
		assertionCounter.incrementAndGet();
		org.junit.Assert.assertSame(message, expected, actual);
	}
	/**
	 * @param message
	 * @param expected
	 * @param actual
	 */
	protected final void assertNotSame(String message, Object unexpected, Object actual) {
		assertionCounter.incrementAndGet();
		org.junit.Assert.assertNotSame(message, unexpected, actual);
	}
	
	/**
	 * @param message
	 * @param expected
	 * @param actual
	 */
	public final void fail(String message, Throwable...thrown) {
		assertionCounter.incrementAndGet();
		for( Throwable t : thrown ) {
			t.printStackTrace();
		}
		org.junit.Assert.fail(message);
	}

	/**
	 * @return
	 */
	public final int getAssertionCount() {
		return assertionCounter.get();
	}

	public final void resetAssertionCounter() {
		assertionCounter.set( 0 );
	}
	
}
