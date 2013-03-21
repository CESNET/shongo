package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.resource.Resource;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class ResourceNotAvailableReport extends ResourceReport
{
    /**
     * Maximum date/time for which the resource is available.
     */
    private DateTime maxDateTime;

    /**
     * Constructor.
     */
    public ResourceNotAvailableReport()
    {
    }

    /**
     * Constructor.
     *
     * @param resource
     * @param maxDateTime
     */
    public ResourceNotAvailableReport(Resource resource, DateTime maxDateTime)
    {
        super(resource);
        this.maxDateTime = maxDateTime;
    }

    /**
     * @return {@link #maxDateTime}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getMaxDateTime()
    {
        return maxDateTime;
    }

    /**
     * @param maxDateTime sets the {@link #maxDateTime}
     */
    public void setMaxDateTime(DateTime maxDateTime)
    {
        this.maxDateTime = maxDateTime;
    }

    @Override
    @Transient
    public State getState()
    {
        return State.ERROR;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("%s is not available for the requested time slot." +
                " The maximum date/time for which the resource can be allocated is %s.",
                getResourceDescription(true), Temporal.formatDateTime(maxDateTime));
    }
}
