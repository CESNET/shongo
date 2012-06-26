package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.common.DateTimeSlot;
import cz.cesnet.shongo.common.DateTimeSpecification;
import cz.cesnet.shongo.common.PersistentObject;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a request created by an user to get allocated some resources for videoconference calls.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ReservationRequest extends PersistentObject
{
    /**
     * Type of reservation.
     */
    public static enum Type
    {
        /**
         * Reservation that can be created by any user.
         */
        NORMAL,

        /**
         * Reservation that can be created only by owner of resources,
         * and the reservation can request only owned resources.
         */
        PERMANENT
    }

    /**
     * A purpose for which the reservation will be used.
     */
    public static enum Purpose
    {
        /**
         * Reservation will be used e.g., for research purposes.
         */
        SCIENCE,

        /**
         * Reservation will be used for education purposes (e.g., for a lecture).
         */
        EDUCATION
    }

    /**
     * State of reservation request.
     */
    public static enum State
    {
        /**
         * State tells that reservation request hasn't corresponding compartment requests created
         * or the reservation request has changed they are out-of-sync.
         */
        NOT_PREPROCESSED,

        /**
         * State tells that reservation request has corresponding compartment requests synced.
         */
        PREPROCESSED
    }

    /**
     * Type of the reservation. Permanent reservation are created by resource owners to
     * allocate the resource for theirs activity.
     */
    private Type type;

    /**
     * Purpose for the reservation (science/education).
     */
    private Purpose purpose;

    /**
     * Name of the reservation that is shown to users.
     */
    private String name;

    /**
     * List of date/time slots for which the reservation is requested.
     */
    private List<DateTimeSlot> requestedSlots = new ArrayList<DateTimeSlot>();

    /**
     * List of compartments that are requested for a reservation. Each
     * compartment represents a group of resources/persons that will
     * be used/participate in a separate videoconference call.
     */
    private List<Compartment> requestedCompartments = new ArrayList<Compartment>();

    /**
     * Specifies the default option who should initiate the call for all requested resources.
     */
    private CallInitiation callInitiation;

    /**
     * Option that specifies whether inter-domain resource lookup can be performed.
     */
    private boolean interDomain;

    /**
     * @return {@link #type}
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public Type getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(Type type)
    {
        this.type = type;
    }

    /**
     * @return {@link #purpose}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Purpose getPurpose()
    {
        return purpose;
    }

    /**
     * @param purpose sets the {@link #purpose}
     */
    public void setPurpose(Purpose purpose)
    {
        this.purpose = purpose;
    }

    /**
     * @return {@link #name}
     */
    @Column
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #requestedSlots}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<DateTimeSlot> getRequestedSlots()
    {
        return Collections.unmodifiableList(requestedSlots);
    }

    /**
     * @param requestedSlot slot to be added to the list of requested slots
     */
    public void addRequestedSlot(DateTimeSlot requestedSlot)
    {
        requestedSlots.add(requestedSlot);
    }

    /**
     * Add slot to the list of requested slots
     *
     * @param dateTime slot date/time
     * @param duration slot duration
     */
    public void addRequestedSlot(DateTimeSpecification dateTime, Period duration)
    {
        requestedSlots.add(new DateTimeSlot(dateTime, duration));
    }

    /**
     * @param requestedSlot slot to be removed from the {@link #requestedSlots}
     */
    public void removeRequestedSlot(DateTimeSlot requestedSlot)
    {
        requestedSlots.remove(requestedSlot);
    }

    /**
     * @return {@link #requestedCompartments}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "reservationRequest")
    @Access(AccessType.FIELD)
    public List<Compartment> getRequestedCompartments()
    {
        return Collections.unmodifiableList(requestedCompartments);
    }

    /**
     * @param compartment compartment to be added to the {@link #requestedCompartments}
     */
    public void addRequestedCompartment(Compartment compartment)
    {
        // Manage bidirectional association
        if (requestedCompartments.contains(compartment) == false) {
            requestedCompartments.add(compartment);
            compartment.setReservationRequest(this);
        }
    }

    /**
     * @param compartment compartment to be removed from the {@link #requestedCompartments}
     */
    public void removeRequestedCompartment(Compartment compartment)
    {
        // Manage bidirectional association
        if (requestedCompartments.contains(compartment)) {
            requestedCompartments.remove(compartment);
            compartment.setReservationRequest(null);
        }
    }

    /**
     * @return a new compartment that was added to the list of requested resources
     */
    public Compartment addRequestedCompartment()
    {
        Compartment compartment = new Compartment();
        addRequestedCompartment(compartment);
        return compartment;
    }

    /**
     * @return {@link #callInitiation}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public CallInitiation getCallInitiation()
    {
        return callInitiation;
    }

    /**
     * @param callInitiation sets the {@link #callInitiation}
     */
    public void setCallInitiation(CallInitiation callInitiation)
    {
        this.callInitiation = callInitiation;
    }

    /**
     * @return {@link #interDomain}
     */
    @Column
    public boolean isInterDomain()
    {
        return interDomain;
    }

    /**
     * @param interDomain sets the {@link #interDomain}
     */
    public void setInterDomain(boolean interDomain)
    {
        this.interDomain = interDomain;
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        map.put("type", getType().toString());
        if (getPurpose() != null) {
            map.put("purpose", getPurpose().toString());
        }
        addCollectionToMap(map, "slots", requestedSlots);
        addCollectionToMap(map, "compartments", requestedCompartments);
    }

    /**
     * Enumerate requested date/time slots in a specific interval.
     *
     * @param interval
     * @return list of all requested absolute date/time slots for given interval
     */
    public List<Interval> enumerateRequestedSlots(Interval interval)
    {
        List<Interval> enumeratedSlots = new ArrayList<Interval>();
        for (DateTimeSlot slot : requestedSlots) {
            enumeratedSlots.addAll(slot.enumerate(interval));
        }
        return enumeratedSlots;
    }

    /**
     * @param referenceDateTime
     * @return true whether reservation request has any requested slot after given reference date/time,
     *         false otherwise
     */
    public boolean hasRequestedSlotAfter(DateTime referenceDateTime)
    {
        for (DateTimeSlot slot : requestedSlots) {
            if (slot.getStart().willOccur(referenceDateTime)) {
                return true;
            }
        }
        return false;
    }
}
