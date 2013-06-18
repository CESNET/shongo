package cz.cesnet.shongo.client.web.auth;

import cz.cesnet.shongo.api.util.IdentifiedObject;
import cz.cesnet.shongo.client.web.UserCache;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.controller.Permission;
import cz.cesnet.shongo.controller.api.SecurityToken;
import org.springframework.security.core.Authentication;

import javax.annotation.Resource;
import java.io.Serializable;

/**
 * Evaluator of user permissions.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PermissionEvaluator implements org.springframework.security.access.PermissionEvaluator
{
    @Resource
    private UserCache userCache;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission)
    {
        SecurityToken securityToken = ((OpenIDConnectAuthenticationToken) authentication).getSecurityToken();
        String entityId;
        if (targetDomainObject instanceof ReservationRequestModel) {
            entityId = ((ReservationRequestModel) targetDomainObject).getId();
        }
        else {
            throw new IllegalArgumentException("Illegal target " + targetDomainObject + ".");
        }
        return userCache.getPermissions(securityToken, entityId).contains((Permission) permission);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
            Object permission)
    {
        throw new UnsupportedOperationException();
    }
}
