package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.Group;
import cz.cesnet.shongo.controller.api.SecurityToken;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListRequest} for groups.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class GroupListRequest extends ListRequest
{
    /**
     * Group-ids of groups which should be returned.
     */
    private Set<String> groupIds = new HashSet<String>();

    /**
     * Group types of groups which should be returned.
     */
    private Set<Group.Type> groupTypes = new HashSet<Group.Type>();

    /**
     * String for filtering users by name, email, etc.
     */
    private String search;

    /**
     * Constructor.
     */
    public GroupListRequest()
    {
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public GroupListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param groupId        to be added to the {@link #groupIds}
     */
    public GroupListRequest(SecurityToken securityToken, String groupId)
    {
        super(securityToken);
        groupIds.add(groupId);
    }

    /**
     * @return {@link #groupIds}
     */
    public Set<String> getGroupIds()
    {
        return groupIds;
    }

    /**
     * @param groupId to be added to the {@link #groupIds}
     */
    public void addGroupId(String groupId)
    {
        groupIds.add(groupId);
    }

    public Set<Group.Type> getGroupTypes()
    {
        return groupTypes;
    }

    /**
     * @param groupType to be added to the {@link #groupTypes}
     */
    public void addGroupType(Group.Type groupType)
    {
        groupTypes.add(groupType);
    }

    /**
     * @return {@link #search}
     */
    public String getSearch()
    {
        return search;
    }

    /**
     * @param search sets the {@link #search}
     */
    public void setSearch(String search)
    {
        this.search = search;
    }

    private static final String GROUP_IDS = "groupIds";
    private static final String GROUP_TYPES = "groupTypes";
    private static final String SEARCH = "search";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(GROUP_IDS, groupIds);
        dataMap.set(GROUP_TYPES, groupTypes);
        dataMap.set(SEARCH, search);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        groupIds = dataMap.getSet(GROUP_IDS, String.class);
        groupTypes = dataMap.getSet(GROUP_TYPES, Group.Type.class);
        search = dataMap.getString(SEARCH);
    }
}
