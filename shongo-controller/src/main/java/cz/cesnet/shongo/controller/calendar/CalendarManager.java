package cz.cesnet.shongo.controller.calendar;

import com.google.common.base.Strings;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.calendar.connector.CalendarConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a {@link Component} for sending {@link ReservationCalendar}s by multiple {@link CalendarConnector}s.
 *
 * @author Marek Perichta <mperichta@cesnet.cz>
 */

public class CalendarManager extends Component {

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



    public synchronized void addCalendars(List<ReservationCalendar> calendars, EntityManager entityManager) {
        for (ReservationCalendar calendar : calendars) {
            addCalendar(calendar, entityManager);
        }
    }


    public synchronized void addCalendar(ReservationCalendar calendar, EntityManager entityManager) {
        //Add only calendar with calendar name set by Resource
        if (!Strings.isNullOrEmpty(calendar.getRemoteCalendarName())) {
            calendar.checkNotPersisted();
            entityManager.persist(calendar);
            calendars.add(calendar);
        }
    }

    public synchronized void removeCalendar(ReservationCalendar calendar) {
        calendars.remove(calendar);
    }

    public synchronized void addCalendarConnector(CalendarConnector calendarConnector) {
        calendarConnectors.add(calendarConnector);
    }

    public synchronized void sendCalendarNotifications(EntityManager entityManager) {
        List<ReservationCalendar> removedCalendars = new LinkedList<>();

        boolean notificationSuccessful = false;
        for (Iterator<ReservationCalendar> iterator = calendars.iterator(); iterator.hasNext(); ) {
            ReservationCalendar calendar = iterator.next();
            notificationSuccessful = sendCalendarNotification(calendar, entityManager);
            iterator.remove();
            removedCalendars.add(calendar);
        }
        if (notificationSuccessful) {
            calendars.addAll(getUnsentCalendars(entityManager));
        }
    }

    /**
     *
     * @param calendar to be sent by {@link CalendarConnector}s.
     */
    private boolean sendCalendarNotification(ReservationCalendar calendar, EntityManager entityManager) {

        if (!enabled) {
            logger.warn("Cannot send '{}' because calendar notifications are disabled.", calendar);
        }
        boolean notificationSuccessful = false;

        if (enabled) {

            // Perform notification in every calendar connector
            for (CalendarConnector calendarConnector : calendarConnectors) {
                if (!calendarConnector.isInitialized()) {
                    logger.warn("Calendar notification can't be sent because calendar connector is not configured.");
                    break;
                }
                notificationSuccessful = calendarConnector.sendCalendarNotification(calendar);
                //TODO following section needs to be modified if another connector is to be added
                if (notificationSuccessful) {
                    entityManager.getTransaction().begin();
                    entityManager.remove(entityManager.contains(calendar) ? calendar : entityManager.merge(calendar));
                    entityManager.getTransaction().commit();
                }
            }

        }
        return notificationSuccessful;

    }

    /**
     * @param entityManager
     * @return top 20 records of unsent {@link ReservationCalendar} ordered by Id
     */
    public List<ReservationCalendar> getUnsentCalendars(EntityManager entityManager) {
        return entityManager.createQuery("SELECT reservationCalendar FROM ReservationCalendar reservationCalendar"
                        + " ORDER BY reservationCalendar.id",
                ReservationCalendar.class)
                .setMaxResults(20)
                .getResultList();
    }
}
