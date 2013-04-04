package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.authorization.AclRecord;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Testing {@link Authorization},
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DummyAuthorization extends Authorization
{
    private static Logger logger = LoggerFactory.getLogger(DummyAuthorization.class);

    /**
     * Testing user #1 information.
     */
    protected static final UserInformation USER1_INFORMATION;

    /**
     * Testing user #2 information.
     */
    protected static final UserInformation USER2_INFORMATION;

    /**
     * Testing user #3 information.
     */
    protected static final UserInformation USER3_INFORMATION;

    /**
     * Known users.
     */
    private static final Map<String, UserInformation> userInformationByAccessToken;

    /**
     * Known users.
     */
    private static final Map<String, UserInformation> userInformationById;

    /**
     * Static initialization.
     */
    static {
        USER1_INFORMATION = new UserInformation();
        USER1_INFORMATION.setUserId("1");
        USER1_INFORMATION.setFirstName("test1");

        USER2_INFORMATION = new UserInformation();
        USER2_INFORMATION.setUserId("2");
        USER2_INFORMATION.setFirstName("test2");

        USER3_INFORMATION = new UserInformation();
        USER3_INFORMATION.setUserId("3");
        USER3_INFORMATION.setFirstName("test3");

        userInformationByAccessToken = new HashMap<String, UserInformation>();
        userInformationByAccessToken.put(
                AbstractControllerTest.SECURITY_TOKEN_ROOT.getAccessToken(), Authorization.ROOT_USER_INFORMATION);
        userInformationByAccessToken.put(
                AbstractControllerTest.SECURITY_TOKEN_USER1.getAccessToken(), USER1_INFORMATION);
        userInformationByAccessToken.put(
                AbstractControllerTest.SECURITY_TOKEN_USER2.getAccessToken(), USER2_INFORMATION);
        userInformationByAccessToken.put(
                AbstractControllerTest.SECURITY_TOKEN_USER3.getAccessToken(), USER3_INFORMATION);

        userInformationById = new HashMap<String, UserInformation>();
        for (UserInformation userInformation : userInformationByAccessToken.values()) {
            userInformationById.put(userInformation.getUserId(), userInformation);
        }
    }

    public DummyAuthorization()
    {
        super(new Configuration());
    }

    public DummyAuthorization(Configuration config)
    {
        super(config);
    }

    @Override
    protected UserInformation onGetUserInformationByAccessToken(String accessToken)
    {
        UserInformation userInformation = userInformationByAccessToken.get(accessToken);
        if (userInformation != null) {
            return userInformation;
        }
        throw new TodoImplementException();
    }

    @Override
    protected UserInformation onGetUserInformationByUserId(String userId)
    {
        UserInformation userInformation = userInformationById.get(userId);
        if (userInformation != null) {
            return userInformation;
        }
        throw new TodoImplementException();
    }

    @Override
    protected Collection<UserInformation> onListUserInformation() throws FaultException
    {
        throw new TodoImplementException();
    }

    @Override
    protected AclRecord onCreateAclRecord(String userId, EntityIdentifier entityId, Role role) throws FaultException
    {
        throw new TodoImplementException();
        /*AclRecord aclRecord = new AclRecord(userId, entityId, role);

        logger.info("Created ACL (id: {}, user: {}, entity: {}, role: {})",
                new Object[]{aclRecord.getId(), userId, entityId, role});


        return aclRecord;*/
    }

    @Override
    protected void onDeleteAclRecord(AclRecord aclRecord) throws FaultException
    {
        throw new TodoImplementException();

        /*logger.info("Deleted ACL (id: {}, user: {}, entity: {}, role: {})",
                new Object[]{aclRecord.getId(), aclRecord.getUserId(), aclRecord.getEntityId(), aclRecord.getRole()});*/
    }

    @Override
    protected AclRecord onGetAclRecord(Long aclRecordId) throws FaultException
    {
        throw new TodoImplementException();
    }

    @Override
    protected Collection<AclRecord> onListAclRecords(String userId, EntityIdentifier entityId, Role role)
            throws FaultException
    {
        throw new TodoImplementException();
        /*logger.info("List ACL (user: {}, entity: {}, role: {})", new Object[]{userId, entityId, role});

        List<AclRecord> aclRecords = new LinkedList<AclRecord>();
        for (AclRecord aclRecord : cache.getAclRecords()) {
            if (userId != null && !userId.equals(aclRecord.getUserId())) {
                continue;
            }
            if (entityId != null && !entityId.equals(aclRecord.getEntityId())) {
                continue;
            }
            if (role != null && !role.equals(aclRecord.getRole())) {
                continue;
            }
            aclRecords.add(aclRecord);
        }
        return aclRecords;*/
    }

    /**
     * @param configuration to be used for initialization
     * @return new instance of {@link DummyAuthorization}
     * @throws IllegalStateException when other {@link Authorization} already exists
     */
    public static DummyAuthorization createInstance(Configuration configuration) throws IllegalStateException
    {
        DummyAuthorization authorization = new DummyAuthorization(configuration);
        Authorization.setInstance(authorization);
        return authorization;
    }
}
