package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents a group of users.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Group extends IdentifiedComplexType
{
    /**
     * {@link Type} of this {@link Group}.
     */
    private Type type;

    /**
     * Identifier of parent {@link Group} or {@code null}.
     */
    private String parentGroupId;

    /**
     * Name of the {@link Group}.
     */
    private String name;

    /**
     * Description of the {@link Group}.
     */
    private String description;

    /**
     * User-ids of administrators.
     */
    private Set<String> administrators = new LinkedHashSet<String>();

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
    public Group(String name, Type type)
    {
        this.name = name;
        this.type = type;
    }

    /**
     * @return {@link #type}
     */
    public Type getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(Type type)
    {
        this.type = type;
    }

    /**
     * @return {@link #parentGroupId}
     */
    public String getParentGroupId()
    {
        return parentGroupId;
    }

    /**
     * @param parentGroupId sets the {@link #parentGroupId}
     */
    public void setParentGroupId(String parentGroupId)
    {
        this.parentGroupId = parentGroupId;
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
     * @return {@link #administrators}
     */
    public Set<String> getAdministrators()
    {
        return Collections.unmodifiableSet(administrators);
    }

    /**
     * @param administrators sets the {@link #administrators}
     */
    public void setAdministrators(Set<String> administrators)
    {
        this.administrators = administrators;
    }

    /**
     * @param administrator to be added to the {@link #administrators}
     */
    public void addAdministrator(String administrator)
    {
        administrators.add(administrator);
    }

    /**
     * @param groupAdministrators to be added to the {@link #administrators}
     */
    public void addAdministrators(Set<String> groupAdministrators)
    {
        administrators.addAll(groupAdministrators);
    }

    @Override
    public String toString()
    {
        return String.format(Group.class.getSimpleName() + "(id: %s, parentGroupId: name: %s)", id, parentGroupId, name);
    }

    private static final String TYPE = "type";
    private static final String PARENT_GROUP_ID = "parentGroupId";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String ADMINISTRATORS = "administrators";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(TYPE, type);
        dataMap.set(PARENT_GROUP_ID, parentGroupId);
        dataMap.set(NAME, name);
        dataMap.set(DESCRIPTION, description);
        dataMap.set(ADMINISTRATORS, administrators);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        type = dataMap.getEnumRequired(TYPE, Type.class);
        parentGroupId = dataMap.getString(PARENT_GROUP_ID);
        name = dataMap.getStringRequired(NAME);
        description = dataMap.getString(DESCRIPTION);
        administrators = dataMap.getSet(ADMINISTRATORS, String.class);
    }

    /**
     * Type of {@link Group}.
     */
    public static enum Type
    {
        /**
         * System group - can be show/managed only by administrators.
         */
        SYSTEM,

        /**
         * User group - can be managed by normal user.
         */
        USER
    }
}
