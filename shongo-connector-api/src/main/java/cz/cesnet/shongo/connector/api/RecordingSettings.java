package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.Recording;
import jade.content.Concept;

/**
 * Settings for starting new {@link Recording}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RecordingSettings implements Concept
{
    /**
     * PIN which should be used when dialing from the recording endpoint.
     */
    private String pin;

    /**
     * Bitrate for recording.
     */
    private String bitrate;

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

    /**
     *
     * @return {@link #bitrate}
     */
    public String getBitrate()
    {
        return bitrate;
    }

    /**
     *
     * @param bitrate sets the {@link #bitrate}
     */
    public void setBitrate(String bitrate)
    {
        this.bitrate = bitrate;
    }

    @Override
    public String toString()
    {
        return String.format(RecordingSettings.class.getSimpleName() + " (pin: %s)", pin);
    }
}
