package cz.cesnet.shongo.controller.oldrequest;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.controller.allocation.AllocatedCompartment;
import cz.cesnet.shongo.controller.allocation.AllocatedCompartmentManager;
import cz.cesnet.shongo.controller.allocation.AllocatedExternalEndpoint;
import cz.cesnet.shongo.controller.allocation.AllocatedItem;
import cz.cesnet.shongo.controller.common.Person;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.*;

/**
 * Manager for {@link CompartmentRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see AbstractManager
 */
public class CompartmentRequestManager extends AbstractManager
{
    /**
     * Constructor.
     *
     * @param entityManager
     */
    public CompartmentRequestManager(EntityManager entityManager)
    {
        super(entityManager);
    }


}
