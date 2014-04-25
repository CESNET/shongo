package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.ExpirationMap;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache for {@link Reporter}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReporterCache
{
    /**
     * Email cache.
     */
    private ExpirationMap<Collection<String>, Map<String, Map<String, Entry>>> recordsByTitleByRecipients =
            new ExpirationMap<Collection<String>, Map<String, Map<String, Entry>>>();

    /**
     * Constructor.
     */
    public ReporterCache()
    {
        this.recordsByTitleByRecipients.setExpiration(Duration.standardHours(1));
    }

    /**
     * @param expiration sets expiration to {@link #recordsByTitleByRecipients}
     */
    public void setExpiration(Duration expiration)
    {
        this.recordsByTitleByRecipients.setExpiration(expiration);
    }

    /**
     * @param recipients of the email
     * @param title of the email
     * @param content of the email
     * @return number of emails which are grouped
     */
    public int apply(Collection<String> recipients, String title, String content)
    {
        // Get by recipient
        Map<String, Map<String, Entry>> entriesByRecipient = recordsByTitleByRecipients.get(recipients);
        if (entriesByRecipient == null) {
            entriesByRecipient = new HashMap<String, Map<String, Entry>>();
        }

        // Update entry to not expire early
        recordsByTitleByRecipients.put(recipients, entriesByRecipient);

        // Get by title
        Map<String, Entry> entriesByTitle = entriesByRecipient.get(title);
        if (entriesByTitle == null) {
            entriesByTitle = new HashMap<String, Entry>();
            entriesByRecipient.put(title, entriesByTitle);
        }

        // Get by title
        Entry entryByContent = entriesByTitle.get(content);
        if (entryByContent == null) {
            entryByContent = new Entry();
            entriesByTitle.put(content, entryByContent);
        }

        // Increment count
        entryByContent.count++;

        // If next sending step is reached
        if (entryByContent.count == entryByContent.nextStep) {
            int result = entryByContent.count;
            entryByContent.count = 0;
            entryByContent.nextStep *= 2;
            return result;
        }
        // Otherwise skip sending
        else {
            return 0;
        }
    }

    /**
     * @param dateTime
     * @param callback
     */
    public void clear(DateTime dateTime, EntryCallback callback)
    {
        for (Map.Entry<Collection<String>, Map<String, Map<String, Entry>>> entryByRecipient :
                recordsByTitleByRecipients.clearExpired(dateTime))
        {
            Collection<String> recipients = entryByRecipient.getKey();
            for (Map.Entry<String, Map<String, Entry>> entryByTitle : entryByRecipient.getValue().entrySet()) {
                String title = entryByTitle.getKey();
                for (Map.Entry<String, Entry> entryByContent : entryByTitle.getValue().entrySet()) {
                    String content = entryByContent.getKey();
                    callback.sendEmail(recipients, title, content, entryByContent.getValue().count);
                }
            }
        }
    }

    /**
     * Callback for {@link #clear}
     */
    public interface EntryCallback
    {
        public void sendEmail(Collection<String> recipients, String title, String content, int count);
    }

    /**
     * Entry
     */
    private static class Entry
    {
        /**
         * Number of same emails.
         */
        private int count = 0;

        /**
         * Number of same emails when the email should be sent.
         */
        private int nextStep = 1;
    }
}
