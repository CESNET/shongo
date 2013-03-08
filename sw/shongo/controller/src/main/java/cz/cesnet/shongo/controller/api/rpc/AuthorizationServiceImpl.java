package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.common.IdentifierFormat;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.apache.commons.lang.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

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

        // TODO: check that resource exits

        UserResourceRole userResourceRole = getUserResourceRole(userId, resourceId, roleId);
        if (userResourceRole != null) {
            return userResourceRole.getId();
        }

        userResourceRole = new UserResourceRole();
        userResourceRole.setUser(Authorization.getInstance().getUserInformation(userId));
        userResourceRole.setResourceId(resourceId);
        userResourceRole.setRoleId(roleId);
        return createUserResourceRole(userResourceRole);
    }

    @Override
    public void deleteUserResourceRole(SecurityToken token, String id) throws EntityNotFoundException
    {
        authorization.validate(token);

        UserResourceRole userResourceRole = userResourceRoleById.get(id);
        if (userResourceRole == null) {
            throw new EntityNotFoundException(UserResourceRole.class, id);
        }
        removeUserResourceRole(userResourceRole);
    }

    @Override
    public UserResourceRole getUserResourceRole(SecurityToken token, String id) throws EntityNotFoundException
    {
        authorization.validate(token);

        UserResourceRole userResourceRole = userResourceRoleById.get(id);
        if (userResourceRole == null) {
            throw new EntityNotFoundException(UserResourceRole.class, id);
        }
        return userResourceRole;
    }

    @Override
    public Collection<UserResourceRole> listUserResourceRoles(SecurityToken token, String userId, String resourceId,
            String roleId) throws FaultException
    {
        authorization.validate(token);

        if (resourceId != null) {
            IdentifierFormat.LocalIdentifier resourceLocalId = IdentifierFormat.parseLocalId(resourceId);
            if (!resourceLocalId.getEntityType().equals(IdentifierFormat.EntityType.RESERVATION_REQUEST)) {
                throw new TodoImplementException(resourceLocalId.getEntityType().toString());
            }
            if (roleId == null || roleId.equals(ROLE_OWNER)) {
                Long entityId = resourceLocalId.getEntityId();
                if (!initializedEntities.contains(entityId)) {
                    EntityManager entityManager = entityManagerFactory.createEntityManager();
                    try {
                        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(
                                entityManager);
                        AbstractReservationRequest reservationRequest = reservationRequestManager.get(entityId);
                        String ownerUserId = reservationRequest.getUserId();
                        UserInformation userInformation = Authorization.getInstance().getUserInformation(ownerUserId);
                        UserResourceRole userResourceRole = new UserResourceRole();
                        userResourceRole.setUser(userInformation);
                        userResourceRole.setResourceId(resourceId);
                        userResourceRole.setRoleId(ROLE_OWNER);
                        createUserResourceRole(userResourceRole);
                        initializedEntities.add(entityId);
                    }
                    finally {
                        entityManager.close();
                    }
                }
            }
        }

        Set<UserResourceRole> userResourceRoles = new HashSet<UserResourceRole>();
        for (UserResourceRole userResourceRole : userResourceRoleById.values()) {
            if (userId != null && !userId.equals(userResourceRole.getUser().getUserId())) {
                continue;
            }
            if (resourceId != null && !resourceId.equals(userResourceRole.getResourceId())) {
                continue;
            }
            if (roleId != null && !roleId.equals(userResourceRole.getRoleId())) {
                continue;
            }
            userResourceRoles.add(userResourceRole);
        }
        return userResourceRoles;
    }

    @Override
    public UserInformation getUser(SecurityToken token, String userId)
    {
        authorization.validate(token);
        return Authorization.getInstance().getUserInformation(userId);
    }

    @Override
    public Collection<UserInformation> listUsers(SecurityToken token, String filter)
    {
        authorization.validate(token);
        List<UserInformation> users = new LinkedList<UserInformation>();
        for (UserInformation userInformation : Authorization.getInstance().listUserInformation()) {
            StringBuilder filterData = null;
            if (filter != null) {
                filterData = new StringBuilder();
                filterData.append(userInformation.getFirstName());
                filterData.append(" ");
                filterData.append(userInformation.getLastName());
                for (String email : userInformation.getEmails()) {
                    filterData.append(email);
                }
                filterData.append(userInformation.getOrganization());
            }
            if (filterData == null || StringUtils.containsIgnoreCase(filterData.toString(), filter)) {
                users.add(userInformation);
            }
        }
        return users;
    }

    private long userResourceRoleId = 0;
    private Map<String, UserResourceRole> userResourceRoleById = new HashMap<String, UserResourceRole>();
    private Set<Long> initializedEntities = new HashSet<Long>();

    private UserResourceRole getUserResourceRole(String userId, String resourceId, String roleId)
    {
        for (UserResourceRole userResourceRole : userResourceRoleById.values()) {
            if (userId != null && !userId.equals(userResourceRole.getUser().getUserId())) {
                continue;
            }
            if (resourceId != null && !resourceId.equals(userResourceRole.getResourceId())) {
                continue;
            }
            if (roleId != null && !roleId.equals(userResourceRole.getRoleId())) {
                continue;
            }
            return userResourceRole;
        }
        return null;
    }

    private String createUserResourceRole(UserResourceRole userResourceRole)
    {
        String newId = String.valueOf(++userResourceRoleId);
        userResourceRole.setId(newId);
        userResourceRoleById.put(newId, userResourceRole);
        return newId;
    }

    private void removeUserResourceRole(UserResourceRole userResourceRole)
    {
        userResourceRoleById.remove(userResourceRole.getId());
    }
}
