package cz.cesnet.shongo.controller.calendar.connector;

import cz.cesnet.shongo.controller.calendar.ReservationCalendar;

/**
 * Created by Marek Perichta on 15.3.2017.
 */
public interface CalendarConnector
{

    void sendCalendarNotification (ReservationCalendar calendar);

}
