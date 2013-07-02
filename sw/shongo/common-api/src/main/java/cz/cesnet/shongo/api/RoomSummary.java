package cz.cesnet.shongo.api;

import jade.content.Concept;
import org.joda.time.DateTime;

/**
 * A brief info about a virtual room at a server.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class RoomSummary extends IdentifiedComplexType
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

    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String ALIAS = "alias";
    public static final String START_DATETIME = "startDateTime";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(NAME, name);
        dataMap.set(DESCRIPTION, description);
        dataMap.set(ALIAS, alias);
        dataMap.set(START_DATETIME, startDateTime);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        name = dataMap.getString(NAME);
        description = dataMap.getString(DESCRIPTION);
        alias = dataMap.getString(ALIAS);
        startDateTime = dataMap.getDateTime(START_DATETIME);
    }

    @Override
    public String toString()
    {
        return String.format(RoomSummary.class.getSimpleName() + " (id: %s, name: %s, startDateTime: %s)",
                getId(), getName(), getStartDateTime());
    }
}
