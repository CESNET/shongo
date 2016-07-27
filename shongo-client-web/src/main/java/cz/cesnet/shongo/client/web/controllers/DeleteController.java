package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.client.web.models.SpecificationType;
import cz.cesnet.shongo.client.web.support.*;
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

    /**
     * Handle view of reservation requests for deletion.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_DELETE, method = RequestMethod.GET)
    public String handleDeleteView (
            SecurityToken securityToken,
            MessageProvider messageProvider,
            @RequestParam (value = "reservationRequestId", required = false) List<String> reservationRequestIds,
            Model model)
    {
        List<ReservationRequestDeleteDetail> reservationDetailList = new ArrayList<>();

        for (String reservationRequestId : reservationRequestIds) {
            reservationDetailList.add(getReservationForDeletion(securityToken, reservationRequestId, messageProvider));
        }

        model.addAttribute("reservationRequestDeleteDetailList", reservationDetailList);
        return "reservationRequestDelete";
    }

    /**
     * Handle confirmation of reservation requests for deletion.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_DELETE, method = RequestMethod.POST)
    public String handleDeleteConfirm(
            SecurityToken securityToken,
            @RequestParam(value = "dependencies", required = false, defaultValue = "true") boolean dependencies,
            @RequestParam (value = "reservationRequestId") List<String> reservationRequestIds)
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
     * Returns detail of reservation request for deletion specified by Id.
     */
    private ReservationRequestDeleteDetail getReservationForDeletion(
            SecurityToken securityToken,
            String reservationRequestId,
            MessageProvider messageProvider)
    {
        DateTimeFormatter formatter = DateTimeFormatter.getInstance(DateTimeFormatter.SHORT, messageProvider.getLocale(), messageProvider.getTimeZone());
        ReservationRequestSummary reservationRequest =
                cache.getReservationRequestSummary(securityToken, reservationRequestId);
        SpecificationType specificationType = SpecificationType.fromReservationRequestSummary(reservationRequest);

        List<ReservationRequestSummary> dependencies =
                ReservationRequestModel.getDeleteDependencies(reservationRequestId, reservationService, securityToken);

        ReservationRequestDeleteDetail reservationRequestDeleteDetail = new ReservationRequestDeleteDetail();
        reservationRequestDeleteDetail.setReservationRequestId(reservationRequestId);
        reservationRequestDeleteDetail.setSpecificationType(specificationType);
        reservationRequestDeleteDetail.setFutureSlotCount(reservationRequest.getFutureSlotCount());
        reservationRequestDeleteDetail.setDependencies(dependencies);
        // TODO smazat a formatovat pro vsechny
//        if (SpecificationType.MEETING_ROOM.equals(specificationType)) {
        Interval slot = reservationRequest.getEarliestSlot();
        reservationRequestDeleteDetail.setSlot(formatter.formatInterval(slot));
        if (reservationRequest.getFutureSlotCount() != null && reservationRequest.getFutureSlotCount() > 0) {
            List<Reservation> reservations = reservationService.getReservationRequestReservations(securityToken, reservationRequestId);
            List<String> reservationSlots = new LinkedList<String>();
            for (Reservation reservation : reservations) {
                reservationSlots.add(formatter.formatInterval(reservation.getSlot()));
            }
            reservationRequestDeleteDetail.setReservationSlots(reservationSlots);
        }
        if (SpecificationType.PERMANENT_ROOM.equals(specificationType)) {
            reservationRequestDeleteDetail.setRoomName(reservationRequest.getRoomName());
        }
//        }
        return reservationRequestDeleteDetail;
    }

    /**
     * View model for deletion request
     */
    public class ReservationRequestDeleteDetail
    {
        /**
         * Id of the reservation request.
         */
        private String reservationRequestId;

        /**
         * see {@link SpecificationType}
         */
        private SpecificationType specificationType;

        /**
         * Dependencies of the reservation request (e.g. capacities of permanent room).
         */
        private List<ReservationRequestSummary> dependencies;

        /**
         * Formated date and time of reserved slots
         */
        private List<String> reservationSlots;

        /**
         * Slot of the reservation request.
         */
        private String slot;

        /**
         * Count of reserved future slots.
         */
        private Integer futureSlotCount;

        /**
         * Name of room
         */
        private String roomName;

        public String getReservationRequestId()
        {
            return reservationRequestId;
        }

        public void setReservationRequestId(String reservationRequestId)
        {
            this.reservationRequestId = reservationRequestId;
        }

        public String getSlot() {
            return slot;
        }

        public void setSlot(String slot) {
            this.slot = slot;
        }

        public SpecificationType getSpecificationType() {
            return specificationType;
        }

        public void setSpecificationType(SpecificationType specificationType) {
            this.specificationType = specificationType;
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

        public void setFutureSlotCount(Integer futureSlotCount)
        {
            this.futureSlotCount = futureSlotCount;
        }

        public Integer getFutureSlotCount()
        {
            return futureSlotCount;
        }

        public String getRoomName()
        {
            return roomName;
        }

        public void setRoomName(String roomName)
        {
            this.roomName = roomName;
        }
    }

}