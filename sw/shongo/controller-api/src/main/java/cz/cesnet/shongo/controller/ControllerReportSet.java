package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.report.*;

/**
 * Auto-generated implementation of {@link AbstractReportSet}.
 *
 * @author cz.cesnet.shongo.tool-report-generator
 */
public class ControllerReportSet extends AbstractReportSet
{
    public static final int ACL_INVALID_ROLE_REPORT = 0;
    public static final int SECURITY_INVALID_TOKEN_REPORT = 0;
    public static final int SECURITY_NOT_AUTHORIZED_REPORT = 0;
    public static final int DEVICE_COMMAND_FAILED_REPORT = 0;
    public static final int IDENTIFIER_INVALID_REPORT = 0;
    public static final int IDENTIFIER_INVALID_DOMAIN_REPORT = 0;
    public static final int IDENTIFIER_INVALID_TYPE_REPORT = 0;
    public static final int RESERVATION_REQUEST_NOT_MODIFIABLE_REPORT = 0;
    public static final int RESERVATION_REQUEST_EMPTY_DURATION_REPORT = 0;
    /**
     * ACL Role {@link #role} is invalid for entity ${entity}.
     */
    public static class AclInvalidRoleReport implements Report, ApiFault
    {
        private String entity;
        private String role;

        public String getEntity()
        {
            return entity;
        }

        public void setEntity(String entity)
        {
            this.entity = entity;
        }

        public String getRole()
        {
            return role;
        }

        public void setRole(String role)
        {
            this.role = role;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getCode()
        {
            return ACL_INVALID_ROLE_REPORT;
        }

        @Override
        public String getMessage()
        {
            String message = "ACL Role ${role} is invalid for entity ${entity}.";
            message = message.replace("${entity}", (entity == null ? "" : entity));
            message = message.replace("${role}", (role == null ? "" : role));
            return message;
        }
    }

    /**
     * Exception for {@link AclInvalidRoleReport}.
     */
    public static class AclInvalidRoleException extends AbstractReportException implements ApiFault
    {
        private AclInvalidRoleReport report;

        public AclInvalidRoleException(AclInvalidRoleReport report)
        {
            this.report = report;
        }

        public AclInvalidRoleException(Throwable throwable, AclInvalidRoleReport report)
        {
            super(throwable);
            this.report = report;
        }

        public AclInvalidRoleException(String entity, String role)
        {
            report = new AclInvalidRoleReport();
            report.setEntity(entity);
            report.setRole(role);
        }

        public AclInvalidRoleException(Throwable throwable, String entity, String role)
        {
            super(throwable);
            report = new AclInvalidRoleReport();
            report.setEntity(entity);
            report.setRole(role);
        }

        public String getEntity()
        {
            return report.getEntity();
        }

        public String getRole()
        {
            return report.getRole();
        }

        @Override
        public AclInvalidRoleReport getReport()
        {
            return report;
        }

        @Override
        public int getCode()
        {
            return report.getCode();
        }
    }
    /**
     * Invalid security token {@link #token}.
     */
    public static class SecurityInvalidTokenReport implements Report, ApiFault
    {
        private String token;

        public String getToken()
        {
            return token;
        }

        public void setToken(String token)
        {
            this.token = token;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getCode()
        {
            return SECURITY_INVALID_TOKEN_REPORT;
        }

        @Override
        public String getMessage()
        {
            String message = "Invalid security token ${token}.";
            message = message.replace("${token}", (token == null ? "" : token));
            return message;
        }
    }

    /**
     * Exception for {@link SecurityInvalidTokenReport}.
     */
    public static class SecurityInvalidTokenException extends AbstractReportException implements ApiFault
    {
        private SecurityInvalidTokenReport report;

        public SecurityInvalidTokenException(SecurityInvalidTokenReport report)
        {
            this.report = report;
        }

        public SecurityInvalidTokenException(Throwable throwable, SecurityInvalidTokenReport report)
        {
            super(throwable);
            this.report = report;
        }

        public SecurityInvalidTokenException(String token)
        {
            report = new SecurityInvalidTokenReport();
            report.setToken(token);
        }

        public SecurityInvalidTokenException(Throwable throwable, String token)
        {
            super(throwable);
            report = new SecurityInvalidTokenReport();
            report.setToken(token);
        }

        public String getToken()
        {
            return report.getToken();
        }

        @Override
        public SecurityInvalidTokenReport getReport()
        {
            return report;
        }

        @Override
        public int getCode()
        {
            return report.getCode();
        }
    }
    /**
     * You are not authorized to {@link #action}.
     */
    public static class SecurityNotAuthorizedReport implements Report, ApiFault
    {
        private String action;

        public String getAction()
        {
            return action;
        }

        public void setAction(String action)
        {
            this.action = action;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getCode()
        {
            return SECURITY_NOT_AUTHORIZED_REPORT;
        }

        @Override
        public String getMessage()
        {
            String message = "You are not authorized to ${action}.";
            message = message.replace("${action}", (action == null ? "" : action));
            return message;
        }
    }

    /**
     * Exception for {@link SecurityNotAuthorizedReport}.
     */
    public static class SecurityNotAuthorizedException extends AbstractReportException implements ApiFault
    {
        private SecurityNotAuthorizedReport report;

        public SecurityNotAuthorizedException(SecurityNotAuthorizedReport report)
        {
            this.report = report;
        }

        public SecurityNotAuthorizedException(Throwable throwable, SecurityNotAuthorizedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public SecurityNotAuthorizedException(String action)
        {
            report = new SecurityNotAuthorizedReport();
            report.setAction(action);
        }

        public SecurityNotAuthorizedException(Throwable throwable, String action)
        {
            super(throwable);
            report = new SecurityNotAuthorizedReport();
            report.setAction(action);
        }

        public String getAction()
        {
            return report.getAction();
        }

        @Override
        public SecurityNotAuthorizedReport getReport()
        {
            return report;
        }

        @Override
        public int getCode()
        {
            return report.getCode();
        }
    }
    /**
     * Command {@link #command} for device ${device} failed: ${error}
     */
    public static class DeviceCommandFailedReport implements Report, ApiFault
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
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getCode()
        {
            return DEVICE_COMMAND_FAILED_REPORT;
        }

        @Override
        public String getMessage()
        {
            String message = "Command ${command} for device ${device} failed: ${error}";
            message = message.replace("${device}", (device == null ? "" : device));
            message = message.replace("${command}", (command == null ? "" : command));
            message = message.replace("${error}", (error == null ? "" : error.toString()));
            return message;
        }
    }

    /**
     * Exception for {@link DeviceCommandFailedReport}.
     */
    public static class DeviceCommandFailedException extends AbstractReportException implements ApiFault
    {
        private DeviceCommandFailedReport report;

        public DeviceCommandFailedException(DeviceCommandFailedReport report)
        {
            this.report = report;
        }

        public DeviceCommandFailedException(Throwable throwable, DeviceCommandFailedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public DeviceCommandFailedException(String device, String command, CommandFailure error)
        {
            report = new DeviceCommandFailedReport();
            report.setDevice(device);
            report.setCommand(command);
            report.setError(error);
        }

        public DeviceCommandFailedException(Throwable throwable, String device, String command, CommandFailure error)
        {
            super(throwable);
            report = new DeviceCommandFailedReport();
            report.setDevice(device);
            report.setCommand(command);
            report.setError(error);
        }

        public String getDevice()
        {
            return report.getDevice();
        }

        public String getCommand()
        {
            return report.getCommand();
        }

        public CommandFailure getError()
        {
            return report.getError();
        }

        @Override
        public DeviceCommandFailedReport getReport()
        {
            return report;
        }

        @Override
        public int getCode()
        {
            return report.getCode();
        }
    }
    /**
     * Identifier {@link #id} is invalid.
     */
    public static class IdentifierInvalidReport implements Report, ApiFault
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
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getCode()
        {
            return IDENTIFIER_INVALID_REPORT;
        }

        @Override
        public String getMessage()
        {
            String message = "Identifier ${id} is invalid.";
            message = message.replace("${id}", (id == null ? "" : id));
            return message;
        }
    }

    /**
     * Exception for {@link IdentifierInvalidReport}.
     */
    public static class IdentifierInvalidException extends AbstractReportException implements ApiFault
    {
        private IdentifierInvalidReport report;

        public IdentifierInvalidException(IdentifierInvalidReport report)
        {
            this.report = report;
        }

        public IdentifierInvalidException(Throwable throwable, IdentifierInvalidReport report)
        {
            super(throwable);
            this.report = report;
        }

        public IdentifierInvalidException(String id)
        {
            report = new IdentifierInvalidReport();
            report.setId(id);
        }

        public IdentifierInvalidException(Throwable throwable, String id)
        {
            super(throwable);
            report = new IdentifierInvalidReport();
            report.setId(id);
        }

        public String getId()
        {
            return report.getId();
        }

        @Override
        public IdentifierInvalidReport getReport()
        {
            return report;
        }

        @Override
        public int getCode()
        {
            return report.getCode();
        }
    }
    /**
     * Identifier {@link #id} doesn't belong to domain ${required-domain}.
     */
    public static class IdentifierInvalidDomainReport implements Report, ApiFault
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
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getCode()
        {
            return IDENTIFIER_INVALID_DOMAIN_REPORT;
        }

        @Override
        public String getMessage()
        {
            String message = "Identifier ${id} doesn't belong to domain ${required-domain}.";
            message = message.replace("${id}", (id == null ? "" : id));
            message = message.replace("${required-domain}", (requiredDomain == null ? "" : requiredDomain));
            return message;
        }
    }

    /**
     * Exception for {@link IdentifierInvalidDomainReport}.
     */
    public static class IdentifierInvalidDomainException extends AbstractReportException implements ApiFault
    {
        private IdentifierInvalidDomainReport report;

        public IdentifierInvalidDomainException(IdentifierInvalidDomainReport report)
        {
            this.report = report;
        }

        public IdentifierInvalidDomainException(Throwable throwable, IdentifierInvalidDomainReport report)
        {
            super(throwable);
            this.report = report;
        }

        public IdentifierInvalidDomainException(String id, String requiredDomain)
        {
            report = new IdentifierInvalidDomainReport();
            report.setId(id);
            report.setRequiredDomain(requiredDomain);
        }

        public IdentifierInvalidDomainException(Throwable throwable, String id, String requiredDomain)
        {
            super(throwable);
            report = new IdentifierInvalidDomainReport();
            report.setId(id);
            report.setRequiredDomain(requiredDomain);
        }

        public String getId()
        {
            return report.getId();
        }

        public String getRequiredDomain()
        {
            return report.getRequiredDomain();
        }

        @Override
        public IdentifierInvalidDomainReport getReport()
        {
            return report;
        }

        @Override
        public int getCode()
        {
            return report.getCode();
        }
    }
    /**
     * Identifier {@link #id} isn't of required type ${required-type}.
     */
    public static class IdentifierInvalidTypeReport implements Report, ApiFault
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
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getCode()
        {
            return IDENTIFIER_INVALID_TYPE_REPORT;
        }

        @Override
        public String getMessage()
        {
            String message = "Identifier ${id} isn't of required type ${required-type}.";
            message = message.replace("${id}", (id == null ? "" : id));
            message = message.replace("${required-type}", (requiredType == null ? "" : requiredType));
            return message;
        }
    }

    /**
     * Exception for {@link IdentifierInvalidTypeReport}.
     */
    public static class IdentifierInvalidTypeException extends AbstractReportException implements ApiFault
    {
        private IdentifierInvalidTypeReport report;

        public IdentifierInvalidTypeException(IdentifierInvalidTypeReport report)
        {
            this.report = report;
        }

        public IdentifierInvalidTypeException(Throwable throwable, IdentifierInvalidTypeReport report)
        {
            super(throwable);
            this.report = report;
        }

        public IdentifierInvalidTypeException(String id, String requiredType)
        {
            report = new IdentifierInvalidTypeReport();
            report.setId(id);
            report.setRequiredType(requiredType);
        }

        public IdentifierInvalidTypeException(Throwable throwable, String id, String requiredType)
        {
            super(throwable);
            report = new IdentifierInvalidTypeReport();
            report.setId(id);
            report.setRequiredType(requiredType);
        }

        public String getId()
        {
            return report.getId();
        }

        public String getRequiredType()
        {
            return report.getRequiredType();
        }

        @Override
        public IdentifierInvalidTypeReport getReport()
        {
            return report;
        }

        @Override
        public int getCode()
        {
            return report.getCode();
        }
    }
    /**
     * Reservation request with identifier {@link #id} cannot be modified or deleted.
     */
    public static class ReservationRequestNotModifiableReport implements Report, ApiFault
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
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getCode()
        {
            return RESERVATION_REQUEST_NOT_MODIFIABLE_REPORT;
        }

        @Override
        public String getMessage()
        {
            String message = "Reservation request with identifier ${id} cannot be modified or deleted.";
            message = message.replace("${id}", (id == null ? "" : id));
            return message;
        }
    }

    /**
     * Exception for {@link ReservationRequestNotModifiableReport}.
     */
    public static class ReservationRequestNotModifiableException extends AbstractReportException implements ApiFault
    {
        private ReservationRequestNotModifiableReport report;

        public ReservationRequestNotModifiableException(ReservationRequestNotModifiableReport report)
        {
            this.report = report;
        }

        public ReservationRequestNotModifiableException(Throwable throwable, ReservationRequestNotModifiableReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ReservationRequestNotModifiableException(String id)
        {
            report = new ReservationRequestNotModifiableReport();
            report.setId(id);
        }

        public ReservationRequestNotModifiableException(Throwable throwable, String id)
        {
            super(throwable);
            report = new ReservationRequestNotModifiableReport();
            report.setId(id);
        }

        public String getId()
        {
            return report.getId();
        }

        @Override
        public ReservationRequestNotModifiableReport getReport()
        {
            return report;
        }

        @Override
        public int getCode()
        {
            return report.getCode();
        }
    }
    /**
     * Reservation request time slot must not be empty.
     */
    public static class ReservationRequestEmptyDurationReport implements Report, ApiFault
    {

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getCode()
        {
            return RESERVATION_REQUEST_EMPTY_DURATION_REPORT;
        }

        @Override
        public String getMessage()
        {
            String message = "Reservation request time slot must not be empty.";
            return message;
        }
    }

    /**
     * Exception for {@link ReservationRequestEmptyDurationReport}.
     */
    public static class ReservationRequestEmptyDurationException extends AbstractReportException implements ApiFault
    {
        private ReservationRequestEmptyDurationReport report;

        public ReservationRequestEmptyDurationException(ReservationRequestEmptyDurationReport report)
        {
            this.report = report;
        }

        public ReservationRequestEmptyDurationException(Throwable throwable, ReservationRequestEmptyDurationReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ReservationRequestEmptyDurationException()
        {
            report = new ReservationRequestEmptyDurationReport();
        }

        public ReservationRequestEmptyDurationException(Throwable throwable, )
        {
            super(throwable);
            report = new ReservationRequestEmptyDurationReport();
        }

        @Override
        public ReservationRequestEmptyDurationReport getReport()
        {
            return report;
        }

        @Override
        public int getCode()
        {
            return report.getCode();
        }
    }
}
