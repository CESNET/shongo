package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.compartment.CompartmentManager;
import cz.cesnet.shongo.controller.request.DateTimeSlotSpecification;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.joda.time.Interval;

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
        implements CompartmentService, Component.EntityManagerFactoryAware, Component.DomainAware
{
    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.Domain
     */
    private cz.cesnet.shongo.controller.Domain domain;

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
    public void init(Configuration configuration)
    {
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        checkDependency(domain, cz.cesnet.shongo.controller.Domain.class);
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
        Long reservationRequestId = domain.parseIdentifier(compartmentIdentifier);

        try {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            entityManager.getTransaction().begin();

            CompartmentManager compartmentManager = new CompartmentManager(entityManager);

            cz.cesnet.shongo.controller.compartment.Compartment compartment =
                    compartmentManager.get(reservationRequestId);

            compartmentManager.delete(compartment);

            entityManager.getTransaction().commit();
            entityManager.close();
        } catch (javax.persistence.RollbackException exception) {
            throw new FaultException("Compartment '" + reservationRequestId.toString() +
                    "' cannot be deleted (it is still referenced).");
        }
    }

    @Override
    public Collection<CompartmentSummary> listCompartments(SecurityToken token)
    {
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
