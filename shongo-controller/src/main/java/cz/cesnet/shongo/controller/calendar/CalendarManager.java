package cz.cesnet.shongo.controller.calendar;

import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.calendar.connector.CalendarConnector;
import cz.cesnet.shongo.controller.notification.executor.NotificationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Marek Perichta on 14.3.2017.
 */
public class CalendarManager extends Component
{

    private static Logger logger = LoggerFactory.getLogger(CalendarManager.class);

    /**
     * Specifies whether the manager should send calendarNotifications or skip them.
     */
    private boolean enabled = true;

    /**
     * List of {@link ReservationCalendar}s that needs to be converted into calendar and sent.
     */
    private List<ReservationCalendar> calendars = new LinkedList<>();


    /**
     * List of {@link CalendarConnector}s for sending {@link ReservationCalendar}s.
     */
    private List<CalendarConnector> calendarConnectors = new ArrayList<>();

    public synchronized void addCalendars (List<ReservationCalendar> calendars) {
        for (ReservationCalendar calendar : calendars) {
            addCalendar(calendar);
        }
    }

    public synchronized void addCalendar(ReservationCalendar calendar) {
        calendars.add(calendar);
    }

    public synchronized void removeCalendar (ReservationCalendar calendar) {
        calendars.remove(calendar);
    }

    public synchronized void addCalendarConnector(CalendarConnector calendarConnector)
    {
        calendarConnectors.add(calendarConnector);
    }

    public synchronized void sendCalendarNotifications()
    {
        // Execute notifications
        List<ReservationCalendar> removedCalendars = new LinkedList<>();

        for (Iterator<ReservationCalendar> iterator = calendars.iterator(); iterator.hasNext(); ) {
            ReservationCalendar calendar = iterator.next();
            sendCalendarNotification(calendar);
            iterator.remove();
            removedCalendars.add(calendar);
        }
    }


    private void sendCalendarNotification(ReservationCalendar calendar)
    {

        if (!enabled) {
            logger.warn("Cannot send '{}' because calendar notifications are disabled.", calendar);
        }

        if (enabled) {
            // Perform notification in every notification executor
            for (CalendarConnector calendarConnector : calendarConnectors) {
                calendarConnector.sendCalendarNotification(calendar);
            }
        }
    }
}
