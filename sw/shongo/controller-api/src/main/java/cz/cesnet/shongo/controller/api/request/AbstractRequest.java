package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.SecurityToken;

/**
 * Abstract API request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractRequest extends AbstractComplexType
{
    /**
     * When this value is set to an attribute filter it requests that the target entity should have the attribute empty.
     */
    public static final String FILTER_EMPTY = "";

    /**
     * When this value is set to an attribute filter it requests that the target entity should have the attribute set.
     */
    public static final String FILTER_NOT_EMPTY = "*";

    /**
     * When this value is set to an attribute filter it requests that the target value should be greater than zero.
     */
    public static final String FILTER_NON_ZERO = "+";

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

    private static final String SECURITY_TOKEN = "securityToken";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(SECURITY_TOKEN, securityToken);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        securityToken = dataMap.getAtomicType(SECURITY_TOKEN, SecurityToken.class);
    }
}
