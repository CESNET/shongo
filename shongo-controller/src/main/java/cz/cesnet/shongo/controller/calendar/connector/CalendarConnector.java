package cz.cesnet.shongo.controller.calendar.connector;

import cz.cesnet.shongo.controller.calendar.ReservationCalendar;

/**
 *
 *
 * @author Marek Perichta <mperichta@cesnet.cz>
 */

public interface CalendarConnector
{

    boolean isInitialized();

    boolean sendCalendarNotification (ReservationCalendar calendar);

}
