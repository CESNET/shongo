package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.controller.booking.domain.Domain;
import cz.cesnet.shongo.controller.booking.reservation.TargetedReservation;

import javax.persistence.*;

/**
 * Represents a {@link cz.cesnet.shongo.controller.booking.reservation.Reservation} for a foreign {@link Resource}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@Entity
public class ForeignResourceReservation extends TargetedReservation
{
    private ForeignResources foreignResources;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @Access(AccessType.FIELD)
    @JoinColumn(name = "foreign_resources_id")
    public ForeignResources getForeignResources()
    {
        return foreignResources;
    }

    public void setForeignResources(ForeignResources foreignResources)
    {
        foreignResources.validate();
        if (foreignResources.getForeignResourceId() == null) {
            throw new CommonReportSet.ObjectInvalidException(getClass().getSimpleName(),
                    "Resource ID has to be set.");
        }
        this.foreignResources = foreignResources;
    }

    @Override
    @Transient
    public Long getTargetId()
    {
        return null;
    }
}
