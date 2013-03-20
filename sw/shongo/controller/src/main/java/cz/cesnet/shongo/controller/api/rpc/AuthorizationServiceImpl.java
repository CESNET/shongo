package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.AclRecord;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
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
                ControllerImplFaultSet.throwEntityNotFoundFault(entityId);
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
        authorization.checkPermission(requesterUserId, entityIdentifier, Permission.WRITE);
        cz.cesnet.shongo.controller.authorization.AclRecord userAclRecord =
                authorization.createAclRecord(userId, entityIdentifier, role);
        return (userAclRecord != null ? userAclRecord.getId() : null);
    }

    @Override
    public void deleteAclRecord(SecurityToken token, String aclRecordId)
            throws FaultException
    {
        String userId = authorization.validate(token);
        cz.cesnet.shongo.controller.authorization.AclRecord aclRecord = authorization.getAclRecord(aclRecordId);
        authorization.checkPermission(userId, aclRecord.getEntityId(), Permission.WRITE);
        authorization.deleteAclRecord(aclRecord);
    }

    @Override
    public AclRecord getAclRecord(SecurityToken token, String aclRecordId)
            throws FaultException
    {
        String userId = authorization.validate(token);
        cz.cesnet.shongo.controller.authorization.AclRecord aclRecord = authorization.getAclRecord(aclRecordId);
        authorization.checkPermission(userId, aclRecord.getEntityId(), Permission.READ);
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
                authorization.checkPermission(requesterUserId, entityIdentifier, Permission.READ);
            }
            else {
                if (!requesterUserId.equals(Authorization.ROOT_USER_ID)) {
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
}
