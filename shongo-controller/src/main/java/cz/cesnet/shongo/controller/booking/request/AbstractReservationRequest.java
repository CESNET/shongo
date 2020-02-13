package cz.cesnet.shongo.controller.booking.request;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.ClassHelper;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestReusement;
import cz.cesnet.shongo.controller.api.Controller;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.scheduler.Scheduler;
import cz.cesnet.shongo.controller.api.ReservationRequestType;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.hibernate.PersistentDateTime;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.report.ReportableSimple;
import cz.cesnet.shongo.util.ObjectHelper;
import org.joda.time.DateTime;
import org.joda.time.Period;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a base class for all reservation requests which contains common attributes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Table(indexes = { @Index(name = "created_by_idx", columnList = "created_by") })
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class AbstractReservationRequest extends PersistentObject implements ReportableSimple
{
    /**
     * Date/time when the {@link AbstractReservationRequest} was created.
     */
    private DateTime createdAt;

    /**
     * User-id of an user who created the {@link AbstractReservationRequest}.
     */
    private String createdBy;

    /**
     * Date/time when the {@link AbstractReservationRequest} was updated.
     */
    private DateTime updatedAt;

    /**
     * User-id of an user who updated the {@link AbstractReservationRequest} (e.g., who modify or delete it).
     */
    private String updatedBy;

    /**
     * {@link Allocation} for this {@link AbstractReservationRequest}.
     * Modified reservation requests share same {@link Allocation} instance.
     */
    private Allocation allocation;

    /**
     * Specifies {@link State} of the {@link AbstractReservationRequest}.
     */
    private State state;

    /**
     * Previous {@link AbstractReservationRequest} which is modified by this {@link AbstractReservationRequest}
     * (it's type must be {@link State#MODIFIED}).
     */
    private AbstractReservationRequest modifiedReservationRequest;

    /**
     * @see ReservationRequestPurpose
     */
    private ReservationRequestPurpose purpose;

    /**
     * Priority of the {@link AbstractReservationRequest} (higher number means that it should be allocated earlier).
     */
    private Integer priority;

    /**
     * Description of the reservation request that is shown to users.
     */
    private String description;

    /**
     * {@link cz.cesnet.shongo.controller.booking.specification.Specification} of target which is requested for a reservation.
     */
    private Specification specification;

    /**
     * Option that specifies whether inter-domain resource lookup can be performed.
     */
    private boolean interDomain;

    /**
     * {@link Allocation} whose allocated {@link Reservation}s can be used by {@link Scheduler} for
     * this {@link #allocation}.
     */
    private Allocation reusedAllocation;

    /**
     * Specifies whether usage of {@link #reusedAllocation} is mandatory or optional.
     */
    private boolean reusedAllocationMandatory;

    /**
     * {@link ReservationRequestReusement} of this {@link AbstractReservationRequest}.
     */
    private ReservationRequestReusement reusement;

    @Id
    @SequenceGenerator(name = "reservation_request_id", sequenceName = "reservation_request_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "reservation_request_id")
    @Override
    public Long getId()
    {
        return id;
    }

    /**
     * @return {@link #createdAt}
     */
    @Column(nullable = false)
    @org.hibernate.annotations.Type(type = PersistentDateTime.NAME)
    @Access(AccessType.FIELD)
    public DateTime getCreatedAt()
    {
        return createdAt;
    }

    private void setCreatedAt(DateTime createdAt)
    {
        this.createdAt = createdAt;
    }

    /**
     * @return {@link #createdBy}
     */
    @Column(nullable = false, length = Controller.USER_ID_COLUMN_LENGTH)
    public String getCreatedBy()
    {
        return createdBy;
    }

    /**
     * @param createdBy sets the {@link #createdBy}
     */
    public void setCreatedBy(String createdBy)
    {
        this.createdBy = createdBy;
    }

    /**
     * @return {@link #updatedAt}
     */
    @Column(nullable = false)
    @org.hibernate.annotations.Type(type = PersistentDateTime.NAME)
    @Access(AccessType.FIELD)
    public DateTime getUpdatedAt()
    {
        return updatedAt;
    }

    private void setUpdatedAt(DateTime updatedAt)
    {
        this.updatedAt = updatedAt;
    }

    /**
     * @return {@link #updatedBy}
     */
    @Column(nullable = false, length = Controller.USER_ID_COLUMN_LENGTH)
    public String getUpdatedBy()
    {
        return updatedBy;
    }

    /**
     * @param updatedBy sets the {@link #updatedBy}
     */
    public void setUpdatedBy(String updatedBy)
    {
        this.updatedBy = updatedBy;
    }

    /**
     * @return {@link #allocation}
     */
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, optional = false)
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
     * @return {@link #state}
     */
    @Column(nullable = false, length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    @Enumerated(EnumType.STRING)
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }

    /**
     * @return {@link #modifiedReservationRequest}
     */
    @OneToOne
    @JoinColumn(name = "modified_reservation_request_id")
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
    @Column(nullable = false, length = AbstractComplexType.ENUM_COLUMN_LENGTH)
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
    @ManyToOne(cascade = CascadeType.ALL, optional = false, fetch = FetchType.LAZY)
    public Specification getSpecification()
    {
        return getLazyImplementation(specification);
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
     * @return {@link #reusedAllocation}
     */
    @ManyToOne
    @JoinColumn(name = "reused_allocation_id")
    @Access(AccessType.FIELD)
    public Allocation getReusedAllocation()
    {
        return reusedAllocation;
    }

    /**
     * @param reusedAllocation sets the {@link #reusedAllocation}
     */
    public void setReusedAllocation(Allocation reusedAllocation)
    {
        this.reusedAllocation = reusedAllocation;
    }

    /**
     * @return {@link #reusedAllocationMandatory}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isReusedAllocationMandatory()
    {
        return reusedAllocationMandatory;
    }

    /**
     * @param reusedAllocationMandatory sets the {@link #reusedAllocationMandatory}
     */
    public void setReusedAllocationMandatory(boolean reusedAllocationMandatory)
    {
        this.reusedAllocationMandatory = reusedAllocationMandatory;
    }

    /**
     * @return {@link #reusement}
     */
    @Column(nullable = false, length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    @Enumerated(EnumType.STRING)
    public ReservationRequestReusement getReusement()
    {
        return reusement;
    }

    /**
     * @param reusement sets the {@link #reusement}
     */
    public void setReusement(ReservationRequestReusement reusement)
    {
        this.reusement = reusement;
    }

    /**
     * Validate {@link AbstractReservationRequest}.
     *
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectInvalidException
     *
     */
    public void validate() throws CommonReportSet.ObjectInvalidException
    {
        if (modifiedReservationRequest != null && !modifiedReservationRequest.getState().equals(State.MODIFIED)) {
            throw new CommonReportSet.ObjectInvalidException(ObjectIdentifier.formatId(modifiedReservationRequest),
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
     * @param entityManager
     * @return new cloned instance of this {@link AbstractReservationRequest}
     */
    public abstract AbstractReservationRequest clone(EntityManager entityManager);

    /**
     * Synchronize properties from given {@code abstractReservationRequest}.
     *
     *
     * @param reservationRequest from which will be copied all properties values to this {@link cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest}
     * @param entityManager
     * @return true if some modification was made
     */
    public boolean synchronizeFrom(AbstractReservationRequest reservationRequest, EntityManager entityManager)
    {
        boolean modified = !ObjectHelper.isSame(getCreatedBy(), reservationRequest.getCreatedBy())
                || !ObjectHelper.isSame(getUpdatedBy(), reservationRequest.getUpdatedBy())
                || !ObjectHelper.isSame(getPurpose(), reservationRequest.getPurpose())
                || !ObjectHelper.isSame(getPriority(), reservationRequest.getPriority())
                || !ObjectHelper.isSame(getDescription(), reservationRequest.getDescription())
                || !ObjectHelper.isSame(isInterDomain(), reservationRequest.isInterDomain())
                || !ObjectHelper.isSamePersistent(getReusedAllocation(), reservationRequest.getReusedAllocation())
                || !ObjectHelper.isSame(isReusedAllocationMandatory(), reservationRequest.isReusedAllocationMandatory())
                || !ObjectHelper.isSame(getReusement(), reservationRequest.getReusement());
        setCreatedBy(reservationRequest.getCreatedBy());
        setUpdatedBy(reservationRequest.getUpdatedBy());
        setPurpose(reservationRequest.getPurpose());
        setPriority(reservationRequest.getPriority());
        setDescription(reservationRequest.getDescription());
        setInterDomain(reservationRequest.isInterDomain());
        setReusedAllocation(reservationRequest.getReusedAllocation());
        setReusedAllocationMandatory(reservationRequest.isReusedAllocationMandatory());
        setReusement(reservationRequest.getReusement());

        Specification oldSpecification = getSpecification();
        Specification newSpecification = reservationRequest.getSpecification();
        if (oldSpecification == null || !oldSpecification.getClass().equals(newSpecification.getClass())) {
            // Setup new specification
            setSpecification(newSpecification.clone(entityManager));
            modified = true;
        }
        else {
            // Check specification for modifications
            modified |= oldSpecification.synchronizeFrom(newSpecification, entityManager);
        }

        return modified;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate()
    {
        if (createdAt == null) {
            createdAt = DateTime.now();
        }
        updatedAt = DateTime.now();
        if (state == null) {
            state = State.ACTIVE;
        }
        if (priority == null) {
            priority = 0;
        }
        if (reusement == null) {
            reusement = ReservationRequestReusement.NONE;
        }
    }

    @Override
    @Transient
    public String getReportDescription()
    {
        return ObjectIdentifier.formatId(this);
    }

    /**
     * @return converted {@link AbstractReservationRequest}
     *         to {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}
     * @param administrator
     */
    public cz.cesnet.shongo.controller.api.AbstractReservationRequest toApi(boolean administrator)
    {
        return toApi(administrator ? Report.UserType.DOMAIN_ADMIN : Report.UserType.USER);
    }

    /**
     * @return converted {@link AbstractReservationRequest}
     *         to {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}
     */
    public cz.cesnet.shongo.controller.api.AbstractReservationRequest toApi(Report.UserType userType)
    {
        cz.cesnet.shongo.controller.api.AbstractReservationRequest api = createApi();
        toApi(api, userType);
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
        Class<? extends AbstractReservationRequest> requestClass = getClassFromApi(api.getClass());
        AbstractReservationRequest reservationRequest = ClassHelper.createInstanceFromClass(requestClass);
        reservationRequest.fromApi(api, entityManager);
        return reservationRequest;
    }

    /**
     * @return new instance of {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}
     */
    protected abstract cz.cesnet.shongo.controller.api.AbstractReservationRequest createApi();

    /**
     * @param api      {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest} to be filled
     * @param userType
     */
    protected void toApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, Report.UserType userType)
    {
        api.setId(ObjectIdentifier.formatId(this));
        api.setUserId(getCreatedBy());
        api.setDateTime(getCreatedAt());
        api.setPurpose(getPurpose());
        api.setPriority(getPriority());
        api.setDescription(getDescription());
        api.setSpecification(getSpecification().toApi());
        api.setInterDomain(isInterDomain());
        if (reusedAllocation != null) {
            api.setReusedReservationRequestId(
                    ObjectIdentifier.formatId(reusedAllocation.getReservationRequest()), reusedAllocationMandatory);
        }
        api.setReusement(getReusement());

        // Reservation request is deleted
        if (state.equals(State.DELETED)) {
            api.setType(ReservationRequestType.DELETED);
        }
        // Reservation request modifies another reservation request
        else if (modifiedReservationRequest != null) {
            api.setType(ReservationRequestType.MODIFIED);
        }
        // Reservation request is new
        else {
            api.setType(ReservationRequestType.NEW);
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
        setPurpose(api.getPurpose());
        setPriority(api.getPriority());
        setDescription(api.getDescription());

        cz.cesnet.shongo.controller.api.Specification specificationApi = api.getSpecification();
        if (specificationApi == null) {
            throw new IllegalArgumentException("Specification must not be null.");
        }
        else if (getSpecification() != null && getSpecification().equalsId(specificationApi.getId())) {
            getSpecification().fromApi(specificationApi, entityManager);
        }
        else {
            setSpecification(Specification.createFromApi(specificationApi, entityManager));
        }
        setInterDomain(api.getInterDomain());

        if (api.getReusedReservationRequestId() != null) {
            Long reusedReservationRequestId =
                    ObjectIdentifier.parseLocalId(api.getReusedReservationRequestId(), ObjectType.RESERVATION_REQUEST);
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            ReservationRequest reusedReservationRequest =
                    reservationRequestManager.getReservationRequest(reusedReservationRequestId);
            if (reusedReservationRequest.getReusement().equals(ReservationRequestReusement.NONE)) {
                throw new ControllerReportSet.ReservationRequestNotReusableException(
                        api.getReusedReservationRequestId());
            }
            setReusedAllocation(reusedReservationRequest.getAllocation());
        }
        setReusedAllocationMandatory(api.isReusedReservationRequestMandatory());
        setReusement(api.getReusement());
    }

    /**
     * {@link Specification} class by {@link cz.cesnet.shongo.controller.api.Specification} class.
     */
    private static final Map<
            Class<? extends cz.cesnet.shongo.controller.api.AbstractReservationRequest>,
            Class<? extends AbstractReservationRequest>> CLASS_BY_API = new HashMap<
            Class<? extends cz.cesnet.shongo.controller.api.AbstractReservationRequest>,
            Class<? extends AbstractReservationRequest>>();

    /**
     * Initialization for {@link #CLASS_BY_API}.
     */
    static {
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.ReservationRequest.class,
                ReservationRequest.class);
        CLASS_BY_API.put(cz.cesnet.shongo.controller.api.ReservationRequestSet.class,
                ReservationRequestSet.class);
    }

    /**
     * @param requestApiClass
     * @return {@link AbstractReservationRequest} class for given {@code apiClass}
     */
    public static Class<? extends AbstractReservationRequest> getClassFromApi(
            Class<? extends cz.cesnet.shongo.controller.api.AbstractReservationRequest> requestApiClass)
    {
        Class<? extends AbstractReservationRequest> requestClass = CLASS_BY_API.get(requestApiClass);
        if (requestClass == null) {
            throw new TodoImplementException(requestApiClass);
        }
        return requestClass;
    }

    /**
     * Enumeration of available states for {@link AbstractReservationRequest}.
     */
    public static enum State
    {
        /**
         * Reservation request is active which means that it is visible to users.
         */
        ACTIVE,

        /**
         * Reservation request is modified which means that another reservation request
         * replaces it and this reservation request is preserved only for history purposes.
         */
        MODIFIED,

        /**
         * Reservation request is deleted which means that it is not visible to users and it is
         * preserved only for history purposes.
         */
        DELETED
    }

}
