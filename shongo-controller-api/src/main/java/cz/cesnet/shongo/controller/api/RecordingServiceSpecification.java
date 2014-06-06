package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

/**
 * Specification of a service for an {@link cz.cesnet.shongo.controller.api.Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RecordingServiceSpecification extends ExecutableServiceSpecification
{
    /**
     * Constructor.
     */
    public RecordingServiceSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param enabled sets the {@link #enabled}
     */
    public RecordingServiceSpecification(boolean enabled)
    {
        setEnabled(enabled);
    }

    public static RecordingServiceSpecification forResource(String resourceId, boolean enabled)
    {
        RecordingServiceSpecification recordingServiceSpecification = new RecordingServiceSpecification(enabled);
        recordingServiceSpecification.setResourceId(resourceId);
        return recordingServiceSpecification;
    }

    public static RecordingServiceSpecification forExecutable(String executableId, boolean enabled)
    {
        RecordingServiceSpecification recordingServiceSpecification = new RecordingServiceSpecification(enabled);
        recordingServiceSpecification.setExecutableId(executableId);
        return recordingServiceSpecification;
    }
}
