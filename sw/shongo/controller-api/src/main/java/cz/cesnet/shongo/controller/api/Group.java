package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;

/**
 * Represents a group of users.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Group extends IdentifiedComplexType
{
    /**
     * Identifier of parent {@link Group} or {@code null}.
     */
    private String parentId;

    /**
     * Name of the {@link Group}.
     */
    private String name;

    /**
     * Description of the {@link Group}.
     */
    private String description;

    /**
     * Constructor.
     */
    public Group()
    {
    }

    /**
     * Constructor.
     *
     * @param name sets the {@link #name}
     */
    public Group(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #parentId}
     */
    public String getParentId()
    {
        return parentId;
    }

    /**
     * @param parentId sets the {@link #parentId}
     */
    public void setParentId(String parentId)
    {
        this.parentId = parentId;
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

    @Override
    public String toString()
    {
        return String.format(Group.class.getSimpleName() + "(id: %s, parentId: name: %s)", id, parentId, name);
    }

    private static final String PARENT_ID = "parentId";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(PARENT_ID, parentId);
        dataMap.set(NAME, name);
        dataMap.set(DESCRIPTION, description);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        parentId = dataMap.getString(PARENT_ID);
        name = dataMap.getString(NAME);
        description = dataMap.getString(DESCRIPTION);
    }
}
