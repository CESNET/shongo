package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.rpc.StructType;
import cz.cesnet.shongo.controller.api.SecurityToken;

/**
 * Abstract API request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractRequest implements StructType
{
    /**
     * {@link SecurityToken} of user who is requesting this request.
     */
    private SecurityToken securityToken;

    /**
     * Constructor.
     */
    public AbstractRequest()
    {
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public AbstractRequest(SecurityToken securityToken)
    {
        this.securityToken = securityToken;
    }

    /**
     * @return {@link #securityToken}
     */
    public SecurityToken getSecurityToken()
    {
        return securityToken;
    }

    /**
     * @param securityToken sets the {@link #securityToken}
     */
    public void setSecurityToken(SecurityToken securityToken)
    {
        this.securityToken = securityToken;
    }

    /**
     * @param accessToken sets the {@link #securityToken}
     */
    public void setSecurityToken(String accessToken)
    {
        this.securityToken = new SecurityToken(accessToken);
    }
}
