package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.AbstractManager;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Manager for {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentManager extends AbstractManager
{
    /**
     * @see CompartmentRequestManager
     */
    private CompartmentRequestManager compartmentRequestManager;

    /**
     * Constructor.
     * @param entityManager
     * @param compartmentRequestManager
     */
    private CompartmentManager(EntityManager entityManager, CompartmentRequestManager compartmentRequestManager)
    {
        super(entityManager);
        this.compartmentRequestManager = compartmentRequestManager;
    }

    /**
     * @param entityManager
     * @return new instance of {@link CompartmentManager}
     */
    public static CompartmentManager createInstance(EntityManager entityManager)
    {
        return createInstance(entityManager, CompartmentRequestManager.createInstance(entityManager));
    }

    /**
     * @param entityManager
     * @param compartmentRequestManager
     * @return new instance of {@link CompartmentManager}
     */
    public static CompartmentManager createInstance(EntityManager entityManager, CompartmentRequestManager compartmentRequestManager)
    {
        return new CompartmentManager(entityManager, compartmentRequestManager);
    }

    /**
     * @param compartment compartment to be removed from the database
     */
    public void delete(Compartment compartment)
    {
        // Delete all compartment request that belongs to compartment
        List<CompartmentRequest> compartmentRequestList = compartmentRequestManager.list(compartment);
        for ( CompartmentRequest compartmentRequest : compartmentRequestList ) {
            compartmentRequestManager.delete(compartmentRequest);
        }
        super.delete(compartment);
    }
    /**
     * Remove all existing compartments which doesn't don't belong to any reservation request.
     */
    public void deleteAllWithoutReservationRequest()
    {
        List<Compartment> compartmentList = entityManager.createQuery(
                "SELECT compartment FROM Compartment compartment WHERE compartment.reservationRequest IS NULL",
                Compartment.class).getResultList();
        for (Compartment compartment : compartmentList) {
            delete(compartment);
        }
    }
}
