package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.report.*;

/**
 * Auto-generated implementation of {@link AbstractReportSet}.
 *
 * @author cz.cesnet.shongo.tool-report-generator
 */
public class ControllerReportSet extends AbstractReportSet
{
    public static final int USER_NOT_EXIST_REPORT = 100;
    public static final int ACL_INVALID_ROLE_REPORT = 101;
    public static final int SECURITY_MISSING_TOKEN_REPORT = 102;
    public static final int SECURITY_INVALID_TOKEN_REPORT = 103;
    public static final int SECURITY_NOT_AUTHORIZED_REPORT = 104;
    public static final int DEVICE_COMMAND_FAILED_REPORT = 105;
    public static final int IDENTIFIER_INVALID_REPORT = 106;
    public static final int IDENTIFIER_INVALID_DOMAIN_REPORT = 107;
    public static final int IDENTIFIER_INVALID_TYPE_REPORT = 108;
    public static final int RESERVATION_REQUEST_NOT_MODIFIABLE_REPORT = 109;
    public static final int RESERVATION_REQUEST_NOT_DELETABLE_REPORT = 110;
    public static final int RESERVATION_REQUEST_NOT_REVERTIBLE_REPORT = 111;
    public static final int RESERVATION_REQUEST_ALREADY_MODIFIED_REPORT = 112;
    public static final int RESERVATION_REQUEST_DELETED_REPORT = 113;
    public static final int RESERVATION_REQUEST_EMPTY_DURATION_REPORT = 114;
    public static final int RESERVATION_REQUEST_NOT_REUSABLE_REPORT = 115;

    /**
     * Set of report messages.
     */
    private static final ReportSetMessages MESSAGES = new ReportSetMessages() {{
        addMessage("user-not-exist", new Report.UserType[]{}, Report.Language.ENGLISH, "User ${user} doesn't exist.");
        addMessage("acl-invalid-role", new Report.UserType[]{}, Report.Language.ENGLISH, "ACL Role ${role} is invalid for entity ${entity}.");
        addMessage("security-missing-token", new Report.UserType[]{}, Report.Language.ENGLISH, "Security token is missing but is required.");
        addMessage("security-invalid-token", new Report.UserType[]{}, Report.Language.ENGLISH, "Invalid security token ${token}.");
        addMessage("security-not-authorized", new Report.UserType[]{}, Report.Language.ENGLISH, "You are not authorized to ${action}.");
        addMessage("device-command-failed", new Report.UserType[]{}, Report.Language.ENGLISH, "Command ${command} for device ${device} failed: ${jadeReportMessage(jadeReport)}");
        addMessage("identifier-invalid", new Report.UserType[]{}, Report.Language.ENGLISH, "Identifier ${id} is invalid.");
        addMessage("identifier-invalid-domain", new Report.UserType[]{}, Report.Language.ENGLISH, "Identifier ${id} doesn't belong to domain ${requiredDomain}.");
        addMessage("identifier-invalid-type", new Report.UserType[]{}, Report.Language.ENGLISH, "Identifier ${id} isn't of required type ${requiredType}.");
        addMessage("reservation-request-not-modifiable", new Report.UserType[]{}, Report.Language.ENGLISH, "Reservation request with identifier ${id} cannot be modified.");
        addMessage("reservation-request-not-deletable", new Report.UserType[]{}, Report.Language.ENGLISH, "Reservation request with identifier ${id} cannot be deleted.");
        addMessage("reservation-request-not-revertible", new Report.UserType[]{}, Report.Language.ENGLISH, "Reservation request with identifier ${id} cannot be reverted.");
        addMessage("reservation-request-already-modified", new Report.UserType[]{}, Report.Language.ENGLISH, "Reservation request with identifier ${id} has already been modified.");
        addMessage("reservation-request-deleted", new Report.UserType[]{}, Report.Language.ENGLISH, "Reservation request with identifier ${id} is deleted.");
        addMessage("reservation-request-empty-duration", new Report.UserType[]{}, Report.Language.ENGLISH, "Reservation request time slot must not be empty.");
        addMessage("reservation-request-not-reusable", new Report.UserType[]{}, Report.Language.ENGLISH, "Reservation request with identifier ${id} cannot be reused.");
    }};

    public static String getMessage(String reportId, Report.UserType userType, Report.Language language, java.util.Map<String, Object> parameters)
    {
        return MESSAGES.getMessage(reportId, userType, language, parameters);
    }

    /**
     * User {@link #user} doesn't exist.
     */
    public static class UserNotExistReport extends AbstractReport implements ApiFault
    {
        protected String user;

        public UserNotExistReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "user-not-exist";
        }

        public UserNotExistReport(String user)
        {
            setUser(user);
        }

        public String getUser()
        {
            return user;
        }

        public void setUser(String user)
        {
            this.user = user;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return USER_NOT_EXIST_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new UserNotExistException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            user = (String) reportSerializer.getParameter("user", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("user", user);
        }

        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("user", user);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("user-not-exist", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link UserNotExistReport}.
     */
    public static class UserNotExistException extends ReportRuntimeException implements ApiFaultException
    {
        public UserNotExistException(UserNotExistReport report)
        {
            this.report = report;
        }

        public UserNotExistException(Throwable throwable, UserNotExistReport report)
        {
            super(throwable);
            this.report = report;
        }

        public UserNotExistException(String user)
        {
            UserNotExistReport report = new UserNotExistReport();
            report.setUser(user);
            this.report = report;
        }

        public UserNotExistException(Throwable throwable, String user)
        {
            super(throwable);
            UserNotExistReport report = new UserNotExistReport();
            report.setUser(user);
            this.report = report;
        }

        public String getUser()
        {
            return getReport().getUser();
        }

        @Override
        public UserNotExistReport getReport()
        {
            return (UserNotExistReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (UserNotExistReport) report;
        }
    }

    /**
     * ACL Role {@link #role} is invalid for entity {@link #entity}.
     */
    public static class AclInvalidRoleReport extends AbstractReport implements ApiFault
    {
        protected String entity;

        protected String role;

        public AclInvalidRoleReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "acl-invalid-role";
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
            return getMessage(UserType.USER, Language.ENGLISH);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("entity", entity);
            parameters.put("role", role);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("acl-invalid-role", userType, language, getParameters());
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
    public static class SecurityMissingTokenReport extends AbstractReport implements ApiFault
    {
        public SecurityMissingTokenReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "security-missing-token";
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
            return getMessage(UserType.USER, Language.ENGLISH);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("security-missing-token", userType, language, getParameters());
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
    public static class SecurityInvalidTokenReport extends AbstractReport implements ApiFault
    {
        protected String token;

        public SecurityInvalidTokenReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "security-invalid-token";
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
            return getMessage(UserType.USER, Language.ENGLISH);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("token", token);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("security-invalid-token", userType, language, getParameters());
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
    public static class SecurityNotAuthorizedReport extends AbstractReport implements ApiFault
    {
        protected String action;

        public SecurityNotAuthorizedReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "security-not-authorized";
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
            return getMessage(UserType.USER, Language.ENGLISH);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("action", action);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("security-not-authorized", userType, language, getParameters());
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
    public static class DeviceCommandFailedReport extends AbstractReport implements ApiFault, ResourceReport
    {
        protected String device;

        protected String command;

        protected cz.cesnet.shongo.JadeReport jadeReport;

        public DeviceCommandFailedReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "device-command-failed";
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
            return getMessage(UserType.USER, Language.ENGLISH);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN | VISIBLE_TO_RESOURCE_ADMIN;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("device", device);
            parameters.put("command", command);
            parameters.put("jadeReport", jadeReport);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("device-command-failed", userType, language, getParameters());
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
    public static class IdentifierInvalidReport extends AbstractReport implements ApiFault
    {
        protected String id;

        public IdentifierInvalidReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "identifier-invalid";
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
            return getMessage(UserType.USER, Language.ENGLISH);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("id", id);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("identifier-invalid", userType, language, getParameters());
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
    public static class IdentifierInvalidDomainReport extends AbstractReport implements ApiFault
    {
        protected String id;

        protected String requiredDomain;

        public IdentifierInvalidDomainReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "identifier-invalid-domain";
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
            return getMessage(UserType.USER, Language.ENGLISH);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("id", id);
            parameters.put("requiredDomain", requiredDomain);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("identifier-invalid-domain", userType, language, getParameters());
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
    public static class IdentifierInvalidTypeReport extends AbstractReport implements ApiFault
    {
        protected String id;

        protected String requiredType;

        public IdentifierInvalidTypeReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "identifier-invalid-type";
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
            return getMessage(UserType.USER, Language.ENGLISH);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("id", id);
            parameters.put("requiredType", requiredType);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("identifier-invalid-type", userType, language, getParameters());
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
     * Reservation request with identifier {@link #id} cannot be modified.
     */
    public static class ReservationRequestNotModifiableReport extends AbstractReport implements ApiFault
    {
        protected String id;

        public ReservationRequestNotModifiableReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "reservation-request-not-modifiable";
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
            return getMessage(UserType.USER, Language.ENGLISH);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("id", id);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("reservation-request-not-modifiable", userType, language, getParameters());
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
     * Reservation request with identifier {@link #id} cannot be deleted.
     */
    public static class ReservationRequestNotDeletableReport extends AbstractReport implements ApiFault
    {
        protected String id;

        public ReservationRequestNotDeletableReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "reservation-request-not-deletable";
        }

        public ReservationRequestNotDeletableReport(String id)
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
            return RESERVATION_REQUEST_NOT_DELETABLE_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ReservationRequestNotDeletableException(this);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("id", id);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("reservation-request-not-deletable", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link ReservationRequestNotDeletableReport}.
     */
    public static class ReservationRequestNotDeletableException extends ReportRuntimeException implements ApiFaultException
    {
        public ReservationRequestNotDeletableException(ReservationRequestNotDeletableReport report)
        {
            this.report = report;
        }

        public ReservationRequestNotDeletableException(Throwable throwable, ReservationRequestNotDeletableReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ReservationRequestNotDeletableException(String id)
        {
            ReservationRequestNotDeletableReport report = new ReservationRequestNotDeletableReport();
            report.setId(id);
            this.report = report;
        }

        public ReservationRequestNotDeletableException(Throwable throwable, String id)
        {
            super(throwable);
            ReservationRequestNotDeletableReport report = new ReservationRequestNotDeletableReport();
            report.setId(id);
            this.report = report;
        }

        public String getId()
        {
            return getReport().getId();
        }

        @Override
        public ReservationRequestNotDeletableReport getReport()
        {
            return (ReservationRequestNotDeletableReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ReservationRequestNotDeletableReport) report;
        }
    }

    /**
     * Reservation request with identifier {@link #id} cannot be reverted.
     */
    public static class ReservationRequestNotRevertibleReport extends AbstractReport implements ApiFault
    {
        protected String id;

        public ReservationRequestNotRevertibleReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "reservation-request-not-revertible";
        }

        public ReservationRequestNotRevertibleReport(String id)
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
            return RESERVATION_REQUEST_NOT_REVERTIBLE_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ReservationRequestNotRevertibleException(this);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("id", id);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("reservation-request-not-revertible", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link ReservationRequestNotRevertibleReport}.
     */
    public static class ReservationRequestNotRevertibleException extends ReportRuntimeException implements ApiFaultException
    {
        public ReservationRequestNotRevertibleException(ReservationRequestNotRevertibleReport report)
        {
            this.report = report;
        }

        public ReservationRequestNotRevertibleException(Throwable throwable, ReservationRequestNotRevertibleReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ReservationRequestNotRevertibleException(String id)
        {
            ReservationRequestNotRevertibleReport report = new ReservationRequestNotRevertibleReport();
            report.setId(id);
            this.report = report;
        }

        public ReservationRequestNotRevertibleException(Throwable throwable, String id)
        {
            super(throwable);
            ReservationRequestNotRevertibleReport report = new ReservationRequestNotRevertibleReport();
            report.setId(id);
            this.report = report;
        }

        public String getId()
        {
            return getReport().getId();
        }

        @Override
        public ReservationRequestNotRevertibleReport getReport()
        {
            return (ReservationRequestNotRevertibleReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ReservationRequestNotRevertibleReport) report;
        }
    }

    /**
     * Reservation request with identifier {@link #id} has already been modified.
     */
    public static class ReservationRequestAlreadyModifiedReport extends AbstractReport implements ApiFault
    {
        protected String id;

        public ReservationRequestAlreadyModifiedReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "reservation-request-already-modified";
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
            return getMessage(UserType.USER, Language.ENGLISH);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("id", id);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("reservation-request-already-modified", userType, language, getParameters());
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
    public static class ReservationRequestDeletedReport extends AbstractReport implements ApiFault
    {
        protected String id;

        public ReservationRequestDeletedReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "reservation-request-deleted";
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
            return getMessage(UserType.USER, Language.ENGLISH);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("id", id);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("reservation-request-deleted", userType, language, getParameters());
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
    public static class ReservationRequestEmptyDurationReport extends AbstractReport implements ApiFault
    {
        public ReservationRequestEmptyDurationReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "reservation-request-empty-duration";
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
            return getMessage(UserType.USER, Language.ENGLISH);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("reservation-request-empty-duration", userType, language, getParameters());
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

    /**
     * Reservation request with identifier {@link #id} cannot be reused.
     */
    public static class ReservationRequestNotReusableReport extends AbstractReport implements ApiFault
    {
        protected String id;

        public ReservationRequestNotReusableReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "reservation-request-not-reusable";
        }

        public ReservationRequestNotReusableReport(String id)
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
            return RESERVATION_REQUEST_NOT_REUSABLE_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ReservationRequestNotReusableException(this);
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
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("id", id);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language)
        {
            return MESSAGES.getMessage("reservation-request-not-reusable", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link ReservationRequestNotReusableReport}.
     */
    public static class ReservationRequestNotReusableException extends ReportRuntimeException implements ApiFaultException
    {
        public ReservationRequestNotReusableException(ReservationRequestNotReusableReport report)
        {
            this.report = report;
        }

        public ReservationRequestNotReusableException(Throwable throwable, ReservationRequestNotReusableReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ReservationRequestNotReusableException(String id)
        {
            ReservationRequestNotReusableReport report = new ReservationRequestNotReusableReport();
            report.setId(id);
            this.report = report;
        }

        public ReservationRequestNotReusableException(Throwable throwable, String id)
        {
            super(throwable);
            ReservationRequestNotReusableReport report = new ReservationRequestNotReusableReport();
            report.setId(id);
            this.report = report;
        }

        public String getId()
        {
            return getReport().getId();
        }

        @Override
        public ReservationRequestNotReusableReport getReport()
        {
            return (ReservationRequestNotReusableReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ReservationRequestNotReusableReport) report;
        }
    }

    @Override
    protected void fillReportClasses()
    {
        addReportClass(UserNotExistReport.class);
        addReportClass(AclInvalidRoleReport.class);
        addReportClass(SecurityMissingTokenReport.class);
        addReportClass(SecurityInvalidTokenReport.class);
        addReportClass(SecurityNotAuthorizedReport.class);
        addReportClass(DeviceCommandFailedReport.class);
        addReportClass(IdentifierInvalidReport.class);
        addReportClass(IdentifierInvalidDomainReport.class);
        addReportClass(IdentifierInvalidTypeReport.class);
        addReportClass(ReservationRequestNotModifiableReport.class);
        addReportClass(ReservationRequestNotDeletableReport.class);
        addReportClass(ReservationRequestNotRevertibleReport.class);
        addReportClass(ReservationRequestAlreadyModifiedReport.class);
        addReportClass(ReservationRequestDeletedReport.class);
        addReportClass(ReservationRequestEmptyDurationReport.class);
        addReportClass(ReservationRequestNotReusableReport.class);
    }
}
