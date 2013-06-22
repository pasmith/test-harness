package sample;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import test.BaseTestCase;

/**
 * @author patrick
 *
 */
public class DateTest extends BaseTestCase<SimpleDateFormat,Date> {

    /* (non-Javadoc)
     * @see test.BaseTestCase#getPerfTestResultMessage()
     */
    @Override protected final String getPerfTestResultMessage() {
        return createPerformanceTestResultMessage("parsed", "dates");
    }

    /* (non-Javadoc)
     * @see test.BaseTestCase#getThreadSafetyResultMessage()
     */
    @Override protected final String getThreadSafetyResultMessage() {
        return createThreadSafetyTestResultMessage( "parsed", "dates");
    }

    /* (non-Javadoc)
     * @see test.BaseTestCase#generateTestData(java.util.Map)
     */
    @Override protected final Date generateTestData(final Map<String, Object> params) throws Exception {
        return new Date();
    }

    /* (non-Javadoc)
     * @see test.BaseTestCase#getComponentUnderTest()
     */
    @Override protected final SimpleDateFormat getComponentUnderTest() throws IOException {
        final SimpleDateFormat day = new SimpleDateFormat( "dd-MMM-yyyy" );
        day.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return day;
    }


    /* (non-Javadoc)
     * @see test.BaseTestCase#verifyFunctionality(java.util.Map, java.lang.Object, java.lang.Object)
     */
    @Override protected final void verifyFunctionality( final Map<String, Object> params, final SimpleDateFormat day, final Date now, final AtomicInteger countsForThisUser ) throws Exception {
        Date today = null;
        synchronized (day) {
            today = day.parse( day.format( now ) );
        }

        assertNotSame( "the value that corresponds to now and today should not be the same", now.getTime(), today.getTime() );
        assertTrue( "now should be later than today, which is at start of day", now.after(today) );

        // increment couter for user
        countsForThisUser.addAndGet( 1 );

    }

}
