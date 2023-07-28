package cz.cesnet.shongo.controller.booking.request.auxdata;

import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.request.auxdata.tagdata.TagData;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;

public class AuxDataService
{

    public static <T extends TagData<?>> List<T> getTagData(
            AbstractReservationRequest reservationRequest,
            AuxDataFilter filter, EntityManager entityManager
    )
    {
        List<TagData<?>> tagData = getAllTagData(reservationRequest, entityManager);

        return tagData
                .stream()
                .filter(data -> data.filter(filter))
                .map(data -> (T) data)
                .collect(Collectors.toList());
    }

    private static List<TagData<?>> getAllTagData(
            AbstractReservationRequest reservationRequest,
            EntityManager entityManager
    )
    {
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        return reservationRequestManager.getAllAuxData(reservationRequest)
                .stream()
                .map(TagData::create)
                .collect(Collectors.toList());
    }
}
