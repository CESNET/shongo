package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.authorization.AclRecord;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.TodoImplementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManagerFactory;
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
        USER1_INFORMATION.addEmail("test1@cesnet.cz");

        USER2_INFORMATION = new UserInformation();
        USER2_INFORMATION.setUserId("2");
        USER2_INFORMATION.setFirstName("test2");
        USER2_INFORMATION.addEmail("test2@cesnet.cz");

        USER3_INFORMATION = new UserInformation();
        USER3_INFORMATION.setUserId("3");
        USER3_INFORMATION.setFirstName("test3");
        USER3_INFORMATION.addEmail("test3@cesnet.cz");

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

    /**
     * Constructor.
     *
     * @param entityManagerFactory sets the {@link #entityManagerFactory}
     */
    public DummyAuthorization(EntityManagerFactory entityManagerFactory)
    {
        this(new Configuration());

        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Constructor.
     *
     * @param configuration to be used
     */
    public DummyAuthorization(Configuration configuration)
    {
        super(configuration);

        this.adminAccessTokens.add(AbstractControllerTest.SECURITY_TOKEN_ROOT.getAccessToken());
    }

    /**
     * @param userId to be added to the {@link #adminModeEnabledUserIds}
     */
    public void addAdminModeEnabledUserId(String userId)
    {
        this.adminModeEnabledUserIds.add(userId);
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
        throw new TodoImplementException(userId);
    }

    @Override
    protected Collection<UserInformation> onListUserInformation()
    {
        throw new TodoImplementException();
    }

    @Override
    protected void onPropagateAclRecordCreation(AclRecord aclRecord)
    {
        logger.info("Propagate ACL creation (id: {}, user: {}, entity: {}, role: {})",
                new Object[]{aclRecord.getId(), aclRecord.getUserId(), aclRecord.getEntityId(), aclRecord.getRole()});
    }

    @Override
    protected void onPropagateAclRecordDeletion(AclRecord aclRecord)
    {
        logger.info("Propagate ACL deletion (id: {}, user: {}, entity: {}, role: {})",
                new Object[]{aclRecord.getId(), aclRecord.getUserId(), aclRecord.getEntityId(), aclRecord.getRole()});
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
