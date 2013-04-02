package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.AclRecord;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.authorization.AclRecordCreateRequest;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestSet;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.apache.commons.lang.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of {@link AuthorizationService}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AuthorizationServiceImpl extends Component
        implements AuthorizationService, Component.EntityManagerFactoryAware, Component.AuthorizationAware
{
    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.authorization.Authorization
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

    /**
     * @param entityId of entity which should be checked for existence
     * @throws FaultException
     */
    private void checkEntityExistence(EntityIdentifier entityId) throws FaultException
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            PersistentObject entity = entityManager.find(entityId.getEntityClass(), entityId.getPersistenceId());
            if (entity == null) {
                ControllerFaultSet.throwEntityNotFoundFault(entityId);
            }
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public String createAclRecord(SecurityToken token, String userId, String entityId, Role role)
            throws FaultException
    {
        String requesterUserId = authorization.validate(token);
        EntityIdentifier entityIdentifier = EntityIdentifier.parse(entityId);
        checkEntityExistence(entityIdentifier);
        if (!authorization.hasPermission(requesterUserId, entityIdentifier, Permission.WRITE)) {
            ControllerFaultSet.throwSecurityNotAuthorizedFault("create ACL for %s", entityId);
        }
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(authorization, entityManager);
        try {
            entityManager.getTransaction().begin();
            AclRecordCreateRequest aclRecordCreateRequest =
                    authorizationManager.createAclRecord(userId, entityIdentifier, role);
            entityManager.getTransaction().commit();
            if (aclRecordCreateRequest == null) {
                return null;
            }
            cz.cesnet.shongo.controller.authorization.AclRecord aclRecord =
                    authorizationManager.executeAclRecordCreateRequest(aclRecordCreateRequest);
            return (aclRecord != null ? aclRecord.getId() : null);
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public void deleteAclRecord(SecurityToken token, String aclRecordId)
            throws FaultException
    {
        String userId = authorization.validate(token);
        cz.cesnet.shongo.controller.authorization.AclRecord aclRecord = authorization.getAclRecord(aclRecordId);
        if (!authorization.hasPermission(userId, aclRecord.getEntityId(), Permission.WRITE)) {
            ControllerFaultSet.throwSecurityNotAuthorizedFault("delete ACL for %s", aclRecord.getEntityId());
        }
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(authorization, entityManager);
        try {
            entityManager.getTransaction().begin();
            authorizationManager.deleteAclRecord(aclRecord);
            entityManager.getTransaction().commit();
            authorizationManager.executeAclRecordRequests();
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public AclRecord getAclRecord(SecurityToken token, String aclRecordId)
            throws FaultException
    {
        String userId = authorization.validate(token);
        cz.cesnet.shongo.controller.authorization.AclRecord aclRecord = authorization.getAclRecord(aclRecordId);
        if (!authorization.hasPermission(userId, aclRecord.getEntityId(), Permission.READ)) {
            ControllerFaultSet.throwSecurityNotAuthorizedFault("read ACL for %s", aclRecord.getEntityId());
        }
        return aclRecord.toApi();
    }

    @Override
    public Collection<AclRecord> listAclRecords(SecurityToken token, String userId, String entityId, Role role)
            throws FaultException
    {
        String requesterUserId = authorization.validate(token);
        EntityIdentifier entityIdentifier = EntityIdentifier.parse(entityId);

        if (!requesterUserId.equals(userId)) {
            if (entityIdentifier != null) {
                if (!authorization.hasPermission(requesterUserId, entityIdentifier, Permission.READ)) {
                    ControllerFaultSet.throwSecurityNotAuthorizedFault("list ACL for %s", entityId);
                }
            }
            else {
                if (!authorization.isAdmin(requesterUserId)) {
                    throw new TodoImplementException("List only ACL to which the requester has permission.");
                }
            }
        }

        if (entityIdentifier != null && !entityIdentifier.isGroup()) {
            checkEntityExistence(entityIdentifier);
        }

        List<AclRecord> aclRecordApiList = new LinkedList<AclRecord>();
        for (cz.cesnet.shongo.controller.authorization.AclRecord aclRecord :
                authorization.getAclRecords(userId, entityIdentifier, role)) {
            aclRecordApiList.add(aclRecord.toApi());
        }
        return aclRecordApiList;
    }

    @Override
    public Collection<Permission> listPermissions(SecurityToken token, String entityId) throws FaultException
    {
        String userId = authorization.validate(token);

        EntityIdentifier entityIdentifier = EntityIdentifier.parse(entityId);
        checkEntityExistence(entityIdentifier);

        return authorization.getPermissions(userId, entityIdentifier);
    }

    @Override
    public UserInformation getUser(SecurityToken token, String userId)
            throws FaultException
    {
        authorization.validate(token);
        return Authorization.getInstance().getUserInformation(userId);
    }

    @Override
    public Collection<UserInformation> listUsers(SecurityToken token, String filter)
            throws FaultException
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

    @Override
    public void setEntityUser(SecurityToken token, String entityId, String newUserId) throws FaultException
    {
        String userId = authorization.validate(token);
        EntityIdentifier entityIdentifier = EntityIdentifier.parse(entityId);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(authorization, entityManager);
        try {
            PersistentObject entity = entityManager.find(entityIdentifier.getEntityClass(),
                    entityIdentifier.getPersistenceId());
            if (entity == null) {
                ControllerFaultSet.throwEntityNotFoundFault(entityIdentifier);
            }
            if (!authorization.isAdmin(userId)) {
                ControllerFaultSet.throwSecurityNotAuthorizedFault("change user for %s", entityId);
            }
            entityManager.getTransaction().begin();
            if (entity instanceof Resource) {
                Resource resource = (Resource) entity;
                for (cz.cesnet.shongo.controller.authorization.AclRecord aclRecord : authorization.getAclRecords(
                        resource.getUserId(), entityIdentifier, Role.OWNER)) {
                    authorizationManager.deleteAclRecord(aclRecord);
                }
                resource.setUserId(newUserId);
                authorizationManager.createAclRecord(newUserId, entityIdentifier, Role.OWNER);
            }
            else if (entity instanceof ReservationRequestSet) {
                ReservationRequestSet reservationRequestSet = (ReservationRequestSet) entity;
                // Change user to reservation request set
                for (cz.cesnet.shongo.controller.authorization.AclRecord aclRecord : authorization.getAclRecords(
                        reservationRequestSet.getUserId(), entityIdentifier, Role.OWNER)) {
                    authorizationManager.deleteAclRecord(aclRecord);
                }
                reservationRequestSet.setUserId(newUserId);
                authorizationManager.createAclRecord(newUserId, entityIdentifier, Role.OWNER);
                // Change user to child reservation requests
                for (ReservationRequest reservationRequest : reservationRequestSet.getReservationRequests()) {
                    EntityIdentifier reservationRequestId = new EntityIdentifier(reservationRequest);
                    for (cz.cesnet.shongo.controller.authorization.AclRecord aclRecord : authorization.getAclRecords(
                            reservationRequest.getUserId(), reservationRequestId, Role.OWNER)) {
                        authorizationManager.deleteAclRecord(aclRecord);
                    }
                    reservationRequest.setUserId(newUserId);
                    authorizationManager.createAclRecord(newUserId, reservationRequestId, Role.OWNER);
                }
            }
            else if (entity instanceof ReservationRequest) {
                ReservationRequest reservationRequest = (ReservationRequest) entity;
                for (cz.cesnet.shongo.controller.authorization.AclRecord aclRecord : authorization.getAclRecords(
                        reservationRequest.getUserId(), entityIdentifier, Role.OWNER)) {
                    authorizationManager.deleteAclRecord(aclRecord);
                }
                reservationRequest.setUserId(newUserId);
                authorizationManager.createAclRecord(newUserId, entityIdentifier, Role.OWNER);
            }
            else {
                throw new FaultException("The user cannot be set for entity of type "
                        + entity.getClass().getSimpleName() + ".");
            }
            entityManager.getTransaction().commit();
            authorizationManager.executeAclRecordRequests();
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }
}
