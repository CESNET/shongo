package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.reservation.TargetedReservation;

import javax.persistence.*;

/**
 * Represents a {@link cz.cesnet.shongo.controller.booking.reservation.Reservation} for a {@link Resource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ResourceReservation extends TargetedReservation
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
    @ManyToOne(fetch = FetchType.LAZY)
    @Access(AccessType.FIELD)
    public Resource getResource()
    {
        return getLazyImplementation(resource);
    }

    /**
     * @param resource sets the {@link #resource}
     */
    public void setResource(Resource resource)
    {
        this.resource = resource;
    }

    @Override
    public cz.cesnet.shongo.controller.api.ResourceReservation toApi(EntityManager entityManager, boolean administrator)
    {
        return (cz.cesnet.shongo.controller.api.ResourceReservation) super.toApi(entityManager, administrator);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Reservation createApi()
    {
        return new cz.cesnet.shongo.controller.api.ResourceReservation();
    }

    @Override
    protected void toApi(Reservation api, EntityManager entityManager, boolean admin)
    {
        cz.cesnet.shongo.controller.api.ResourceReservation resourceReservationApi =
                (cz.cesnet.shongo.controller.api.ResourceReservation) api;
        resourceReservationApi.setResourceId(ObjectIdentifier.formatId(resource));
        resourceReservationApi.setResourceName(resource.getName());
        super.toApi(api, entityManager, admin);
    }

    @Override
    @Transient
    public Long getTargetId()
    {
        return resource.getId();
    }

    @Override
    @Transient
    public Resource getAllocatedResource()
    {
        return resource;
    }
}
