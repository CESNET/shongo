package cz.cesnet.shongo;

import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.report.Reportable;
import cz.cesnet.shongo.report.SerializableReport;
import jade.content.Concept;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class JadeReport extends Report implements Concept
{
    /**
     * Identifier which can be used to store unique identifier of the {@link JadeReport}.
     */
    public Long id;

    /**
     * Constructor.
     */
    public JadeReport()
    {
    }
}