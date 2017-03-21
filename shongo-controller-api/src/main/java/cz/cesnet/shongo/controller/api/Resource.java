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
     * Order in which the resource should be tried to be allocated ({@code null} means the last).
     */
    private Integer allocationOrder;

    /**
     * Specifies the maximum future for which the resource can be scheduled.
     */
    private Object maximumFuture;

    /**
     * List of additional administrator emails (basic administrators are specified by owners in
     * {@link cz.cesnet.shongo.controller.api.AclEntry}s).
     */
    private List<String> administratorEmails = new LinkedList<String>();

    /**
     * Child resources shongo-ids.
     */
    private List<String> childResourceIds = new ArrayList<String>();

    /**
     * Are reservations of this resource public.
     */
    private boolean calendarPublic;

    /**
     * Hash key used for public calendar URL
     */
    private String calendarUriKey;

    /**
     * Name of calendar on calendar server.
     */
    private String remoteCalendarName;

    /**
     * If all reservation request must be first confirmed by owner.
     */
    private boolean confirmByOwner;

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
     * @return {@link #allocationOrder}
     */
    public Integer getAllocationOrder()
    {
        return allocationOrder;
    }

    /**
     * @return {@link #confirmByOwner}
     */
    public boolean isConfirmByOwner()
    {
        return confirmByOwner;
    }

    /**
     * @param confirmByOwner sets the {@link #confirmByOwner}
     */
    public void setConfirmByOwner(boolean confirmByOwner)
    {
        this.confirmByOwner = confirmByOwner;
    }

    /**
     * @param allocationOrder sets the {@link #allocationOrder}
     */
    public void setAllocationOrder(Integer allocationOrder)
    {
        this.allocationOrder = allocationOrder;
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
            throw new TodoImplementException(maximumFuture.getClass());
        }
    }

    /**
     * @return {@link #administratorEmails}
     */
    public List<String> getAdministratorEmails()
    {
        return administratorEmails;
    }

    /**
     * @param administratorEmail to be added to the {@link #administratorEmails}
     */
    public void addAdministratorEmail(String administratorEmail)
    {
        administratorEmails.add(administratorEmail);
    }

    /**
     * @param administratorEmail to be removed from the {@link #administratorEmails}
     */
    public void removeAdministratorEmail(String administratorEmail)
    {
        administratorEmails.remove(administratorEmail);
    }

    /**
     * @return {@link #childResourceIds}
     */
    public List<String> getChildResourceIds()
    {
        return childResourceIds;
    }

    /**
     * @param childResourceId shongo-id to be added to the {@link #childResourceIds}
     */
    public void addChildResourceId(String childResourceId)
    {
        childResourceIds.add(childResourceId);
    }

    public boolean isCalendarPublic() {
        return calendarPublic;
    }

    public void setCalendarPublic(boolean calendarPublic) {
        this.calendarPublic = calendarPublic;
    }

    public String getCalendarUriKey() {
        return calendarUriKey;
    }

    public void setCalendarUriKey(String calendarUriKey) {
        this.calendarUriKey = calendarUriKey;
    }

    public String getRemoteCalendarName() {
        return remoteCalendarName;
    }

    public void setRemoteCalendarName(String remoteCalendarName) {
        this.remoteCalendarName = remoteCalendarName;
    }

    public static final String USER_ID = "userId";
    public static final String PARENT_RESOURCE_ID = "parentResourceId";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String CAPABILITIES = "capabilities";
    public static final String ALLOCATABLE = "allocatable";
    public static final String ALLOCATION_ORDER = "allocationOrder";
    public static final String MAXIMUM_FUTURE = "maximumFuture";
    public static final String ADMINISTRATOR_EMAILS = "administratorEmails";
    public static final String CHILD_RESOURCE_IDS = "childResourceIds";
    public static final String IS_CALENDAR_PUBLIC = "calendarPublic";
    public static final String CALENDAR_URI_KEY = "calendarUriKey";
    public static final String REMOTE_CALENDAR_NAME = "remoteCalendarName";
    public static final String CONFIRM_BY_OWNER = "confirmByOwner";

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
        dataMap.set(ALLOCATION_ORDER, allocationOrder);
        dataMap.set(ADMINISTRATOR_EMAILS, administratorEmails);
        dataMap.set(CHILD_RESOURCE_IDS, childResourceIds);
        dataMap.set(IS_CALENDAR_PUBLIC, calendarPublic);
        dataMap.set(CALENDAR_URI_KEY, calendarUriKey);
        dataMap.set(REMOTE_CALENDAR_NAME, remoteCalendarName);
        dataMap.set(CONFIRM_BY_OWNER, confirmByOwner);

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
        userId = dataMap.getString(USER_ID, Controller.USER_ID_COLUMN_LENGTH);
        parentResourceId = dataMap.getString(PARENT_RESOURCE_ID);
        name = dataMap.getStringRequired(NAME, DEFAULT_COLUMN_LENGTH);
        description = dataMap.getString(DESCRIPTION);
        capabilities = dataMap.getList(CAPABILITIES, Capability.class);
        allocatable = dataMap.getBool(ALLOCATABLE);
        allocationOrder = dataMap.getInteger(ALLOCATION_ORDER);
        maximumFuture = dataMap.getVariant(MAXIMUM_FUTURE, DateTime.class, Period.class);
        administratorEmails = dataMap.getStringList(ADMINISTRATOR_EMAILS, DEFAULT_COLUMN_LENGTH);
        childResourceIds = dataMap.getList(CHILD_RESOURCE_IDS, String.class);
        calendarPublic = dataMap.getBool(IS_CALENDAR_PUBLIC);
        calendarUriKey = dataMap.getString(CALENDAR_URI_KEY);
        remoteCalendarName = dataMap.getString(REMOTE_CALENDAR_NAME);
        confirmByOwner = dataMap.getBool(CONFIRM_BY_OWNER);
    }
}
