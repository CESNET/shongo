package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.Group;
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
    protected static final UserData USER1_DATA;

    /**
     * Testing user #2 information.
     */
    protected static final UserData USER2_DATA;

    /**
     * Testing user #3 information.
     */
    protected static final UserData USER3_DATA;

    /**
     * Known users.
     */
    private static final Map<String, UserData> userDataByAccessToken;

    /**
     * Known users.
     */
    private static final Map<String, UserData> userDataById;

    /**
     * {@link Group}s.
     */
    private final Map<String, Group> groups = new HashMap<String, Group>();

    /**
     * User-ids in {@link Group}s.
     */
    private final Map<String, Set<String>> userIdsInGroup = new HashMap<String, Set<String>>();

    /**
     * Static initialization.
     */
    static {
        USER1_DATA = new UserData();
        UserInformation user1Information = USER1_DATA.getUserInformation();
        user1Information.setUserId("1");
        user1Information.setFirstName("test1");
        user1Information.setEmail("test1@cesnet.cz");

        USER2_DATA = new UserData();
        UserInformation user2Information = USER2_DATA.getUserInformation();
        user2Information.setUserId("2");
        user2Information.setFirstName("test2");
        user2Information.setEmail("test2@cesnet.cz");

        USER3_DATA = new UserData();
        UserInformation user3Information = USER3_DATA.getUserInformation();
        user3Information.setUserId("3");
        user3Information.setFirstName("test3");
        user3Information.setEmail("test3@cesnet.cz");

        userDataByAccessToken = new HashMap<String, UserData>();
        userDataByAccessToken.put(
                AbstractControllerTest.SECURITY_TOKEN_ROOT.getAccessToken(), Authorization.ROOT_USER_DATA);
        userDataByAccessToken.put(
                AbstractControllerTest.SECURITY_TOKEN_USER1.getAccessToken(), USER1_DATA);
        userDataByAccessToken.put(
                AbstractControllerTest.SECURITY_TOKEN_USER2.getAccessToken(), USER2_DATA);
        userDataByAccessToken.put(
                AbstractControllerTest.SECURITY_TOKEN_USER3.getAccessToken(), USER3_DATA);

        userDataById = new HashMap<String, UserData>();
        for (UserData userData : userDataByAccessToken.values()) {
            userDataById.put(userData.getUserId(), userData);
        }
    }

    /**
     * Constructor.
     *
     * @param configuration to be used
     */
    public DummyAuthorization(ControllerConfiguration configuration)
    {
        super(configuration);

        this.adminAccessTokens.add(AbstractControllerTest.SECURITY_TOKEN_ROOT.getAccessToken());
        createGroup(new Group(adminGroupName));
    }

    /**
     * Constructor.
     *
     * @param entityManagerFactory sets the {@link #entityManagerFactory}
     */
    public DummyAuthorization(EntityManagerFactory entityManagerFactory)
    {
        this(new ControllerConfiguration());

        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * @param userId to be added to the group of administrators
     */
    public void addAdminUserId(String userId)
    {
        addGroupUser(getGroupIdByName(adminGroupName), userId);
    }

    @Override
    protected UserData onGetUserDataByAccessToken(String accessToken)
            throws ControllerReportSet.UserNotExistsException
    {
        UserData userData = userDataByAccessToken.get(accessToken);
        if (userData != null) {
            return userData;
        }
        throw new ControllerReportSet.UserNotExistsException(accessToken);
    }

    @Override
    protected UserData onGetUserDataByUserId(String userId)
            throws ControllerReportSet.UserNotExistsException
    {
        UserData userData = userDataById.get(userId);
        if (userData != null) {
            return userData;
        }
        throw new ControllerReportSet.UserNotExistsException(userId);
    }

    @Override
    protected String onGetUserIdByPrincipalName(String principalName)
            throws ControllerReportSet.UserNotExistsException
    {
        for (UserData userData : userDataById.values()) {
            UserInformation userInformation = userData.getUserInformation();
            if (userInformation.hasPrincipalName(principalName)) {
                return userData.getUserId();
            }
        }
        throw new ControllerReportSet.UserNotExistsException(principalName);
    }

    @Override
    protected Collection<UserData> onListUserData(String search)
    {
        List<UserInformation> users = new LinkedList<UserInformation>();
        for (UserData userData : userDataById.values()) {
            users.add(userData.getUserInformation());
        }
        UserInformation.filter(users, search);
        List<UserData> userData = new LinkedList<UserData>();
        for (UserInformation user : users) {
            userData.add(userDataById.get(user.getUserId()));
        }
        return userData;
    }

    @Override
    public List<Group> onListGroups()
    {
        return new LinkedList<Group>(groups.values());
    }

    @Override
    public Set<String> onListGroupUserIds(String groupId)
    {
        if (!groups.containsKey(groupId)) {
            throw new ControllerReportSet.GroupNotExistsException(groupId);
        }
        Set<String> userIds = userIdsInGroup.get(groupId);
        if (userIds == null) {
            userIds = Collections.emptySet();
        }
        return userIds;
    }

    @Override
    public String onCreateGroup(Group group)
    {
        int groupId = groups.size();
        while (groups.containsKey(String.valueOf(groupId))) {
            groupId++;
        }
        group.setId(String.valueOf(groupId));
        groups.put(group.getId(), group);
        return group.getId();
    }

    @Override
    public void onDeleteGroup(String groupId)
    {
        if (!groups.containsKey(groupId)) {
            throw new ControllerReportSet.GroupNotExistsException(groupId);
        }
        groups.remove(groupId);
        userIdsInGroup.remove(groupId);
    }

    @Override
    public void onAddGroupUser(String groupId, String userId)
    {
        if (!groups.containsKey(groupId)) {
            throw new ControllerReportSet.GroupNotExistsException(groupId);
        }
        Set<String> userIds = userIdsInGroup.get(groupId);
        if (userIds == null) {
            userIds = new HashSet<String>();
            userIdsInGroup.put(groupId, userIds);
        }
        userIds.add(userId);
    }

    @Override
    public void onRemoveGroupUser(String groupId, String userId)
    {
        if (!groups.containsKey(groupId)) {
            throw new ControllerReportSet.GroupNotExistsException(groupId);
        }
        Set<String> userIds = userIdsInGroup.get(groupId);
        if (userIds == null || !userIds.contains(userId)) {
            throw new ControllerReportSet.UserNotInGroupException(groupId, userId);
        }
        userIds.remove(userId);
    }

    /**
     * @param configuration to be used for initialization
     * @return new instance of {@link DummyAuthorization}
     * @throws IllegalStateException when other {@link Authorization} already exists
     */
    public static DummyAuthorization createInstance(ControllerConfiguration configuration) throws IllegalStateException
    {
        DummyAuthorization authorization = new DummyAuthorization(configuration);
        Authorization.setInstance(authorization);
        return authorization;
    }
}
