package cz.cesnet.shongo.controller.booking.request.auxdata;

import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.request.auxdata.tagdata.TagData;
import cz.cesnet.shongo.controller.booking.resource.Tag;

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
                .map(auxDataMerged -> {
                    Tag tag = new Tag();
                    tag.setName(auxDataMerged.getTagName());
                    tag.setType(auxDataMerged.getType());
                    tag.setData(auxDataMerged.getData());

                    AuxData auxData = new AuxData();
                    auxData.setTagName(auxDataMerged.getTagName());
                    auxData.setEnabled(auxDataMerged.getEnabled());
                    auxData.setData(auxDataMerged.getAuxData());

                    return TagData.create(tag, auxData);
                }).collect(Collectors.toList());
    }
}
