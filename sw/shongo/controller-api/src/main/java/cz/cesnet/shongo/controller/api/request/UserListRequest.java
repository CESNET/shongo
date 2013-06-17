package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.controller.api.SecurityToken;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListRequest} for reservations.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserListRequest extends ListRequest
{
    /**
     * Identifiers of the Shongo user.
     */
    private Set<String> userIds = new HashSet<String>();

    /**
     * String for filtering users by name, email, etc.
     */
    private String filter;

    /**
     * Constructor.
     */
    public UserListRequest()
    {
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public UserListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param userId        to be added to the {@link #userIds}
     */
    public UserListRequest(SecurityToken securityToken, String userId)
    {
        super(securityToken);
        userIds.add(userId);
    }

    public Set<String> getUserIds()
    {
        return userIds;
    }

    public void setUserIds(Set<String> userIds)
    {
        this.userIds = userIds;
    }

    public void addUserId(String userId)
    {
        userIds.add(userId);
    }

    public String getFilter()
    {
        return filter;
    }

    public void setFilter(String filter)
    {
        this.filter = filter;
    }
}
