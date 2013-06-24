package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents an entity that can be scheduled.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Resource extends IdentifiedComplexType
{
    /**
     * User-id of the owner user.
     */
    private String userId;

    /**
     * Parent resource shongo-id for the resource.
     */
    private String parentResourceId;

    /**
     * Name of the resource.
     */
    private String name;

    /**
     * Description of the resource.
     */
    private String description;

    /**
     * List of capabilities which the resource has.
     */
    private List<Capability> capabilities = new LinkedList<Capability>();

    /**
     * Specifies whether resource can be scheduled by a scheduler.
     */
    private Boolean allocatable;

    /**
     * Specifies the maximum future for which the resource can be scheduled.
     */
    private Object maximumFuture;

    /**
     * List of persons that are notified when the {@link Resource} is allocated or when are
     * encountered any technical issues.
     */
    private List<Person> administrators = new LinkedList<Person>();

    /**
     * Child resources shongo-ids.
     */
    private List<String> childResourceIds = new ArrayList<String>();

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
     * @return {@link #parentResourceId}
     */
    public String getParentResourceId()
    {
        return parentResourceId;
    }

    /**
     * @param parentResourceId sets the {@link #parentResourceId}
     */
    public void setParentResourceId(String parentResourceId)
    {
        this.parentResourceId = parentResourceId;
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
     * @return {@link #capabilities}
     */
    public List<Capability> getCapabilities()
    {
        return capabilities;
    }

    /**
     * @param capability capability to be added to the {@link #capabilities}
     */
    public void addCapability(Capability capability)
    {
        capabilities.add(capability);
    }

    /**
     * @param capability capability to be removed from the {@link #capabilities}
     */
    public void removeCapability(Capability capability)
    {
        capabilities.remove(capability);
    }

    /**
     * @return {@link #allocatable}
     */
    public Boolean getAllocatable()
    {
        return (allocatable != null ? allocatable : Boolean.FALSE);
    }

    /**
     * @param allocatable sets the {@link #allocatable}
     */
    public void setAllocatable(Boolean allocatable)
    {
        this.allocatable = allocatable;
    }

    /**
     * @return {@link #maximumFuture}
     */
    public Object getMaximumFuture()
    {
        return maximumFuture;
    }

    /**
     * @param maximumFuture sets the {@link #maximumFuture}
     */
    public void setMaximumFuture(Object maximumFuture)
    {
        if (maximumFuture instanceof Period || maximumFuture instanceof DateTime || maximumFuture instanceof String) {
            this.maximumFuture = maximumFuture;
        }
        else {
            throw new TodoImplementException(maximumFuture.getClass().getCanonicalName());
        }
    }

    /**
     * @return {@link #administrators}
     */
    public List<Person> getAdministrators()
    {
        return administrators;
    }

    /**
     * @param person to be added to the {@link #administrators}
     */
    public void addAdministrator(Person person)
    {
        administrators.add(person);
    }

    /**
     * @param person to be removed from the {@link #administrators}
     */
    public void removeAdministrator(Person person)
    {
        administrators.remove(person);
    }

    /**
     * @return {@link #childResourceIds}
     */
    public List<String> getChildResourceIds()
    {
        return childResourceIds;
    }

    /**
     * @param childResourceId shonog-id to be added to the {@link #childResourceIds}
     */
    public void addChildResourceId(String childResourceId)
    {
        childResourceIds.add(childResourceId);
    }

    public static final String USER_ID = "userId";
    public static final String PARENT_RESOURCE_ID = "parentResourceId";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String CAPABILITIES = "capabilities";
    public static final String ALLOCATABLE = "allocatable";
    public static final String MAXIMUM_FUTURE = "maximumFuture";
    public static final String ADMINISTRATORS = "administrators";
    public static final String CHILD_RESOURCE_IDS = "childResourceIds";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_ID, userId);
        dataMap.set(PARENT_RESOURCE_ID, parentResourceId);
        dataMap.set(NAME, name);
        dataMap.set(DESCRIPTION, description);
        dataMap.set(CAPABILITIES, capabilities);
        dataMap.set(ALLOCATABLE, allocatable);
        dataMap.set(ADMINISTRATORS, administrators);
        dataMap.set(CHILD_RESOURCE_IDS, childResourceIds);

        if (maximumFuture instanceof DateTime) {
            dataMap.set(MAXIMUM_FUTURE, (DateTime) maximumFuture);
        }
        else if (maximumFuture instanceof Period) {
            dataMap.set(MAXIMUM_FUTURE, (Period) maximumFuture);
        }
        else if (maximumFuture instanceof String) {
            dataMap.set(MAXIMUM_FUTURE, (String) maximumFuture);
        }

        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userId = dataMap.getString(USER_ID);
        parentResourceId = dataMap.getString(PARENT_RESOURCE_ID);
        name = dataMap.getStringRequired(NAME);
        description = dataMap.getString(DESCRIPTION);
        capabilities = dataMap.getList(CAPABILITIES, Capability.class);
        allocatable = dataMap.getBool(ALLOCATABLE);
        maximumFuture = dataMap.getVariant(MAXIMUM_FUTURE, DateTime.class, Period.class);
        administrators = dataMap.getList(ADMINISTRATORS, Person.class);
        childResourceIds = dataMap.getList(CHILD_RESOURCE_IDS, String.class);
    }
}
