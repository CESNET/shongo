package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.common.IdentifierFormat;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AuthorizationServiceImpl extends Component
        implements AuthorizationService, Component.EntityManagerFactoryAware, Component.AuthorizationAware
{
    private static final String ROLE_OWNER = "owner";
    private static final String ROLE_REUSER = "reuser";

    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see Authorization
     */
    private Authorization authorization;

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        checkDependency(authorization, Authorization.class);
        super.init(configuration);
    }

    @Override
    public String getServiceName()
    {
        return "Authorization";
    }

    @Override
    public String createUserResourceRole(SecurityToken token, String userId, String resourceId, String roleId)
    {
        authorization.validate(token);

        throw new RuntimeException("TODO: Implement AuthorizationServiceImpl.createUserResourceRole");
    }

    @Override
    public void deleteUserResourceRole(SecurityToken token, String id)
    {
        authorization.validate(token);

        throw new RuntimeException("TODO: Implement AuthorizationServiceImpl.deleteUserResourceRole");
    }

    @Override
    public UserResourceRole getUserResourceRole(SecurityToken token, String id)
    {
        authorization.validate(token);

        throw new RuntimeException("TODO: Implement AuthorizationServiceImpl.getUserResourceRole");
    }

    @Override
    public Collection<UserResourceRole> listUserResourceRoles(SecurityToken token, String userId, String resourceId,
            String roleId) throws FaultException
    {
        authorization.validate(token);

        List<UserResourceRole> userResourceRoles = new LinkedList<UserResourceRole>();
        if (resourceId != null) {
            IdentifierFormat.LocalIdentifier resourceLocalId = IdentifierFormat.parseLocalId(resourceId);
            if (!resourceLocalId.getEntityType().equals(IdentifierFormat.EntityType.RESERVATION_REQUEST)) {
                throw new TodoImplementException(resourceLocalId.getEntityType().toString());
            }
            if (roleId.equals(ROLE_OWNER)) {
                Long entityId = resourceLocalId.getEntityId();
                EntityManager entityManager = entityManagerFactory.createEntityManager();
                try {
                    ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
                    AbstractReservationRequest reservationRequest = reservationRequestManager.get(entityId);
                    String ownerUserId = reservationRequest.getUserId();
                    if (userId == null || userId.equals(ownerUserId)) {
                        UserResourceRole userResourceRole = new UserResourceRole();
                        userResourceRole.setUserId(ownerUserId);
                        userResourceRole.setResourceId(resourceId);
                        userResourceRole.setRoleId(roleId);
                        userResourceRoles.add(userResourceRole);
                    }
                }
                finally {
                    entityManager.close();
                }
            }
        }
        return userResourceRoles;
    }

    @Override
    public Collection<UserInformation> listUsers(SecurityToken token, String name)
    {
        authorization.validate(token);
        return Authorization.getInstance().listUserInformation();
    }
}
