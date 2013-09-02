package cz.cesnet.shongo.controller.notification;


import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.reservation.RoomReservation;
import cz.cesnet.shongo.controller.settings.UserSettingsProvider;

import java.util.*;

/**
 * {@link Notification} for a {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationNotification extends Notification
{
    /**
     * {@link Locale}s for users which doesn't have preferred {@link Locale}.
     */
    public static List<Locale> AVAILABLE_LOCALES = new LinkedList<Locale>(){{
        add(cz.cesnet.shongo.controller.api.UserSettings.LOCALE_ENGLISH);
        add(cz.cesnet.shongo.controller.api.UserSettings.LOCALE_CZECH);
    }};

    /**
     * @see Type
     */
    private Type type;

    /**
     * Parameters.
     */
    String userId = null;
    cz.cesnet.shongo.controller.api.Reservation reservation = null;
    cz.cesnet.shongo.controller.api.AbstractReservationRequest reservationRequest = null;
    List<cz.cesnet.shongo.controller.api.AliasReservation> aliasReservations =
            new LinkedList<cz.cesnet.shongo.controller.api.AliasReservation>();

    /**
     * Constructor.
     *
     * @param type
     * @param reservation
     */
    public ReservationNotification(Type type, Reservation reservation, AuthorizationManager authorizationManager,
            UserSettingsProvider userSettingsProvider)
    {
        super(userSettingsProvider);

        this.type = type;
        this.userId = reservation.getUserId();

        // Add recipients
        for (String userId : authorizationManager.getUserIdsWithRole(new EntityIdentifier(reservation), Role.OWNER)) {
            addRecipient(authorizationManager.getUserInformation(userId), false);
        }
        addRecipientByReservation(reservation);

        AbstractReservationRequest reservationRequest = reservation.getTopReservationRequest();
        try {
            if (reservationRequest != null) {
                this.reservationRequest = reservationRequest.toApi(false);
            }
            this.reservation = reservation.toApi(false);

            if (reservation.getClass().equals(Reservation.class)) {
                Collection<AliasReservation> childAliasReservations =
                        reservation.getChildReservations(AliasReservation.class);
                if (childAliasReservations.size() > 0) {
                    for (AliasReservation aliasReservation : childAliasReservations) {
                        aliasReservations.add(aliasReservation.toApi(false));
                    }
                }
            }
            else if (reservation instanceof AliasReservation) {
                AliasReservation aliasReservation = (AliasReservation) reservation;
                aliasReservations.add(aliasReservation.toApi(false));
            }
        }
        catch (Exception exception) {
            Reporter.reportInternalError(Reporter.NOTIFICATION,
                    "Failed to create reservation notification", exception);
        }
    }

    /**
     * Add recipients by given {@code reservation}.
     *
     * @param reservation
     */
    public void addRecipientByReservation(Reservation reservation)
    {
        if (reservation instanceof ResourceReservation) {
            ResourceReservation resourceReservation = (ResourceReservation) reservation;
            for (Person person : resourceReservation.getResource().getAdministrators()) {
                addRecipient(person.getInformation(), true);
            }
        }
        if (reservation instanceof RoomReservation) {
            RoomReservation roomReservation = (RoomReservation) reservation;
            for (Person person : roomReservation.getDeviceResource().getAdministrators()) {
                addRecipient(person.getInformation(), true);
            }
        }
        if (reservation instanceof AliasReservation) {
            AliasReservation aliasReservation = (AliasReservation) reservation;
            for (Person person : aliasReservation.getAliasProviderCapability().getResource().getAdministrators()) {
                addRecipient(person.getInformation(), true);
            }
        }
        for (Reservation childReservation : reservation.getChildReservations()) {
            addRecipientByReservation(childReservation);
        }
    }

    @Override
    protected List<Locale> getAvailableLocals()
    {
        return AVAILABLE_LOCALES;
    }

    @Override
    protected NotificationMessage renderMessage(NotificationConfiguration configuration)
    {
        String messageName = type.getName() + " reservation " + reservation.getId();
        Map<String, Object> messageParameters = new HashMap<String, Object>();
        messageParameters.put("type", type);
        messageParameters.put("userId", userId);
        messageParameters.put("reservation", reservation);
        messageParameters.put("reservationRequest", reservationRequest);
        messageParameters.put("aliasReservations", aliasReservations);
        return renderMessageTemplate(configuration, messageName, "reservation-mail.ftl", messageParameters, "notification");
    }

    /**
     * Type of the {@link ReservationNotification}.
     */
    public static enum Type
    {
        /**
         * {@link ReservationNotification#reservation} is new.
         */
        NEW("New"),

        /**
         * {@link ReservationNotification#reservation} has been modified.
         */
        MODIFIED("Modified"),

        /**
         * {@link ReservationNotification#reservation} has been deleted.
         */
        DELETED("Deleted");

        /**
         * Name of the {@link Type}.
         */
        private String name;

        /**
         * Constructor.
         *
         * @param name sets the {@link #name}
         */
        private Type(String name)
        {
            this.name = name;
        }

        /**
         * @return {@link #name}
         */
        public String getName()
        {
            return name;
        }
    }
}
