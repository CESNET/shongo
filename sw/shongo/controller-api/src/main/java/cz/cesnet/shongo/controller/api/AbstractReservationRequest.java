package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
import org.joda.time.DateTime;

/**
 * Request for reservation of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractReservationRequest extends IdentifiedChangeableObject
{
    /**
     * Identifier of the resource.
     */
    private String identifier;

    /**
     * Date/time when the reservation request was created.
     */
    private DateTime created;

    /**
     * @see cz.cesnet.shongo.controller.ReservationRequestType
     */
    public static final String TYPE = "type";

    /**
     * Name of the reservation request.
     */
    public static final String NAME = "name";

    /**
     * @see cz.cesnet.shongo.controller.ReservationRequestPurpose
     */
    public static final String PURPOSE = "purpose";

    /**
     * Description of the reservation request.
     */
    public static final String DESCRIPTION = "description";

    /**
     * Specifies whether the scheduler should try allocate resources from other domains.
     */
    public static final String INTER_DOMAIN = "interDomain";

    /**
     * Constructor.
     */
    public AbstractReservationRequest()
    {
    }

    /**
     * @return {@link #identifier}
     */
    public String getIdentifier()
    {
        return identifier;
    }

    /**
     * @param identifier sets the {@link #identifier}
     */
    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    /**
     * @return {@link #created}
     */
    public DateTime getCreated()
    {
        return created;
    }

    /**
     * @param created sets the {@link #created}
     */
    public void setCreated(DateTime created)
    {
        this.created = created;
    }

    /**
     * @return {@link #TYPE}
     */
    @Required
    public ReservationRequestType getType()
    {
        return getPropertyStorage().getValue(TYPE);
    }

    /**
     * @param type sets the {@link #TYPE}
     */
    public void setType(ReservationRequestType type)
    {
        getPropertyStorage().setValue(TYPE, type);
    }

    /**
     * @return {@link #NAME}
     */
    public String getName()
    {
        return getPropertyStorage().getValue(NAME);
    }

    /**
     * @param name sets the {@link #NAME}
     */
    public void setName(String name)
    {
        getPropertyStorage().setValue(NAME, name);
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
}
