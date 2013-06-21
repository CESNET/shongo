package cz.cesnet.shongo.oldapi;

import cz.cesnet.shongo.oldapi.rpc.StructType;
import cz.cesnet.shongo.oldapi.util.IdentifiedObject;
import jade.content.Concept;
import org.joda.time.DateTime;

/**
 * A brief info about a virtual room at a server.
 * <p/>
 * TODO: synchronize with Room
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class RoomSummary extends IdentifiedObject implements StructType, Concept
{
    /**
     * User readable name of the room.
     */
    private String name;

    /**
     * User readable description of the room.
     */
    private String description;

    /**
     * Description of main alias(es).
     */
    private String alias;

    /**
     * Date/time when the room was started.
     */
    private DateTime startDateTime;

    /**
     * Constructor.
     */
    public RoomSummary()
    {
    }

    /**
     * @return {@link #name}
     */
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
     * @return {@link #alias}
     */
    public String getAlias()
    {
        return alias;
    }

    /**
     * @param alias {@link #alias}
     */
    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    /**
     * @return {@link #startDateTime}
     */
    public DateTime getStartDateTime()
    {
        return startDateTime;
    }

    /**
     * @param startDateTime sets the {@link #startDateTime}
     */
    public void setStartDateTime(DateTime startDateTime)
    {
        this.startDateTime = startDateTime;
    }

    @Override
    public String toString()
    {
        return String.format(RoomSummary.class.getSimpleName() + " (id: %s, name: %s, startDateTime: %s)",
                getId(), getName(), getStartDateTime());
    }
}
