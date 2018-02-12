package cz.cesnet.shongo.controller.calendar.connector;

import com.google.common.base.Strings;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.LocalDomain;
import cz.cesnet.shongo.controller.calendar.ReservationCalendar;
import cz.cesnet.shongo.controller.util.iCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;

/**
 * Connector for sending iCalendar notifications to CalDAV server specified in configuration.
 *
 * @author Marek Perichta <mperichta@cesnet.cz>
 */

public class CalDAVConnector implements CalendarConnector
{

    private final Logger logger = LoggerFactory.getLogger(CalDAVConnector.class);

    private final ControllerConfiguration configuration;


    public CalDAVConnector (ControllerConfiguration configuration) {

        this.configuration = configuration;
        if (!configuration.containsKey(ControllerConfiguration.CALDAV_URL)) {
            logger.warn("Cannot initialize CalDAV connector because server configuration is empty.");
            return;
        }
    }

    public boolean isInitialized() {
        return configuration.containsKey(ControllerConfiguration.CALDAV_URL);
    }

    protected void processError(HttpsURLConnection connection) throws CalendarServerConnectException
    {
        String actionUrl = connection.getURL().toString();
        try {
            int errorCode = connection.getResponseCode();
            switch (errorCode) {
                case 400:
                    throw new CalendarServerConnectException(actionUrl, "400 Bad Request - " + connection.getResponseMessage());
                case 401:
                    throw new CalendarServerConnectException(actionUrl, "401 Unauthorized - " + connection.getResponseMessage());
                case 403:
                    throw new CalendarServerConnectException(actionUrl, "401 Forbidden - " + connection.getResponseMessage());
                case 404:
                    throw new CalendarServerConnectException(actionUrl, "404 Not Found - " + connection.getResponseMessage());
                case 500:
                    throw new CalendarServerConnectException(actionUrl, "500 Internal Server Error - " + connection.getResponseMessage());
                default:
                    if (errorCode > 400 && errorCode != 404) {
                        throw new CalendarServerConnectException(actionUrl, errorCode + " " + connection.getResponseMessage());
                    }
            }
        } catch (IOException e) {
            String message = "Failed to get connection response code for " + actionUrl;
            logger.error(message);
        }
    }

    /**
     *  Creates connection to CalDAV server.
     *
     * @param reservationId component of url
     * @param calendarName component of url
     * @return
     */
    protected HttpsURLConnection buildConnection (String reservationId, String calendarName) {
        HttpsURLConnection urlConnection = null;
        String calendarUrl = configuration.getString(ControllerConfiguration.CALDAV_URL);
        String actionUrl = calendarUrl + "/" + calendarName + "/" + reservationId + ".ics";

        //logger.debug("Building connection to " + actionUrl);
        try {

            String authStringEnc = null;
            if (configuration.hasCalDAVBasicAuth()) {
                authStringEnc = configuration.getCalDAVEncodedBasicAuth();
            }
            URL url = new URL(actionUrl);
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
        } catch (Exception e) {
            String message = "Failed to get connection for " + actionUrl;
            logger.error(message);
            e.printStackTrace();
        }
        return urlConnection;

    }

    /**
     * Sends http request to CalDAV server with iCalendar.
     * @param calendar to be sent
     */
    public boolean sendCalendarNotification (ReservationCalendar calendar) {
        if (Strings.isNullOrEmpty(calendar.getRemoteCalendarName())) {
            logger.warn("Cannot send calendar notification. Calendar name is not set.");
            return false ;
        }
        HttpsURLConnection connection = buildConnection(calendar.getReservationId(), calendar.getRemoteCalendarName());

        if (connection == null ) {
            return false;
        }
        try {
            logger.debug("Sending calendar notification of '{}' to calendar '{}'...",
                    new Object[]{calendar.getReservationId(), calendar.getRemoteCalendarName()});

            if (calendar.getType().equals(ReservationCalendar.Type.NEW)) {
                connection.setRequestMethod("PUT");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "text/calendar");
                String message = renderiCalendarString(calendar);
                connection.getOutputStream().write(message.getBytes());
            } else if (calendar.getType().equals(ReservationCalendar.Type.DELETED)) {
                connection.setRequestMethod("DELETE");
                connection.connect();
            }
            processError(connection);
            return true;
        } catch (Exception e) {
            String message = "Failed to perform request for reservation (" + calendar.getReservationId() + ")";
            logger.error(message, e);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     *
     * @param calendar to be converted
     * @return iCalendar string
     */
    protected String renderiCalendarString (ReservationCalendar calendar) {
        switch (calendar.getType()) {
            case NEW:
                ReservationCalendar.New newReservationCalendar = (ReservationCalendar.New) calendar;
                iCalendar iCalendar = new iCalendar();

                cz.cesnet.shongo.controller.util.iCalendar.Event event = iCalendar.addEvent(LocalDomain.getLocalDomainName(), newReservationCalendar.getReservationId(), newReservationCalendar.getDescription());
                event.setInterval(newReservationCalendar.getSlot(), newReservationCalendar.getSlot().getChronology().getZone());
                event.setOrganizer("mailto:" + newReservationCalendar.getOrganizerEmail());
                event.setOrganizerName(newReservationCalendar.getOrganizerName());
                event.setLocation(newReservationCalendar.getResourceName());
                return iCalendar.toString();
            case DELETED:
                return "";
        }

        logger.error("Cannot match calendar type when rendering iCalendar string.");
        return "";
    }

}
