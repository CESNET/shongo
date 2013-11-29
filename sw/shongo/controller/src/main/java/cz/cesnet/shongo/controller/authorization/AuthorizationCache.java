package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.api.UserInformation;
import org.joda.time.Duration;

import java.util.Set;

/**
 * Represents a cache of {@link AclRecord}s
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
     * Cache of {@link cz.cesnet.shongo.api.UserInformation} by user-id.
     */
    private ExpirationMap<String, Authorization.UserData> userInformationCache =
            new ExpirationMap<String, Authorization.UserData>();

    /**
     * Cache of {@link AclRecord} by {@link AclRecord#id}.
     */
    private ExpirationMap<Long, AclRecord> aclRecordCache = new ExpirationMap<Long, AclRecord>();

    /**
     * Cache of {@link AclUserState} by user-id.
     */
    private ExpirationMap<String, AclUserState> aclUserStateCache = new ExpirationMap<String, AclUserState>();

    /**
     * Cache of {@link AclEntityState} by {@link AclRecord.EntityId}.
     */
    private ExpirationMap<AclRecord.EntityId, AclEntityState> aclEntityStateCache =
            new ExpirationMap<AclRecord.EntityId, AclEntityState>();

    /**
     * Cache of group-id by group name.
     */
    private ExpirationMap<String, String> groupIdByName = new ExpirationMap<String, String>();

    /**
     * Cache of user-ids by group-ids for users which are in the group.
     */
    private ExpirationMap<String, Set<String>> userIdsByGroupId = new ExpirationMap<String, Set<String>>();

    /**
     * @param expiration sets the {@link #userIdByAccessTokenCache} expiration
     */
    public void setUserIdExpiration(Duration expiration)
    {
        userIdByAccessTokenCache.setExpiration(expiration);
        userIdByPrincipalNameCache.setExpiration(expiration);
    }

    /**
     * @param expiration sets the {@link #userInformationCache} expiration
     */
    public void setUserInformationExpiration(Duration expiration)
    {
        userInformationCache.setExpiration(expiration);
    }

    /**
     * @param expiration sets the {@link #aclUserStateCache} expiration
     */
    public void setAclExpiration(Duration expiration)
    {
        aclRecordCache.setExpiration(expiration);
        aclUserStateCache.setExpiration(expiration);
        aclEntityStateCache.setExpiration(expiration);
    }

    /**
     * @param expiration sets the {@link #groupIdByName} expiration
     */
    public void setGroupExpiration(Duration expiration)
    {
        groupIdByName.setExpiration(expiration);
    }

    /**
     * Clear the cache.
     */
    public synchronized void clear()
    {
        userIdByAccessTokenCache.clear();
        userIdByPrincipalNameCache.clear();
        userInformationCache.clear();
        aclRecordCache.clear();
        aclUserStateCache.clear();
        aclEntityStateCache.clear();
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
     * @return {@link UserInformation} by given {@code userId}
     */
    public synchronized Authorization.UserData getUserDataByUserId(String userId)
    {
        return userInformationCache.get(userId);
    }

    /**
     * Put given {@code userInformation} to the cache by the given {@code userId}.
     *
     * @param userId
     * @param userData
     */
    public synchronized void putUserDataByUserId(String userId, Authorization.UserData userData)
    {
        userInformationCache.put(userId, userData);
    }

    /**
     * @param aclRecordId
     * @return {@link AclRecord} by given {@code aclRecordId}
     */
    public synchronized AclRecord getAclRecordById(Long aclRecordId)
    {
        return aclRecordCache.get(aclRecordId);
    }

    /**
     * @return {@link AclRecord}s
     */
    public synchronized Iterable<AclRecord> getAclRecords()
    {
        return aclRecordCache;
    }

    /**
     * Put given {@code userAcl} to the cache
     *
     * @param aclRecord
     */
    public synchronized void putAclRecordById(AclRecord aclRecord)
    {
        aclRecordCache.put(aclRecord.getId(), aclRecord);
    }

    /**
     * Remove given {@code userAcl} from the cache
     *
     * @param aclRecord
     * @return removed {@link AclRecord}
     */
    public synchronized AclRecord removeAclRecordById(AclRecord aclRecord)
    {
        return aclRecordCache.remove(aclRecord.getId());
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
     * @param entityId
     * @return {@link AclEntityState} by given {@code entityId}
     */
    public synchronized AclEntityState getAclEntityStateByEntityId(AclRecord.EntityId entityId)
    {
        return aclEntityStateCache.get(entityId);
    }

    /**
     * Put given {@code aclEntityState} to the cache by the given {@code entityId}.
     *
     * @param entityId
     * @param aclEntityState
     */
    public synchronized void putAclEntityStateByEntityId(AclRecord.EntityId entityId, AclEntityState aclEntityState)
    {
        aclEntityStateCache.put(entityId, aclEntityState);
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
    public synchronized Set<String> getUserIdsInGroup(String groupId)
    {
        return userIdsByGroupId.get(groupId);
    }

    /**
     * Put given {@code userIds} to the cache by the given {@code groupId}.
     *
     * @param groupId
     * @param userIds
     */
    public synchronized void putUserIdsInGroup(String groupId, Set<String> userIds)
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
