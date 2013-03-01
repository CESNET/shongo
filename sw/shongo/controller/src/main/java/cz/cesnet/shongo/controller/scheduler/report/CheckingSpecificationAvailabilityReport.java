package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class CheckingSpecificationAvailabilityReport extends Report
{
    /**
     * Constructor.
     */
    public CheckingSpecificationAvailabilityReport()
    {
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Checking specification availability report.");
    }
}
