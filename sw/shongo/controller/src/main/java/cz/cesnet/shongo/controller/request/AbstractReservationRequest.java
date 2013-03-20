package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.ControllerFaultSet;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.report.ReportablePersistentObject;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.apache.commons.lang.ObjectUtils;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Period;

import javax.persistence.*;
import java.util.*;

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
     * User-id of an user who created the {@link AbstractReservationRequest}.
     */
    private String userId;

    /**
     * Date/time when the {@link AbstractReservationRequest} was created.
     */
    private DateTime created;

    /**
     * @see ReservationRequestPurpose
     */
    private ReservationRequestPurpose purpose;

    /**
     * Priority of the {@link AbstractReservationRequest}.
     */
    private Integer priority;

    /**
     * Description of the reservation request that is shown to users.
     */
    private String description;

    /**
     * {@link Specification} of target which is requested for a reservation.
     */
    private Specification specification;

    /**
     * Option that specifies whether inter-domain resource lookup can be performed.
     */
    private boolean interDomain;

    /**
     * List of {@link Reservation}s of allocated resources which can be used by {@link Scheduler} for allocation of
     * this {@link cz.cesnet.shongo.controller.request.AbstractReservationRequest}.
     */
    private List<Reservation> providedReservations = new ArrayList<Reservation>();

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
     * @return {@link #purpose}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public ReservationRequestPurpose getPurpose()
    {
        return purpose;
    }

    /**
     * @param purpose sets the {@link #purpose}
     */
    public void setPurpose(ReservationRequestPurpose purpose)
    {
        this.purpose = purpose;
    }

    /**
     * @return {@link #priority}
     */
    @Column(nullable = false)
    public Integer getPriority()
    {
        return priority;
    }

    /**
     * @param priority sets the {@link #priority}
     */
    public void setPriority(Integer priority)
    {
        this.priority = priority;
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
     * @return {@link #specification}
     */
    @ManyToOne(cascade = CascadeType.ALL)
    public Specification getSpecification()
    {
        return specification;
    }

    /**
     * @param specification sets the {@link #specification}
     */
    public void setSpecification(Specification specification)
    {
        this.specification = specification;
    }

    /**
     * @return {@link #interDomain}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
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

    /**
     * @return {@link #providedReservations}
     */
    @ManyToMany
    @Access(AccessType.FIELD)
    public List<Reservation> getProvidedReservations()
    {
        return providedReservations;
    }

    /**
     * @param providedReservations sets the {@link #providedReservations}
     */
    public void setProvidedReservations(List<Reservation> providedReservations)
    {
        this.providedReservations.clear();
        for (Reservation providedReservation : providedReservations) {
            this.providedReservations.add(providedReservation);
        }
    }

    /**
     * @param providedReservation to be added to the {@link #providedReservations}
     */
    public void addProvidedReservation(Reservation providedReservation)
    {
        providedReservations.add(providedReservation);
    }

    /**
     * @param providedReservationId for {@link Reservation} to be removed from {@link #providedReservations}
     */
    public void removeProvidedReservation(Long providedReservationId)
    {
        for (int index = 0; index < providedReservations.size(); index++) {
            if (providedReservations.get(index).getId().equals(providedReservationId)) {
                providedReservations.remove(index);
                break;
            }
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
        boolean modified = !ObjectUtils.equals(getPurpose(), abstractReservationRequest.getPurpose())
                || !ObjectUtils.equals(getPriority(), abstractReservationRequest.getPriority())
                || !ObjectUtils.equals(getDescription(), abstractReservationRequest.getDescription())
                || !ObjectUtils.equals(isInterDomain(), abstractReservationRequest.isInterDomain());
        setPurpose(abstractReservationRequest.getPurpose());
        setPriority(abstractReservationRequest.getPriority());
        setDescription(abstractReservationRequest.getDescription());
        setInterDomain(abstractReservationRequest.isInterDomain());
        if (!ObjectUtils.equals(getProvidedReservations(), abstractReservationRequest.getProvidedReservations())) {
            setProvidedReservations(abstractReservationRequest.getProvidedReservations());
            modified = true;
        }
        return modified;
    }

    @PrePersist
    protected void onCreate()
    {
        if (created == null) {
            created = DateTime.now();
        }
        if (priority == null) {
            priority = 0;
        }
    }

    /**
     * Validate {@link AbstractReservationRequest}.
     *
     * @throws FaultException
     */
    public void validate() throws FaultException
    {
    }

    /**
     * Validate given slot {@code duration} if it is longer than 0 seconds.
     *
     * @param duration to be validated
     * @throws FaultException
     */
    protected static void validateSlotDuration(Period duration) throws FaultException
    {
        if (duration.equals(new Period())) {
            ControllerFaultSet.throwReservationRequestEmptyDurationFault();
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
        api.setId(EntityIdentifier.formatId(this));
        api.setUserId(getUserId());
        api.setCreated(getCreated());
        api.setPurpose(getPurpose());
        api.setPriority(getPriority());
        api.setDescription(getDescription());
        api.setSpecification(getSpecification().toApi());
        api.setInterDomain(isInterDomain());
        for (Reservation providedReservation : getProvidedReservations()) {
            api.addProvidedReservationId(EntityIdentifier.formatId(providedReservation));
        }
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
        if (api.isPropertyFilled(cz.cesnet.shongo.controller.api.AbstractReservationRequest.PURPOSE)) {
            setPurpose(api.getPurpose());
        }
        if (api.isPropertyFilled(cz.cesnet.shongo.controller.api.AbstractReservationRequest.PRIORITY)) {
            setPriority(api.getPriority());
        }
        if (api.isPropertyFilled(cz.cesnet.shongo.controller.api.AbstractReservationRequest.DESCRIPTION)) {
            setDescription(api.getDescription());
        }
        if (api.isPropertyFilled(cz.cesnet.shongo.controller.api.ReservationRequest.SPECIFICATION)) {
            cz.cesnet.shongo.controller.api.Specification specificationApi = api.getSpecification();
            if (specificationApi == null) {
                setSpecification(null);
            }
            else if (getSpecification() != null && getSpecification().equalsId(specificationApi.getId())) {
                getSpecification().fromApi(specificationApi, entityManager);
            }
            else {
                setSpecification(Specification.createFromApi(specificationApi, entityManager));
            }
        }
        if (api.isPropertyFilled(cz.cesnet.shongo.controller.api.AbstractReservationRequest.INTER_DOMAIN)) {
            setInterDomain(api.getInterDomain());
        }

        // Create/modify provided reservations
        ReservationManager reservationManager = new ReservationManager(entityManager);
        for (String providedReservationId : api.getProvidedReservationIds()) {
            if (api.isPropertyItemMarkedAsNew(api.PROVIDED_RESERVATION_IDS, providedReservationId)) {
                Long id = EntityIdentifier.parseId(
                        Reservation.class, providedReservationId);
                Reservation providedReservation = reservationManager.get(id);
                addProvidedReservation(providedReservation);
            }
        }
        // Delete provided reservations
        Set<String> apiDeletedProvidedReservationIds =
                api.getPropertyItemsMarkedAsDeleted(api.PROVIDED_RESERVATION_IDS);
        for (String providedReservationId : apiDeletedProvidedReservationIds) {
            Long id = EntityIdentifier.parseId(
                    Reservation.class, providedReservationId);
            removeProvidedReservation(id);
        }
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("created", getCreated());
        map.put("purpose", getPurpose());
        map.put("priority", getPriority());
        map.put("description", getDescription());
        map.put("specification", getSpecification());
        map.put("interDomain", isInterDomain());
    }
}
