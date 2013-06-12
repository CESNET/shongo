package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.report.*;

/**
 * Auto-generated implementation of {@link AbstractReportSet}.
 *
 * @author cz.cesnet.shongo.tool-report-generator
 */
public class ControllerReportSet extends AbstractReportSet
{
    public static final int ACL_INVALID_ROLE_REPORT = 100;
    public static final int SECURITY_MISSING_TOKEN_REPORT = 101;
    public static final int SECURITY_INVALID_TOKEN_REPORT = 102;
    public static final int SECURITY_NOT_AUTHORIZED_REPORT = 103;
    public static final int DEVICE_COMMAND_FAILED_REPORT = 104;
    public static final int IDENTIFIER_INVALID_REPORT = 105;
    public static final int IDENTIFIER_INVALID_DOMAIN_REPORT = 106;
    public static final int IDENTIFIER_INVALID_TYPE_REPORT = 107;
    public static final int RESERVATION_REQUEST_NOT_MODIFIABLE_REPORT = 108;
    public static final int RESERVATION_REQUEST_ALREADY_MODIFIED_REPORT = 109;
    public static final int RESERVATION_REQUEST_DELETED_REPORT = 110;
    public static final int RESERVATION_REQUEST_EMPTY_DURATION_REPORT = 111;

    /**
     * ACL Role {@link #role} is invalid for entity {@link #entity}.
     */
    public static class AclInvalidRoleReport extends Report implements ApiFault
    {
        protected String entity;

        protected String role;

        public AclInvalidRoleReport()
        {
        }

        public AclInvalidRoleReport(String entity, String role)
        {
            setEntity(entity);
            setRole(role);
        }

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
        public int getFaultCode()
        {
            return ACL_INVALID_ROLE_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
        }

        @Override
        public Exception getException()
        {
            return new AclInvalidRoleException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            entity = (String) reportSerializer.getParameter("entity", String.class);
            role = (String) reportSerializer.getParameter("role", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("entity", entity);
            reportSerializer.setParameter("role", role);
        }

        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("ACL Role ");
                    message.append((role == null ? "null" : role));
                    message.append(" is invalid for entity ");
                    message.append((entity == null ? "null" : entity));
                    message.append(".");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link AclInvalidRoleReport}.
     */
    public static class AclInvalidRoleException extends ReportRuntimeException implements ApiFaultException
    {
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
            AclInvalidRoleReport report = new AclInvalidRoleReport();
            report.setEntity(entity);
            report.setRole(role);
            this.report = report;
        }

        public AclInvalidRoleException(Throwable throwable, String entity, String role)
        {
            super(throwable);
            AclInvalidRoleReport report = new AclInvalidRoleReport();
            report.setEntity(entity);
            report.setRole(role);
            this.report = report;
        }

        public String getEntity()
        {
            return getReport().getEntity();
        }

        public String getRole()
        {
            return getReport().getRole();
        }

        @Override
        public AclInvalidRoleReport getReport()
        {
            return (AclInvalidRoleReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (AclInvalidRoleReport) report;
        }
    }

    /**
     * Security token is missing but is required.
     */
    public static class SecurityMissingTokenReport extends Report implements ApiFault
    {
        public SecurityMissingTokenReport()
        {
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return SECURITY_MISSING_TOKEN_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
        }

        @Override
        public Exception getException()
        {
            return new SecurityMissingTokenException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
        }

        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Security token is missing but is required.");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link SecurityMissingTokenReport}.
     */
    public static class SecurityMissingTokenException extends ReportRuntimeException implements ApiFaultException
    {
        public SecurityMissingTokenException(SecurityMissingTokenReport report)
        {
            this.report = report;
        }

        public SecurityMissingTokenException(Throwable throwable, SecurityMissingTokenReport report)
        {
            super(throwable);
            this.report = report;
        }

        public SecurityMissingTokenException()
        {
            SecurityMissingTokenReport report = new SecurityMissingTokenReport();
            this.report = report;
        }

        public SecurityMissingTokenException(Throwable throwable)
        {
            super(throwable);
            SecurityMissingTokenReport report = new SecurityMissingTokenReport();
            this.report = report;
        }

        @Override
        public SecurityMissingTokenReport getReport()
        {
            return (SecurityMissingTokenReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (SecurityMissingTokenReport) report;
        }
    }

    /**
     * Invalid security token {@link #token}.
     */
    public static class SecurityInvalidTokenReport extends Report implements ApiFault
    {
        protected String token;

        public SecurityInvalidTokenReport()
        {
        }

        public SecurityInvalidTokenReport(String token)
        {
            setToken(token);
        }

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
        public int getFaultCode()
        {
            return SECURITY_INVALID_TOKEN_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
        }

        @Override
        public Exception getException()
        {
            return new SecurityInvalidTokenException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            token = (String) reportSerializer.getParameter("token", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("token", token);
        }

        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Invalid security token ");
                    message.append((token == null ? "null" : token));
                    message.append(".");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link SecurityInvalidTokenReport}.
     */
    public static class SecurityInvalidTokenException extends ReportRuntimeException implements ApiFaultException
    {
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
            SecurityInvalidTokenReport report = new SecurityInvalidTokenReport();
            report.setToken(token);
            this.report = report;
        }

        public SecurityInvalidTokenException(Throwable throwable, String token)
        {
            super(throwable);
            SecurityInvalidTokenReport report = new SecurityInvalidTokenReport();
            report.setToken(token);
            this.report = report;
        }

        public String getToken()
        {
            return getReport().getToken();
        }

        @Override
        public SecurityInvalidTokenReport getReport()
        {
            return (SecurityInvalidTokenReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (SecurityInvalidTokenReport) report;
        }
    }

    /**
     * You are not authorized to {@link #action}.
     */
    public static class SecurityNotAuthorizedReport extends Report implements ApiFault
    {
        protected String action;

        public SecurityNotAuthorizedReport()
        {
        }

        public SecurityNotAuthorizedReport(String action)
        {
            setAction(action);
        }

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
        public int getFaultCode()
        {
            return SECURITY_NOT_AUTHORIZED_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
        }

        @Override
        public Exception getException()
        {
            return new SecurityNotAuthorizedException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            action = (String) reportSerializer.getParameter("action", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("action", action);
        }

        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("You are not authorized to ");
                    message.append((action == null ? "null" : action));
                    message.append(".");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link SecurityNotAuthorizedReport}.
     */
    public static class SecurityNotAuthorizedException extends ReportRuntimeException implements ApiFaultException
    {
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
            SecurityNotAuthorizedReport report = new SecurityNotAuthorizedReport();
            report.setAction(action);
            this.report = report;
        }

        public SecurityNotAuthorizedException(Throwable throwable, String action)
        {
            super(throwable);
            SecurityNotAuthorizedReport report = new SecurityNotAuthorizedReport();
            report.setAction(action);
            this.report = report;
        }

        public String getAction()
        {
            return getReport().getAction();
        }

        @Override
        public SecurityNotAuthorizedReport getReport()
        {
            return (SecurityNotAuthorizedReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (SecurityNotAuthorizedReport) report;
        }
    }

    /**
     * Command {@link #command} for device {@link #device} failed: {@link #jadeReport}
     */
    public static class DeviceCommandFailedReport extends Report implements ApiFault, ResourceReport
    {
        protected String device;

        protected String command;

        protected cz.cesnet.shongo.JadeReport jadeReport;

        public DeviceCommandFailedReport()
        {
        }

        public DeviceCommandFailedReport(String device, String command, cz.cesnet.shongo.JadeReport jadeReport)
        {
            setDevice(device);
            setCommand(command);
            setJadeReport(jadeReport);
        }

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

        public cz.cesnet.shongo.JadeReport getJadeReport()
        {
            return jadeReport;
        }

        public void setJadeReport(cz.cesnet.shongo.JadeReport jadeReport)
        {
            this.jadeReport = jadeReport;
        }

        @Override
        public String getResourceId()
        {
            return device;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return DEVICE_COMMAND_FAILED_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
        }

        @Override
        public Exception getException()
        {
            return new DeviceCommandFailedException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            device = (String) reportSerializer.getParameter("device", String.class);
            command = (String) reportSerializer.getParameter("command", String.class);
            jadeReport = (cz.cesnet.shongo.JadeReport) reportSerializer.getParameter("jadeReport", cz.cesnet.shongo.JadeReport.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("device", device);
            reportSerializer.setParameter("command", command);
            reportSerializer.setParameter("jadeReport", jadeReport);
        }

        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN | VISIBLE_TO_RESOURCE_ADMIN;
        }

        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Command ");
                    message.append((command == null ? "null" : command));
                    message.append(" for device ");
                    message.append((device == null ? "null" : device));
                    message.append(" failed: ");
                    message.append((jadeReport == null ? "null" : jadeReport.getReportDescription(messageType)));
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link DeviceCommandFailedReport}.
     */
    public static class DeviceCommandFailedException extends ReportRuntimeException implements ApiFaultException
    {
        public DeviceCommandFailedException(DeviceCommandFailedReport report)
        {
            this.report = report;
        }

        public DeviceCommandFailedException(Throwable throwable, DeviceCommandFailedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public DeviceCommandFailedException(String device, String command, cz.cesnet.shongo.JadeReport jadeReport)
        {
            DeviceCommandFailedReport report = new DeviceCommandFailedReport();
            report.setDevice(device);
            report.setCommand(command);
            report.setJadeReport(jadeReport);
            this.report = report;
        }

        public DeviceCommandFailedException(Throwable throwable, String device, String command, cz.cesnet.shongo.JadeReport jadeReport)
        {
            super(throwable);
            DeviceCommandFailedReport report = new DeviceCommandFailedReport();
            report.setDevice(device);
            report.setCommand(command);
            report.setJadeReport(jadeReport);
            this.report = report;
        }

        public String getDevice()
        {
            return getReport().getDevice();
        }

        public String getCommand()
        {
            return getReport().getCommand();
        }

        public cz.cesnet.shongo.JadeReport getJadeReport()
        {
            return getReport().getJadeReport();
        }

        @Override
        public DeviceCommandFailedReport getReport()
        {
            return (DeviceCommandFailedReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (DeviceCommandFailedReport) report;
        }
    }

    /**
     * Identifier {@link #id} is invalid.
     */
    public static class IdentifierInvalidReport extends Report implements ApiFault
    {
        protected String id;

        public IdentifierInvalidReport()
        {
        }

        public IdentifierInvalidReport(String id)
        {
            setId(id);
        }

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
        public int getFaultCode()
        {
            return IDENTIFIER_INVALID_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
        }

        @Override
        public Exception getException()
        {
            return new IdentifierInvalidException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            id = (String) reportSerializer.getParameter("id", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("id", id);
        }

        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Identifier ");
                    message.append((id == null ? "null" : id));
                    message.append(" is invalid.");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link IdentifierInvalidReport}.
     */
    public static class IdentifierInvalidException extends ReportRuntimeException implements ApiFaultException
    {
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
            IdentifierInvalidReport report = new IdentifierInvalidReport();
            report.setId(id);
            this.report = report;
        }

        public IdentifierInvalidException(Throwable throwable, String id)
        {
            super(throwable);
            IdentifierInvalidReport report = new IdentifierInvalidReport();
            report.setId(id);
            this.report = report;
        }

        public String getId()
        {
            return getReport().getId();
        }

        @Override
        public IdentifierInvalidReport getReport()
        {
            return (IdentifierInvalidReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (IdentifierInvalidReport) report;
        }
    }

    /**
     * Identifier {@link #id} doesn't belong to domain {@link #requiredDomain}.
     */
    public static class IdentifierInvalidDomainReport extends Report implements ApiFault
    {
        protected String id;

        protected String requiredDomain;

        public IdentifierInvalidDomainReport()
        {
        }

        public IdentifierInvalidDomainReport(String id, String requiredDomain)
        {
            setId(id);
            setRequiredDomain(requiredDomain);
        }

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
        public int getFaultCode()
        {
            return IDENTIFIER_INVALID_DOMAIN_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
        }

        @Override
        public Exception getException()
        {
            return new IdentifierInvalidDomainException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            id = (String) reportSerializer.getParameter("id", String.class);
            requiredDomain = (String) reportSerializer.getParameter("requiredDomain", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("id", id);
            reportSerializer.setParameter("requiredDomain", requiredDomain);
        }

        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Identifier ");
                    message.append((id == null ? "null" : id));
                    message.append(" doesn't belong to domain ");
                    message.append((requiredDomain == null ? "null" : requiredDomain));
                    message.append(".");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link IdentifierInvalidDomainReport}.
     */
    public static class IdentifierInvalidDomainException extends ReportRuntimeException implements ApiFaultException
    {
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
            IdentifierInvalidDomainReport report = new IdentifierInvalidDomainReport();
            report.setId(id);
            report.setRequiredDomain(requiredDomain);
            this.report = report;
        }

        public IdentifierInvalidDomainException(Throwable throwable, String id, String requiredDomain)
        {
            super(throwable);
            IdentifierInvalidDomainReport report = new IdentifierInvalidDomainReport();
            report.setId(id);
            report.setRequiredDomain(requiredDomain);
            this.report = report;
        }

        public String getId()
        {
            return getReport().getId();
        }

        public String getRequiredDomain()
        {
            return getReport().getRequiredDomain();
        }

        @Override
        public IdentifierInvalidDomainReport getReport()
        {
            return (IdentifierInvalidDomainReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (IdentifierInvalidDomainReport) report;
        }
    }

    /**
     * Identifier {@link #id} isn't of required type {@link #requiredType}.
     */
    public static class IdentifierInvalidTypeReport extends Report implements ApiFault
    {
        protected String id;

        protected String requiredType;

        public IdentifierInvalidTypeReport()
        {
        }

        public IdentifierInvalidTypeReport(String id, String requiredType)
        {
            setId(id);
            setRequiredType(requiredType);
        }

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
        public int getFaultCode()
        {
            return IDENTIFIER_INVALID_TYPE_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
        }

        @Override
        public Exception getException()
        {
            return new IdentifierInvalidTypeException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            id = (String) reportSerializer.getParameter("id", String.class);
            requiredType = (String) reportSerializer.getParameter("requiredType", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("id", id);
            reportSerializer.setParameter("requiredType", requiredType);
        }

        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Identifier ");
                    message.append((id == null ? "null" : id));
                    message.append(" isn't of required type ");
                    message.append((requiredType == null ? "null" : requiredType));
                    message.append(".");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link IdentifierInvalidTypeReport}.
     */
    public static class IdentifierInvalidTypeException extends ReportRuntimeException implements ApiFaultException
    {
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
            IdentifierInvalidTypeReport report = new IdentifierInvalidTypeReport();
            report.setId(id);
            report.setRequiredType(requiredType);
            this.report = report;
        }

        public IdentifierInvalidTypeException(Throwable throwable, String id, String requiredType)
        {
            super(throwable);
            IdentifierInvalidTypeReport report = new IdentifierInvalidTypeReport();
            report.setId(id);
            report.setRequiredType(requiredType);
            this.report = report;
        }

        public String getId()
        {
            return getReport().getId();
        }

        public String getRequiredType()
        {
            return getReport().getRequiredType();
        }

        @Override
        public IdentifierInvalidTypeReport getReport()
        {
            return (IdentifierInvalidTypeReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (IdentifierInvalidTypeReport) report;
        }
    }

    /**
     * Reservation request with identifier {@link #id} cannot be modified or deleted.
     */
    public static class ReservationRequestNotModifiableReport extends Report implements ApiFault
    {
        protected String id;

        public ReservationRequestNotModifiableReport()
        {
        }

        public ReservationRequestNotModifiableReport(String id)
        {
            setId(id);
        }

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
        public int getFaultCode()
        {
            return RESERVATION_REQUEST_NOT_MODIFIABLE_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
        }

        @Override
        public Exception getException()
        {
            return new ReservationRequestNotModifiableException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            id = (String) reportSerializer.getParameter("id", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("id", id);
        }

        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Reservation request with identifier ");
                    message.append((id == null ? "null" : id));
                    message.append(" cannot be modified or deleted.");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link ReservationRequestNotModifiableReport}.
     */
    public static class ReservationRequestNotModifiableException extends ReportRuntimeException implements ApiFaultException
    {
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
            ReservationRequestNotModifiableReport report = new ReservationRequestNotModifiableReport();
            report.setId(id);
            this.report = report;
        }

        public ReservationRequestNotModifiableException(Throwable throwable, String id)
        {
            super(throwable);
            ReservationRequestNotModifiableReport report = new ReservationRequestNotModifiableReport();
            report.setId(id);
            this.report = report;
        }

        public String getId()
        {
            return getReport().getId();
        }

        @Override
        public ReservationRequestNotModifiableReport getReport()
        {
            return (ReservationRequestNotModifiableReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ReservationRequestNotModifiableReport) report;
        }
    }

    /**
     * Reservation request with identifier {@link #id} has already been modified.
     */
    public static class ReservationRequestAlreadyModifiedReport extends Report implements ApiFault
    {
        protected String id;

        public ReservationRequestAlreadyModifiedReport()
        {
        }

        public ReservationRequestAlreadyModifiedReport(String id)
        {
            setId(id);
        }

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
        public int getFaultCode()
        {
            return RESERVATION_REQUEST_ALREADY_MODIFIED_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
        }

        @Override
        public Exception getException()
        {
            return new ReservationRequestAlreadyModifiedException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            id = (String) reportSerializer.getParameter("id", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("id", id);
        }

        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Reservation request with identifier ");
                    message.append((id == null ? "null" : id));
                    message.append(" has already been modified.");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link ReservationRequestAlreadyModifiedReport}.
     */
    public static class ReservationRequestAlreadyModifiedException extends ReportRuntimeException implements ApiFaultException
    {
        public ReservationRequestAlreadyModifiedException(ReservationRequestAlreadyModifiedReport report)
        {
            this.report = report;
        }

        public ReservationRequestAlreadyModifiedException(Throwable throwable, ReservationRequestAlreadyModifiedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ReservationRequestAlreadyModifiedException(String id)
        {
            ReservationRequestAlreadyModifiedReport report = new ReservationRequestAlreadyModifiedReport();
            report.setId(id);
            this.report = report;
        }

        public ReservationRequestAlreadyModifiedException(Throwable throwable, String id)
        {
            super(throwable);
            ReservationRequestAlreadyModifiedReport report = new ReservationRequestAlreadyModifiedReport();
            report.setId(id);
            this.report = report;
        }

        public String getId()
        {
            return getReport().getId();
        }

        @Override
        public ReservationRequestAlreadyModifiedReport getReport()
        {
            return (ReservationRequestAlreadyModifiedReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ReservationRequestAlreadyModifiedReport) report;
        }
    }

    /**
     * Reservation request with identifier {@link #id} is deleted.
     */
    public static class ReservationRequestDeletedReport extends Report implements ApiFault
    {
        protected String id;

        public ReservationRequestDeletedReport()
        {
        }

        public ReservationRequestDeletedReport(String id)
        {
            setId(id);
        }

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
        public int getFaultCode()
        {
            return RESERVATION_REQUEST_DELETED_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
        }

        @Override
        public Exception getException()
        {
            return new ReservationRequestDeletedException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            id = (String) reportSerializer.getParameter("id", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("id", id);
        }

        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Reservation request with identifier ");
                    message.append((id == null ? "null" : id));
                    message.append(" is deleted.");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link ReservationRequestDeletedReport}.
     */
    public static class ReservationRequestDeletedException extends ReportRuntimeException implements ApiFaultException
    {
        public ReservationRequestDeletedException(ReservationRequestDeletedReport report)
        {
            this.report = report;
        }

        public ReservationRequestDeletedException(Throwable throwable, ReservationRequestDeletedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ReservationRequestDeletedException(String id)
        {
            ReservationRequestDeletedReport report = new ReservationRequestDeletedReport();
            report.setId(id);
            this.report = report;
        }

        public ReservationRequestDeletedException(Throwable throwable, String id)
        {
            super(throwable);
            ReservationRequestDeletedReport report = new ReservationRequestDeletedReport();
            report.setId(id);
            this.report = report;
        }

        public String getId()
        {
            return getReport().getId();
        }

        @Override
        public ReservationRequestDeletedReport getReport()
        {
            return (ReservationRequestDeletedReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ReservationRequestDeletedReport) report;
        }
    }

    /**
     * Reservation request time slot must not be empty.
     */
    public static class ReservationRequestEmptyDurationReport extends Report implements ApiFault
    {
        public ReservationRequestEmptyDurationReport()
        {
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return RESERVATION_REQUEST_EMPTY_DURATION_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
        }

        @Override
        public Exception getException()
        {
            return new ReservationRequestEmptyDurationException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
        }

        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Reservation request time slot must not be empty.");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link ReservationRequestEmptyDurationReport}.
     */
    public static class ReservationRequestEmptyDurationException extends ReportRuntimeException implements ApiFaultException
    {
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
            ReservationRequestEmptyDurationReport report = new ReservationRequestEmptyDurationReport();
            this.report = report;
        }

        public ReservationRequestEmptyDurationException(Throwable throwable)
        {
            super(throwable);
            ReservationRequestEmptyDurationReport report = new ReservationRequestEmptyDurationReport();
            this.report = report;
        }

        @Override
        public ReservationRequestEmptyDurationReport getReport()
        {
            return (ReservationRequestEmptyDurationReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ReservationRequestEmptyDurationReport) report;
        }
    }

    @Override
    protected void fillReportClasses()
    {
        addReportClass(AclInvalidRoleReport.class);
        addReportClass(SecurityMissingTokenReport.class);
        addReportClass(SecurityInvalidTokenReport.class);
        addReportClass(SecurityNotAuthorizedReport.class);
        addReportClass(DeviceCommandFailedReport.class);
        addReportClass(IdentifierInvalidReport.class);
        addReportClass(IdentifierInvalidDomainReport.class);
        addReportClass(IdentifierInvalidTypeReport.class);
        addReportClass(ReservationRequestNotModifiableReport.class);
        addReportClass(ReservationRequestAlreadyModifiedReport.class);
        addReportClass(ReservationRequestDeletedReport.class);
        addReportClass(ReservationRequestEmptyDurationReport.class);
    }
}
