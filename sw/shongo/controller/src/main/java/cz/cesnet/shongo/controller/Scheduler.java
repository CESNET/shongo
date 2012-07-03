package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.PrintableObject;
import cz.cesnet.shongo.controller.allocation.AllocatedCompartment;
import cz.cesnet.shongo.controller.allocation.AllocatedCompartmentManager;
import cz.cesnet.shongo.controller.allocation.AllocatedResource;
import cz.cesnet.shongo.controller.request.CompartmentRequest;
import cz.cesnet.shongo.controller.request.CompartmentRequestManager;
import cz.cesnet.shongo.controller.request.ResourceSpecification;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;

/**
 * Represents a component of a domain controller that is responsible for scheduling resources for compartment requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Scheduler extends Component
{
    private static Logger logger = LoggerFactory.getLogger(Scheduler.class);

    @Override
    public void init()
    {
        super.init();
    }

    @Override
    public void destroy()
    {
        super.destroy();
    }

    /**
     * Run scheduler for a given interval.
     *
     * @param interval
     */
    public void run(Interval interval)
    {
        checkInitialized();

        logger.debug("Running scheduler...");

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(getEntityManager());
        List<CompartmentRequest> compartmentRequests = compartmentRequestManager.listCompleted(interval);

        // TODO: Apply some priority

        for (CompartmentRequest compartmentRequest : compartmentRequests) {
            allocateCompartmentRequest(compartmentRequest, entityManager);
        }

        entityManager.getTransaction().commit();
        entityManager.close();
    }

    /**
     * Allocate compartment request.
     *
     * @param compartmentRequest
     */
    private void allocateCompartmentRequest(CompartmentRequest compartmentRequest, EntityManager entityManager)
    {
        logger.info("Allocating compartment request '{}'...", compartmentRequest.getId());

        AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(entityManager);

        // Get existing allocated compartment
        AllocatedCompartment allocatedCompartment =
                allocatedCompartmentManager.getByCompartmentRequest(compartmentRequest);
        // If doesn't exists create a new
        if (allocatedCompartment == null) {
            allocatedCompartment = new AllocatedCompartment();
            allocatedCompartment.setCompartmentRequest(compartmentRequest);
            allocatedCompartmentManager.create(allocatedCompartment);
        }

        // Get list of requested resources
        Map<ResourceSpecification, List<Person>> requestedResourcesWithPersons =
                compartmentRequest.getRequestedResourcesWithPersons();

        // Get list of already allocated resources
        List<AllocatedResource> allocatedResources = allocatedCompartment.getAllocatedResources();

        // Schedule a new allocation
        if (allocatedResources.size() == 0) {
            System.err.println(PrintableObject.toString(requestedResourcesWithPersons));

            // TODO: Allocate endpoints

            // TODO: Allocate aliases for endpoint (if needed)

            // TODO: Allocate virtual rooms and gateways for connecting endpoints

            throw new RuntimeException("TODO: Implement");
        }
        // Reschedule existing allocation
        else {
            if (true) {
                throw new RuntimeException("TODO: Implement");
            }
        }

        allocatedCompartmentManager.update(allocatedCompartment);
    }

    /**
     * Run scheduler on given entityManagerFactory and interval.
     *
     * @param entityManagerFactory
     * @param interval
     */
    public static void run(EntityManagerFactory entityManagerFactory, Interval interval)
    {
        Scheduler scheduler = new Scheduler();
        scheduler.setEntityManagerFactory(entityManagerFactory);
        scheduler.init();
        scheduler.run(interval);
        scheduler.destroy();
    }
}
