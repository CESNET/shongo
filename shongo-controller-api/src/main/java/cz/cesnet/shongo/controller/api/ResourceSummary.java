package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an entity that can be scheduled.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceSummary extends IdentifiedComplexType
{
    /**
     * User-id of the owner user.
     */
    private String userId;

    /**
     * Type of the resource.
     */
    private Type type;

    /**
     * Name of the resource.
     */
    private String name;

    /**
     * Technologies of the resource.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Tags of the resource.
     */
    private Set<Tag> tags = new HashSet<>();

    /**
     * Parent resource shongo-id.
     */
    private String parentResourceId;

    /**
     * Specifies whether resource can be scheduled by a scheduler.
     */
    private Boolean allocatable;

    /**
     * Order in which the resource should be tried to be allocated ({@code null} means the last).
     */
    private Integer allocationOrder;

    /**
     * Description of the resource.
     */
    private String description;

    /**
     * Are reservations of this resource public.
     */
    private boolean calendarPublic;

    /**
     * Hash key used for public calendar URL
     */
    private String calendarUriKey;

    private String remoteCalendarName;

    /**
     * Name of the resource {@link Domain}, should be null when resource is local.
     */
    private String domainName;

    /**
     * If all reservation request must be first confirmed by owner.
     */
    private boolean confirmByOowner;

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
     * @return {@link #type}
     */
    public Type getType() {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @return {@link #technologies}
     */
    public Set<Technology> getTechnologies()
    {
        return technologies;
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies.clear();
        if (technologies != null) {
            this.technologies.addAll(technologies);
        }
    }

    /**
     * @param technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        this.technologies.add(technology);
    }

    /**
     * @return {@link #tags}
     */
    public Set<Tag> getTags() {
        return tags;
    }

    /**
     * @param tag to be added to the {@link #tags}
     */
    public void addTag(Tag tag) {
        tags.add(tag);
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getDomainName()
    {
        return domainName;
    }

    public void setDomainName(String domainName)
    {
        this.domainName = domainName;
    }

    public boolean isConfirmByOowner()
    {
        return confirmByOowner;
    }

    public void setConfirmByOowner(boolean confirmByOowner)
    {
        this.confirmByOowner = confirmByOowner;
    }

    public String getRemoteCalendarName() {
        return remoteCalendarName;
    }

    public void setRemoteCalendarName(String remoteCalendarName) {
        this.remoteCalendarName = remoteCalendarName;
    }

    /**
     * @param allocationOrder sets the {@link #allocationOrder}
     */
    public void setAllocationOrder(Integer allocationOrder)
    {
        this.allocationOrder = allocationOrder;
    }

    public enum Type {
        ROOM_PROVIDER,
        RECORDING_SERVICE,
        RESOURCE,
    }

    private static final String USER_ID = "userId";
    private static final String NAME = "name";
    private static final String TECHNOLOGIES = "technologies";
    private static final String PARENT_RESOURCE_ID = "parentResourceId";
    private static final String ALLOCATABLE = "allocatable";
    private static final String ALLOCATION_ORDER = "allocationOrder";
    private static final String DESCRIPTION = "description";
    private static final String CALENDAR_PUBLIC = "calendarPublic";
    private static final String CALENDAR_URI_KEY = "calendarUriKey";
    private static final String DOMAIN_NAME = "domainName";
    private static final String CONFIRM_BY_OWNER = "confirmByOwner";
    public static final String REMOTE_CALENDAR_NAME = "remoteCalendarName";
    public static final String TYPE = "type";
    public static final String TAGS = "tags";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_ID, userId);
        dataMap.set(NAME, name);
        dataMap.set(ALLOCATABLE, allocatable);
        dataMap.set(ALLOCATION_ORDER, allocationOrder);
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(PARENT_RESOURCE_ID, parentResourceId);
        dataMap.set(DESCRIPTION, description);
        dataMap.set(CALENDAR_PUBLIC, calendarPublic);
        dataMap.set(CALENDAR_URI_KEY, calendarUriKey);
        dataMap.set(DOMAIN_NAME, domainName);
        dataMap.set(CONFIRM_BY_OWNER, confirmByOowner);
        dataMap.set(REMOTE_CALENDAR_NAME, remoteCalendarName);
        dataMap.set(TYPE, type);
        dataMap.set(TAGS, tags);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userId = dataMap.getString(USER_ID);
        name = dataMap.getString(NAME);
        allocatable = dataMap.getBool(ALLOCATABLE);
        allocationOrder = dataMap.getInteger(ALLOCATION_ORDER);
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
        parentResourceId = dataMap.getString(PARENT_RESOURCE_ID);
        description = dataMap.getString(DESCRIPTION);
        calendarPublic = dataMap.getBool(CALENDAR_PUBLIC);
        calendarUriKey = dataMap.getString(CALENDAR_URI_KEY);
        domainName = dataMap.getString(DOMAIN_NAME);
        confirmByOowner = dataMap.getBool(CONFIRM_BY_OWNER);
        remoteCalendarName = dataMap.getString(REMOTE_CALENDAR_NAME);
        type = dataMap.getEnum(TYPE, Type.class);
        tags = dataMap.getSet(TAGS, Tag.class);
    }
}
