package cz.cesnet.shongo.controller;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.joda.time.DateTime;
import org.joda.time.Duration;
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
    public void test() throws Exception
    {
        org.apache.log4j.Logger reporterLogger = LogManager.getLogger(Reporter.class);
        org.apache.log4j.Level reporterLoggerLevel = reporterLogger.getLevel();
        reporterLogger.setLevel(Level.OFF);
        Domain.setLocalDomain(new Domain("test"));

        // Create email sender
        EmailSender emailSender = new EmailSender("test", null) {
            @Override
            public boolean isInitialized() {
                return true;
            }

            @Override
            public void sendEmail(Email email) throws MessagingException {
                logger.info(email.getSubject());
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
        reporter.setCacheExpiration(Duration.standardSeconds(1));

        // Report
        for (int index = 0; index < 100; index++) {
            reporter.reportInternalError(Reporter.SCHEDULER, "Test", new Exception());
        }
        Thread.sleep(1000);
        reporter.clearCache(DateTime.now());

        Domain.setLocalDomain(null);
        reporterLogger.setLevel(reporterLoggerLevel);
    }
}
