package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.CommonService;
import cz.cesnet.shongo.controller.api.ControllerInfo;
import org.springframework.beans.factory.annotation.Required;

import javax.annotation.Resource;
import javax.persistence.*;
import java.util.List;

/**
 * Room service implementation.
 *
 * @author Martin Srom
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
