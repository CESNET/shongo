package cz.cesnet.shongo.controller.settings;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

/**
 * Provider for {@link UserSettings}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserSettingsProvider
{
    /**
     * Entity manager that is used for managing persistent objects.
     */
    protected EntityManager entityManager;

    /**
     * Constructor.
     *
     * @param entityManager sets the {@link #entityManager}
     */
    public UserSettingsProvider(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    /**
     * @param userId
     * @return {@link cz.cesnet.shongo.controller.settings.UserSettings} for given {@code userId}
     */
    public UserSettings getUserSettings(String userId)
    {
        return getUserSettings(userId, entityManager);
    }

    /**
     * @param userId
     * @param entityManager
     * @return {@link cz.cesnet.shongo.controller.settings.UserSettings} for given {@code userId}
     */
    public static UserSettings getUserSettings(String userId, EntityManager entityManager)
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
