package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.report.*;

/**
 * Auto-generated implementation of {@link AbstractReportSet}.
 *
 * @author cz.cesnet.shongo.tool-report-generator
 */
public class ControllerReportSet extends AbstractReportSet
{
    public static final int USER_NOT_EXISTS_CODE = 100;
    public static final int GROUP_NOT_EXISTS_CODE = 101;
    public static final int GROUP_ALREADY_EXISTS_CODE = 102;
    public static final int USER_ALREADY_IN_GROUP_CODE = 103;
    public static final int USER_NOT_IN_GROUP_CODE = 104;
    public static final int ACL_INVALID_OBJECT_ROLE_CODE = 105;
    public static final int SECURITY_MISSING_TOKEN_CODE = 106;
    public static final int SECURITY_INVALID_TOKEN_CODE = 107;
    public static final int SECURITY_NOT_AUTHORIZED_CODE = 108;
    public static final int DEVICE_COMMAND_FAILED_CODE = 109;
    public static final int IDENTIFIER_INVALID_CODE = 110;
    public static final int IDENTIFIER_INVALID_DOMAIN_CODE = 111;
    public static final int IDENTIFIER_INVALID_TYPE_CODE = 112;
    public static final int RESERVATION_REQUEST_NOT_MODIFIABLE_CODE = 113;
    public static final int RESERVATION_REQUEST_NOT_DELETABLE_CODE = 114;
    public static final int RESERVATION_REQUEST_NOT_REVERTIBLE_CODE = 115;
    public static final int RESERVATION_REQUEST_ALREADY_MODIFIED_CODE = 116;
    public static final int RESERVATION_REQUEST_DELETED_CODE = 117;
    public static final int RESERVATION_REQUEST_EMPTY_DURATION_CODE = 118;
    public static final int RESERVATION_REQUEST_NOT_REUSABLE_CODE = 119;
    public static final int EXECUTABLE_INVALID_CONFIGURATION_CODE = 120;
    public static final int EXECUTABLE_NOT_RECORDABLE_CODE = 121;
    public static final int EXECUTABLE_NOT_REUSABLE_CODE = 122;

    public static final String USER_NOT_EXISTS = "user-not-exists";
    public static final String GROUP_NOT_EXISTS = "group-not-exists";
    public static final String GROUP_ALREADY_EXISTS = "group-already-exists";
    public static final String USER_ALREADY_IN_GROUP = "user-already-in-group";
    public static final String USER_NOT_IN_GROUP = "user-not-in-group";
    public static final String ACL_INVALID_OBJECT_ROLE = "acl-invalid-object-role";
    public static final String SECURITY_MISSING_TOKEN = "security-missing-token";
    public static final String SECURITY_INVALID_TOKEN = "security-invalid-token";
    public static final String SECURITY_NOT_AUTHORIZED = "security-not-authorized";
    public static final String DEVICE_COMMAND_FAILED = "device-command-failed";
    public static final String IDENTIFIER_INVALID = "identifier-invalid";
    public static final String IDENTIFIER_INVALID_DOMAIN = "identifier-invalid-domain";
    public static final String IDENTIFIER_INVALID_TYPE = "identifier-invalid-type";
    public static final String RESERVATION_REQUEST_NOT_MODIFIABLE = "reservation-request-not-modifiable";
    public static final String RESERVATION_REQUEST_NOT_DELETABLE = "reservation-request-not-deletable";
    public static final String RESERVATION_REQUEST_NOT_REVERTIBLE = "reservation-request-not-revertible";
    public static final String RESERVATION_REQUEST_ALREADY_MODIFIED = "reservation-request-already-modified";
    public static final String RESERVATION_REQUEST_DELETED = "reservation-request-deleted";
    public static final String RESERVATION_REQUEST_EMPTY_DURATION = "reservation-request-empty-duration";
    public static final String RESERVATION_REQUEST_NOT_REUSABLE = "reservation-request-not-reusable";
    public static final String EXECUTABLE_INVALID_CONFIGURATION = "executable-invalid-configuration";
    public static final String EXECUTABLE_NOT_RECORDABLE = "executable-not-recordable";
    public static final String EXECUTABLE_NOT_REUSABLE = "executable-not-reusable";

    /**
     * Set of report messages.
     */
    private static final ReportSetMessages MESSAGES = new ReportSetMessages() {{
        addMessage(USER_NOT_EXISTS, new Report.UserType[]{}, Report.Language.ENGLISH, "User ${user} doesn't exist.");
        addMessage(GROUP_NOT_EXISTS, new Report.UserType[]{}, Report.Language.ENGLISH, "Group ${group} doesn't exist.");
        addMessage(GROUP_ALREADY_EXISTS, new Report.UserType[]{}, Report.Language.ENGLISH, "Group ${group} already exists.");
        addMessage(USER_ALREADY_IN_GROUP, new Report.UserType[]{}, Report.Language.ENGLISH, "User ${user} is already in group ${group}.");
        addMessage(USER_NOT_IN_GROUP, new Report.UserType[]{}, Report.Language.ENGLISH, "User ${user} isn't in group ${group}.");
        addMessage(ACL_INVALID_OBJECT_ROLE, new Report.UserType[]{}, Report.Language.ENGLISH, "ACL role ${role} is invalid for object ${objectType}.");
        addMessage(SECURITY_MISSING_TOKEN, new Report.UserType[]{}, Report.Language.ENGLISH, "Security token is missing but is required.");
        addMessage(SECURITY_INVALID_TOKEN, new Report.UserType[]{}, Report.Language.ENGLISH, "Invalid security token ${token}.");
        addMessage(SECURITY_NOT_AUTHORIZED, new Report.UserType[]{}, Report.Language.ENGLISH, "You are not authorized to ${action}.");
        addMessage(DEVICE_COMMAND_FAILED, new Report.UserType[]{}, Report.Language.ENGLISH, "Command ${command} for device ${device} failed: ${jadeReportMessage(jadeReport)}");
        addMessage(IDENTIFIER_INVALID, new Report.UserType[]{}, Report.Language.ENGLISH, "Identifier ${id} is invalid.");
        addMessage(IDENTIFIER_INVALID_DOMAIN, new Report.UserType[]{}, Report.Language.ENGLISH, "Identifier ${id} doesn't belong to domain ${requiredDomain}.");
        addMessage(IDENTIFIER_INVALID_TYPE, new Report.UserType[]{}, Report.Language.ENGLISH, "Identifier ${id} isn't of required type ${requiredType}.");
        addMessage(RESERVATION_REQUEST_NOT_MODIFIABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "Reservation request with identifier ${id} cannot be modified.");
        addMessage(RESERVATION_REQUEST_NOT_DELETABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "Reservation request with identifier ${id} cannot be deleted.");
        addMessage(RESERVATION_REQUEST_NOT_REVERTIBLE, new Report.UserType[]{}, Report.Language.ENGLISH, "Reservation request with identifier ${id} cannot be reverted.");
        addMessage(RESERVATION_REQUEST_ALREADY_MODIFIED, new Report.UserType[]{}, Report.Language.ENGLISH, "Reservation request with identifier ${id} has already been modified.");
        addMessage(RESERVATION_REQUEST_DELETED, new Report.UserType[]{}, Report.Language.ENGLISH, "Reservation request with identifier ${id} is deleted.");
        addMessage(RESERVATION_REQUEST_EMPTY_DURATION, new Report.UserType[]{}, Report.Language.ENGLISH, "Reservation request time slot must not be empty.");
        addMessage(RESERVATION_REQUEST_NOT_REUSABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "Reservation request with identifier ${id} cannot be reused.");
        addMessage(EXECUTABLE_INVALID_CONFIGURATION, new Report.UserType[]{}, Report.Language.ENGLISH, "Configuration ${configuration} is invalid for executable with identifier ${id}.");
        addMessage(EXECUTABLE_NOT_RECORDABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "Executable with identifier ${id} isn't recordable.");
        addMessage(EXECUTABLE_NOT_REUSABLE, new Report.UserType[]{}, Report.Language.ENGLISH, "Executable with identifier ${id} cannot be reused.");
    }};

    public static String getMessage(String reportId, Report.UserType userType, Report.Language language, org.joda.time.DateTimeZone timeZone, java.util.Map<String, Object> parameters)
    {
        return MESSAGES.getMessage(reportId, userType, language, timeZone, parameters);
    }

    /**
     * User {@link #user} doesn't exist.
     */
    public static class UserNotExistsReport extends AbstractReport implements ApiFault
    {
        protected String user;

        public UserNotExistsReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "user-not-exists";
        }

        public UserNotExistsReport(String user)
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
            return USER_NOT_EXISTS_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new UserNotExistsException(this);
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("user-not-exists", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link UserNotExistsReport}.
     */
    public static class UserNotExistsException extends ReportRuntimeException implements ApiFaultException
    {
        public UserNotExistsException(UserNotExistsReport report)
        {
            this.report = report;
        }

        public UserNotExistsException(Throwable throwable, UserNotExistsReport report)
        {
            super(throwable);
            this.report = report;
        }

        public UserNotExistsException(String user)
        {
            UserNotExistsReport report = new UserNotExistsReport();
            report.setUser(user);
            this.report = report;
        }

        public UserNotExistsException(Throwable throwable, String user)
        {
            super(throwable);
            UserNotExistsReport report = new UserNotExistsReport();
            report.setUser(user);
            this.report = report;
        }

        public String getUser()
        {
            return getReport().getUser();
        }

        @Override
        public UserNotExistsReport getReport()
        {
            return (UserNotExistsReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (UserNotExistsReport) report;
        }
    }

    /**
     * Group {@link #group} doesn't exist.
     */
    public static class GroupNotExistsReport extends AbstractReport implements ApiFault
    {
        protected String group;

        public GroupNotExistsReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "group-not-exists";
        }

        public GroupNotExistsReport(String group)
        {
            setGroup(group);
        }

        public String getGroup()
        {
            return group;
        }

        public void setGroup(String group)
        {
            this.group = group;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return GROUP_NOT_EXISTS_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new GroupNotExistsException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            group = (String) reportSerializer.getParameter("group", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("group", group);
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
            parameters.put("group", group);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("group-not-exists", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link GroupNotExistsReport}.
     */
    public static class GroupNotExistsException extends ReportRuntimeException implements ApiFaultException
    {
        public GroupNotExistsException(GroupNotExistsReport report)
        {
            this.report = report;
        }

        public GroupNotExistsException(Throwable throwable, GroupNotExistsReport report)
        {
            super(throwable);
            this.report = report;
        }

        public GroupNotExistsException(String group)
        {
            GroupNotExistsReport report = new GroupNotExistsReport();
            report.setGroup(group);
            this.report = report;
        }

        public GroupNotExistsException(Throwable throwable, String group)
        {
            super(throwable);
            GroupNotExistsReport report = new GroupNotExistsReport();
            report.setGroup(group);
            this.report = report;
        }

        public String getGroup()
        {
            return getReport().getGroup();
        }

        @Override
        public GroupNotExistsReport getReport()
        {
            return (GroupNotExistsReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (GroupNotExistsReport) report;
        }
    }

    /**
     * Group {@link #group} already exists.
     */
    public static class GroupAlreadyExistsReport extends AbstractReport implements ApiFault
    {
        protected String group;

        public GroupAlreadyExistsReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "group-already-exists";
        }

        public GroupAlreadyExistsReport(String group)
        {
            setGroup(group);
        }

        public String getGroup()
        {
            return group;
        }

        public void setGroup(String group)
        {
            this.group = group;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return GROUP_ALREADY_EXISTS_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new GroupAlreadyExistsException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            group = (String) reportSerializer.getParameter("group", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("group", group);
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
            parameters.put("group", group);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("group-already-exists", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link GroupAlreadyExistsReport}.
     */
    public static class GroupAlreadyExistsException extends ReportRuntimeException implements ApiFaultException
    {
        public GroupAlreadyExistsException(GroupAlreadyExistsReport report)
        {
            this.report = report;
        }

        public GroupAlreadyExistsException(Throwable throwable, GroupAlreadyExistsReport report)
        {
            super(throwable);
            this.report = report;
        }

        public GroupAlreadyExistsException(String group)
        {
            GroupAlreadyExistsReport report = new GroupAlreadyExistsReport();
            report.setGroup(group);
            this.report = report;
        }

        public GroupAlreadyExistsException(Throwable throwable, String group)
        {
            super(throwable);
            GroupAlreadyExistsReport report = new GroupAlreadyExistsReport();
            report.setGroup(group);
            this.report = report;
        }

        public String getGroup()
        {
            return getReport().getGroup();
        }

        @Override
        public GroupAlreadyExistsReport getReport()
        {
            return (GroupAlreadyExistsReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (GroupAlreadyExistsReport) report;
        }
    }

    /**
     * User {@link #user} is already in group {@link #group}.
     */
    public static class UserAlreadyInGroupReport extends AbstractReport implements ApiFault
    {
        protected String group;

        protected String user;

        public UserAlreadyInGroupReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "user-already-in-group";
        }

        public UserAlreadyInGroupReport(String group, String user)
        {
            setGroup(group);
            setUser(user);
        }

        public String getGroup()
        {
            return group;
        }

        public void setGroup(String group)
        {
            this.group = group;
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
            return USER_ALREADY_IN_GROUP_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new UserAlreadyInGroupException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            group = (String) reportSerializer.getParameter("group", String.class);
            user = (String) reportSerializer.getParameter("user", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("group", group);
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
            parameters.put("group", group);
            parameters.put("user", user);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("user-already-in-group", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link UserAlreadyInGroupReport}.
     */
    public static class UserAlreadyInGroupException extends ReportRuntimeException implements ApiFaultException
    {
        public UserAlreadyInGroupException(UserAlreadyInGroupReport report)
        {
            this.report = report;
        }

        public UserAlreadyInGroupException(Throwable throwable, UserAlreadyInGroupReport report)
        {
            super(throwable);
            this.report = report;
        }

        public UserAlreadyInGroupException(String group, String user)
        {
            UserAlreadyInGroupReport report = new UserAlreadyInGroupReport();
            report.setGroup(group);
            report.setUser(user);
            this.report = report;
        }

        public UserAlreadyInGroupException(Throwable throwable, String group, String user)
        {
            super(throwable);
            UserAlreadyInGroupReport report = new UserAlreadyInGroupReport();
            report.setGroup(group);
            report.setUser(user);
            this.report = report;
        }

        public String getGroup()
        {
            return getReport().getGroup();
        }

        public String getUser()
        {
            return getReport().getUser();
        }

        @Override
        public UserAlreadyInGroupReport getReport()
        {
            return (UserAlreadyInGroupReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (UserAlreadyInGroupReport) report;
        }
    }

    /**
     * User {@link #user} isn't in group {@link #group}.
     */
    public static class UserNotInGroupReport extends AbstractReport implements ApiFault
    {
        protected String group;

        protected String user;

        public UserNotInGroupReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "user-not-in-group";
        }

        public UserNotInGroupReport(String group, String user)
        {
            setGroup(group);
            setUser(user);
        }

        public String getGroup()
        {
            return group;
        }

        public void setGroup(String group)
        {
            this.group = group;
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
            return USER_NOT_IN_GROUP_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new UserNotInGroupException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            group = (String) reportSerializer.getParameter("group", String.class);
            user = (String) reportSerializer.getParameter("user", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("group", group);
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
            parameters.put("group", group);
            parameters.put("user", user);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("user-not-in-group", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link UserNotInGroupReport}.
     */
    public static class UserNotInGroupException extends ReportRuntimeException implements ApiFaultException
    {
        public UserNotInGroupException(UserNotInGroupReport report)
        {
            this.report = report;
        }

        public UserNotInGroupException(Throwable throwable, UserNotInGroupReport report)
        {
            super(throwable);
            this.report = report;
        }

        public UserNotInGroupException(String group, String user)
        {
            UserNotInGroupReport report = new UserNotInGroupReport();
            report.setGroup(group);
            report.setUser(user);
            this.report = report;
        }

        public UserNotInGroupException(Throwable throwable, String group, String user)
        {
            super(throwable);
            UserNotInGroupReport report = new UserNotInGroupReport();
            report.setGroup(group);
            report.setUser(user);
            this.report = report;
        }

        public String getGroup()
        {
            return getReport().getGroup();
        }

        public String getUser()
        {
            return getReport().getUser();
        }

        @Override
        public UserNotInGroupReport getReport()
        {
            return (UserNotInGroupReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (UserNotInGroupReport) report;
        }
    }

    /**
     * ACL role {@link #role} is invalid for object {@link #objectType}.
     */
    public static class AclInvalidObjectRoleReport extends AbstractReport implements ApiFault
    {
        protected String objectType;

        protected String role;

        public AclInvalidObjectRoleReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "acl-invalid-object-role";
        }

        public AclInvalidObjectRoleReport(String objectType, String role)
        {
            setObjectType(objectType);
            setRole(role);
        }

        public String getObjectType()
        {
            return objectType;
        }

        public void setObjectType(String objectType)
        {
            this.objectType = objectType;
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
            return ACL_INVALID_OBJECT_ROLE_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new AclInvalidObjectRoleException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            objectType = (String) reportSerializer.getParameter("objectType", String.class);
            role = (String) reportSerializer.getParameter("role", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("objectType", objectType);
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
            parameters.put("objectType", objectType);
            parameters.put("role", role);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("acl-invalid-object-role", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link AclInvalidObjectRoleReport}.
     */
    public static class AclInvalidObjectRoleException extends ReportRuntimeException implements ApiFaultException
    {
        public AclInvalidObjectRoleException(AclInvalidObjectRoleReport report)
        {
            this.report = report;
        }

        public AclInvalidObjectRoleException(Throwable throwable, AclInvalidObjectRoleReport report)
        {
            super(throwable);
            this.report = report;
        }

        public AclInvalidObjectRoleException(String objectType, String role)
        {
            AclInvalidObjectRoleReport report = new AclInvalidObjectRoleReport();
            report.setObjectType(objectType);
            report.setRole(role);
            this.report = report;
        }

        public AclInvalidObjectRoleException(Throwable throwable, String objectType, String role)
        {
            super(throwable);
            AclInvalidObjectRoleReport report = new AclInvalidObjectRoleReport();
            report.setObjectType(objectType);
            report.setRole(role);
            this.report = report;
        }

        public String getObjectType()
        {
            return getReport().getObjectType();
        }

        public String getRole()
        {
            return getReport().getRole();
        }

        @Override
        public AclInvalidObjectRoleReport getReport()
        {
            return (AclInvalidObjectRoleReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (AclInvalidObjectRoleReport) report;
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
            return SECURITY_MISSING_TOKEN_CODE;
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("security-missing-token", userType, language, timeZone, getParameters());
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
            return SECURITY_INVALID_TOKEN_CODE;
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("security-invalid-token", userType, language, timeZone, getParameters());
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
            return SECURITY_NOT_AUTHORIZED_CODE;
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("security-not-authorized", userType, language, timeZone, getParameters());
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
            return DEVICE_COMMAND_FAILED_CODE;
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("device-command-failed", userType, language, timeZone, getParameters());
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
            return IDENTIFIER_INVALID_CODE;
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("identifier-invalid", userType, language, timeZone, getParameters());
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
            return IDENTIFIER_INVALID_DOMAIN_CODE;
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("identifier-invalid-domain", userType, language, timeZone, getParameters());
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
            return IDENTIFIER_INVALID_TYPE_CODE;
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("identifier-invalid-type", userType, language, timeZone, getParameters());
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
            return RESERVATION_REQUEST_NOT_MODIFIABLE_CODE;
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("reservation-request-not-modifiable", userType, language, timeZone, getParameters());
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
            return RESERVATION_REQUEST_NOT_DELETABLE_CODE;
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("reservation-request-not-deletable", userType, language, timeZone, getParameters());
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
            return RESERVATION_REQUEST_NOT_REVERTIBLE_CODE;
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("reservation-request-not-revertible", userType, language, timeZone, getParameters());
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
            return RESERVATION_REQUEST_ALREADY_MODIFIED_CODE;
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("reservation-request-already-modified", userType, language, timeZone, getParameters());
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
            return RESERVATION_REQUEST_DELETED_CODE;
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("reservation-request-deleted", userType, language, timeZone, getParameters());
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
            return RESERVATION_REQUEST_EMPTY_DURATION_CODE;
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("reservation-request-empty-duration", userType, language, timeZone, getParameters());
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
            return RESERVATION_REQUEST_NOT_REUSABLE_CODE;
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("reservation-request-not-reusable", userType, language, timeZone, getParameters());
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

    /**
     * Configuration {@link #configuration} is invalid for executable with identifier {@link #id}.
     */
    public static class ExecutableInvalidConfigurationReport extends AbstractReport implements ApiFault
    {
        protected String id;

        protected String configuration;

        public ExecutableInvalidConfigurationReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "executable-invalid-configuration";
        }

        public ExecutableInvalidConfigurationReport(String id, String configuration)
        {
            setId(id);
            setConfiguration(configuration);
        }

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        public String getConfiguration()
        {
            return configuration;
        }

        public void setConfiguration(String configuration)
        {
            this.configuration = configuration;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return EXECUTABLE_INVALID_CONFIGURATION_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ExecutableInvalidConfigurationException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            id = (String) reportSerializer.getParameter("id", String.class);
            configuration = (String) reportSerializer.getParameter("configuration", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("id", id);
            reportSerializer.setParameter("configuration", configuration);
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
            parameters.put("configuration", configuration);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("executable-invalid-configuration", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ExecutableInvalidConfigurationReport}.
     */
    public static class ExecutableInvalidConfigurationException extends ReportRuntimeException implements ApiFaultException
    {
        public ExecutableInvalidConfigurationException(ExecutableInvalidConfigurationReport report)
        {
            this.report = report;
        }

        public ExecutableInvalidConfigurationException(Throwable throwable, ExecutableInvalidConfigurationReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ExecutableInvalidConfigurationException(String id, String configuration)
        {
            ExecutableInvalidConfigurationReport report = new ExecutableInvalidConfigurationReport();
            report.setId(id);
            report.setConfiguration(configuration);
            this.report = report;
        }

        public ExecutableInvalidConfigurationException(Throwable throwable, String id, String configuration)
        {
            super(throwable);
            ExecutableInvalidConfigurationReport report = new ExecutableInvalidConfigurationReport();
            report.setId(id);
            report.setConfiguration(configuration);
            this.report = report;
        }

        public String getId()
        {
            return getReport().getId();
        }

        public String getConfiguration()
        {
            return getReport().getConfiguration();
        }

        @Override
        public ExecutableInvalidConfigurationReport getReport()
        {
            return (ExecutableInvalidConfigurationReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ExecutableInvalidConfigurationReport) report;
        }
    }

    /**
     * Executable with identifier {@link #id} isn't recordable.
     */
    public static class ExecutableNotRecordableReport extends AbstractReport implements ApiFault
    {
        protected String id;

        public ExecutableNotRecordableReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "executable-not-recordable";
        }

        public ExecutableNotRecordableReport(String id)
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
            return EXECUTABLE_NOT_RECORDABLE_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ExecutableNotRecordableException(this);
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("executable-not-recordable", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ExecutableNotRecordableReport}.
     */
    public static class ExecutableNotRecordableException extends ReportRuntimeException implements ApiFaultException
    {
        public ExecutableNotRecordableException(ExecutableNotRecordableReport report)
        {
            this.report = report;
        }

        public ExecutableNotRecordableException(Throwable throwable, ExecutableNotRecordableReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ExecutableNotRecordableException(String id)
        {
            ExecutableNotRecordableReport report = new ExecutableNotRecordableReport();
            report.setId(id);
            this.report = report;
        }

        public ExecutableNotRecordableException(Throwable throwable, String id)
        {
            super(throwable);
            ExecutableNotRecordableReport report = new ExecutableNotRecordableReport();
            report.setId(id);
            this.report = report;
        }

        public String getId()
        {
            return getReport().getId();
        }

        @Override
        public ExecutableNotRecordableReport getReport()
        {
            return (ExecutableNotRecordableReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ExecutableNotRecordableReport) report;
        }
    }

    /**
     * Executable with identifier {@link #id} cannot be reused.
     */
    public static class ExecutableNotReusableReport extends AbstractReport implements ApiFault
    {
        protected String id;

        public ExecutableNotReusableReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "executable-not-reusable";
        }

        public ExecutableNotReusableReport(String id)
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
            return EXECUTABLE_NOT_REUSABLE_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ExecutableNotReusableException(this);
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
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("executable-not-reusable", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ExecutableNotReusableReport}.
     */
    public static class ExecutableNotReusableException extends ReportRuntimeException implements ApiFaultException
    {
        public ExecutableNotReusableException(ExecutableNotReusableReport report)
        {
            this.report = report;
        }

        public ExecutableNotReusableException(Throwable throwable, ExecutableNotReusableReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ExecutableNotReusableException(String id)
        {
            ExecutableNotReusableReport report = new ExecutableNotReusableReport();
            report.setId(id);
            this.report = report;
        }

        public ExecutableNotReusableException(Throwable throwable, String id)
        {
            super(throwable);
            ExecutableNotReusableReport report = new ExecutableNotReusableReport();
            report.setId(id);
            this.report = report;
        }

        public String getId()
        {
            return getReport().getId();
        }

        @Override
        public ExecutableNotReusableReport getReport()
        {
            return (ExecutableNotReusableReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ExecutableNotReusableReport) report;
        }
    }

    @Override
    protected void fillReportClasses()
    {
        addReportClass(UserNotExistsReport.class);
        addReportClass(GroupNotExistsReport.class);
        addReportClass(GroupAlreadyExistsReport.class);
        addReportClass(UserAlreadyInGroupReport.class);
        addReportClass(UserNotInGroupReport.class);
        addReportClass(AclInvalidObjectRoleReport.class);
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
        addReportClass(ExecutableInvalidConfigurationReport.class);
        addReportClass(ExecutableNotRecordableReport.class);
        addReportClass(ExecutableNotReusableReport.class);
    }
}
