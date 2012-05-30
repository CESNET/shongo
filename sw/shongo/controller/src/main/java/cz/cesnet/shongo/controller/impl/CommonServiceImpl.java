package cz.cesnet.shongo.controller.impl;

import cz.cesnet.shongo.controller.api.CommonService;
import cz.cesnet.shongo.controller.api.ControllerInfo;

import javax.annotation.Resource;
import javax.persistence.EntityManager;

/**
 * Room service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommonServiceImpl implements CommonService
{
    @Resource
    private EntityManager entityManager;

    @Override
    public String getServiceName()
    {
        return "Common";
    }

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
