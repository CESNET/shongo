package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.common.IdentifierFormat;
import cz.cesnet.shongo.controller.fault.PersistentEntityNotFoundException;
import cz.cesnet.shongo.controller.report.ReportablePersistentObject;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.apache.commons.lang.ObjectUtils;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a base class for all reservation requests which contains common attributes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class AbstractReservationRequest extends ReportablePersistentObject
{
    /**
     * User-id of an user who is owner of the {@link AbstractReservationRequest}.
     */
    private String userId;

    /**
     * Date/time when the reservation request was created.
     */
    private DateTime created;

    /**
     * Description of the reservation that is shown to users.
     */
    private String description;

    /**
     * List of allocated {@link cz.cesnet.shongo.controller.reservation.Reservation}s.
     */
    protected List<Reservation> reservations = new ArrayList<Reservation>();

    /**
     * @return {@link #userId}
     */
    @Column(nullable = false)
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    /**
     * @return {@link #created}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.FIELD)
    public DateTime getCreated()
    {
        return created;
    }

    /**
     * @return {@link #description}
     */
    @Column
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description sets the {@link #description}
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return {@link #reservations}
     */
    @OneToMany(mappedBy = "reservationRequest", targetEntity = Reservation.class)
    @Access(AccessType.FIELD)
    public List<Reservation> getReservations()
    {
        return Collections.unmodifiableList(reservations);
    }

    /**
     * @param id of the {@link Reservation}
     * @return {@link Reservation} with given {@code id}
     * @throws cz.cesnet.shongo.controller.fault.PersistentEntityNotFoundException
     *          when the {@link Reservation} doesn't exist
     */
    @Transient
    protected Reservation getReservationById(Long id) throws PersistentEntityNotFoundException
    {
        for (Reservation reservation : reservations) {
            if (reservation.getId().equals(id)) {
                return reservation;
            }
        }
        throw new PersistentEntityNotFoundException(Reservation.class, id);
    }

    /**
     * @param reservation to be added to the {@link #reservations}
     */
    public void addReservation(Reservation reservation)
    {
        // Manage bidirectional association
        if (reservations.contains(reservation) == false) {
            reservations.add(reservation);
            reservation.setReservationRequest(this);
        }
    }

    /**
     * @param reservation to be removed from the {@link #reservations}
     */
    public void removeReservation(Reservation reservation)
    {
        // Manage bidirectional association
        if (reservations.contains(reservation)) {
            reservations.remove(reservation);
            reservation.setReservationRequest(null);
        }
    }

    /**
     * Synchronize properties from given {@code abstractReservationRequest}.
     *
     * @param abstractReservationRequest from which will be copied all properties values to
     *                                   this {@link AbstractReservationRequest}
     * @return true if some modification was made
     */
    public boolean synchronizeFrom(AbstractReservationRequest abstractReservationRequest)
    {
        boolean modified = !ObjectUtils.equals(getDescription(), abstractReservationRequest.getDescription());
        setDescription(abstractReservationRequest.getDescription());
        return modified;
    }

    @PrePersist
    protected void onCreate()
    {
        if (created == null) {
            created = DateTime.now();
        }
    }

    /**
     * @return converted {@link AbstractReservationRequest}
     *         to {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}
     * @throws FaultException
     */
    public cz.cesnet.shongo.controller.api.AbstractReservationRequest toApi() throws FaultException
    {
        cz.cesnet.shongo.controller.api.AbstractReservationRequest api = createApi();
        toApi(api);
        return api;
    }

    /**
     * @param api
     * @param entityManager
     * @return new instance of {@link AbstractReservationRequest} from
     *         {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}
     * @throws FaultException
     */
    public static AbstractReservationRequest createFromApi(
            cz.cesnet.shongo.controller.api.AbstractReservationRequest api, EntityManager entityManager)
            throws FaultException
    {
        AbstractReservationRequest reservationRequest;
        if (api instanceof cz.cesnet.shongo.controller.api.ReservationRequest) {
            reservationRequest = new ReservationRequest();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.ReservationRequestSet) {
            reservationRequest = new ReservationRequestSet();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.PermanentReservationRequest) {
            reservationRequest = new PermanentReservationRequest();
        }
        else {
            throw new TodoImplementException(api.getClass().getCanonicalName());
        }
        reservationRequest.fromApi(api, entityManager);
        return reservationRequest;
    }

    /**
     * @return new instance of {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}
     */
    protected abstract cz.cesnet.shongo.controller.api.AbstractReservationRequest createApi();

    /**
     * @param api {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest} to be filled
     */
    protected void toApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api)
            throws FaultException
    {
        api.setId(IdentifierFormat.formatGlobalId(this));
        api.setUserId(getUserId());
        api.setCreated(getCreated());
        api.setDescription(getDescription());
    }

    /**
     * Synchronize {@link AbstractReservationRequest} from
     * {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}.
     *
     * @param api
     * @param entityManager
     * @throws FaultException
     */
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, EntityManager entityManager)
            throws FaultException
    {
        if (api.isPropertyFilled(cz.cesnet.shongo.controller.api.AbstractReservationRequest.DESCRIPTION)) {
            setDescription(api.getDescription());
        }
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("created", getCreated());
        map.put("description", getDescription());
    }
}
