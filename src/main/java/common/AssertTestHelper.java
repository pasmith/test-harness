package common;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicLong;

import test.TestFixture;
import assertions.Assert;

/**
 * class that lets tests access default scoped members.
 * @author patrick
 *
 */
public class AssertTestHelper implements TestFixture {

	/**
	 * 
	 */
	private static Class<?> assertClass;
	
	static {
		try {
			assertClass = Class.forName( "common.Assert" );
		} catch (ClassNotFoundException e) {
			assertClass = Assert.class;
		}
	}
	
    /**
     * helper method for tests to return value of global counter. This helper is
     * used by all tests. It allows tests to access the default scoped
     * 'global' member of Assert.
     *
     * @return value of global counter
     * @throws NoSuchFieldException 
     * @throws SecurityException 
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    public static long getGlobalAssertionCount() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    	AtomicLong global = (AtomicLong) assertClass.getDeclaredField( "global" ).get( null );
        return global.get();
    }

    /**
     * helper method for tests to return value of global counter. This helper is
     * used by the AssertTest class to test the Assert utility framework. It
     * allows tests to access the default scoped 'anonymous' member of Assert.
     *
     * @return value of anonymous counter
     * @throws NoSuchFieldException 
     * @throws IllegalAccessException 
     * @throws SecurityException 
     * @throws IllegalArgumentException 
     */
    public static long getAnonymousAssertionCount() throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
    	AtomicLong anonymous = (AtomicLong) assertClass.getField( "anonymous" ).get( null );
        return anonymous.get();
    }

    /**
     * helper method that allows tests to reset the counters in the Assert framework.
     * @throws NoSuchMethodException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws SecurityException 
     * @throws IllegalArgumentException 
     *
     */
    public static void resetCounters() throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    	assertClass.getMethod( "reset" ).invoke( null );
    }

    /**
     * don't allow this class to be created.
     */
    private AssertTestHelper(){}
}