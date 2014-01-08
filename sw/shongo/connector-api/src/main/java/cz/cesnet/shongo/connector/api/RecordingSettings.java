package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.Recording;

/**
 * Settings for starting new {@link Recording}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RecordingSettings
{
    /**
     * PIN which should be used when dialing from the recording endpoint.
     */
    private String pin;

    /**
     * @return {@link #pin}
     */
    public String getPin()
    {
        return pin;
    }

    /**
     * @param pin sets the {@link #pin}
     */
    public void setPin(String pin)
    {
        this.pin = pin;
    }

    @Override
    public String toString()
    {
        return String.format(RecordingSettings.class.getSimpleName() + " (pin: %s)", pin);
    }
}
