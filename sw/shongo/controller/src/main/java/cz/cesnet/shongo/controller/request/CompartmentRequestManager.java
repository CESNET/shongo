package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.common.AbstractManager;
import cz.cesnet.shongo.common.Person;
import cz.cesnet.shongo.controller.Scheduler;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Manager for {@link CompartmentRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see AbstractManager
 */
public class CompartmentRequestManager extends AbstractManager
{
    /**
     * @see Scheduler
     */
    private Scheduler scheduler;

    /**
     * @see PersonRequestManager
     */
    private PersonRequestManager personRequestManager;

    /**
     * Constructor.
     *
     * @param entityManager
     */
    public CompartmentRequestManager(EntityManager entityManager, Scheduler scheduler)
    {
        this(entityManager, scheduler, new PersonRequestManager(entityManager));
    }

    /**
     * Constructor.
     *
     * @param entityManager
     * @param personRequestManager
     */
    public CompartmentRequestManager(EntityManager entityManager, Scheduler scheduler,
            PersonRequestManager personRequestManager)
    {
        super(entityManager);
        this.scheduler = scheduler;
        this.personRequestManager = personRequestManager;
    }

    /**
     * Create a new compartment request for a reservation request.
     *
     * @param compartment   compartment which will be requested in the compartment request
     * @param requestedSlot date/time slot for which the compartment request will be created
     * @return created compartment request
     */
    public CompartmentRequest create(Compartment compartment, Interval requestedSlot)
    {
        // Create compartment request
        CompartmentRequest compartmentRequest = new CompartmentRequest();
        compartmentRequest.setReservationRequest(compartment.getReservationRequest());
        compartmentRequest.setCompartment(compartment);
        compartmentRequest.setRequestedSlot(requestedSlot);

        // Add requested persons by requested resources
        for (ResourceSpecification resourceSpecification : compartment.getRequestedResources()) {
            for (Person person : resourceSpecification.getRequestedPersons()) {
                // TODO: Check whether person isn't already in compartment (may be do the check in reservation request)
                PersonRequest personRequest = personRequestManager.create(person, resourceSpecification);
                compartmentRequest.addRequestedPerson(personRequest);
            }
        }

        // Add directly requested persons
        for (Person person : compartment.getRequestedPersons()) {
            PersonRequest personRequest = personRequestManager.create(person, null);
            compartmentRequest.addRequestedPerson(personRequest);
        }

        // Set proper compartment request state
        compartmentRequest.updateState();

        super.create(compartmentRequest);

        scheduler.onNewCompartmentRequest(compartmentRequest);

        return compartmentRequest;
    }

    /**
     * Modify compartment request.
     *
     * @param compartmentRequest
     * @param compartment
     */
    public void update(CompartmentRequest compartmentRequest, Compartment compartment)
    {
        //throw new RuntimeException("TODO: Implement compartment request check");

        super.update(compartmentRequest);

        scheduler.onUpdateCompartmentRequest(compartmentRequest);
    }

    /**
     * Remove compartment request.
     *
     * @param compartmentRequest
     */
    public void delete(CompartmentRequest compartmentRequest)
    {
        scheduler.onDeleteCompartmentRequest(compartmentRequest);

        super.delete(compartmentRequest);
    }

    /**
     * @param reservationRequest
     * @return list of existing compartment requests for a {@link ReservationRequest}
     */
    public List<CompartmentRequest> list(ReservationRequest reservationRequest)
    {
        // Get existing compartment requests for compartment
        List<CompartmentRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT request FROM CompartmentRequest request " +
                        "WHERE request.reservationRequest.identifierAsString = :identifier",
                CompartmentRequest.class).setParameter("identifier", reservationRequest.getIdentifierAsString())
                .getResultList();
        return compartmentRequestList;
    }

    /**
     * @param compartment
     * @return list of existing compartments request from a given compartment
     */
    public List<CompartmentRequest> list(Compartment compartment)
    {
        List<CompartmentRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT request FROM CompartmentRequest request WHERE request.compartment = :compartment",
                CompartmentRequest.class).setParameter("compartment", compartment).getResultList();
        return compartmentRequestList;
    }
}
