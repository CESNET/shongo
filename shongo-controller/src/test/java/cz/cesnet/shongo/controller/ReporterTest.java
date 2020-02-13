package cz.cesnet.shongo.controller;

import org.junit.Assert;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import java.util.LinkedList;
import java.util.List;

/**
 * Tests for {@link cz.cesnet.shongo.controller.Reporter}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReporterTest
{
    private static Logger logger = LoggerFactory.getLogger(ReporterTest.class);

    @Test
    public void testGrouping() throws Exception
    {
        org.apache.log4j.Logger reporterLogger = LogManager.getLogger(Reporter.class);
        org.apache.log4j.Level reporterLoggerLevel = reporterLogger.getLevel();
        if (reporterLogger != null) {
            reporterLogger.setLevel(Level.OFF);
        }
        LocalDomain.setLocalDomain(new LocalDomain("test"));

        try {
            // Create email sender
            final MutableInt emailCount = new MutableInt(0);
            EmailSender emailSender = new EmailSender("test", null) {
                @Override
                public boolean isInitialized() {
                    return true;
                }

                @Override
                public void sendEmail(Email email) throws MessagingException {
                    logger.info(email.getSubject());
                    emailCount.increment();
                }
            };

            // Create reporter
            final List<String> administratorEmails = new LinkedList<String>();
            administratorEmails.add("shongo@cesnet.cz");
            Reporter reporter = Reporter.create(new Reporter(emailSender, null) {
                @Override
                protected List<String> getAdministratorEmails() {
                    return administratorEmails;
                }
            });
//            reporter.setCacheExpiration(Duration.standardSeconds(1));

            // Report
            for (int index = 0; index < 3024; index++) {
                reporter.reportInternalError(Reporter.SCHEDULER, "Test", new Exception());
            }
            reporter.clearCache(null);
            reporter.destroy();
            // 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1000, 1000, 1
            Assert.assertEquals(13, emailCount.intValue());
        }
        finally {
            LocalDomain.setLocalDomain(null);
            if (reporterLogger != null) {
                reporterLogger.setLevel(reporterLoggerLevel);
            }
        }
    }
}
