package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

/**
 * {@link Reservation} for existing {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExistingReservation extends Reservation
{
    /**
     * {@link Reservation}.
     */
    private Reservation reservation;

    /**
     * @return {@link #reservation}
     */
    public Reservation getReservation()
    {
        return reservation;
    }

    /**
     * @param reservation sets the {@link #reservation}
     */
    public void setReservation(Reservation reservation)
    {
        this.reservation = reservation;
    }

    private static final String RESERVATION = "reservation";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESERVATION, reservation);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        reservation = dataMap.getComplexType(RESERVATION, Reservation.class);
    }
}
