package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.AclRecord;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.fault.PersistentEntityNotFoundException;
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

    /**
     * @param entityId of entity which should be checked for existence
     * @throws PersistentEntityNotFoundException
     *
     */
    private void checkEntityExistence(EntityIdentifier entityId) throws FaultException
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            PersistentObject entity = entityManager.find(entityId.getEntityClass(), entityId.getPersistenceId());
            if (entity == null) {
                throw new PersistentEntityNotFoundException(entityId.getEntityClass(), entityId.getPersistenceId());
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
        authorization.validate(token);

        EntityIdentifier entityIdentifier = EntityIdentifier.parse(entityId);
        checkEntityExistence(entityIdentifier);

        cz.cesnet.shongo.controller.authorization.AclRecord userAclRecord =
                authorization.createAclRecord(userId, entityIdentifier, role);
        return userAclRecord.getId();
    }

    @Override
    public void deleteAclRecord(SecurityToken token, String aclRecordId)
            throws FaultException
    {
        authorization.validate(token);
        authorization.deleteAclRecord(aclRecordId);
    }

    @Override
    public AclRecord getAclRecord(SecurityToken token, String aclRecordId)
            throws FaultException
    {
        authorization.validate(token);
        cz.cesnet.shongo.controller.authorization.AclRecord aclRecord = authorization.getAclRecord(aclRecordId);
        return aclRecord.toApi(authorization);
    }

    @Override
    public Collection<AclRecord> listAclRecords(SecurityToken token, String userId, String entityId, Role role)
            throws FaultException
    {
        authorization.validate(token);

        Collection<cz.cesnet.shongo.controller.authorization.AclRecord> aclRecords = null;
        if (role == null) {
            if (entityId != null) {
                EntityIdentifier entityIdentifier = EntityIdentifier.parse(entityId);
                checkEntityExistence(entityIdentifier);
                if (userId != null) {
                    aclRecords = authorization.getAclRecords(userId, entityIdentifier);
                }
                else {
                    aclRecords = authorization.getAclRecords(entityIdentifier);
                }
            }
        }

        if (aclRecords == null) {
            EntityIdentifier entityIdentifier = null;
            if (entityId != null) {
                entityIdentifier = EntityIdentifier.parse(entityId);
            }
            aclRecords = authorization.getAclRecords(userId, entityIdentifier, role);
        }

        List<AclRecord> aclRecordApiList = new LinkedList<AclRecord>();
        for (cz.cesnet.shongo.controller.authorization.AclRecord aclRecord : aclRecords) {
            aclRecordApiList.add(aclRecord.toApi(authorization));
        }
        return aclRecordApiList;
    }

    @Override
    public Collection<Permission> listPermissions(SecurityToken token, String entityId) throws FaultException
    {
        UserInformation userInformation = authorization.validate(token);

        EntityIdentifier entityIdentifier = EntityIdentifier.parse(entityId);
        checkEntityExistence(entityIdentifier);

        return authorization.getPermissions(userInformation.getUserId(), entityIdentifier);
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
