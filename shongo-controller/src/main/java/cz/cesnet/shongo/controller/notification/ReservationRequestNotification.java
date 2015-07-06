package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersistentObject;
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
import cz.cesnet.shongo.controller.booking.room.RoomSpecification;
import org.joda.time.*;

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
     * List of periodic slots of {@link ReservationRequest}
     */
    private List<DateTimeSlot> periodicSlots = new LinkedList<DateTimeSlot>();

    /**
     * List of periodic slots of modified {@link ReservationRequest}
     */
    private List<DateTimeSlot> previousPeriodicSlots = new LinkedList<DateTimeSlot>();
    /**
     * List of {@link AbstractNotification}s which are part of the {@link ReservationNotification}.
     */
    private List<AbstractReservationRequestNotification> notifications =
            new LinkedList<AbstractReservationRequestNotification>();

    /**
     * First notification of the slot (mapped by it), used in renderMessage().
     *
     * @see cz.cesnet.shongo.controller.notification.ReservationRequestNotification method setFirstSlotNotification()
     */
    Map<DateTimeSlot,AbstractReservationRequestNotification> firstNotifications;

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

        int slotMinutesBefore = 0;
        int slotMinutesAfter = 0;
        if (reservationRequest.getSpecification() instanceof RoomSpecification) {
            slotMinutesBefore = ((RoomSpecification) reservationRequest.getSpecification()).getSlotMinutesBefore();
            slotMinutesAfter = ((RoomSpecification) reservationRequest.getSpecification()).getSlotMinutesAfter();
        }
        reservationRequest = PersistentObject.getLazyImplementation(reservationRequest);
        if (reservationRequest instanceof ReservationRequest) {
            Interval originSlot = ((ReservationRequest) reservationRequest).getSlot();
            periodicSlots.add(new AbsoluteDateTimeSlot(originSlot.getStart(), originSlot.getEnd()));
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

        AbstractReservationRequest previousReservationRequest = reservationRequest.getModifiedReservationRequest();
        if (previousReservationRequest != null) {
            previousReservationRequest = PersistentObject.getLazyImplementation(previousReservationRequest);
            if (previousReservationRequest instanceof ReservationRequest) {
                Interval originSlot = ((ReservationRequest) previousReservationRequest).getSlot();
                previousPeriodicSlots.add(new AbsoluteDateTimeSlot(originSlot.getStart(), originSlot.getEnd()));
            } else if (previousReservationRequest instanceof ReservationRequestSet) {
                ReservationRequestSet reservationRequestSet = (ReservationRequestSet) previousReservationRequest;
                for (DateTimeSlot slot : reservationRequestSet.getSlots()) {
                    if (slot instanceof AbsoluteDateTimeSlot) {
                        previousPeriodicSlots.add((AbsoluteDateTimeSlot) slot);
                    } else if (slot instanceof PeriodicDateTimeSlot) {
                        previousPeriodicSlots.add((PeriodicDateTimeSlot) slot);
                    } else {
                        throw new TodoImplementException("Missing DateTimeSlot type");
                    }
                }
            } else {
                throw new TodoImplementException("Missing AbstractReservationRequest type");
            }
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

        // First successful notifications mapped by it's slot
        this.firstNotifications = new LinkedHashMap<DateTimeSlot, AbstractReservationRequestNotification>();
        // All allocation failed notifications mapped by it's slot
        Map<DateTimeSlot,List<AllocationFailedNotification>> failedNotifications = new LinkedHashMap<DateTimeSlot, List<AllocationFailedNotification>>();
        // All deleted notifications mapped by it's slot
        Map<DateTimeSlot,List<ReservationNotification.Deleted>> deletedNotifications = new LinkedHashMap<DateTimeSlot, List<ReservationNotification.Deleted>>();

        // Number of child events of each type
        // Map all failed/deleted notification to their slots
        // Assign first New notification or if there is none failed/deleted to it's slot
        int allocationFailedNotifications = 0;
        int newReservationNotifications = 0;
        int deletedReservationNotifications = 0;
        for (AbstractReservationRequestNotification notification : notifications) {
            Interval notificationSlot = notification.getSlot();
            DateTimeSlot slot;
            // Get suitable time slot from reservationRequest for this notification
            if (!(notification instanceof ReservationNotification.Deleted)) {
                slot = getAbsolutePeriodicSlot(notificationSlot);
            }
            else {
                // Try to get suitable time slot from modifiedReservationRequest for Deleted notifications,
                // or just render it one by one
                try {
                    slot = getPreviousAbsolutePeriodicSlot(notificationSlot);
                } catch (IllegalArgumentException ex) {
                    deletedReservationNotifications++;

                    List<ReservationNotification.Deleted> slotsDeletedNotifications = deletedNotifications.get(null);
                    if (slotsDeletedNotifications == null) {
                        slotsDeletedNotifications = new LinkedList<ReservationNotification.Deleted>();
                        deletedNotifications.put(null, slotsDeletedNotifications);
                    }
                    slotsDeletedNotifications.add((ReservationNotification.Deleted) notification);
                    continue;
                }
            }
            setFirstSlotNotification(slot,notification);

            if (notification instanceof AllocationFailedNotification) {
                allocationFailedNotifications++;

                List<AllocationFailedNotification> slotsFailedNotifications = failedNotifications.get(slot);
                if (slotsFailedNotifications == null) {
                    slotsFailedNotifications = new LinkedList<AllocationFailedNotification>();
                    failedNotifications.put(slot,slotsFailedNotifications);
                }
                slotsFailedNotifications.add((AllocationFailedNotification) notification);
            }
            else if (notification instanceof ReservationNotification.New) {
                newReservationNotifications++;
            }
            else if (notification instanceof ReservationNotification.Deleted) {
                deletedReservationNotifications++;

                List<ReservationNotification.Deleted> slotsDeletedNotifications = deletedNotifications.get(slot);
                if (slotsDeletedNotifications == null) {
                    slotsDeletedNotifications = new LinkedList<ReservationNotification.Deleted>();
                    deletedNotifications.put(slot,slotsDeletedNotifications);
                }
                slotsDeletedNotifications.add((ReservationNotification.Deleted) notification);
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

        for (Map.Entry<DateTimeSlot, AbstractReservationRequestNotification> entry : firstNotifications.entrySet()) {
            DateTimeSlot slot = entry.getKey();
            AbstractReservationRequestNotification firstReservationNotification = entry.getValue();

            // Add periodicity to render message
            if (slot instanceof PeriodicDateTimeSlot) {
                PeriodicDateTime periodicDateTimeSlot = ((PeriodicDateTimeSlot) slot).getPeriodicDateTime();
                firstReservationNotification.setPeriod(periodicDateTimeSlot.getPeriod());
                firstReservationNotification.setEnd(periodicDateTimeSlot.getEnd());
            }

            // Add error messages to time slot notification
            if (failedNotifications.get(slot) != null) {
                for (AllocationFailedNotification failedNotification : failedNotifications.get(slot)) {
                    if (!firstReservationNotification.equals(failedNotification)) {
                        firstReservationNotification.addFailedRequestNotification(failedNotification);
                    }
                }
                failedNotifications.remove(slot);
            }

            // Render deleted notifications in total if not all requests in time slot have been deleted
            /*if (!(firstReservationNotification instanceof ReservationNotification.Deleted) || failedNotifications.get(slot) != null) {
                List<ReservationNotification.Deleted> deletedNotificationsList = deletedNotifications.get(slot);
                if (deletedNotificationsList != null) {
                    for (ReservationNotification.Deleted deletedNotification : deletedNotificationsList) {
                        if (!firstReservationNotification.equals(deletedNotification) || !(firstReservationNotification instanceof ReservationNotification.Deleted)) {
                            firstReservationNotification.addAdditionalDeletedSlot(deletedNotification.getSlot());
                        }
                    }
                    deletedNotifications.remove(slot);
                }
            }*/

            NotificationMessage childMessage = firstReservationNotification.renderMessage(configuration, manager);
            message.appendChildMessage(childMessage);
        }

        /*
        for (AbstractNotification notification : notifications) {
            NotificationMessage childMessage;
            if (notification instanceof ConfigurableNotification) {
                ConfigurableNotification configurableEvent = (ConfigurableNotification) notification;
                childMessage = configurableEvent.renderMessage(configuration, manager);
            } else {
                throw new TodoImplementException(notification.getClass());
            }
            message.appendChildMessage(childMessage);
        }
        */

        List<ReservationNotification.Deleted>  deletedWithoutSlot = deletedNotifications.get(null);
        if (deletedWithoutSlot != null) {
            for (ReservationNotification.Deleted deletedNotification : deletedWithoutSlot) {
                NotificationMessage childMessage = deletedNotification.renderMessage(configuration, manager);
                message.appendChildMessage(childMessage);
            }
        }

        return message;
    }

    @Override
    protected void onAfterAdded(NotificationManager notificationManager, EntityManager entityManager)
    {
        super.onAfterAdded(notificationManager, entityManager);

        Long reservationRequestId = ObjectIdentifier.parseLocalId(getReservationRequestId(), ObjectType.RESERVATION_REQUEST);
        notificationManager.reservationRequestNotificationsById.put(reservationRequestId, this);
    }

    @Override
    protected void onAfterRemoved(NotificationManager notificationManager)
    {
        super.onAfterRemoved(notificationManager);

        Long reservationRequestId = ObjectIdentifier.parseLocalId(getReservationRequestId(), ObjectType.RESERVATION_REQUEST);
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

    /**
     *
     * @param interval
     * @return {@link cz.cesnet.shongo.controller.booking.datetime.DateTimeSlot) which contains given interval
     */
    private DateTimeSlot getAbsolutePeriodicSlot(Interval interval)
    {
        if (this.periodicSlots != null) {
            for (DateTimeSlot slot : this.periodicSlots) {
                if (slot.contains(interval)) {
                    return slot;
                }
            }
        }
        throw new IllegalArgumentException("Given time slot is not contained by any of the requested time slots.");
    }

    /**
     *
     * @param interval
     * @return {@link cz.cesnet.shongo.controller.booking.datetime.DateTimeSlot) which contains given interval
     */
    private DateTimeSlot getPreviousAbsolutePeriodicSlot(Interval interval)
    {
        if (this.previousPeriodicSlots != null) {
            for (DateTimeSlot slot : this.previousPeriodicSlots) {
                if (slot.contains(interval)) {
                    return slot;
                }
            }
        }
        throw new IllegalArgumentException("Given time slot is not contained by any of the requested time slots.");
    }

    /**
     * Sets first notification for given time slot. First notification is always instance of
     * {@link cz.cesnet.shongo.controller.notification.ReservationNotification.New} if it exists.
     * Otherwise it will be {@link cz.cesnet.shongo.controller.notification.ReservationNotification.Deleted}
     * or {@link cz.cesnet.shongo.controller.notification.AllocationFailedNotification}
     *
     * @param slot slot to be assigned
     * @param notification to add
     */
    private void setFirstSlotNotification(DateTimeSlot slot, AbstractReservationRequestNotification notification)
    {
        AbstractReservationRequestNotification firstNotification = firstNotifications.get(slot);
        if (firstNotification == null) {
            firstNotifications.put(slot, notification);
            return;
        }
        if (notification instanceof ReservationNotification.New) {
            if (notification.getSlotStart().isBefore(firstNotification.getSlotStart())) {
                firstNotifications.put(slot, notification);
            }
        }
        else if (!(firstNotification instanceof ReservationNotification.New)) {
            if (notification.getSlotStart().isBefore(firstNotification.getSlotStart())) {
                firstNotifications.put(slot, notification);
            }
        }
    }
}
