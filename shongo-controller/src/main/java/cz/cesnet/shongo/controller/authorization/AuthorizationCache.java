package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.acl.AclEntry;
import cz.cesnet.shongo.controller.acl.AclObjectIdentity;
import cz.cesnet.shongo.controller.api.Group;
import org.joda.time.Duration;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Represents a cache of {@link AclEntry}s
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AuthorizationCache
{
    /**
     * Cache of user-id by access token.
     */
    private ExpirationMap<String, String> userIdByAccessTokenCache = new ExpirationMap<String, String>();

    /**
     * Cache of user-id by principal name.
     */
    private ExpirationMap<String, String> userIdByPrincipalNameCache = new ExpirationMap<String, String>();

    /**
     * Cache of {@link UserInformation} by user-id.
     */
    private ExpirationMap<String, UserData> userDataByUserIdCache =
            new ExpirationMap<String, UserData>();

    /**
     * Cache of {@link UserAuthorizationData} by access token.
     */
    private ExpirationMap<String, UserAuthorizationData> userAuthorizationDataByAccessTokenCache =
            new ExpirationMap<String, UserAuthorizationData>();

    /**
     * Cache of {@link AclEntry} by {@link AclEntry#id}.
     */
    private ExpirationMap<Long, AclEntry> aclEntryCache = new ExpirationMap<Long, AclEntry>();

    /**
     * Cache of {@link AclUserState} by user-id.
     */
    private ExpirationMap<String, AclUserState> aclUserStateCache = new ExpirationMap<String, AclUserState>();

    /**
     * Cache of {@link AclObjectState} by {@link AclObjectIdentity}.
     */
    private ExpirationMap<AclObjectIdentity, AclObjectState> aclObjectStateCache =
            new ExpirationMap<AclObjectIdentity, AclObjectState>();

    /**
     * Cache of {@link Group} by group-id.
     */
    private ExpirationMap<String, Group> groupByGroupId = new ExpirationMap<String, Group>();

    /**
     * Cache of group-id by group name.
     */
    private ExpirationMap<String, String> groupIdByName = new ExpirationMap<String, String>();

    /**
     * Cache of user-ids by group-ids for users which are in the group.
     */
    private ExpirationMap<String, UserIdSet> userIdsByGroupId = new ExpirationMap<String, UserIdSet>();

    /**
     * @param expiration sets the {@link #userIdByAccessTokenCache} expiration
     */
    public synchronized void setUserIdExpiration(Duration expiration)
    {
        userIdByAccessTokenCache.setExpiration(expiration);
        userIdByPrincipalNameCache.setExpiration(expiration);
    }

    /**
     * @param expiration sets the {@link #userDataByUserIdCache} expiration
     */
    public synchronized void setUserInformationExpiration(Duration expiration)
    {
        userDataByUserIdCache.setExpiration(expiration);
        userAuthorizationDataByAccessTokenCache.setExpiration(expiration);
    }

    /**
     * @param expiration sets the {@link #aclUserStateCache} expiration
     */
    public synchronized void setAclExpiration(Duration expiration)
    {
        aclEntryCache.setExpiration(expiration);
        aclUserStateCache.setExpiration(expiration);
        aclObjectStateCache.setExpiration(expiration);
    }

    /**
     * @param expiration sets the {@link #groupIdByName} expiration
     */
    public synchronized void setGroupExpiration(Duration expiration)
    {
        groupByGroupId.setExpiration(expiration);
        groupIdByName.setExpiration(expiration);
    }

    /**
     * Clear the cache.
     */
    public synchronized void clear()
    {
        userIdByAccessTokenCache.clear();
        userIdByPrincipalNameCache.clear();
        userDataByUserIdCache.clear();
        userAuthorizationDataByAccessTokenCache.clear();
        aclEntryCache.clear();
        aclUserStateCache.clear();
        aclObjectStateCache.clear();
        groupByGroupId.clear();
        groupIdByName.clear();
    }

    /**
     * @param accessToken
     * @return user-id by given {@code accessToken}
     */
    public synchronized String getUserIdByAccessToken(String accessToken)
    {
        return userIdByAccessTokenCache.get(accessToken);
    }

    /**
     * Put given {@code userId} to the cache by the given {@code accessToken}.
     *
     * @param accessToken
     * @param userId
     */
    public synchronized void putUserIdByAccessToken(String accessToken, String userId)
    {
        userIdByAccessTokenCache.put(accessToken, userId);
    }

    /**
     * @param principalName
     * @return user-id by given {@code principalName}
     */
    public synchronized String getUserIdByPrincipalName(String principalName)
    {
        return userIdByPrincipalNameCache.get(principalName);
    }

    /**
     * @param principalName
     * @return whether user with given {@code principalName} exists in cache
     */
    public synchronized boolean hasUserIdByPrincipalName(String principalName)
    {
        return userIdByPrincipalNameCache.contains(principalName);
    }

    /**
     * Put given {@code userId} to the cache by the given {@code principalName}.
     *
     * @param principalName
     * @param userId
     */
    public synchronized void putUserIdByPrincipalName(String principalName, String userId)
    {
        userIdByPrincipalNameCache.put(principalName, userId);
    }

    /**
     * @param userId
     * @return {@link UserData} by given {@code userId}
     */
    public synchronized UserData getUserDataByUserId(String userId)
    {
        return userDataByUserIdCache.get(userId);
    }

    /**
     * @param userId
     * @return true whether user with given {@code userId} has cached {@link UserData}
     */
    public synchronized boolean hasUserDataByUserId(String userId)
    {
        return userDataByUserIdCache.contains(userId);
    }

    /**
     * Put given {@code UserData} to the cache by the given {@code userId}.
     *
     * @param userId
     * @param userData
     */
    public synchronized void putUserDataByUserId(String userId, UserData userData)
    {
        userDataByUserIdCache.put(userId, userData);
    }

    /**
     * @param accessToken
     * @return {@link UserAuthorizationData} by given {@code accessToken}
     */
    public synchronized UserAuthorizationData getUserAuthorizationDataByAccessToken(String accessToken)
    {
        return userAuthorizationDataByAccessTokenCache.get(accessToken);
    }

    /**
     * @param accessToken
     * @return true whether user with given {@code accessToken} has cached {@link UserAuthorizationData}
     */
    public synchronized boolean hasUserAuthorizationDataByAccessToken(String accessToken)
    {
        return userAuthorizationDataByAccessTokenCache.contains(accessToken);
    }

    /**
     * Put given {@code userAuthorizationData} to the cache by the given {@code accessToken}.
     *
     * @param accessToken
     * @param userAuthorizationData
     */
    public synchronized void putUserAuthorizationDataByAccessToken(String accessToken,
            UserAuthorizationData userAuthorizationData)
    {
        userAuthorizationDataByAccessTokenCache.put(accessToken, userAuthorizationData);
    }

    /**
     * @param aclEntryId
     * @return {@link AclEntry} by given {@code aclEntryId}
     */
    public synchronized AclEntry getAclEntryById(Long aclEntryId)
    {
        return aclEntryCache.get(aclEntryId);
    }

    /**
     * @return {@link AclEntry}s
     */
    public synchronized Iterable<AclEntry> getAclEntries()
    {
        return aclEntryCache;
    }

    /**
     * Put given {@code userAcl} to the cache
     *
     * @param aclEntry
     */
    public synchronized void putAclEntryById(AclEntry aclEntry)
    {
        aclEntryCache.put(aclEntry.getId(), aclEntry);
    }

    /**
     * Remove given {@code userAcl} from the cache
     *
     * @param aclEntry
     * @return removed {@link AclEntry}
     */
    public synchronized AclEntry removeAclEntryById(AclEntry aclEntry)
    {
        return aclEntryCache.remove(aclEntry.getId());
    }

    /**
     * @param userId
     * @return {@link AclUserState} by given {@code userId}
     */
    public synchronized AclUserState getAclUserStateByUserId(String userId)
    {
        return aclUserStateCache.get(userId);
    }

    /**
     * @return List of {@link AclUserState}
     */
    public synchronized Collection<AclUserState> listAclUserStates()
    {
        return aclUserStateCache.values();
    }

    /**
     * Put given {@code userAcl} to the cache by the given {@code userId}.
     *
     * @param userId
     * @param aclUserState
     */
    public synchronized void putAclUserStateByUserId(String userId, AclUserState aclUserState)
    {
        aclUserStateCache.put(userId, aclUserState);
    }

    /**
     * @param aclObjectIdentity
     * @return {@link AclObjectState} by given {@code aclObjectIdentity}
     */
    public synchronized AclObjectState getAclObjectStateByIdentity(AclObjectIdentity aclObjectIdentity)
    {
        return aclObjectStateCache.get(aclObjectIdentity);
    }

    /**
     * Put given {@code aclObjectState} to the cache by the given {@code aclObjectIdentity}.
     *
     * @param aclObjectIdentity
     * @param aclObjectState
     */
    public synchronized void putAclObjectStateByIdentity(AclObjectIdentity aclObjectIdentity,
            AclObjectState aclObjectState)
    {
        aclObjectStateCache.put(aclObjectIdentity, aclObjectState);
    }

    /**
     * @param groupId
     * @return {@link Group} by given {@code groupId}
     */
    public synchronized Group getGroupByGroupId(String groupId)
    {
        return groupByGroupId.get(groupId);
    }

    /**
     * @param groupId
     * @return true whether group with given {@code groupId} has cached {@link Group}
     */
    public synchronized boolean hasGroupByGroupId(String groupId)
    {
        return groupByGroupId.contains(groupId);
    }

    /**
     * Put given {@code Group} to the cache by the given {@code groupId}.
     *
     * @param groupId
     * @param group
     */
    public synchronized void putGroupByGroupId(String groupId, Group group)
    {
        groupByGroupId.put(groupId, group);
    }

    /**
     * @param groupName
     * @return group-id for given {@code groupName}
     */
    public synchronized String getGroupIdByName(String groupName)
    {
        return groupIdByName.get(groupName);
    }

    /**
     * Put given {@code groupId} to the cache by the given {@code groupName}.
     *
     * @param groupName
     * @param groupId
     */
    public synchronized void putGroupIdByName(String groupName, String groupId)
    {
        groupIdByName.put(groupName, groupId);
    }

    /**
     * @param groupId
     * @return set of user-ids for given {@code groupId}
     */
    public synchronized UserIdSet getUserIdsInGroup(String groupId)
    {
        return userIdsByGroupId.get(groupId);
    }

    /**
     * Put given {@code userIds} to the cache by the given {@code groupId}.
     *
     * @param groupId
     * @param userIds
     */
    public synchronized void putUserIdsInGroup(String groupId, UserIdSet userIds)
    {
        userIdsByGroupId.put(groupId, userIds);
    }

    /**
     * Remove cached information about group with given {@code groupId}.
     *
     * @param groupId
     */
    public synchronized void removeGroup(String groupId)
    {
        groupIdByName.removeByValue(groupId);
        userIdsByGroupId.remove(groupId);
    }
}
