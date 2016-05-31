package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.notification.NotificationManager;

import javax.persistence.EntityManagerFactory;

/**
 * Represents a component of a domain controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Component
{
    /**
     * @see ControllerConfiguration
     */
    protected ControllerConfiguration configuration;

    /**
     * @return {@link #configuration}
     */
    public ControllerConfiguration getConfiguration()
    {
        return configuration;
    }

    /**
     * Initialize domain controller component.
     */
    public final void init()
    {
        init(new ControllerConfiguration());
    }

    /**
     * Initialize domain controller component.
     *
     * @param configuration
     */
    public void init(ControllerConfiguration configuration)
    {
        this.configuration = configuration;
    }

    /**
     * Destroy domain controller component.
     */
    public void destroy()
    {
    }

    /**
     * Checks if dependency is filled.
     *
     * @param dependency
     * @param dependencyType
     * @throws RuntimeException
     */
    protected void checkDependency(Object dependency, Class dependencyType) throws RuntimeException
    {
        if (dependency == null) {
            throw new RuntimeException("Component " + getClass().getName()
                    + " doesn't have the " + dependencyType.getSimpleName() + " set!");
        }
    }

    /**
     * {@link Component} which contains reference to current {@link LocalDomain}.
     */
    public static interface EntityManagerFactoryAware
    {
        /**
         * @param entityManagerFactory sets the {@link javax.persistence.EntityManagerFactory} to the component.
         */
        public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory);
    }

    /**
     * Class extending {@link Component} can implement this interface to be aware of {@link ControllerAgent}.
     */
    public static interface ControllerAgentAware
    {
        /**
         * @param controllerAgent {@link ControllerAgent} which can be used by implementing {@link Component}
         */
        public void setControllerAgent(ControllerAgent controllerAgent);
    }

    /**
     * Class extending {@link Component} can implement this interface to be aware of {@link cz.cesnet.shongo.controller.authorization.Authorization}.
     */
    public static interface AuthorizationAware
    {
        /**
         * @param authorization {@link cz.cesnet.shongo.controller.authorization.Authorization} which can be used by implementing {@link Component}
         */
        public void setAuthorization(Authorization authorization);
    }

    /**
     * Class extending {@link Component} can implement this interface to be aware of {@link NotificationManager}.
     */
    public static interface NotificationManagerAware
    {
        /**
         * @param notificationManager {@link NotificationManager} which can be used by implementing {@link Component}
         */
        public void setNotificationManager(NotificationManager notificationManager);
    }

    /**
     * Class extending {@link Component} can implement this interface and the thread returned from
     * the {@link #getThread()} method will be automatically started after the controller is started.
     */
    public static interface WithThread
    {
        public Thread getThread();
    }

    /**
     * Class extending {@link Component} can implement this interface to be aware of {@link cz.cesnet.shongo.controller.authorization.Authorization}.
     */
    public static interface WithEmailSender
    {
        /**
         * @param emailSender {@link EmailSender} which can be used by implementing {@link Component}
         */
        public void setEmailSender(EmailSender emailSender);
    }
}
