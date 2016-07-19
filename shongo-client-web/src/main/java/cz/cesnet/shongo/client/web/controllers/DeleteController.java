package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebNavigation;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.client.web.models.SpecificationType;
import cz.cesnet.shongo.client.web.support.*;
import cz.cesnet.shongo.client.web.support.interceptors.NavigationInterceptor;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.Interval;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Controller for reverting and deleting reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class DeleteController
{
    @Resource
    private ReservationService reservationService;

    @Resource
    private Cache cache;

    /**
     * Handle revert of reservation request.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_REVERT, method = RequestMethod.GET)
    public String handleRevert(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        // Get reservation request
        reservationRequestId = reservationService.revertReservationRequest(securityToken, reservationRequestId);
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.DETAIL_VIEW, reservationRequestId);
    }


    private ReservationRequestDetail getReservationForDeletion (
            SecurityToken securityToken,
            String reservationRequestId,
            MessageProvider messageProvider)
    {
        DateTimeFormatter formatter = DateTimeFormatter.getInstance(DateTimeFormatter.SHORT, messageProvider.getLocale(), messageProvider.getTimeZone());
        ReservationRequestSummary reservationRequest =
                cache.getReservationRequestSummary(securityToken, reservationRequestId);
        SpecificationType specificationType = SpecificationType.fromReservationRequestSummary(reservationRequest);
        String roomName;
        if (SpecificationType.MEETING_ROOM.equals(specificationType)) {
            String resourceId = reservationRequest.getResourceId();
            ResourceSummary resource = cache.getResourceSummary(securityToken, resourceId);
            if (resource == null) {
                throw new IllegalStateException("Summary for resource " + resourceId + " cannot be retrieved.");
            }
            roomName = resource.getName();
        }
        else {
            roomName = reservationRequest.getRoomName();
        }
        String title = messageProvider.getMessage("views.reservationRequestDelete.title",
                messageProvider.getMessage("views.specificationType.forWithName." + specificationType,
                        roomName != null ? roomName : ""));
        List<ReservationRequestSummary> dependencies =
                ReservationRequestModel.getDeleteDependencies(reservationRequestId, reservationService, securityToken);


        ReservationRequestDetail reservationRequestDetail = new ReservationRequestDetail();
        reservationRequestDetail.setId(reservationRequestId);
        reservationRequestDetail.setTitleDescription(title);
        reservationRequestDetail.setSpecificationType(specificationType);
        reservationRequestDetail.setReservationRequest(reservationRequest);
        reservationRequestDetail.setDependencies(dependencies);
        if (SpecificationType.MEETING_ROOM.equals(specificationType)) {
            Interval slot = reservationRequest.getEarliestSlot();
            reservationRequestDetail.setSlot(formatter.formatInterval(slot));
            if (reservationRequest.getFutureSlotCount() != null && reservationRequest.getFutureSlotCount() > 0) {
                List<Reservation> reservations = reservationService.getReservationRequestReservations(securityToken, reservationRequestId);
                List<String> reservationSlots = new LinkedList<String>();
                for (Reservation reservation : reservations) {
                    reservationSlots.add(formatter.formatInterval(reservation.getSlot()));
                }
                reservationRequestDetail.setReservationSlots(reservationSlots);
            }
        }
        return reservationRequestDetail;
    }

    /**
     * Handle view of reservation requests for deletion.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_DELETE, method = RequestMethod.GET)
    public String handleDeleteMultipleView (
            HttpServletRequest request,
            SecurityToken securityToken,
            MessageProvider messageProvider,
            @RequestParam (value = "reservationRequestId", required = false) List<String> reservationRequestIds,
            Model model)
    {
        List<ReservationRequestDetail> reservationsDetail = new ArrayList<>();

        for (String reservationRequestId : reservationRequestIds) {
            reservationsDetail.add(getReservationForDeletion(securityToken, reservationRequestId, messageProvider));
        }
        model.addAttribute("reservationsDetail", reservationsDetail);
        return "reservationRequestDeleteMultiple";
    }

    /**
     * Handle confirmation of reservation requests for deletion.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_DELETE, method = RequestMethod.POST)
    public String handleDeleteMultipleConfirm(
            HttpServletRequest request,
            SecurityToken securityToken,
            @RequestParam(value = "dependencies", required = false, defaultValue = "false") boolean dependencies,
            @RequestParam (value = "reservationRequestId", required = false) List<String> reservationRequestIds)
    {
        for (String reservationRequestId : reservationRequestIds) {
            if (dependencies) {
                List<ReservationRequestSummary> reservationRequestDependencies =
                        ReservationRequestModel.getDeleteDependencies(
                                reservationRequestId, reservationService, securityToken);
                for (ReservationRequestSummary reservationRequestSummary : reservationRequestDependencies) {
                    reservationService.deleteReservationRequest(securityToken, reservationRequestSummary.getId());
                }
            }
            reservationService.deleteReservationRequest(securityToken, reservationRequestId);
        }
        return "redirect:" + ClientWebUrl.HOME;
    }


    /**
     * View model for deletion request
     */
    public class ReservationRequestDetail {

        private String id;

        private String titleDescription;

        private SpecificationType specificationType;

        private ReservationRequestSummary reservationRequest;

        private List<ReservationRequestSummary> dependencies;

        /**
         * Formated date and time of reserved slots
         */
        private List<String> reservationSlots;

        /**
         * Slot for which Reservation is allocated.
         */
        private String slot;


        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSlot() {
            return slot;
        }

        public void setSlot(String slot) {
            this.slot = slot;
        }

        public String getTitleDescription() {
            return titleDescription;
        }

        public void setTitleDescription(String titleDescription) {
            this.titleDescription = titleDescription;
        }

        public SpecificationType getSpecificationType() {
            return specificationType;
        }

        public void setSpecificationType(SpecificationType specificationType) {
            this.specificationType = specificationType;
        }

        public ReservationRequestSummary getReservationRequest() {
            return reservationRequest;
        }

        public void setReservationRequest(ReservationRequestSummary reservationRequest) {
            this.reservationRequest = reservationRequest;
        }

        public List<ReservationRequestSummary> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<ReservationRequestSummary> dependencies) {
            this.dependencies = dependencies;
        }

        public List<String> getReservationSlots() {
            return reservationSlots;
        }

        public void setReservationSlots(List<String> reservationSlots) {
            this.reservationSlots = reservationSlots;
        }
    }

}