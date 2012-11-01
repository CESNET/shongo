package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.executor.Compartment;
import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class AllocatingCompartmentReport extends Report
{
    /**
     * @see cz.cesnet.shongo.controller.executor.Compartment
     */
    private Compartment compartment;

    /**
     * Constructor.
     */
    public AllocatingCompartmentReport()
    {
    }

    /**
     * Constructor.
     *
     * @param compartment
     */
    public AllocatingCompartmentReport(Compartment compartment)
    {
        this.compartment = compartment;
    }

    /**
     * @return {@link #compartment}
     */
    @OneToOne(cascade = CascadeType.PERSIST)
    @Access(AccessType.FIELD)
    public Compartment getCompartment()
    {
        return compartment;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Allocating compartment for %d endpoints.", compartment.getTotalEndpointCount());
    }
}
