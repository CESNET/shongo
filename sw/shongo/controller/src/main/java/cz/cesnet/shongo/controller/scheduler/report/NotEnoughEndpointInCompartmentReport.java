package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #toString()}
 */
@Entity
public class NotEnoughEndpointInCompartmentReport extends Report
{
    @Override
    @Transient
    public String getText()
    {
        return "Not enough endpoints are requested for the compartment.";
    }

    @Override
    @Transient
    public String getHelp()
    {
        return "Compartment must contain at least one existing resource or at least two external endpoints.";
    }
}
