package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.api.xmlrpc.StructType;

/**
 * Represents a base information about controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerInfo implements StructType
{
    public String name;

    public String description;
}
