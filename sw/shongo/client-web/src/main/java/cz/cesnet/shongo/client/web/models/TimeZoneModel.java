package cz.cesnet.shongo.client.web.models;

import org.joda.time.DateTimeZone;
import org.joda.time.format.*;

import java.util.*;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TimeZoneModel
{
    public static org.joda.time.format.DateTimeFormatter DATE_TIME_ZONE_ID_FORMATTER = DateTimeFormat.forPattern("ZZ");

    private static Map<String, String> timeZones = null;

    public static Map<String, String> getTimeZones()
    {
        if (timeZones == null) {
            Map<Integer, List<DateTimeZone>> dateTimeZonesByOffset = new TreeMap<Integer, List<DateTimeZone>>();
            for (String dateTimeZoneId : DateTimeZone.getAvailableIDs()) {
                DateTimeZone dateTimeZone = DateTimeZone.forID(dateTimeZoneId);
                int dateTimeZoneOffset = dateTimeZone.getOffset(0);
                List<DateTimeZone> dateTimeZones = dateTimeZonesByOffset.get(dateTimeZoneOffset);
                if (dateTimeZones == null) {
                    dateTimeZones = new LinkedList<DateTimeZone>();
                    dateTimeZonesByOffset.put(dateTimeZoneOffset, dateTimeZones);
                }
                dateTimeZones.add(dateTimeZone);
            }

            timeZones = new LinkedHashMap<String, String>();
            for (List<DateTimeZone> offsetDateTimeZones : dateTimeZonesByOffset.values()) {
                String dateTimeZoneId = getDateTimeZoneId(offsetDateTimeZones.get(0));
                StringBuilder dateTimeZoneDescription = new StringBuilder();
                Set<String> dateTimeZoneNames = new TreeSet<String>();
                for (DateTimeZone dateTimeZone : offsetDateTimeZones) {
                    String dateTimeZoneName = dateTimeZone.getName(0);
                    if (!dateTimeZoneName.startsWith(dateTimeZoneId)) {
                        dateTimeZoneName = dateTimeZoneName.replaceAll(" Time", "");
                        dateTimeZoneName = dateTimeZoneName.replaceAll(".+ \\((.+)\\)", "$1");
                        dateTimeZoneNames.add(dateTimeZoneName);
                    }
                }
                for (String dateTimeZoneName : dateTimeZoneNames) {
                    if (dateTimeZoneDescription.length() > 0 ) {
                        dateTimeZoneDescription.append(", ");
                    }
                    dateTimeZoneDescription.append(dateTimeZoneName);
                }
                if (dateTimeZoneDescription.length() == 0) {
                    continue;
                }
                timeZones.put(dateTimeZoneId, dateTimeZoneDescription.toString());
            }
        }
        return timeZones;
    }

    public static String getDateTimeZoneId(DateTimeZone dateTimeZone)
    {
        return DATE_TIME_ZONE_ID_FORMATTER.withZone(dateTimeZone).print(0);
    }
}
