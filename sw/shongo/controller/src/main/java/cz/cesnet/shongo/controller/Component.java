package cz.cesnet.shongo.controller;

import javax.persistence.EntityManagerFactory;

/**
 * Represents a component of a domain controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Component
{
    /**
     * Initialize domain controller component.
     */
    public final void init()
    {
        init(new Configuration());
    }

    /**
     * Initialize domain controller component.
     *
     * @param configuration
     */
    public void init(Configuration configuration)
    {
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
     * @throws IllegalStateException
     */
    protected void checkDependency(Object dependency, Class dependencyType) throws IllegalStateException
    {
        if (dependency == null) {
            throw new IllegalStateException("Component " + getClass().getName()
                    + " doesn't have the " + dependencyType.getSimpleName() + " set!");
        }
    }

    /**
     * {@link Component} which contains reference to current {@link Domain}.
     */
    public static interface EntityManagerFactoryAware
    {
        /**
         * @param entityManagerFactory sets the {@link javax.persistence.EntityManagerFactory} to the component.
         */
        public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory);
    }

    /**
     * {@link Component} which contains reference to current {@link Domain}.
     */
    public static interface DomainAware
    {
        /**
         * @param domain sets the {@link Domain} to the component
         */
        public void setDomain(Domain domain);
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
     * Class extending {@link Component} can implement this interface and the thread returned from
     * the {@link #getThread()} method will be automatically started after the controller is started.
     */
    public static interface WithThread
    {
        public Thread getThread();
    }
}
