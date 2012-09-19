package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

/**
 * Represents a {@link Reservation} for a {@link Resource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ResourceReservation extends Reservation
{
    /**
     * {@link Resource} which is allocated.
     */
    private Resource resource;

    /**
     * @return {@link #resource}
     */
    @OneToOne
    public Resource getResource()
    {
        return resource;
    }

    /**
     * @param resource sets the {@link #resource}
     */
    public void setResource(Resource resource)
    {
        this.resource = resource;
    }

    /**
     * Add child {@link Reservation}s for all {@link Resource} parents.
     *
     * @param cacheTransaction to be checked if the resource parent isn't already there
     */
    public void addChildReservationsForResourceParents(Cache.Transaction cacheTransaction)
    {
        Resource parentResource = resource.getParentResource();
        if (parentResource != null && !cacheTransaction.containsResource(parentResource)) {
            ResourceReservation resourceReservation = new ResourceReservation();
            resourceReservation.setSlot(getSlot());
            resourceReservation.setResource(parentResource);
            addChildReservation(resourceReservation);

            cacheTransaction.addReservation(resourceReservation);

            resourceReservation.addChildReservationsForResourceParents(cacheTransaction);
        }
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Reservation createApi()
    {
        return new cz.cesnet.shongo.controller.api.ResourceReservation();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Reservation api, Domain domain)
    {
        cz.cesnet.shongo.controller.api.ResourceReservation resourceReservationApi =
                (cz.cesnet.shongo.controller.api.ResourceReservation) api;
        resourceReservationApi.setResourceIdentifier(domain.formatIdentifier(getResource().getId()));
        resourceReservationApi.setName(getResource().getName());
        super.toApi(api, domain);
    }
}
