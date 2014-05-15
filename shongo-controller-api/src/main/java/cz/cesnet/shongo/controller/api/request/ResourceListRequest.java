package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.Group;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.SecurityToken;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListRequest} for {@link Resource}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceListRequest extends ListRequest
{
    /**
     * User-ids of resource owners.
     */
    private Set<String> userIds = new HashSet<String>();

    /**
     * Name of the resource.
     */
    private String name;

    /**
     * Constructor.
     */
    public ResourceListRequest()
    {
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public ResourceListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    /**
     * @return {@link #userIds}
     */
    public Set<String> getUserIds()
    {
        return userIds;
    }

    /**
     * @param userId to be added to the {@link #userIds}
     */
    public void addUserId(String userId)
    {
        userIds.add(userId);
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

    private static final String USER_IDS = "userIds";
    private static final String NAME = "name";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_IDS, userIds);
        dataMap.set(NAME, name);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userIds = dataMap.getSet(USER_IDS, String.class);
        name = dataMap.getString(NAME);
    }
}
