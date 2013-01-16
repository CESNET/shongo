package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.util.IdentifiedChangeableObject;
import org.joda.time.DateTime;

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
     * Date/time when the reservation request was created.
     */
    private DateTime created;

    /**
     * Description of the reservation request.
     */
    public static final String DESCRIPTION = "description";

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
}
