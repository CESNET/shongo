package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.fault.Fault;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.jade.CommandFailure;

public class FaultSet extends cz.cesnet.shongo.api.FaultSet
{
    public static final int DEVICE_COMMAND_FAILED_FAULT = 17;
    public static final int IDENTIFIER_INVALID_FAULT = 18;
    public static final int IDENTIFIER_INVALID_DOMAIN_FAULT = 19;
    public static final int IDENTIFIER_INVALID_TYPE_FAULT = 20;
    public static final int RESERVATION_REQUEST_NOT_MODIFIABLE_FAULT = 21;
    public static final int RESERVATION_REQUEST_EMPTY_DURATION_FAULT = 22;

    /**
     * Command {@link #command} for device {device} failed: {error}
     */
    public static class DeviceCommandFailedFault implements Fault
    {
        private String device;
        private String command;
        private CommandFailure error;

        public String getDevice()
        {
            return device;
        }

        public void setDevice(String device)
        {
            this.device = device;
        }

        public String getCommand()
        {
            return command;
        }

        public void setCommand(String command)
        {
            this.command = command;
        }

        public CommandFailure getError()
        {
            return error;
        }

        public void setError(CommandFailure error)
        {
            this.error = error;
        }

        @Override
        public int getCode()
        {
            return DEVICE_COMMAND_FAILED_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Command {command} for device {device} failed: {error}";
            message = message.replace("{device}", (device == null ? "" : device));
            message = message.replace("{command}", (command == null ? "" : command));
            message = message.replace("{error}", (error == null ? "" : error.toString()));
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link DeviceCommandFailedFault}
     */
    public static DeviceCommandFailedFault createDeviceCommandFailedFault(String device, String command, CommandFailure error)
    {
        DeviceCommandFailedFault deviceCommandFailedFault = new DeviceCommandFailedFault();
        deviceCommandFailedFault.setDevice(device);
        deviceCommandFailedFault.setCommand(command);
        deviceCommandFailedFault.setError(error);
        return deviceCommandFailedFault;
    }

    /**
     * @return new instance of {@link DeviceCommandFailedFault}
     */
    public static <T> T throwDeviceCommandFailedFault(String device, String command, CommandFailure error) throws FaultException
    {
        DeviceCommandFailedFault deviceCommandFailedFault = createDeviceCommandFailedFault(device, command, error);
        throw deviceCommandFailedFault.createException();
    }

    /**
     * Identifier {@link #id} is invalid.
     */
    public static class IdentifierInvalidFault implements Fault
    {
        private String id;

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        @Override
        public int getCode()
        {
            return IDENTIFIER_INVALID_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Identifier {id} is invalid.";
            message = message.replace("{id}", (id == null ? "" : id));
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link IdentifierInvalidFault}
     */
    public static IdentifierInvalidFault createIdentifierInvalidFault(String id)
    {
        IdentifierInvalidFault identifierInvalidFault = new IdentifierInvalidFault();
        identifierInvalidFault.setId(id);
        return identifierInvalidFault;
    }

    /**
     * @return new instance of {@link IdentifierInvalidFault}
     */
    public static <T> T throwIdentifierInvalidFault(String id) throws FaultException
    {
        IdentifierInvalidFault identifierInvalidFault = createIdentifierInvalidFault(id);
        throw identifierInvalidFault.createException();
    }

    /**
     * Identifier {@link #id} doesn't belong to domain {required-domain}.
     */
    public static class IdentifierInvalidDomainFault implements Fault
    {
        private String id;
        private String requiredDomain;

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        public String getRequiredDomain()
        {
            return requiredDomain;
        }

        public void setRequiredDomain(String requiredDomain)
        {
            this.requiredDomain = requiredDomain;
        }

        @Override
        public int getCode()
        {
            return IDENTIFIER_INVALID_DOMAIN_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Identifier {id} doesn't belong to domain {required-domain}.";
            message = message.replace("{id}", (id == null ? "" : id));
            message = message.replace("{required-domain}", (requiredDomain == null ? "" : requiredDomain));
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link IdentifierInvalidDomainFault}
     */
    public static IdentifierInvalidDomainFault createIdentifierInvalidDomainFault(String id, String requiredDomain)
    {
        IdentifierInvalidDomainFault identifierInvalidDomainFault = new IdentifierInvalidDomainFault();
        identifierInvalidDomainFault.setId(id);
        identifierInvalidDomainFault.setRequiredDomain(requiredDomain);
        return identifierInvalidDomainFault;
    }

    /**
     * @return new instance of {@link IdentifierInvalidDomainFault}
     */
    public static <T> T throwIdentifierInvalidDomainFault(String id, String requiredDomain) throws FaultException
    {
        IdentifierInvalidDomainFault identifierInvalidDomainFault = createIdentifierInvalidDomainFault(id, requiredDomain);
        throw identifierInvalidDomainFault.createException();
    }

    /**
     * Identifier {@link #id} isn't of required type {required-type}.
     */
    public static class IdentifierInvalidTypeFault implements Fault
    {
        private String id;
        private String requiredType;

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        public String getRequiredType()
        {
            return requiredType;
        }

        public void setRequiredType(String requiredType)
        {
            this.requiredType = requiredType;
        }

        @Override
        public int getCode()
        {
            return IDENTIFIER_INVALID_TYPE_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Identifier {id} isn't of required type {required-type}.";
            message = message.replace("{id}", (id == null ? "" : id));
            message = message.replace("{required-type}", (requiredType == null ? "" : requiredType));
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link IdentifierInvalidTypeFault}
     */
    public static IdentifierInvalidTypeFault createIdentifierInvalidTypeFault(String id, String requiredType)
    {
        IdentifierInvalidTypeFault identifierInvalidTypeFault = new IdentifierInvalidTypeFault();
        identifierInvalidTypeFault.setId(id);
        identifierInvalidTypeFault.setRequiredType(requiredType);
        return identifierInvalidTypeFault;
    }

    /**
     * @return new instance of {@link IdentifierInvalidTypeFault}
     */
    public static <T> T throwIdentifierInvalidTypeFault(String id, String requiredType) throws FaultException
    {
        IdentifierInvalidTypeFault identifierInvalidTypeFault = createIdentifierInvalidTypeFault(id, requiredType);
        throw identifierInvalidTypeFault.createException();
    }

    /**
     * Reservation request with identifier {@link #id} cannot be modified or deleted.
     */
    public static class ReservationRequestNotModifiableFault implements Fault
    {
        private String id;

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        @Override
        public int getCode()
        {
            return RESERVATION_REQUEST_NOT_MODIFIABLE_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Reservation request with identifier {id} cannot be modified or deleted.";
            message = message.replace("{id}", (id == null ? "" : id));
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link ReservationRequestNotModifiableFault}
     */
    public static ReservationRequestNotModifiableFault createReservationRequestNotModifiableFault(String id)
    {
        ReservationRequestNotModifiableFault reservationRequestNotModifiableFault = new ReservationRequestNotModifiableFault();
        reservationRequestNotModifiableFault.setId(id);
        return reservationRequestNotModifiableFault;
    }

    /**
     * @return new instance of {@link ReservationRequestNotModifiableFault}
     */
    public static <T> T throwReservationRequestNotModifiableFault(String id) throws FaultException
    {
        ReservationRequestNotModifiableFault reservationRequestNotModifiableFault = createReservationRequestNotModifiableFault(id);
        throw reservationRequestNotModifiableFault.createException();
    }

    /**
     * Reservation request time slot must not be empty.
     */
    public static class ReservationRequestEmptyDurationFault implements Fault
    {

        @Override
        public int getCode()
        {
            return RESERVATION_REQUEST_EMPTY_DURATION_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Reservation request time slot must not be empty.";
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link ReservationRequestEmptyDurationFault}
     */
    public static ReservationRequestEmptyDurationFault createReservationRequestEmptyDurationFault()
    {
        ReservationRequestEmptyDurationFault reservationRequestEmptyDurationFault = new ReservationRequestEmptyDurationFault();
        return reservationRequestEmptyDurationFault;
    }

    /**
     * @return new instance of {@link ReservationRequestEmptyDurationFault}
     */
    public static <T> T throwReservationRequestEmptyDurationFault() throws FaultException
    {
        ReservationRequestEmptyDurationFault reservationRequestEmptyDurationFault = createReservationRequestEmptyDurationFault();
        throw reservationRequestEmptyDurationFault.createException();
    }

    @Override
    protected void fillFaults()
    {
        super.fillFaults();
        addFault(DEVICE_COMMAND_FAILED_FAULT, DeviceCommandFailedFault.class);
        addFault(IDENTIFIER_INVALID_FAULT, IdentifierInvalidFault.class);
        addFault(IDENTIFIER_INVALID_DOMAIN_FAULT, IdentifierInvalidDomainFault.class);
        addFault(IDENTIFIER_INVALID_TYPE_FAULT, IdentifierInvalidTypeFault.class);
        addFault(RESERVATION_REQUEST_NOT_MODIFIABLE_FAULT, ReservationRequestNotModifiableFault.class);
        addFault(RESERVATION_REQUEST_EMPTY_DURATION_FAULT, ReservationRequestEmptyDurationFault.class);
    }
}
