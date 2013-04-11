package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import org.joda.time.Duration;

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
    private ExpirationMap<String, String> userIdCache = new ExpirationMap<String, String>();

    /**
     * Cache of {@link cz.cesnet.shongo.api.UserInformation} by user-id.
     */
    private ExpirationMap<String, UserInformation> userInformationCache = new ExpirationMap<String, UserInformation>();

    /**
     * Cache of {@link AclRecord} by {@link AclRecord#id}.
     */
    private ExpirationMap<Long, AclRecord> aclRecordCache = new ExpirationMap<Long, AclRecord>();

    /**
     * Cache of {@link AclUserState} by user-id.
     */
    private ExpirationMap<String, AclUserState> aclUserStateCache = new ExpirationMap<String, AclUserState>();

    /**
     * Cache of {@link AclEntityState} by {@link EntityIdentifier}.
     */
    private ExpirationMap<EntityIdentifier, AclEntityState> aclEntityStateCache = new ExpirationMap<EntityIdentifier, AclEntityState>();

    /**
     * @param expiration sets the {@link #userIdCache} expiration
     */
    public void setUserIdExpiration(Duration expiration)
    {
        userIdCache.setExpiration(expiration);
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
     * @param accessToken
     * @return user-id by given {@code accessToken}
     */
    public synchronized String getUserIdByAccessToken(String accessToken)
    {
        return userIdCache.get(accessToken);
    }

    /**
     * Put given {@code userId} to the cache by the given {@code accessToken}.
     *
     * @param accessToken
     * @param userId
     */
    public synchronized void putUserIdByAccessToken(String accessToken, String userId)
    {
        userIdCache.put(accessToken, userId);
    }

    /**
     * @param userId
     * @return {@link UserInformation} by given {@code userId}
     */
    public synchronized UserInformation getUserInformationByUserId(String userId)
    {
        return userInformationCache.get(userId);
    }

    /**
     * Put given {@code userInformation} to the cache by the given {@code userId}.
     *
     * @param userId
     * @param userInformation
     */
    public synchronized void putUserInformationByUserId(String userId, UserInformation userInformation)
    {
        userInformationCache.put(userId, userInformation);
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
     */
    public synchronized void removeAclRecordById(AclRecord aclRecord)
    {
        aclRecordCache.remove(aclRecord.getId());
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
    public synchronized AclEntityState getAclEntityStateByEntityId(EntityIdentifier entityId)
    {
        return aclEntityStateCache.get(entityId);
    }

    /**
     * Put given {@code aclEntityState} to the cache by the given {@code entityId}.
     *
     * @param entityId
     * @param aclEntityState
     */
    public synchronized void putAclEntityStateByEntityId(EntityIdentifier entityId, AclEntityState aclEntityState)
    {
        aclEntityStateCache.put(entityId, aclEntityState);
    }
}
