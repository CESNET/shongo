package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestReusement;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;

/**
 * Request for reservation of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractReservationRequest extends IdentifiedComplexType
{
    /**
     * Type of the reservation request.
     */
    private ReservationRequestType type;

    /**
     * Date/time when the reservation request was created.
     */
    private DateTime dateTime;

    /**
     * User-id of the user who created the reservation request.
     */
    private String userId;

    /**
     * @see ReservationRequestPurpose
     */
    private ReservationRequestPurpose purpose;

    /**
     * Priority of the reservation request.
     */
    private Integer priority;

    /**
     * Description of the reservation request.
     */
    private String description;

    /**
     * {@link Specification} which is requested for the reservation.
     */
    private Specification specification;

    /**
     * Specifies whether the scheduler should try allocate resources from other domains.
     */
    private boolean interDomain;

    /**
     * Shongo-id for {@link ReservationRequest} whose allocated {@link Reservation}s are reusable
     * to the {@link AbstractReservationRequest}.
     */
    private String reusedReservationRequestId;

    /**
     * {@link ReservationRequestReusement} of this {@link AbstractReservationRequest}.
     */
    private ReservationRequestReusement reusement;

    /**
     * Constructor.
     */
    public AbstractReservationRequest()
    {
    }

    /**
     * @return {@link #type}
     */
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
     * @return {@link #dateTime}
     */
    public DateTime getDateTime()
    {
        return dateTime;
    }

    /**
     * @param dateTime sets the {@link #dateTime}
     */
    public void setDateTime(DateTime dateTime)
    {
        this.dateTime = dateTime;
    }

    /**
     * @return {@link #userId}
     */
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
     * @return {@link #purpose}
     */
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
    public Specification getSpecification()
    {
        return specification;
    }

    /**
     * @param specification sets the {@link #specification}
     */
    public <T extends Specification> T setSpecification(T specification)
    {
        this.specification = specification;
        return specification;
    }

    /**
     * @return {@link #interDomain}
     */
    public Boolean getInterDomain()
    {
        return interDomain;
    }

    /**
     * @param interDomain sets the {@link #interDomain}
     */
    public void setInterDomain(Boolean interDomain)
    {
        this.interDomain = interDomain;
    }

    /**
     * @return {@link #reusedReservationRequestId}
     */
    public String getReusedReservationRequestId()
    {
        return reusedReservationRequestId;
    }

    /**
     * @param reusedReservationRequestId sets the {@link #reusedReservationRequestId}
     */
    public void setReusedReservationRequestId(String reusedReservationRequestId)
    {
        this.reusedReservationRequestId = reusedReservationRequestId;
    }

    /**
     * @return {@link #reusement}
     */
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
     * @param reservationService
     * @param securityToken
     * @return last {@link Reservation}
     */
    public Reservation getLastReservation(ReservationService reservationService, SecurityToken securityToken)
    {
        if (this instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) this;
            return reservationRequest.getLastReservation(reservationService, securityToken);
        }
        return null;
    }

    private static final String TYPE = "type";
    private static final String DATETIME = "dateTime";
    private static final String USER_ID = "userId";
    private static final String PURPOSE = "purpose";
    private static final String PRIORITY = "priority";
    private static final String DESCRIPTION = "description";
    private static final String SPECIFICATION = "specification";
    private static final String INTER_DOMAIN = "interDomain";
    private static final String REUSED_RESERVATION_REQUEST_ID = "reusedReservationRequestId";
    private static final String REUSEMENT = "reusement";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(TYPE, type);
        dataMap.set(DATETIME, dateTime);
        dataMap.set(USER_ID, userId);
        dataMap.set(PURPOSE, purpose);
        dataMap.set(PRIORITY, priority);
        dataMap.set(DESCRIPTION, description);
        dataMap.set(SPECIFICATION, specification);
        dataMap.set(INTER_DOMAIN, interDomain);
        dataMap.set(REUSED_RESERVATION_REQUEST_ID, reusedReservationRequestId);
        dataMap.set(REUSEMENT, reusement);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        type = dataMap.getEnum(TYPE, ReservationRequestType.class);
        dateTime = dataMap.getDateTime(DATETIME);
        userId = dataMap.getString(USER_ID);
        purpose = dataMap.getEnumRequired(PURPOSE, ReservationRequestPurpose.class);
        priority = dataMap.getInteger(PRIORITY);
        description = dataMap.getString(DESCRIPTION);
        specification = dataMap.getComplexTypeRequired(SPECIFICATION, Specification.class);
        interDomain = dataMap.getBool(INTER_DOMAIN);
        reusedReservationRequestId = dataMap.getString(REUSED_RESERVATION_REQUEST_ID);
        reusement = dataMap.getEnum(REUSEMENT, ReservationRequestReusement.class);
    }
}
