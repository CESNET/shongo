package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.api.UserInformation;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.HashMap;
import java.util.Map;

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
     * Cache of {@link UserAcl} by user-id.
     */
    private Cache<String, UserAcl> userAclCache = new Cache<String, UserAcl>();

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
     * @param expiration sets the {@link #userAclCache} expiration
     */
    public void setUserAclExpiration(Duration expiration)
    {
        userAclCache.setExpiration(expiration);
    }

    /**
     * @param accessToken
     * @return user-id by given {@code accessToken}
     */
    public synchronized String getCachedUserIdByAccessToken(String accessToken)
    {
        return userIdCache.get(accessToken);
    }

    /**
     * Put given {@code userId} to the cache by the given {@code accessToken}.
     *
     * @param accessToken
     * @param userId
     */
    public synchronized void putCachedUserIdByAccessToken(String accessToken, String userId)
    {
        userIdCache.put(accessToken, userId);
    }

    /**
     * @param userId
     * @return {@link UserInformation} by given {@code userId}
     */
    public synchronized UserInformation getCachedUserInformationByUserId(String userId)
    {
        return userInformationCache.get(userId);
    }

    /**
     * Put given {@code userInformation} to the cache by the given {@code userId}.
     *
     * @param userId
     * @param userInformation
     */
    public synchronized void putCachedUserInformationByUserId(String userId, UserInformation userInformation)
    {
        userInformationCache.put(userId, userInformation);
    }

    /**
     * @param userId
     * @return {@link UserInformation} by given {@code userId}
     */
    public synchronized UserAcl getCachedUserAclByUserId(String userId)
    {
        return userAclCache.get(userId);
    }

    /**
     * Put given {@code userAcl} to the cache by the given {@code userId}.
     *
     * @param userId
     * @param userAcl
     */
    public synchronized void putCachedUserAclByUserId(String userId, UserAcl userAcl)
    {
        userAclCache.put(userId, userAcl);
    }

    /**
     * Represents a cache of {@link V} by {@link K} with {@link #expiration}.
     * @param <K>
     * @param <V>
     */
    private static class Cache<K, V>
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
    }
}
