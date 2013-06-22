package assertions;

import test.TestFixture;

/**
 * class that lets tests access default scoped members.
 * @author patrick
 *
 */
public class AssertTestHelper implements TestFixture {
    /**
     * helper method for tests to return value of global counter. This helper is
     * used by all tests. It allows tests to access the default scoped
     * 'global' member of Assert.
     *
     * @return value of global counter
     */
    public static long getGlobalAssertionCount() {
        return Assert.global.get();
    }

    /**
     * helper method for tests to return value of global counter. This helper is
     * used by the AssertTest class to test the Assert utility framework. It
     * allows tests to access the default scoped 'anonymous' member of Assert.
     *
     * @return value of anonymous counter
     */
    public static long getAnonymousAssertionCount() {
        return Assert.anonymous.get();
    }

    /**
     * helper method that allows tests to reset the counters in the Assert framework.
     *
     */
    public static void resetCounters() {
        Assert.reset();
    }

    /**
     * don't allow this class to be created.
     */
    private AssertTestHelper(){}
}