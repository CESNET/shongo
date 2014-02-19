package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.*;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.Breadcrumb;
import cz.cesnet.shongo.client.web.support.BreadcrumbProvider;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.client.web.support.NavigationPage;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.*;

/**
 * Controller for displaying detail.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class DetailController extends AbstractDetailController implements BreadcrumbProvider
{
    /**
     * {@link cz.cesnet.shongo.client.web.support.Breadcrumb} for the {@link #handleDetailView}
     */
    private Breadcrumb breadcrumb;

    @Override
    public Breadcrumb createBreadcrumb(NavigationPage navigationPage, String requestURI)
    {
        if (navigationPage == null) {
            return null;
        }
        if (ClientWebNavigation.DETAIL.isNavigationPage(navigationPage)) {
            breadcrumb = new Breadcrumb(navigationPage.getParentNavigationPage(), requestURI);
            return breadcrumb;
        }
        return new Breadcrumb(navigationPage, requestURI);
    }

    /**
     * Handle detail view.
     */
    @RequestMapping(value = ClientWebUrl.DETAIL_VIEW, method = RequestMethod.GET)
    public ModelAndView handleDetailView(
            SecurityToken securityToken,
            Locale locale,
            @PathVariable(value = "objectId") String objectId,
            @RequestParam(value = "tab", required = false) String tab)
    {
        String reservationRequestId = getReservationRequestId(securityToken, objectId);
        AbstractReservationRequest abstractReservationRequest =
                reservationService.getReservationRequest(securityToken, reservationRequestId);
        ReservationRequestModel reservationRequestModel = new ReservationRequestModel(
                abstractReservationRequest, new CacheProvider(cache, securityToken));
        ReservationRequestSummary reservationRequestSummary =
                cache.getReservationRequestSummary(securityToken, reservationRequestId);
        SpecificationType specificationType = reservationRequestModel.getSpecificationType();
        ModelAndView modelAndView = new ModelAndView("detail");
        modelAndView.addObject("tab", tab);
        modelAndView.addObject("titleDescription", messageSource.getMessage("views.detail.title." + specificationType,
                new Object[]{reservationRequestSummary.getRoomName()}, locale));

        // Initialize breadcrumb
        if (breadcrumb != null) {
            breadcrumb.addItems(reservationRequestModel.getBreadcrumbItems(ClientWebUrl.DETAIL_VIEW));
        }

        return modelAndView;
    }
}
