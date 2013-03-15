package cz.cesnet.shongo.fault;

import cz.cesnet.shongo.CommonFaultSet;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class FaultException extends Exception implements FaultThrowable
{
    private Fault fault;

    public FaultException(Fault fault)
    {
        this.fault = fault;
    }

    public FaultException(Throwable throwable)
    {
        super(throwable);
        this.fault = CommonFaultSet.createUnknownErrorFault(throwable.getMessage());
    }

    public FaultException(String message, Object... objects)
    {
        this.fault = CommonFaultSet.createUnknownErrorFault(FaultSet.formatMessage(message, objects));
    }

    public FaultException(Throwable throwable, String message, Object... objects)
    {
        super(throwable);
        this.fault = CommonFaultSet.createUnknownErrorFault(FaultSet.formatMessage(message, objects));
    }

    @Override
    public Fault getFault()
    {
        return fault;
    }

    public <F extends Fault> F getFault(Class<F> faultType)
    {
        return faultType.cast(fault);
    }

    public Class<? extends Fault> getFaultClass()
    {
        return fault.getClass();
    }
}
