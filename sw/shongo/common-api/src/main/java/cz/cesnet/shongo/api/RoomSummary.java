package cz.cesnet.shongo.api;

import cz.cesnet.shongo.api.util.IdentifiedObject;
import cz.cesnet.shongo.api.xmlrpc.StructType;
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
        return String.format("Room %s (name: %s, startDateTime: %s)", getId(), getName(), getStartDateTime());
    }
}
