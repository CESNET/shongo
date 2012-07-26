package cz.cesnet.shongo.controller;

import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Represents a component of a domain controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Component
{
    /**
     * Factory to create entity manager that can be used for loading/saving entities by the component.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * Determines whether component has been initialized.
     */
    private boolean initialized = false;

    /**
     * @param entityManagerFactory sets the {@link #entityManagerFactory}
     */
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * @return entity manager
     */
    protected EntityManager getEntityManager()
    {
        return this.entityManagerFactory.createEntityManager();
    }

    /**
     * Checks whether the component is initialized.
     *
     * @throws IllegalStateException
     */
    protected void checkInitialized() throws IllegalStateException
    {
        if (initialized == false) {
            throw new IllegalStateException("Componet " + getClass().getName() + " hasn't been initialized yet!");
        }
    }

    /**
     * Initialize domain controller component.
     */
    public void init()
    {
        if (entityManagerFactory == null) {
            throw new IllegalStateException("Component " + getClass().getName()
                    + " doesn't have the entity manager factory set!");
        }
        initialized = true;
    }

    /**
     * Destroy domain controller component.
     */
    public void destroy()
    {
    }

    /**
     * @param interval
     * @return formatted interval to string
     */
    public static String formatInterval(Interval interval)
    {
        return interval.toString();
    }

    /**
     * {@link Component} which contains reference to current {@link Domain}.
     */
    public static class WithDomain extends Component
    {
        /**
         * @see Domain
         */
        protected Domain domain;

        /**
         * @return {@link #domain}
         */
        public Domain getDomain()
        {
            return domain;
        }

        /**
         * @param domain sets the {@link #domain}
         */
        public void setDomain(Domain domain)
        {
            this.domain = domain;
        }

        @Override
        public void init()
        {
            super.init();
            if (domain == null) {
                throw new IllegalStateException(getClass().getName() + " doesn't have the domain set!");
            }
        }
    }

    /**
     * Object extending {@link Component} can implement this interface to be aware of {@link ControllerAgent}.
     */
    public static interface ControllerAgentAware
    {
        /**
         * @param controllerAgent {@link ControllerAgent} which can be used by implementing {@link Component}
         */
        public void setControllerAgent(ControllerAgent controllerAgent);
    }
}
