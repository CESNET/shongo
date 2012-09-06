package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.TransactionHelper;
import cz.cesnet.shongo.controller.allocation.AllocatedCompartment;
import cz.cesnet.shongo.controller.allocation.AllocatedCompartmentManager;
import cz.cesnet.shongo.controller.allocation.AllocatedItem;
import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.controller.request.CallInitiation;
import cz.cesnet.shongo.controller.request.CompartmentRequest;
import cz.cesnet.shongo.controller.request.CompartmentRequestManager;
import cz.cesnet.shongo.controller.scheduler.Task;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.util.TemporalHelper;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.util.List;

/**
 * Represents a component of a domain controller that is responsible for allocating resources
 * from a {@link CompartmentRequest} to the {@link AllocatedCompartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Scheduler extends Component
{
    private static Logger logger = LoggerFactory.getLogger(Scheduler.class);

    /**
     * @see {@link Cache}
     */
    private Cache cache;

    /**
     * @param cache sets the {@link #cache}
     */
    public void setCache(Cache cache)
    {
        this.cache = cache;
    }

    /**
     * Run scheduler on given entityManagerFactory and interval.
     *
     * @param entityManager
     * @param interval
     */
    public static void createAndRun(Interval interval, EntityManager entityManager, Cache resourceDatabase)
            throws FaultException
    {
        Scheduler scheduler = new Scheduler();
        scheduler.setCache(resourceDatabase);
        scheduler.init();
        scheduler.run(interval, entityManager);
        scheduler.destroy();
    }

    /**
     * Run scheduler for a given interval.
     *
     * @param interval
     */
    public void run(Interval interval, EntityManager entityManager) throws FaultException
    {
        logger.info("Running scheduler for interval '{}'...", TemporalHelper.formatInterval(interval));

        TransactionHelper.Transaction transaction = TransactionHelper.beginTransaction(entityManager);

        // Delete all allocated compartments which was marked for deletion
        AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(entityManager);
        allocatedCompartmentManager.deleteAllMarked(cache);

        // Set current interval as working to the cache (it will reload allocations only when
        // the interval changes)
        cache.setWorkingInterval(interval, entityManager);

        try {
            CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager);
            List<CompartmentRequest> compartmentRequests = compartmentRequestManager.listCompleted(interval);

            // TODO: Process permanent first
            // TODO: Apply some other priority to compartment requests

            for (CompartmentRequest compartmentRequest : compartmentRequests) {
                allocateCompartmentRequest(compartmentRequest, entityManager);
            }

            transaction.commit();
        }
        catch (Exception exception) {
            transaction.rollback();
            throw new FaultException(exception, ControllerFault.SCHEDULER_FAILED);
        }
    }

    /**
     * Allocate compartment request.
     *
     * @param compartmentRequest
     */
    private void allocateCompartmentRequest(CompartmentRequest compartmentRequest, EntityManager entityManager)
    {
        logger.info("Allocating compartment request '{}'...", compartmentRequest.getId());

        CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager);
        AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(entityManager);

        // Get existing allocated compartment
        AllocatedCompartment allocatedCompartment =
                allocatedCompartmentManager.getByCompartmentRequest(compartmentRequest);

        // TODO: Try to intelligently reallocate and not delete old allocation
        // Delete old allocation
        if (allocatedCompartment != null) {
            allocatedCompartmentManager.delete(allocatedCompartment, cache);
        }

        try {
            // Get requested slot and check it's maximum duration
            Interval requestedSlot = compartmentRequest.getRequestedSlot();
            if (requestedSlot.toDuration().isLongerThan(cache.getAllocatedResourceMaximumDuration())) {
                throw new FaultException("Requested slot '%s' is longer than maximum '%s'!",
                        requestedSlot.toPeriod().normalizedStandard().toString(),
                        cache.getAllocatedResourceMaximumDuration().toString());
            }

            // Get list of requested resources
            List<CompartmentRequest.RequestedResource> requestedResources =
                    compartmentRequest.getRequestedResourcesForScheduler();

            // Initialize scheduler task (by adding all requested resources to it)
            Task task = new Task(requestedSlot, cache);
            CallInitiation callInitiation = compartmentRequest.getCompartment().getCallInitiation();
            if (callInitiation != null) {
                task.setCallInitiation(callInitiation);
            }
            for (CompartmentRequest.RequestedResource requestedResource : requestedResources) {
                task.addResource(requestedResource.getResourceSpecification());
            }

            // Create new allocated compartment
            allocatedCompartment = task.createAllocatedCompartment();
            allocatedCompartment.setCompartmentRequest(compartmentRequest);

            // TODO: Add persons for allocated devices

            // Create allocated compartment
            allocatedCompartmentManager.create(allocatedCompartment);

            // Add allocated items to the cache
            for (AllocatedItem allocatedItem : allocatedCompartment.getAllocatedItems()) {
                cache.addAllocatedItem(allocatedItem);
            }

            // Set compartment state to allocated
            compartmentRequest.setState(CompartmentRequest.State.ALLOCATED);
            compartmentRequestManager.update(compartmentRequest);
        }
        catch (FaultException exception) {
            compartmentRequest.setState(CompartmentRequest.State.ALLOCATION_FAILED, exception.getMessage());
            compartmentRequestManager.update(compartmentRequest);
        }
    }
}
