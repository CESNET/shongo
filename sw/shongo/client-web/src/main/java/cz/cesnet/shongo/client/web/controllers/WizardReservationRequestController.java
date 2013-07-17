package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.WizardPage;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.List;

/**
 * Controller for displaying wizard interface.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class WizardReservationRequestController extends AbstractWizardController
{
    @Resource
    private ReservationService reservationService;

    private static enum Page
    {
        RESERVATION_REQUEST,
        RESERVATION_REQUEST_DETAIL
    }

    @Override
    protected void initWizardPages(List<WizardPage> wizardPages)
    {
        wizardPages.add(WizardController.createSelectWizardPage());
        wizardPages.add(new WizardPage(Page.RESERVATION_REQUEST, ClientWebUrl.WIZARD_RESERVATION_REQUEST_LIST,
                "views.wizard.page.reservationRequestList"));
        wizardPages.add(new WizardPage(Page.RESERVATION_REQUEST_DETAIL, ClientWebUrl.WIZARD_RESERVATION_REQUEST_DETAIL,
                "views.wizard.page.reservationRequestDetail"));
    }

    /**
     * Display list of reservation requests.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_RESERVATION_REQUEST_LIST, method = RequestMethod.GET)
    public ModelAndView handleReservationRequestList()
    {
        return getWizardView(Page.RESERVATION_REQUEST, "wizardReservationRequestList.jsp");
    }

    /**
     * Display detail of reservation request with given {@code reservationRequestId}.
     *
     * @param reservationRequestId
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_RESERVATION_REQUEST_DETAIL, method = RequestMethod.GET)
    public ModelAndView handleReservationRequestDetail(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            Model model)
    {
        ReservationRequestModel reservationRequest = new ReservationRequestModel(
                reservationService.getReservationRequest(securityToken, reservationRequestId));
        model.addAttribute("reservationRequest", reservationRequest);
        return getWizardView(Page.RESERVATION_REQUEST_DETAIL, "wizardReservationRequestDetail.jsp");
    }
}
