package cz.cesnet.shongo.report;

/**
 * Represents a {@link AbstractReport} whose parameters can be serialized.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface SerializableReport extends Report
{
    /**
     * @param reportSerializer from which should be the {@link cz.cesnet.shongo.report.SerializableReport} parameters de-serialized
     */
    public abstract void readParameters(ReportSerializer reportSerializer);

    /**
     * @param reportSerializer to which should be the {@link cz.cesnet.shongo.report.SerializableReport} parameters serialized
     */
    public abstract void writeParameters(ReportSerializer reportSerializer);
}
