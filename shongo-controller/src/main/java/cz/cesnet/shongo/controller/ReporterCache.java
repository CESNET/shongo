package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.ExpirationMap;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Cache for {@link Reporter}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReporterCache
{
    private static final int MAX_NEXT_STEP = 1000;

    private Duration expiration;

    /**
     * Email cache.
     */
    private Map<Collection<String>, ExpirationMap<String, Map<String, Entry>>> recordsByTitleByRecipients =
            new HashMap<Collection<String>, ExpirationMap<String, Map<String, Entry>>>();

    /**
     * Constructor.
     */
    public ReporterCache()
    {
        setExpiration(Duration.standardHours(1));
    }

    /**
     * @param expiration sets expiration to {@link #recordsByTitleByRecipients}
     */
    public void setExpiration(Duration expiration)
    {
        this.expiration = expiration;
        for (ExpirationMap<String, Map<String, Entry>> value : recordsByTitleByRecipients.values()) {
            value.setExpiration(expiration);
        }
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
        ExpirationMap<String, Map<String, Entry>> entriesByRecipient = recordsByTitleByRecipients.get(recipients);
        if (entriesByRecipient == null) {
            entriesByRecipient = new ExpirationMap<String, Map<String, Entry>>();
            entriesByRecipient.setExpiration(expiration);
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
            if (entryByContent.nextStep > MAX_NEXT_STEP) {
                entryByContent.nextStep = MAX_NEXT_STEP;
            }
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
        Iterator<Map.Entry<Collection<String>, ExpirationMap<String, Map<String, Entry>>>> iterator =
                recordsByTitleByRecipients.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Collection<String>, ExpirationMap<String, Map<String, Entry>>> entryByRecipient = iterator.next();
            Collection<String> recipients = entryByRecipient.getKey();
            ExpirationMap<String, Map<String, Entry>> value = entryByRecipient.getValue();
            for (Map.Entry<String, Map<String, Entry>> entryByTitle : value.clearExpired(dateTime)) {
                String title = entryByTitle.getKey();
                for (Map.Entry<String, Entry> entryByContent : entryByTitle.getValue().entrySet()) {
                    String content = entryByContent.getKey();
                    callback.sendEmail(recipients, title, content, entryByContent.getValue().count);
                }
            }
            if (value.isEmpty()) {
                iterator.remove();
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
