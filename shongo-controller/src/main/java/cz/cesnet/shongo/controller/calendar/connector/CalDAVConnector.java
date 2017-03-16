package cz.cesnet.shongo.controller.calendar.connector;

import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.calendar.ReservationCalendar;
import org.postgresql.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Created by Marek Perichta on 15.3.2017.
 */
public class CalDAVConnector implements CalendarConnector
{

    private final Logger logger = LoggerFactory.getLogger(CalDAVConnector.class);

    private final ControllerConfiguration configuration;

    private String calDAVServer;

    public CalDAVConnector (ControllerConfiguration configuration) {

        this.configuration = configuration;
        if (!configuration.containsKey(ControllerConfiguration.CALDAV_SERVER)) {
            logger.warn("Cannot initialize CalDAV connector because server configuration is empty.");
            return;
        }

        this.calDAVServer = configuration.getString(ControllerConfiguration.CALDAV_SERVER);
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
                    if (errorCode > 400) {
                        throw new CalendarServerConnectException(actionUrl, errorCode + " " + connection.getResponseMessage());
                    }
            }
        } catch (IOException e) {
            String message = "Failed to get connection response code for " + actionUrl;
            logger.error(message);
        }
    }

    protected HttpsURLConnection buildConnection (String reservationId) {
        HttpsURLConnection urlConnection = null;
        String serverUrl = configuration.getString(ControllerConfiguration.CALDAV_SERVER);
        serverUrl = serverUrl + reservationId + ".ics";


        logger.debug("Buliding connection to " + serverUrl);
        try {

            String authStringEnc = null;
            if (configuration.hasCalDAVBasicAuth()) {
                authStringEnc = configuration.getCalDAVEncodedBasicAuth();
            }
            URL url = new URL(serverUrl);
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
        } catch (Exception e) {
            String message = "Failed to get connection for " + serverUrl;
            logger.error(message);
            e.printStackTrace();
        }
        return urlConnection;

    }

    //TODO should this method be public?
    public void sendCalendarNotification (ReservationCalendar calendar) {
        if (!configuration.containsKey(ControllerConfiguration.CALDAV_SERVER)) return;
        HttpsURLConnection connection = buildConnection(calendar.getReservationId());
        if (connection == null) return;
        try {

            if (calendar.getType().equals("NEW")) {
                connection.setRequestMethod("PUT");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "text/calendar");
                String message = calendar.getCalendarString();
                connection.getOutputStream().write(message.getBytes());
                processError(connection);

            } else if (calendar.getType().equals("DELETE")) {
                connection.setRequestMethod("DELETE");
            }

        } catch (Exception e) {
            String message = "Failed to perform request for reservation (" + calendar.getReservationId() + ")";
            logger.error(message, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


}
