package cz.cesnet.shongo.client.web.models;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TimeZoneModel
{
    private static Logger logger = LoggerFactory.getLogger(TimeZoneModel.class);

    /**
     * Pattern for matching available time zones.
     */
    private static final Pattern PATTERN = Pattern.compile("((?!Etc).+/.+)|("
            + "Cuba|Egypt|Eire|Hongkong|Iceland|Iran|Israel|Jamaica|Japan|Kwajalein|Libya|Navajo|UTC"
            + ")");

    /**
     * Formatter for timezone name.
     */
    private static org.joda.time.format.DateTimeFormatter TIME_ZONE_NAME_FORMATTER =
            DateTimeFormat.forPattern("ZZZ (ZZ)");

    /**
     * Formatter for timezone.
     */
    private static org.joda.time.format.DateTimeFormatter TIME_ZONE_FORMATTER =
            DateTimeFormat.forPattern("ZZ");

    /**
     * List of available time zones.
     */
    private static List<DateTimeZone> timeZones = null;

    /**
     * @return {@link #timeZones}
     */
    private static List<DateTimeZone> getTimeZones()
    {
        if (timeZones == null) {
            org.joda.time.format.DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("ZZZ");
            Map<String, DateTimeZone> timeZoneByName = new TreeMap<String, DateTimeZone>();
            for (String timeZoneId : DateTimeZone.getAvailableIDs()) {
                DateTimeZone timeZone = DateTimeZone.forID(timeZoneId);
                if (!PATTERN.matcher(timeZoneId).matches()) {
                    logger.debug("Skipping '{}'...", timeZoneId);
                    continue;
                }
                String dateTimeZoneName = dateTimeFormatter.withZone(timeZone).print(0);
                if (timeZoneByName.containsKey(dateTimeZoneName)) {
                    logger.debug("Replacing '{}' by '{}' for '{}'...",
                            new Object[]{timeZoneByName.get(dateTimeZoneName), timeZoneId, dateTimeZoneName});
                }
                timeZoneByName.put(dateTimeZoneName, timeZone);
            }
            timeZones = new LinkedList<DateTimeZone>();
            for (DateTimeZone timeZone : timeZoneByName.values()) {
                timeZones.add(timeZone);
            }
        }
        return timeZones;
    }

    /**
     * @param dateTime
     * @return map of timeZoneId => timeZoneName
     */
    public static Map<String, String> getTimeZones(DateTime dateTime)
    {
        Map<String, String> timeZones = new TreeMap<String, String>();
        for (DateTimeZone timeZone : getTimeZones()) {
            String timeZoneName = TIME_ZONE_NAME_FORMATTER.withZone(timeZone).print(dateTime);
            timeZoneName = timeZoneName.replaceAll("US/", "United States/");
            timeZoneName = timeZoneName.replaceAll("_", " ");
            timeZones.put(timeZone.getID(), timeZoneName);
        }
        return timeZones;
    }

    public static String formatTimeZone(DateTimeZone timeZone)
    {
        return TIME_ZONE_FORMATTER.withZone(timeZone).print(DateTime.now());
    }
}
