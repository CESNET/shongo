package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.api.util.IdentifiedChangeableObject;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Request for reservation of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractReservationRequest extends IdentifiedChangeableObject
{
    /**
     * User-id of the owner user.
     */
    private String userId;

    /**
     * Type of the reservation request.
     */
    private ReservationRequestType type;

    /**
     * Date/time when the reservation request was updated.
     */
    private DateTime dateTime;

    /**
     * @see cz.cesnet.shongo.controller.ReservationRequestPurpose
     */
    public static final String PURPOSE = "purpose";

    /**
     * Priority of the reservation request.
     */
    public static final String PRIORITY = "priority";

    /**
     * Description of the reservation request.
     */
    public static final String DESCRIPTION = "description";

    /**
     * {@link Specification} which is requested for the reservation.
     */
    public static final String SPECIFICATION = "specification";

    /**
     * Specifies whether the scheduler should try allocate resources from other domains.
     */
    public static final String INTER_DOMAIN = "interDomain";

    /**
     * Collection of shongo-ids for {@link cz.cesnet.shongo.controller.api.Reservation}s which are provided
     * to the {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}.
     */
    public static final String PROVIDED_RESERVATION_IDS = "providedReservationIds";

    /**
     * Constructor.
     */
    public AbstractReservationRequest()
    {
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
     * @return {@link #PURPOSE}
     */
    @Required
    public ReservationRequestPurpose getPurpose()
    {
        return getPropertyStorage().getValue(PURPOSE);
    }

    /**
     * @param purpose sets the {@link #PURPOSE}
     */
    public void setPurpose(ReservationRequestPurpose purpose)
    {
        getPropertyStorage().setValue(PURPOSE, purpose);
    }

    /**
     * @return {@link #PRIORITY}
     */
    public Integer getPriority()
    {
        return getPropertyStorage().getValue(PRIORITY);
    }

    /**
     * @param priority sets the {@link #PRIORITY}
     */
    public void setPriority(Integer priority)
    {
        getPropertyStorage().setValue(PRIORITY, priority);
    }

    /**
     * @return {@link #DESCRIPTION}
     */
    public String getDescription()
    {
        return getPropertyStorage().getValue(DESCRIPTION);
    }

    /**
     * @param description sets the {@link #DESCRIPTION}
     */
    public void setDescription(String description)
    {
        getPropertyStorage().setValue(DESCRIPTION, description);
    }

    /**
     * @return {@link #SPECIFICATION}
     */
    @Required
    public Specification getSpecification()
    {
        return getPropertyStorage().getValue(SPECIFICATION);
    }

    /**
     * @param specification sets the {@link #SPECIFICATION}
     */
    public <T extends Specification> T setSpecification(T specification)
    {
        getPropertyStorage().setValue(SPECIFICATION, specification);
        return specification;
    }

    /**
     * @return {@link #INTER_DOMAIN}
     */
    public Boolean getInterDomain()
    {
        return getPropertyStorage().getValue(INTER_DOMAIN);
    }

    /**
     * @param interDomain sets the {@link #INTER_DOMAIN}
     */
    public void setInterDomain(Boolean interDomain)
    {
        getPropertyStorage().setValue(INTER_DOMAIN, interDomain);
    }

    /**
     * @return {@link #PROVIDED_RESERVATION_IDS}
     */
    public List<String> getProvidedReservationIds()
    {
        return getPropertyStorage().getCollection(PROVIDED_RESERVATION_IDS, List.class);
    }

    /**
     * @param providedReservationIds sets the {@link #PROVIDED_RESERVATION_IDS}
     */
    public void setProvidedReservationIds(List<String> providedReservationIds)
    {
        getPropertyStorage().setCollection(PROVIDED_RESERVATION_IDS, providedReservationIds);
    }

    /**
     * @param providedReservationId to be added to the {@link #PROVIDED_RESERVATION_IDS}
     */
    public void addProvidedReservationId(String providedReservationId)
    {
        getPropertyStorage().addCollectionItem(PROVIDED_RESERVATION_IDS, providedReservationId,
                List.class);
    }

    /**
     * @param providedReservationId to be removed from the {@link #PROVIDED_RESERVATION_IDS}
     */
    public void removeProvidedReservationId(String providedReservationId)
    {
        getPropertyStorage().removeCollectionItem(PROVIDED_RESERVATION_IDS, providedReservationId);
    }
}
