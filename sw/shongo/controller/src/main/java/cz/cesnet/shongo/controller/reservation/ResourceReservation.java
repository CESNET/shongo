package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.common.IdentifierFormat;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.report.DurationLongerThanMaximumReport;
import cz.cesnet.shongo.Temporal;
import org.joda.time.Period;

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
     * Constructor.
     */
    public ResourceReservation()
    {
    }

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

    @Override
    public void validate(Cache cache) throws ReportException
    {
        ReservationTask.checkMaximumDuration(getSlot(), cache.getResourceReservationMaximumDuration());
    }

    @Override
    public cz.cesnet.shongo.controller.api.ResourceReservation toApi()
    {
        return (cz.cesnet.shongo.controller.api.ResourceReservation) super.toApi();
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Reservation createApi()
    {
        return new cz.cesnet.shongo.controller.api.ResourceReservation();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Reservation api)
    {
        cz.cesnet.shongo.controller.api.ResourceReservation resourceReservationApi =
                (cz.cesnet.shongo.controller.api.ResourceReservation) api;
        resourceReservationApi.setResourceId(IdentifierFormat.formatGlobalId(getResource()));
        resourceReservationApi.setResourceName(getResource().getName());
        super.toApi(api);
    }
}
