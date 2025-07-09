package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.*;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AbstractDetailController
{
    @Resource
    protected ReservationService reservationService;

    @Resource
    protected ExecutableService executableService;

    @Resource
    protected Cache cache;

    @Resource
    protected MessageSource messageSource;

    protected String getReservationRequestId(SecurityToken securityToken, String objectId)
    {
        return cache.getReservationRequestId(securityToken, objectId);
    }

    protected String getExecutableId(SecurityToken securityToken, String objectId)
    {
        return cache.getExecutableId(securityToken, objectId);
    }

    protected Executable getExecutable(SecurityToken securityToken, String objectId)
    {
        String executableId = cache.getExecutableId(securityToken, objectId);
        return cache.getExecutable(securityToken, executableId);
    }

    protected AbstractRoomExecutable getRoomExecutable(SecurityToken securityToken, String objectId)
    {
        Executable executable = getExecutable(securityToken, objectId);
        if (executable instanceof AbstractRoomExecutable) {
            return (AbstractRoomExecutable) executable;
        }
        else {
            throw new UnsupportedApiException(executable);
        }
    }
}
