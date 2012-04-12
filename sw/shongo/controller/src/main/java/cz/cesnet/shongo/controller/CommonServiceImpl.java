package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.ControllerInfo;

/**
 * Room service implementation.
 *
 * @author Martin Srom
 */
public class CommonServiceImpl implements CommonService
{
    @Override
    public ControllerInfo getControllerInfo()
    {
        ControllerInfo controllerInfo = new ControllerInfo(
                "Debugging Controller",
                "Controller platform used for debugging purposes"
        );
        return controllerInfo;
    }
}
