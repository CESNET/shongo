package cz.cesnet.shongo.controller;

/**
 * Fault enumeration for controller faults
 *
 * @author Martin Srom
 */
public enum Fault implements cz.cesnet.shongo.Fault
{
    ReservationType_NotFilled(100, "Reservation type is not filled."),
    Date_NotFilled(101, "Date is not filled."),
    PeriodicDate_Required(102, "Periodic date is required."),
    Reservation_NotFound(103, "Reservation with id '%s' not found.");

    private int code;
    private String string;

    private Fault(int code, String string) {
        this.code = code;
        this.string = string;
    }

    public int getCode() {
        return code;
    }

    public String getString() {
        return string;
    }
}

