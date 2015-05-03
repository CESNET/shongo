package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.SecurityToken;

/**
 *
 * @author Ondřej Pavelka <pavelka@cesnet.cz>
 */
public class DomainListRequest extends AbstractRequest
{
    String domainId;

    public DomainListRequest(String domainId) {
        this.domainId = domainId;
    }

    public DomainListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }


    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String resourceId) {
        this.domainId = resourceId;
    }
}
