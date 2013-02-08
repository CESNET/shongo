package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class AllocatingExecutableReport extends Report
{
    /**
     * Constructor.
     */
    public AllocatingExecutableReport()
    {
    }

    @Override
    @Transient
    public String getText()
    {
        return "Allocating new executable";
    }
}
