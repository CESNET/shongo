package cz.cesnet.shongo.client.web.models;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Model for {@link DateTimeZone}.
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
            DateTimeFormat.forPattern("ZZZ");

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
     * Map of available time zones by locale.
     */
    private static Map<Locale, Map<DateTimeZone, String>> timeZonesByLocale =
            new HashMap<Locale, Map<DateTimeZone, String>>();

    /**
     * @return {@link #timeZones}
     */
    private synchronized static List<DateTimeZone> getTimeZones()
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
     * @param locale
     * @return map time zone name by {@link DateTimeZone} for given {@code locale}
     */
    private synchronized static Map<DateTimeZone, String> getTimeZonesForLocale(Locale locale)
    {
        Map<DateTimeZone, String> timeZones = timeZonesByLocale.get(locale);
        if (timeZones == null) {
            timeZones = new HashMap<DateTimeZone, String>();
            for (DateTimeZone timeZone : getTimeZones()) {
                String timeZoneName = TIME_ZONE_NAME_FORMATTER.withZone(timeZone).print(0);
                timeZoneName = timeZoneName.replaceAll("_", " ");
                if (locale.getLanguage().equals("cs")) {
                    timeZoneName = timeZoneName.replaceAll("Africa/", "Afrika/");
                    timeZoneName = timeZoneName.replaceAll("America/", "Amerika/");
                    timeZoneName = timeZoneName.replaceAll("Antarctica/", "Antarktida/");
                    timeZoneName = timeZoneName.replaceAll("Asia/", "Asie/");
                    timeZoneName = timeZoneName.replaceAll("Atlantic/", "Atlanský oceán/");
                    timeZoneName = timeZoneName.replaceAll("Australia/", "Austrálie/");
                    timeZoneName = timeZoneName.replaceAll("Europe/", "Evropa/");
                    timeZoneName = timeZoneName.replaceAll("/Prague", "/Praha");
                    timeZoneName = timeZoneName.replaceAll("Indian/", "Indický oceán/");
                    timeZoneName = timeZoneName.replaceAll("Pacific/", "Pacifik/");
                    timeZoneName = timeZoneName.replaceAll("UTC", "Koordinovaný světový čas");
                }
                timeZoneName = timeZoneName.replaceAll("/", " / ");
                timeZones.put(timeZone, timeZoneName);
            }
            timeZonesByLocale.put(locale, timeZones);
        }
        return timeZones;

    }

    /**
     * @param locale
     * @param dateTime
     * @return map of timeZoneId => timeZoneName
     */
    public static Map<String, String> getTimeZones(Locale locale, DateTime dateTime)
    {
        Map<String, String> timeZones = new TreeMap<String, String>();
        for (Map.Entry<DateTimeZone, String> timeZoneEntry : getTimeZonesForLocale(locale).entrySet()) {
            DateTimeZone timeZone = timeZoneEntry.getKey();
            String timeZoneName = timeZoneEntry.getValue();
            String timeZoneOffset = TIME_ZONE_FORMATTER.withZone(timeZone).print(dateTime);
            timeZones.put(timeZone.getID(), timeZoneName + " (" + timeZoneOffset + ")");
        }
        return timeZones;
    }

    /**
     * @param locale
     * @return map of timeZoneId => timeZoneName
     */
    public static Map<String, String> getTimeZones(Locale locale)
    {
        return getTimeZones(locale, DateTime.now());
    }

    /**
     * @param timeZone
     * @return formatted given {@code timeZone}
     */
    public static String formatTimeZone(DateTimeZone timeZone)
    {
        return TIME_ZONE_FORMATTER.withZone(timeZone).print(DateTime.now());
    }

    /**
     * @param timeZone
     * @param locale
     * @return name for given {@code timeZone} and {@code name}
     */
    public static String formatTimeZoneName(DateTimeZone timeZone, Locale locale)
    {
        return getTimeZonesForLocale(locale).get(timeZone);
    }
}
