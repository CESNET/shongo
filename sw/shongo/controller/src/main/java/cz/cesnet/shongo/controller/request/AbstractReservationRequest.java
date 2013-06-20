package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.util.ObjectHelper;
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
public abstract class AbstractReservationRequest extends PersistentObject implements Cloneable
{
    /**
     * User-id of an user who created the {@link AbstractReservationRequest}.
     */
    private String userId;

    /**
     * Date/time when the {@link AbstractReservationRequest} was created.
     */
    private DateTime createdAt;

    /**
     * Date/time when the {@link AbstractReservationRequest} was updated.
     */
    private DateTime updateAt;

    /**
     * {@link Allocation} for this {@link AbstractReservationRequest}.
     * Modified reservation requests share same {@link Allocation} instance.
     */
    private Allocation allocation;

    /**
     * Specifies {@link cz.cesnet.shongo.controller.ReservationRequestType} of the {@link AbstractReservationRequest}.
     */
    private ReservationRequestType type;

    /**
     * Previous {@link AbstractReservationRequest} which is modified by this {@link AbstractReservationRequest}
     * (it's type must be {@link cz.cesnet.shongo.controller.ReservationRequestType#MODIFIED}).
     */
    private AbstractReservationRequest modifiedReservationRequest;

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
     * @return {@link #createdAt}
     */
    @Column(nullable = false)
    @org.hibernate.annotations.Type(type = "DateTime")
    @Access(AccessType.FIELD)
    public DateTime getCreatedAt()
    {
        return createdAt;
    }

    /**
     * @return {@link #updateAt}
     */
    @Column(nullable = false)
    @org.hibernate.annotations.Type(type = "DateTime")
    @Access(AccessType.FIELD)
    public DateTime getUpdateAt()
    {
        return updateAt;
    }

    /**
     * @return {@link #allocation}
     */
    @ManyToOne(cascade = CascadeType.ALL, optional = false)
    @Access(AccessType.FIELD)
    public Allocation getAllocation()
    {
        return allocation;
    }

    /**
     * @param allocation sets the {@link #allocation}
     */
    public void setAllocation(Allocation allocation)
    {
        this.allocation = allocation;
    }

    /**
     * @return {@link #type}
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public ReservationRequestType getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(ReservationRequestType type)
    {
        this.type = type;
    }

    /**
     * @return {@link #modifiedReservationRequest}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public AbstractReservationRequest getModifiedReservationRequest()
    {
        return modifiedReservationRequest;
    }

    /**
     * @param modifiedReservationRequest sets the {@link #modifiedReservationRequest}
     */
    public void setModifiedReservationRequest(AbstractReservationRequest modifiedReservationRequest)
    {
        this.modifiedReservationRequest = modifiedReservationRequest;
    }

    /**
     * @return {@link #purpose}
     */
    @Column(nullable = false)
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
    @ManyToOne(cascade = CascadeType.ALL, optional = false)
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
        return Collections.unmodifiableList(providedReservations);
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
     * Validate {@link AbstractReservationRequest}.
     *
     * @throws CommonReportSet.EntityInvalidException
     *
     */
    public void validate() throws CommonReportSet.EntityInvalidException
    {
        if (modifiedReservationRequest != null && !modifiedReservationRequest.getType().equals(ReservationRequestType.MODIFIED)) {
            throw new CommonReportSet.EntityInvalidException(EntityIdentifier.formatId(modifiedReservationRequest),
                    "Modified reservation request isn't of type MODIFIED.");
        }
    }

    /**
     * Validate given slot {@code duration} if it is longer than 0 seconds.
     *
     * @param duration to be validated
     * @throws ControllerReportSet.ReservationRequestEmptyDurationException
     *
     */
    protected static void validateSlotDuration(Period duration)
            throws ControllerReportSet.ReservationRequestEmptyDurationException
    {
        if (duration.equals(new Period())) {
            throw new ControllerReportSet.ReservationRequestEmptyDurationException();
        }
    }

    /**
     * @return new cloned instance of this {@link AbstractReservationRequest}
     */
    @Override
    public abstract AbstractReservationRequest clone();

    /**
     * Synchronize properties from given {@code abstractReservationRequest}.
     *
     * @param reservationRequest from which will be copied all properties values to this {@link AbstractReservationRequest}
     * @return true if some modification was made
     */
    public boolean synchronizeFrom(AbstractReservationRequest reservationRequest)
    {
        boolean modified = !ObjectHelper.isSame(getUserId(), reservationRequest.getUserId())
                || !ObjectHelper.isSame(getPurpose(), reservationRequest.getPurpose())
                || !ObjectHelper.isSame(getPriority(), reservationRequest.getPriority())
                || !ObjectHelper.isSame(getDescription(), reservationRequest.getDescription())
                || !ObjectHelper.isSame(isInterDomain(), reservationRequest.isInterDomain());
        setUserId(reservationRequest.getUserId());
        setPurpose(reservationRequest.getPurpose());
        setPriority(reservationRequest.getPriority());
        setDescription(reservationRequest.getDescription());
        setInterDomain(reservationRequest.isInterDomain());

        Specification specification = reservationRequest.getSpecification();
        if (this.specification == null || this.specification.getClass() != specification.getClass()) {
            // Setup new specification
            setSpecification(specification.clone());
            modified = true;
        }
        else {
            // Check specification for modifications
            modified |= this.specification.synchronizeFrom(specification);
        }

        if (!ObjectHelper.isSame(getProvidedReservations(), reservationRequest.getProvidedReservations())) {
            setProvidedReservations(reservationRequest.getProvidedReservations());
            modified = true;
        }
        return modified;
    }

    @PrePersist
    @PreUpdate
    protected void onCreate()
    {
        if (createdAt == null) {
            createdAt = DateTime.now();
        }
        updateAt = DateTime.now();
        if (type == null) {
            type = ReservationRequestType.CREATED;
        }
        if (priority == null) {
            priority = 0;
        }
    }

    /**
     * @return converted {@link AbstractReservationRequest}
     *         to {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}
     */
    public cz.cesnet.shongo.controller.api.AbstractReservationRequest toApi(boolean admin)
    {
        return toApi(admin ? Report.MessageType.DOMAIN_ADMIN : Report.MessageType.USER);
    }

    /**
     * @return converted {@link AbstractReservationRequest}
     *         to {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}
     */
    public cz.cesnet.shongo.controller.api.AbstractReservationRequest toApi(Report.MessageType messageType)
    {
        cz.cesnet.shongo.controller.api.AbstractReservationRequest api = createApi();
        toApi(api, messageType);
        return api;
    }

    /**
     * @param api
     * @param entityManager
     * @return new instance of {@link AbstractReservationRequest} from
     *         {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}
     */
    public static AbstractReservationRequest createFromApi(
            cz.cesnet.shongo.controller.api.AbstractReservationRequest api, EntityManager entityManager)
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
     * @param api         {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest} to be filled
     * @param messageType
     */
    protected void toApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, Report.MessageType messageType)
    {
        api.setId(EntityIdentifier.formatId(this));
        api.setUserId(getUserId());
        api.setType(getType());
        api.setDateTime(getCreatedAt());
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
     */
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, EntityManager entityManager)
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

}
