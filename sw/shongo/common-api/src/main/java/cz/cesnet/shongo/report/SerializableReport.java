package cz.cesnet.shongo.report;

/**
 * Represents a {@link Report} whose parameters can be serialized.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface SerializableReport
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
