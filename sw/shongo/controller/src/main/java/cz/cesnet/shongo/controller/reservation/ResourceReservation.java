package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.common.EntityIdentifier;
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
     * Constructor.
     */
    public ResourceReservation()
    {
    }

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

    @Override
    public cz.cesnet.shongo.controller.api.ResourceReservation toApi(boolean admin)
    {
        return (cz.cesnet.shongo.controller.api.ResourceReservation) super.toApi(admin);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Reservation createApi()
    {
        return new cz.cesnet.shongo.controller.api.ResourceReservation();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Reservation api, boolean admin)
    {
        cz.cesnet.shongo.controller.api.ResourceReservation resourceReservationApi =
                (cz.cesnet.shongo.controller.api.ResourceReservation) api;
        resourceReservationApi.setResourceId(EntityIdentifier.formatId(resource));
        resourceReservationApi.setResourceName(resource.getName());
        super.toApi(api, admin);
    }
}
