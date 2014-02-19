package cz.cesnet.shongo.client.web.auth;

import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.client.web.models.RoomModel;
import cz.cesnet.shongo.controller.ControllerConnectException;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.SystemPermission;
import cz.cesnet.shongo.controller.api.AbstractReservationRequest;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.SecurityToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static Logger logger = LoggerFactory.getLogger(PermissionEvaluator.class);

    @Resource
    private Cache cache;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permissionValue)
    {
        try {
            SecurityToken securityToken = ((OpenIDConnectAuthenticationToken) authentication).getSecurityToken();
            if (targetDomainObject == null) {
                SystemPermission systemPermission;
                if (permissionValue instanceof SystemPermission) {
                    systemPermission = (SystemPermission) permissionValue;
                }
                else {
                    systemPermission = SystemPermission.valueOf(permissionValue.toString());
                }
                return cache.hasSystemPermission(securityToken, systemPermission);
            }
            else {
                String objectId;
                if (targetDomainObject instanceof String) {
                    objectId = (String) targetDomainObject;
                }
                else if (targetDomainObject instanceof RoomModel) {
                    objectId = ((RoomModel) targetDomainObject).getId();
                }
                else if (targetDomainObject instanceof Executable) {
                    objectId = ((Executable) targetDomainObject).getId();
                }
                else if (targetDomainObject instanceof AbstractReservationRequest) {
                    objectId = ((AbstractReservationRequest) targetDomainObject).getId();
                }
                else if (targetDomainObject instanceof ReservationRequestModel) {
                    objectId = ((ReservationRequestModel) targetDomainObject).getId();
                }
                else {
                    throw new IllegalArgumentException("Illegal target " + targetDomainObject + ".");
                }
                ObjectPermission objectPermission;
                if (permissionValue instanceof ObjectPermission) {
                    objectPermission = (ObjectPermission) permissionValue;
                }
                else {
                    objectPermission = ObjectPermission.valueOf(permissionValue.toString());
                }
                return cache.getObjectPermissions(securityToken, objectId).contains(objectPermission);
            }
        }
        catch (ControllerConnectException exception) {
            logger.error("Failed to check permission", exception);
            return false;
        }
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
            Object permission)
    {
        throw new UnsupportedOperationException();
    }
}
