package cz.cesnet.shongo.fault;

import cz.cesnet.shongo.CommonFaultSet;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class FaultRuntimeException extends RuntimeException implements FaultThrowable
{
    private Fault fault;

    public FaultRuntimeException(Fault fault)
    {
        this.fault = fault;
    }

    public FaultRuntimeException(Throwable throwable, Fault fault)
    {
        super(throwable);
        this.fault = fault;
    }

    public FaultRuntimeException(Throwable throwable)
    {
        super(throwable);
        this.fault = CommonFaultSet.createUnknownErrorFault(throwable.getMessage());
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
