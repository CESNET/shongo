package cz.cesnet.shongo;

import cz.cesnet.shongo.report.AbstractReport;
import jade.content.Concept;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class JadeReport extends AbstractReport implements Concept
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
