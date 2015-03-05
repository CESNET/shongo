package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.datetime.AbsoluteDateTimeSlot;
import cz.cesnet.shongo.controller.booking.datetime.DateTimeSlot;
import cz.cesnet.shongo.controller.booking.datetime.PeriodicDateTime;
import cz.cesnet.shongo.controller.booking.datetime.PeriodicDateTimeSlot;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestSet;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * {@link ConfigurableNotification} for changes in allocation of {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestNotification extends AbstractReservationRequestNotification
{
    private Target target;

    private Long reusedReservationRequestId;

    /**
     * True if no notifications has been sent yet for given {@link ReservationRequest}
     */
    private boolean first = true;

    /**
     * Map of periodic slots of {@link ReservationRequest}
     */
    //private Map<Interval, DateTime> periodicSlots = new HashMap<Interval, DateTime>();

    private List<DateTimeSlot> periodicSlots = new ArrayList<DateTimeSlot>();;

    /**
     * List of {@link AbstractNotification}s which are part of the {@link ReservationNotification}.
     */
    private List<AbstractReservationRequestNotification> notifications =
            new LinkedList<AbstractReservationRequestNotification>();

    /**
     * Constructor.
     *
     * @param reservationRequest
     * @param authorizationManager
     */
    public ReservationRequestNotification(AbstractReservationRequest reservationRequest,
            AuthorizationManager authorizationManager)
    {
        super(reservationRequest);

        Allocation allocation = reservationRequest.getAllocation();
        if (allocation.isNotified()) {
            this.first = false;
        }
        else {
            allocation.setNotified(true);
        }

        if (reservationRequest instanceof ReservationRequest) {
            Interval originSlot = ((ReservationRequest) reservationRequest).getSlot();
            AbsoluteDateTimeSlot slot = new AbsoluteDateTimeSlot(originSlot.getStart(),originSlot.getEnd());
            periodicSlots.add(slot);
        }
        else if (reservationRequest instanceof ReservationRequestSet) {
            ReservationRequestSet reservationRequestSet = (ReservationRequestSet) reservationRequest;
            for (DateTimeSlot slot : reservationRequestSet.getSlots()) {
                if (slot instanceof AbsoluteDateTimeSlot) {
                    periodicSlots.add((AbsoluteDateTimeSlot) slot);
                }
                else if (slot instanceof PeriodicDateTimeSlot) {
                    periodicSlots.add((PeriodicDateTimeSlot) slot);
                }
                else {
                    throw new TodoImplementException("Missing DateTimeSlot type");
                }
            }
        }
        else {
            throw new TodoImplementException("Missing AbstractReservationRequest type");
        }

        EntityManager entityManager = authorizationManager.getEntityManager();
        this.target = Target.createInstance(reservationRequest, entityManager);

        Allocation reusedAllocation = reservationRequest.getReusedAllocation();
        if (reusedAllocation != null) {
            reusedReservationRequestId = reusedAllocation.getReservationRequest().getId();
        }

        for (String userId : authorizationManager.getUserIdsWithRole(reservationRequest, ObjectRole.OWNER).getUserIds()) {
            addRecipient(authorizationManager.getUserInformation(userId), false);
        }
    }

    /**
     * @param notification to be added to the {@link #notifications}
     */
    public void addNotification(AbstractReservationRequestNotification notification)
    {
        notifications.add(notification);
        Collections.sort(notifications, new Comparator<AbstractReservationRequestNotification>()
        {
            @Override
            public int compare(AbstractReservationRequestNotification notification1,
                    AbstractReservationRequestNotification notification2)
            {
                return notification1.getSlotStart().compareTo(notification2.getSlotStart());
            }
        });
    }

    @Override
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
    }

    @Override
    public Interval getSlot()
    {
        throw new TodoImplementException();
    }

    @Override
    protected ConfigurableNotification.Configuration createConfiguration(Locale locale, DateTimeZone timeZone,
            boolean administrator)
    {
        return new ParentConfiguration(locale, timeZone, administrator);
    }

    @Override
    protected NotificationMessage renderMessage(Configuration configuration, NotificationManager manager)
    {
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification", manager);

        // Number of child events of each type
        int allocationFailedNotifications = 0;
        int newReservationNotifications = 0;
        int deletedReservationNotifications = 0;
        for (AbstractNotification notification : notifications) {
            if (notification instanceof AllocationFailedNotification) {
                allocationFailedNotifications++;
            }
            else if (notification instanceof ReservationNotification.New) {
                newReservationNotifications++;
            }
            else if (notification instanceof ReservationNotification.Deleted) {
                deletedReservationNotifications++;
            }
            else {
                throw new TodoImplementException(notification.getClass());
            }
        }

        // Description of reservation request target
        StringBuilder targetBuilder = new StringBuilder();
        targetBuilder.append(renderContext.message("reservationRequest.for." + target.getType()));
        if (target instanceof Target.Room) {
            Target.Room room = (Target.Room) target;
            String roomName = room.getName();
            if (roomName != null) {
                targetBuilder.append(" ");
                targetBuilder.append(roomName);
            }
        }
        else if (target instanceof Target.Alias) {
            Target.Alias alias = (Target.Alias) target;
            String roomName = alias.getRoomName();
            if (roomName != null) {
                targetBuilder.append(" ");
                targetBuilder.append(roomName);
            }
        }

        // Description of the reservation request result
        StringBuilder resultDescriptionBuilder = new StringBuilder();
        int totalNotifications = notifications.size();
        if (totalNotifications == allocationFailedNotifications) {
            resultDescriptionBuilder.append(renderContext.message("reservationRequest.result.failed"));
        }
        else if (totalNotifications == newReservationNotifications
                || totalNotifications == deletedReservationNotifications) {
            resultDescriptionBuilder.append(renderContext.message("reservationRequest.result.success"));
        }
        else {
            Map<String, Integer> childrenTypes = new HashMap<String, Integer>();
            childrenTypes.put("reservationRequest.child.failed", allocationFailedNotifications);
            childrenTypes.put("reservationRequest.child.new", newReservationNotifications);
            childrenTypes.put("reservationRequest.child.deleted", deletedReservationNotifications);

            if (allocationFailedNotifications > 0) {
                resultDescriptionBuilder.append(renderContext.message("reservationRequest.result.partialSuccess"));
            }
            else {
                resultDescriptionBuilder.append(renderContext.message("reservationRequest.result.success"));
            }
            resultDescriptionBuilder.append(" (");
            resultDescriptionBuilder.append(renderContext.message("reservationRequest.child"));
            resultDescriptionBuilder.append(": ");
            boolean separator = false;
            for (Map.Entry<String, Integer> entry : childrenTypes.entrySet()) {
                if (entry.getValue() == 0) {
                    continue;
                }
                if (separator) {
                    resultDescriptionBuilder.append(", ");
                }
                resultDescriptionBuilder.append(renderContext.message(
                        entry.getKey(), entry.getValue()));
                separator = true;
            }
            resultDescriptionBuilder.append(")");
        }

        // Build notification title
        StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append(renderContext.message(
                "reservationRequest", renderContext.formatDateTime(getReservationRequestUpdatedAt()), targetBuilder));
        titleBuilder.append(" ");
        titleBuilder.append(resultDescriptionBuilder);

        NotificationMessage message = renderTemplateMessage(
                renderContext, titleBuilder.toString(), "reservation-request.ftl");

        Map<DateTimeSlot,ReservationNotification> firstNotifications = new LinkedHashMap<DateTimeSlot, ReservationNotification>();
        for (AbstractNotification notification : notifications) {
            for (DateTimeSlot slot : periodicSlots) {
                if (notification instanceof ReservationNotification
                        || (totalNotifications == allocationFailedNotifications && notification instanceof AllocationFailedNotification)
                        || (totalNotifications == deletedReservationNotifications && notification instanceof AllocationFailedNotification)) {
                    ReservationNotification reservationNotification = (ReservationNotification) notification;
                    Interval notificationSlot = reservationNotification.getSlot();

                    if (slot.contains(notificationSlot)) {
                        ReservationNotification firstNotification = firstNotifications.get(slot);
                        if (firstNotification == null || notificationSlot.getStart().isBefore(firstNotification.getSlotStart())) {
                            firstNotifications.put(slot, reservationNotification);
                        }
                    }
                }
                else {
                    throw new TodoImplementException(notification.getClass());
                }
            }

            /*NotificationMessage childMessage;
            if (notification instanceof ConfigurableNotification) {
                ConfigurableNotification configurableEvent = (ConfigurableNotification) notification;
                childMessage = configurableEvent.renderMessage(configuration, manager);
            } else {
                throw new TodoImplementException(notification.getClass());
            }
            message.appendChildMessage(childMessage);*/
        }

        if (totalNotifications == newReservationNotifications) {
            for (DateTimeSlot slot : periodicSlots) {
                //String msg = "vse uspesne, zacatek request" + renderContext.formatPeriodicSlot(slot);
                message.appendLine("NEW");
            }
        }
        else if (totalNotifications == deletedReservationNotifications) {
            //TODO: vypsat to stejne jen s jinym titulkem o uspesnem smazanim
            for (DateTimeSlot slot : periodicSlots) {
                //String msg = "vse NE, zacatek request" + slot.getStart();
                message.appendLine("DELETED");
            }
        }
        else if (totalNotifications == allocationFailedNotifications) {
            //TODO: stejne s fail hlaskou
        } else {
            //TODO: vypsat uspesne stejne jako v prvnim krkoku
            //TODO: explicitne vypsat nepovedene

            //TODO: v jinem pripade nechat!!!DOCASNE!!!
            for (AbstractNotification notification : notifications) {
                NotificationMessage childMessage;
                if (notification instanceof ConfigurableNotification) {
                    ConfigurableNotification configurableEvent = (ConfigurableNotification) notification;
                    childMessage = configurableEvent.renderMessage(configuration, manager);
                } else {
                    throw new TodoImplementException(notification.getClass());
                }
                //TODO: message.appendChildMessage(childMessage);
            }
        }

        for (Map.Entry<DateTimeSlot,ReservationNotification> entry : firstNotifications.entrySet()) {
            ReservationNotification reservationNotification = entry.getValue();
            if (entry.getKey() instanceof PeriodicDateTimeSlot) {
                PeriodicDateTime periodicDateTime = ((PeriodicDateTimeSlot) entry.getKey()).getPeriodicDateTime();
                reservationNotification.setPeriod(periodicDateTime.getPeriod());
                reservationNotification.setEnd(periodicDateTime.getEnd());
            }
            NotificationMessage childMessage = reservationNotification.renderMessage(configuration, manager);
            message.appendChildMessage(childMessage);
        }

        return message;
    }

    @Override
    protected void onAfterAdded(NotificationManager notificationManager, EntityManager entityManager)
    {
        super.onAfterAdded(notificationManager, entityManager);

        Long reservationRequestId = ObjectIdentifier.parseId(getReservationRequestId(), ObjectType.RESERVATION_REQUEST);
        notificationManager.reservationRequestNotificationsById.put(reservationRequestId, this);
    }

    @Override
    protected void onAfterRemoved(NotificationManager notificationManager)
    {
        super.onAfterRemoved(notificationManager);

        Long reservationRequestId = ObjectIdentifier.parseId(getReservationRequestId(), ObjectType.RESERVATION_REQUEST);
        notificationManager.reservationRequestNotificationsById.remove(reservationRequestId);
    }

    @Override
    public boolean preprocess(NotificationManager notificationManager)
    {
        if (notifications.size() == 0) {
            return true;
        }
        if (!first) {
            boolean skip = false;
            for (AbstractNotification notification : notifications) {
                if (!(notification instanceof ReservationNotification.New)) {
                    skip = true;
                }
            }
            if (!skip) {
                return true;
            }
        }
        if (reusedReservationRequestId != null && notifications.size() == 1) {
            AbstractReservationRequestNotification notification = notifications.get(0);
            if (notification instanceof ReservationNotification.Deleted) {
                ReservationRequestNotification reservationRequestNotification =
                        notificationManager.reservationRequestNotificationsById.get(reusedReservationRequestId);
                if (reservationRequestNotification != null) {
                    reservationRequestNotification.addNotification(notification);
                    return true;
                }
            }
        }
        return false;
    }
}
