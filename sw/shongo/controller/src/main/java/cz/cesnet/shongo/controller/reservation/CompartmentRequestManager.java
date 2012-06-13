package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.AbsoluteDateTimeSlot;
import cz.cesnet.shongo.common.AbstractManager;
import cz.cesnet.shongo.common.Identifier;
import cz.cesnet.shongo.common.Person;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * A reservation database for domain controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentRequestManager extends AbstractManager
{
    /**
     * @see PersonRequestManager
     */
    PersonRequestManager personRequestManager;

    /**
     * Constructor.
     * @param entityManager
     * @param personRequestManager
     */
    private CompartmentRequestManager(EntityManager entityManager, PersonRequestManager personRequestManager)
    {
        super(entityManager);
        this.personRequestManager = personRequestManager;
    }

    /**
     * @param entityManager
     * @return new instance of {@link CompartmentRequestManager}
     */
    public static CompartmentRequestManager createInstance(EntityManager entityManager)
    {
        return createInstance(entityManager, PersonRequestManager.createInstance(entityManager));
    }

    /**
     * @param entityManager
     * @param personRequestManager
     * @return new instance of {@link CompartmentRequestManager}
     */
    public static CompartmentRequestManager createInstance(EntityManager entityManager, PersonRequestManager personRequestManager)
    {
        return new CompartmentRequestManager(entityManager, personRequestManager);
    }

    /**
     * Create a new compartment request for a reservation request.
     *
     * @param compartment   compartment which will be requested in the compartment request
     * @param requestedSlot date/time slot for which the compartment request will be created
     * @return created compartment request
     */
    public CompartmentRequest create(Compartment compartment, AbsoluteDateTimeSlot requestedSlot)
    {
        // Create compartment request
        CompartmentRequest compartmentRequest = new CompartmentRequest();
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

        // TODO: Notify scheduler about compartment request creation

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

        // TODO: Notify scheduler about compartment request modification
    }

    /**
     * Remove compartment request.
     *
     * @param compartmentRequest
     */
    public void delete(CompartmentRequest compartmentRequest)
    {
        super.delete(compartmentRequest);

        // TODO: Notify scheduler about compartment request removal
    }

    /**
     * @param reservationRequestIdentifier
     * @return list of existing compartment requests for a {@link ReservationRequest} with the given identifier
     */
    public List<CompartmentRequest> list(Identifier reservationRequestIdentifier)
    {
        // Get existing compartment requests for compartment
        List<CompartmentRequest> compartmentRequestList = entityManager.createQuery(
                "SELECT request FROM CompartmentRequest request " +
                        "WHERE request.compartment.reservationRequest.identifierAsString = :identifier",
                CompartmentRequest.class).setParameter("identifier", reservationRequestIdentifier.toString())
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
