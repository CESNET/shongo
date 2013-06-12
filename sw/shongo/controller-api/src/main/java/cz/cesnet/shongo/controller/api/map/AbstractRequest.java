package cz.cesnet.shongo.controller.api.map;

import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.map.AbstractObject;
import cz.cesnet.shongo.map.DataMap;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractRequest extends AbstractObject
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

    @Override
    public DataMap toData()
    {
        DataMap data = super.toData();
        if (securityToken != null) {
            data.set("securityToken", securityToken.toString());
        }
        return data;
    }

    @Override
    public void fromData(DataMap data)
    {
        super.fromData(data);
        String accessToken = data.getString("securityToken");
        if (accessToken != null) {
            securityToken = new SecurityToken(accessToken);
        }
    }
}
