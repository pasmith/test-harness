package test;

import static common.AssertTestHelper.getGlobalAssertionCount;
import static test.utilities.Utilities.isEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import org.junit.Before;
import org.junit.Test;

/**
 * this class sets up the basic test framework. it defines how to configure
 * runtime environment, test data, and defines template methods so that
 * performance and thread safety testing are done in a consistent way. this
 * class even wraps junit assertions so that we can measure at runtime how many
 * assertions were actually evaluated during test execution.
 * 
 * @author patrick
 * 
 */
public abstract class BaseTestCase<C, D> implements TestFixture {

	protected static final String COUNT_FOR_THIS_USER = "count.for.this.user";

	/**
	 * data cleansing - some answers include quotes
	 */
	protected static final String QUOTE_REPLACEMENT = Matcher.quoteReplacement("\\\"");

	// constants for messages displayed from test harness
	static final String MUST_CONTAIN_SEQUENCE = "the value '%s' must contain the sequence '%s'";
	static final String NOT_EMPTY = "the value of '%s' cannot be empty.";
	static final String MUST_BE_POSITIVE = "the value of '%s' must be greater than 0.";
	static final String MUST_BE_ZERO = "the value of '%s' must be 0 at start of test.";
	static final String MUST_BE = "the value of '%s' must be an '%s'.";
	static final String MUST_CONTAIN = "the parameter map for '%s' must contain a '%s' field.";
	static final String ASSERTION_COUNT = "%d assertions were evaluated during the %s test.";
	static final String STARTING = "starting '%s' test for '%s'.";

	/**
	 * provides ability to randomize some aspects of the test if necessary.
	 */
	protected static final Random random = new Random();

	/**
	 * shared counter to use for thread safety testing
	 */
	protected static AtomicInteger SHARED_COUNTER = new AtomicInteger(0);

	/**
	 * performance and thread safety tests must provide this value.
	 * 
	 * for performance tests, the string is expected to have a place holder for
	 * timing data, for example: "it took %.2f seconds to load 4000 records."
	 * 
	 * for thread safety tests, the string is expected to have a place holder
	 * for both timing data as well as user count, for example:
	 * "it took %.2f seconds for %d users to load 15000 records."
	 * 
	 * 
	 * note: the dynamic values like '4000' above can be added in the
	 * <code>generateTestData</code> since it has access to this message.)
	 */
	protected static final String RESULT_MESSAGE = "result.message";

	/**
	 * set this property to <code>true</code> in the parameter map returned by
	 * <code>getThreadSafetyTestingParameters</code> to temporarily disable
	 * thread safety testing.
	 */
	protected static final String DISABLE_THREAD_SAFETY_TEST = "disable.thread.safety.test";

	/**
	 * set this property to <code>true</code> in the parameter map returned by
	 * <code>getPerformanceTestingParameters</code> to temporarily disable
	 * performance testing.
	 */
	protected static final String DISABLE_PERFORMANCE_TEST = "disable.performance.test";

	/**
	 * the max number of items to use for performance testing - also used for
	 * thread safety testing.
	 */
	protected static final String NUM_ITEMS = "number.items";

	/**
	 * the id of the user - used for thread safety testing to distinguish who is
	 * performing which action.
	 */
	protected static final String USER_ID = "user.id";

	/**
	 * the number simultaneous access to set up to test for thread safety
	 */
	protected static final String NUMBER_OF_SIMULTANEOUS_USERS = "number.of.simultaneous.users";

	/**
     *
     */
	private static final String BASIC_PERFORMANCE_TESTING_MESSAGE_TEMPLATE = "%s %s %s in %s seconds.";

	/**
	 * the userId to use in the test
	 */
	protected String userId = null;
	
	/**
	 * for thread safety testing
	 */
	protected ExecutorService executioners = null;

	/**
	 * junit wrapper to keep track of the number of assertions that were
	 * actually evaluated during test execution.
	 */
	private final JunitWrapper junitWrapper = new JunitWrapper();

	/**
	 * initialize the background process runner
	 */
	@Before
	public final void initializeTestServices() {
		executioners = Executors.newCachedThreadPool();
	}

	/*
	 * TEST SETUP
	 */
	/**
	 * usually empty, but can be used to set up default values
	 * 
	 * @param params
	 * @return
	 * @throws Exception
	 */
	protected Map<String, Object> getFunctionalTestingParameters() {
		// TODO lazy load the map
		if( userId == null || userId.isEmpty() ) {
			userId = "user";
		}
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put( USER_ID, userId );
        params.put( NUM_ITEMS, 20 );
		return params;
	}

	/**
	 * set things up like the size of a batch for stress testing
	 * 
	 * @param params
	 * @return
	 * @throws Exception
	 */
	protected Map<String, Object> getPerformanceTestingParameters() {
		// define parameters
		final Map<String, Object> params = getFunctionalTestingParameters();
		params.put(DISABLE_PERFORMANCE_TEST, false);
		params.put(RESULT_MESSAGE, getPerfTestResultMessage());
        params.put( NUM_ITEMS, 50 );
		return params;
	}

    /**
     * @return
     * @throws Exception
     */
    protected final String getUser() {
        return (String) getFunctionalTestingParameters().get( USER_ID );
    }

	/**
	 * specify the message to use for performance testing
	 * 
	 * @return
	 */
	protected abstract String getPerfTestResultMessage();

	/*
	 * (non-Javadoc)
	 * 
	 * @see test.BaseTestCase#getThreadSafetyTestingParameters()
	 */
	protected Map<String, Object> getThreadSafetyTestingParameters()
			throws Exception {
		// determine number of users
		final int n = random.nextInt(10) + 2;

		// define parameters
		final Map<String, Object> params = getPerformanceTestingParameters();
		params.put(NUMBER_OF_SIMULTANEOUS_USERS, n);
		params.put(DISABLE_THREAD_SAFETY_TEST, false);
		params.put(RESULT_MESSAGE, getThreadSafetyResultMessage());

		return params;
	}

	/**
	 * specify the message to use for thread safety testing
	 * 
	 * @return
	 */
	protected abstract String getThreadSafetyResultMessage();

	/**
	 * call back method that lets tests generate the data they need for testing.
	 * 
	 * this method is called inside the test template method before
	 * getComponentUnderTest. so methods decorated with @Before and @BeforeClass
	 * can still be used to initialize states and services.
	 * 
	 * @return
	 * @throws Exception
	 */
	protected abstract D generateTestData(Map<String, Object> params) throws Exception;

	/**
	 * get the component to test
	 * 
	 * @return component under test
	 * @throws IOException
	 * @throws Exception
	 * @throws InvalidConfigurationException
	 */
	protected abstract C getComponentUnderTest() throws Exception;

	/*
	 * Accessors
	 */

	/**
	 * indicates whether the current test execution is a performance test
	 * 
	 * @return
	 */
	protected final boolean isPerformanceTest() {
		return isInMethod("test.BaseTestCase", "verifyPerformance");
	}

	/**
	 * indicates whether the current test execution is a thread safety test
	 * 
	 * @param params
	 * @return
	 */
	protected final boolean isThreadSafetyTest(final Map<String, Object> params) {
		return params.get(NUMBER_OF_SIMULTANEOUS_USERS) != null;
	}

	/**
	 * utility method to determine if the method specified is in the current
	 * execution stack trace.
	 * 
	 * @param names
	 *            the method name to look for. class name is optional: you can
	 *            specify method name only ("methodName"), or class name and
	 *            method name ("className", "methodName").
	 * 
	 * @return <code>true</code> if the method is found in the current thread's
	 *         execution stack, <code>false</code> otherwise.
	 */
	protected final boolean isInMethod(final String... names) {
		assertTrue("method name must be specified", names.length > 0);
		final String methodName = names.length > 1 ? names[1].trim() : names[0]
				.trim();
		final String className = names.length > 1 ? names[0].trim() : null;
		assertFalse("method name cannot be empty", isEmpty(methodName));
		if (names.length > 1) {
			assertFalse("class name cannot be empty", isEmpty(className));
		}
		boolean inMethod = false;
		for (final StackTraceElement element : Thread.getAllStackTraces().get(
				Thread.currentThread())) {
			if (((className == null) || className
					.equals(element.getClassName()))
					&& methodName.equals(element.getMethodName())) {
				inMethod = true;
				break;
			}
		}
		return inMethod;
	}

	/*
	 * COMPONENT UNDER TEST
	 */

	/**
	 * check the functionality of the component under test
	 * 
	 * @throws Exception
	 */
	protected abstract void verifyFunctionality(Map<String, Object> params,
			C componentUnderTest, D testData, AtomicInteger countsForThisUser)
			throws Exception;

	/**
	 * check for bottle necks in component under test
	 */
	protected final String verifyPerformance(final Map<String, Object> params,
			final C componentUnderTest, final D testData) throws Exception {
		// see if performance testing has been disabled
		String message = "performance testing has been disabled.";
		if (disabled(params.get(DISABLE_PERFORMANCE_TEST))) {
			System.out.println(message);
			return message;
		}

		// check the parameters map
		final Object result = params.get(RESULT_MESSAGE);
		org.junit.Assert.assertNotNull(
				String.format(MUST_CONTAIN, "performance", RESULT_MESSAGE),
				result);
		org.junit.Assert.assertTrue(
				String.format(MUST_BE, NUMBER_OF_SIMULTANEOUS_USERS, "String"),
				result instanceof String);
		org.junit.Assert.assertFalse(String.format(NOT_EMPTY, RESULT_MESSAGE),
				isEmpty((String) result));
		org.junit.Assert.assertFalse(
				String.format(MUST_CONTAIN_SEQUENCE, RESULT_MESSAGE, "%.2f"),
				((String) result).indexOf("%.2f") < 0);

		// run the performance test
		final long start = System.currentTimeMillis();
		final AtomicInteger counter = new AtomicInteger(0);
		int n = random.nextInt( ((Number)params.get( NUM_ITEMS )).intValue() );
		for( int i=0;i<n;i++ ) {
			verifyFunctionality(params, componentUnderTest, testData, counter);
		}
		setCountsForUser(params, counter.get());
		final float time = (System.currentTimeMillis() - start) / 1000f;
		message = String.format((String) result,
				params.get(COUNT_FOR_THIS_USER), time);
		System.out.println(message);

		return message;
	}

	/**
	 * helper method to handle parsing value of boolean 'disableXXX' fields.
	 * 
	 * used for disabling performance and thread safety testing during
	 * development.
	 * 
	 * @param value
	 * @return
	 */
	private final boolean disabled(final Object value) {
		if (value == null) {
			return false;
		} else {
			if (value instanceof Boolean) {
				return (Boolean) value;
			} else {
				return Boolean.parseBoolean((String) value);
			}
		}
	}

	/**
	 * verify that the component is thread safe
	 */
	private final String verifyThreadSafety(final Map<String, Object> params,
			final D testData, final ExecutorService executor) throws Exception {
		// see if thread safety testing has been disabled
		String message = "thread safety testing has been disabled.";
		if (disabled(params.get(DISABLE_THREAD_SAFETY_TEST))) {
			System.out.println(message);
			return message;
		}

		// check the parameters map
		// TODO format these with jansi
		final Object result = params.get(RESULT_MESSAGE);
		org.junit.Assert.assertNotNull(
				String.format(MUST_CONTAIN, "thread safety", RESULT_MESSAGE),
				result);
		org.junit.Assert.assertTrue(
				String.format(MUST_BE, NUMBER_OF_SIMULTANEOUS_USERS, "String"),
				result instanceof String);
		org.junit.Assert.assertFalse(String.format(NOT_EMPTY, RESULT_MESSAGE),
				isEmpty((String) result));
		org.junit.Assert.assertFalse(
				String.format(MUST_CONTAIN_SEQUENCE, RESULT_MESSAGE, "%.2f"),
				((String) result).indexOf("%.2f") < 0);
		org.junit.Assert.assertFalse(
				String.format(MUST_CONTAIN_SEQUENCE, RESULT_MESSAGE, "%d"),
				((String) result).indexOf("%d") < 0);

		final Object num = params.get(NUMBER_OF_SIMULTANEOUS_USERS);
		org.junit.Assert.assertNotNull(String.format(MUST_CONTAIN,
				"thread safety", NUMBER_OF_SIMULTANEOUS_USERS), num);
		org.junit.Assert
				.assertTrue(String.format(MUST_BE,
						NUMBER_OF_SIMULTANEOUS_USERS, "Integer"),
						num instanceof Number);
		org.junit.Assert.assertTrue(
				String.format(MUST_BE_POSITIVE, NUMBER_OF_SIMULTANEOUS_USERS),
				((Number) num).intValue() > 0);

		// run the thread safety test. start each users in parallel on different
		// threads
		final int n = ((Number) num).intValue();
		final ArrayList<Callable<String>> users = new ArrayList<Callable<String>>(
				n);
		final long start = System.currentTimeMillis();
		for (int i = 0; i < n; i++) {
			final int userNum = i;
			// for each user, run a performance test
			users.add(new Callable<String>() {
				@Override
				public final String call() throws Exception {
					// load the performance data parameters
					final Map<String, Object> params = getPerformanceTestingParameters();
					// add user Id
					params.put(USER_ID, String.format("user-%d", userNum));
					params.put(NUMBER_OF_SIMULTANEOUS_USERS, num);
					// generate a performance data set for this user
					final D data = generateTestData(params);
					// run the performance test for this user in parallel to
					// other users
					return verifyPerformance(params, getComponentUnderTest(),
							data);
				}
			});
		}

		for (final Future<String> user : executor.invokeAll(users)) {
			try {
				user.get();
			} catch (final Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// tests are done.
		final float time = (System.currentTimeMillis() - start) / 1000f;
		message = String.format((String) result, n, SHARED_COUNTER.intValue(),
				time);
		System.out.println(message);

		return message;
	}

	/*
	 * TEST EXECUTION
	 */

	/**
	 * test template method that checks functionality, performance, and thread
	 * safety
	 * 
	 * @throws Exception
	 */
	@Test
	public final void verifyComponent() throws Exception {

		// // get the provider under test
		// final C cut = getComponentUnderTest();

		// run the functional test
		Map<String, Object> params = getFunctionalTestingParameters();
		try {
			// initialize the counter to 0
			resetAssertionCounter();

			// reset test counter to 0
			SHARED_COUNTER.set(0);
			// provider.setLogContext( "automated.testing", String.format(
			// "%s.%s", getClass().getSimpleName(), counter.incrementAndGet() )
			// );

			// get value of global defensive programming assertion counter
			final long before = getGlobalAssertionCount();

			// run the test
			System.out.println(String.format(STARTING, "functional", getClass()
					.getSimpleName()));
			final AtomicInteger counter = new AtomicInteger(0);
			verifyFunctionality(params, getComponentUnderTest(),
					generateTestData(params), counter);

			// set the counts for this user
			setCountsForUser(params, counter.get());

			// report on the number of new defensive programming assertions that
			// were evaluated while running tests
			System.out
					.println(String
							.format("%d runtime defensive programming assertions were evaluated.",
									getGlobalAssertionCount() - before));
		} finally {
			System.out.println(String.format(ASSERTION_COUNT,
					getAssertionCount(), "functional"));
			System.out.println();
		}

		// reset if needed
		reset();

		// run the performance test
		params = getPerformanceTestingParameters();
		try {
			// initialize the counter to 0
			resetAssertionCounter();

			// reset test counter to 0
			SHARED_COUNTER.set(0);

			// get value of global defensive programming assertion counter
			final long before = getGlobalAssertionCount();

			// run the test
			System.out.println(String.format(STARTING, "performance",
					getClass().getSimpleName()));
			verifyPerformance(params, getComponentUnderTest(),
					generateTestData(params));

			// report on the number of new defensive programming assertions that
			// were evaluated while running tests
			System.out
					.println(String
							.format("%d runtime defensive programming assertions were evaluated.",
									getGlobalAssertionCount() - before));

		} finally {
			System.out.println(String.format(ASSERTION_COUNT,
					getAssertionCount(), "performance"));
			System.out.println();
		}

		// reset if needed
		reset();

		// run the thread safety test
		params = getThreadSafetyTestingParameters();
		try {
			// initialize the counter to 0
			resetAssertionCounter();

			// reset test counter to 0
			SHARED_COUNTER.set(0);

			// get value of global defensive programming assertion counter
			final long before = getGlobalAssertionCount();

			// run the test
			System.out.println(String.format(STARTING, "thread safety",
					getClass().getSimpleName()));
			verifyThreadSafety(params, generateTestData(params), executioners);

			// report on the number of new defensive programming assertions that
			// were evaluated while running tests
			System.out
					.println(String
							.format("%d runtime defensive programming assertions were evaluated.",
									getGlobalAssertionCount() - before));

		} finally {
			System.out.println(String.format(ASSERTION_COUNT,
					getAssertionCount(), "thread safety"));
			System.out.println();
		}

	}

	/**
	 * reset data states before starting performance or thread safety testing
	 * 
	 * @throws Exception
	 */
	protected void reset() throws Exception {
		// nothing to reset. override if needed
	}

	/**
	 * convenience method that sets counts for this user.
	 * 
	 * @param params
	 * 
	 */
	private void setCountsForUser(final Map<String, Object> params,
			final int count) {
		if (isPerformanceTest()) {
			params.put(COUNT_FOR_THIS_USER, count);
		}

		if (isThreadSafetyTest(params)) {
			SHARED_COUNTER.addAndGet(count);
		}
	}

	/**
	 * @param message
	 * @param condition
	 */
	protected final void assertFalse(final String message,
			final boolean condition) {
		junitWrapper.assertFalse(message, condition);
	}

	/**
	 * @param message
	 * @param condition
	 */
	protected final void assertTrue(final String message,
			final boolean condition) {
		junitWrapper.assertTrue(message, condition);
	}

	/**
	 * @param message
	 * @param object
	 */
	protected final void assertNotNull(final String message, final Object object) {
		junitWrapper.assertNotNull(message, object);
	}

	/**
	 * @param message
	 * @param object
	 */
	protected final void assertNull(final String message, final Object object) {
		junitWrapper.assertNull(message, object);
	}

	/**
	 * @param message
	 * @param expected
	 * @param actual
	 * @param positiveInfinity 
	 */
	protected final void assertEquals(final String message,
			final Object expected, final Object actual) {
		junitWrapper.assertEquals(message, expected, actual);
	}
	
	/**
	 * @param message
	 * @param expected
	 * @param actual
	 * @param positiveInfinity 
	 */
	protected final void assertEquals(final String message,
			final double expected, final double actual, double tolerance) {
		junitWrapper.assertEquals(message, expected, actual, tolerance);
	}


	/**
	 * @param message
	 * @param expected
	 * @param actual
	 */
	protected final void assertSame(final String message,
			final Object expected, final Object actual) {
		junitWrapper.assertSame(message, expected, actual);
	}

	/**
	 * @param message
	 * @param expected
	 * @param actual
	 */
	protected final void assertNotSame(final String message,
			final Object expected, final Object actual) {
		junitWrapper.assertNotSame(message, expected, actual);
	}

	/**
	 * @param message
	 * @param thrown
	 */
	protected final void fail(final String message, final Throwable... thrown) {
		junitWrapper.fail(message, thrown);
	}

	/**
	 * @return
	 */
	private final int getAssertionCount() {
		return junitWrapper.getAssertionCount();
	}

	/**
	 *
	 */
	private final void resetAssertionCounter() {
		junitWrapper.resetAssertionCounter();
	}

	/**
	 * convenience method to create a result message for performance tests that
	 * meets the expected form.
	 * 
	 * @param verb
	 * @param noun
	 * @return
	 */
	protected String createPerformanceTestResultMessage(final String verb, final String noun) {
		final String template = String.format(BASIC_PERFORMANCE_TESTING_MESSAGE_TEMPLATE, verb, "%s", noun, "%s");
		return String.format(template, "%s", "%.2f");
	}

	/**
	 * convenience method to create a result message for thread safety tests
	 * that meets the expected form.
	 * 
	 * @param verb
	 * @param noun
	 * @return
	 */
	protected String createThreadSafetyTestResultMessage(final String verb, final String noun) {
		final String perfTemplate = String.format(BASIC_PERFORMANCE_TESTING_MESSAGE_TEMPLATE, verb, "%s", noun, "%s");
		final String threadSafetyTemplate = String.format("%s users %s", "%s", perfTemplate);
		return String.format(threadSafetyTemplate, "%d", "%d", "%.2f");
	}

	/**
	 * @return
	 */
	public static final boolean isThreadSafetyTest( Thread thread ) {
		for( StackTraceElement method : thread.getStackTrace() ) {
			System.out.println( method.getMethodName() );
			if( method.getMethodName().contains( "verifyThreadSafety" ) ) {
				return true;
			}
		}
		return false;
	}
	
}
