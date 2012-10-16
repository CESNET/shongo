package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.compartment.CompartmentManager;
import cz.cesnet.shongo.fault.EntityToDeleteIsReferencedException;
import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reservation service implementation
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentServiceImpl extends Component
        implements CompartmentService, Component.EntityManagerFactoryAware, Component.DomainAware,
                   Component.AuthorizationAware
{
    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.Domain
     */
    private cz.cesnet.shongo.controller.Domain domain;

    /**
     * @see Authorization
     */
    private Authorization authorization;

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void setDomain(cz.cesnet.shongo.controller.Domain domain)
    {
        this.domain = domain;
    }

    @Override
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        checkDependency(domain, cz.cesnet.shongo.controller.Domain.class);
        checkDependency(authorization, Authorization.class);
        super.init(configuration);
    }

    @Override
    public String getServiceName()
    {
        return "Compartment";
    }

    @Override
    public void deleteCompartment(SecurityToken token, String compartmentIdentifier) throws FaultException
    {
        authorization.validate(token);

        Long compartmentId = domain.parseIdentifier(compartmentIdentifier);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        try {
            CompartmentManager compartmentManager = new CompartmentManager(entityManager);

            cz.cesnet.shongo.controller.compartment.Compartment compartment =
                    compartmentManager.get(compartmentId);

            compartmentManager.delete(compartment);

            entityManager.getTransaction().commit();
        }
        catch (javax.persistence.RollbackException exception) {
            throw new EntityToDeleteIsReferencedException(Compartment.class, compartmentId);
        }
        catch (FaultException exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw exception;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Collection<CompartmentSummary> listCompartments(SecurityToken token)
    {
        authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        CompartmentManager compartmentManager = new CompartmentManager(entityManager);

        List<cz.cesnet.shongo.controller.compartment.Compartment> list = compartmentManager.list();
        List<CompartmentSummary> summaryList = new ArrayList<CompartmentSummary>();
        for (cz.cesnet.shongo.controller.compartment.Compartment compartment : list) {
            CompartmentSummary summary = new CompartmentSummary();
            summary.setIdentifier(domain.formatIdentifier(compartment.getId()));
            summary.setSlot(compartment.getSlot());
            summary.setState(compartment.getState().toApi());
            summaryList.add(summary);
        }

        entityManager.close();

        return summaryList;
    }

    @Override
    public Compartment getCompartment(SecurityToken token, String compartmentIdentifier) throws FaultException
    {
        authorization.validate(token);

        Long id = domain.parseIdentifier(compartmentIdentifier);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        CompartmentManager compartmentManager = new CompartmentManager(entityManager);

        cz.cesnet.shongo.controller.compartment.Compartment compartment =
                compartmentManager.get(id);
        Compartment compartmentApi = compartment.toApi(domain);

        entityManager.close();

        return compartmentApi;
    }
}
