package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.request.Specification;
import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class SpecificationNotAllocatableReport extends Report
{
    /**
     * Identification of resource.
     */
    private Specification specification;

    /**
     * Constructor.
     */
    public SpecificationNotAllocatableReport()
    {
    }

    /**
     * Constructor.
     *
     * @param specification
     */
    public SpecificationNotAllocatableReport(Specification specification)
    {
        this.specification = specification;
    }

    /**
     * @return {@link #specification}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public Specification getSpecification()
    {
        return specification;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("The specification of type '%s' is not supposed to be allocated.",
                specification.getClass().getSimpleName());
    }
}
