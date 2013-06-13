package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.rpc.StructType;
import cz.cesnet.shongo.controller.api.SecurityToken;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractRequest implements StructType
{
    /**
     * {@link SecurityToken} of user who is requesting this request.
     */
    private SecurityToken securityToken;

    public SecurityToken getSecurityToken()
    {
        return securityToken;
    }

    public void setSecurityToken(SecurityToken securityToken)
    {
        this.securityToken = securityToken;
    }

    public void setSecurityToken(String accessToken)
    {
        this.securityToken = new SecurityToken(accessToken);
    }
}
