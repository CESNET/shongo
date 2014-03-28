package cz.cesnet.shongo.controller.settings;

import cz.cesnet.shongo.controller.SystemPermission;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.UserData;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

/**
 * Manager for {@link UserSettings}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserSettingsManager
{
    /**
     * {@link EntityManager} that is used for loading user settings.
     */
    private EntityManager entityManager;

    /**
     * {@link Authorization} which is used for loading user settings.
     */
    private Authorization authorization;

    /**
     * Constructor.
     *
     * @param entityManager sets the {@link #entityManager}
     * @param authorization
     */
    public UserSettingsManager(EntityManager entityManager, Authorization authorization)
    {
        this.entityManager = entityManager;
        this.authorization = authorization;
    }

    /**
     * @param securityToken
     * @return {@link cz.cesnet.shongo.controller.api.UserSettings} for given {@code securityToken}
     */
    public cz.cesnet.shongo.controller.api.UserSettings getUserSettings(SecurityToken securityToken)
    {
        cz.cesnet.shongo.controller.settings.UserSessionSettings userSessionSettings =
                authorization.getUserSessionSettings(securityToken);

        cz.cesnet.shongo.controller.api.UserSettings userSettingsApi = getUserSettings(securityToken.getUserId());
        userSettingsApi.setAdministratorMode(userSessionSettings.getAdministratorMode());
        return userSettingsApi;
    }

    /**
     * @param userId
     * @return {@link cz.cesnet.shongo.controller.api.UserSettings} for given {@code userId}
     */
    public cz.cesnet.shongo.controller.api.UserSettings getUserSettings(String userId)
    {
        cz.cesnet.shongo.controller.api.UserSettings userSettingsApi =
                new cz.cesnet.shongo.controller.api.UserSettings();

        // Set user settings
        UserSettings userSettings = getPersistentUserSettings(userId);
        if (userSettings != null) {
            userSettings.toApi(userSettingsApi);
        }
        else {
            // Default values
            userSettingsApi.setUseWebService(true);
            userSettingsApi.setSystemAdministratorNotifications(true);
            userSettingsApi.setResourceAdministratorNotifications(true);
        }

        // Set web service data
        if (userSettingsApi.isUseWebService()) {
            UserData userData = authorization.getUserData(userId);
            userSettingsApi.setLocale(userData.getLocale());
        }

        return userSettingsApi;
    }

    public void updateUserSessionSettings(SecurityToken securityToken,
            cz.cesnet.shongo.controller.api.UserSettings userSettingsApi)
    {
        cz.cesnet.shongo.controller.settings.UserSessionSettings userSessionSettings =
                authorization.getUserSessionSettings(securityToken);

        // Update administrator mode setting by user permission
        boolean administratorMode = userSettingsApi.getAdministratorMode();
        if (administratorMode) {
            if (!authorization.hasSystemPermission(securityToken, SystemPermission.ADMINISTRATION)) {
                administratorMode = false;
            }
        }
        // If administrator mode setting has changed, store the change
        if (administratorMode != userSessionSettings.getAdministratorMode()) {
            userSessionSettings.setAdministratorMode(administratorMode);
            authorization.updateUserSessionSettings(userSessionSettings);
        }
    }

    public void updateUserSettings(String userId, cz.cesnet.shongo.controller.api.UserSettings userSettingsApi)
    {
        // Update user settings
        UserSettings userSettings = getPersistentUserSettings(userId);
        if (userSettings == null) {
            userSettings = new cz.cesnet.shongo.controller.settings.UserSettings();
            userSettings.setUserId(userId);
        }
        userSettings.fromApi(userSettingsApi);

        entityManager.persist(userSettings);
    }

    private UserSettings getPersistentUserSettings(String userId)
    {
        try {
            return entityManager.createQuery("SELECT userSettings FROM UserSettings userSettings"
                    + " WHERE userSettings.userId = :userId",
                    UserSettings.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
        }
        catch (NoResultException exception) {
            return null;
        }
    }
}
