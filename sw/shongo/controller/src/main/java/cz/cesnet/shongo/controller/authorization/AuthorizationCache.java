package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.*;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AuthorizationCache
{
    /**
     * Cache of user-id by access token.
     */
    private Cache<String, String> userIdCache = new Cache<String, String>();

    /**
     * Cache of {@link cz.cesnet.shongo.api.UserInformation} by user-id.
     */
    private Cache<String, UserInformation> userInformationCache = new Cache<String, UserInformation>();

    /**
     * Cache of {@link AclRecord} by {@link AclRecord#id}.
     */
    private Cache<String, AclRecord> aclRecordCache = new Cache<String, AclRecord>();

    /**
     * Cache of {@link AclUserState} by user-id.
     */
    private Cache<String, AclUserState> aclUserStateCache = new Cache<String, AclUserState>();

    /**
     * Cache of {@link AclEntityState} by {@link EntityIdentifier}.
     */
    private Cache<EntityIdentifier, AclEntityState> aclEntityStateCache = new Cache<EntityIdentifier, AclEntityState>();

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
    public synchronized AclRecord getAclRecordById(String aclRecordId)
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

    /**
     * Represents a cache of {@link V} by {@link K} with {@link #expiration}.
     * @param <K>
     * @param <V>
     */
    private static class Cache<K, V> implements Iterable<V>
    {
        /**
         * Cache of {@link V} by {@link K}.
         */
        private Map<K, CacheEntry<V>> entries = new HashMap<K, CacheEntry<V>>();

        /**
         * Specifies expiration for the {@link #entries}.
         */
        private Duration expiration = null;

        /**
         * @param expiration sets the {@link #expiration}
         */
        public void setExpiration(Duration expiration)
        {
            this.expiration = expiration;
        }

        /**
         * @param key
         * @return {@link V} by given {@code key}
         */
        public synchronized V get(K key)
        {
            CacheEntry<V> entry = entries.get(key);
            if (entry != null) {
                if (entry.expirationDateTime == null || entry.expirationDateTime.isAfter(DateTime.now())) {
                    return entry.value;
                }
                else {
                    entries.remove(key);
                }
            }
            return null;
        }

        /**
         * Put given {@code value} to the cache by the given {@code key}.
         *
         * @param key
         * @param value
         */
        public synchronized void put(K key, V value)
        {
            Cache.CacheEntry<V> entry = entries.get(key);
            if (entry == null) {
                entry = new CacheEntry<V>();
                entries.put(key, entry);
            }
            if (expiration != null) {
                entry.expirationDateTime = DateTime.now().plus(expiration);
            }
            else {
                entry.expirationDateTime = null;
            }
            entry.value = value;
        }

        /**
         * Remove given {@code key}.
         *
         * @param key
         */
        public synchronized void remove(K key)
        {
            entries.remove(key);
        }

        /**
         * Entry for {@link Cache}.
         */
        public static class CacheEntry<V>
        {
            /**
             * Expiration {@link DateTime}.
             */
            private DateTime expirationDateTime;

            /**
             * Value.
             */
            private V value;
        }

        @Override
        public Iterator<V> iterator()
        {
            final Iterator<CacheEntry<V>> iterator = entries.values().iterator();
            return new Iterator<V>()
            {
                @Override
                public boolean hasNext()
                {
                    return iterator.hasNext();
                }

                @Override
                public V next()
                {
                    return iterator.next().value;
                }

                @Override
                public void remove()
                {
                    iterator.remove();
                }
            };
        }
    }
}
