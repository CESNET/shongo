package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
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

    public Set<String> getGroupIds()
    {
        return groupIds;
    }

    public void setGroupIds(Set<String> groupIds)
    {
        this.groupIds.clear();
        this.groupIds.addAll(groupIds);
    }

    public void addGroupId(String groupId)
    {
        groupIds.add(groupId);
    }

    public String getSearch()
    {
        return search;
    }

    public void setSearch(String search)
    {
        this.search = search;
    }

    private static final String GROUP_IDS = "groupIds";
    private static final String SEARCH = "search";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(GROUP_IDS, groupIds);
        dataMap.set(SEARCH, search);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        groupIds = dataMap.getSet(GROUP_IDS, String.class);
        search = dataMap.getString(SEARCH);
    }
}
