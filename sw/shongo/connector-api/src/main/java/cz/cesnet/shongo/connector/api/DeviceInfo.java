package cz.cesnet.shongo.connector.api;

/**
 * Brief static info about a device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DeviceInfo
{
    String name;
    String description;

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
