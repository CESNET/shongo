package cz.cesnet.shongo.controller.api;

/**
 * Represents information about controller.
 *
 * @author Martin Srom
 */
public class ControllerInfo
{
    /**
     * Name of controller.
     */
    private String name;

    /**
     * Description of controller.
     */
    private String description;

    public ControllerInfo(String name, String description)
    {
        this.name = name;
        this.description = description;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }
}
