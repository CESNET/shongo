package cz.cesnet.shongo;

import cz.cesnet.shongo.report.*;

/**
 * Auto-generated implementation of {@link AbstractReportSet}.
 *
 * @author cz.cesnet.shongo.tool-report-generator
 */
public class CommonReportSet extends AbstractReportSet
{
    public static final int UNKNOWN_ERROR_CODE = 0;
    public static final int TYPE_MISMATCH_CODE = 1;
    public static final int TYPE_ILLEGAL_VALUE_CODE = 2;
    public static final int VALUE_MAXIMUM_LENGTH_EXCEEDED_CODE = 3;
    public static final int CLASS_UNDEFINED_CODE = 4;
    public static final int CLASS_INSTANTIATION_ERROR_CODE = 5;
    public static final int CLASS_ATTRIBUTE_UNDEFINED_CODE = 6;
    public static final int CLASS_ATTRIBUTE_TYPE_MISMATCH_CODE = 7;
    public static final int CLASS_ATTRIBUTE_REQUIRED_CODE = 8;
    public static final int CLASS_ATTRIBUTE_VALUE_MAXIMUM_LENGTH_EXCEEDED_CODE = 9;
    public static final int CLASS_ATTRIBUTE_READONLY_CODE = 10;
    public static final int CLASS_COLLECTION_REQUIRED_CODE = 11;
    public static final int COLLECTION_ITEM_NULL_CODE = 12;
    public static final int COLLECTION_ITEM_TYPE_MISMATCH_CODE = 13;
    public static final int OBJECT_NOT_EXISTS_CODE = 14;
    public static final int OBJECT_INVALID_CODE = 15;
    public static final int OBJECT_NOT_DELETABLE_REFERENCED_CODE = 16;
    public static final int METHOD_NOT_DEFINED_CODE = 17;

    public static final String UNKNOWN_ERROR = "unknown-error";
    public static final String TYPE_MISMATCH = "type-mismatch";
    public static final String TYPE_ILLEGAL_VALUE = "type-illegal-value";
    public static final String VALUE_MAXIMUM_LENGTH_EXCEEDED = "value-maximum-length-exceeded";
    public static final String CLASS_UNDEFINED = "class-undefined";
    public static final String CLASS_INSTANTIATION_ERROR = "class-instantiation-error";
    public static final String CLASS_ATTRIBUTE_UNDEFINED = "class-attribute-undefined";
    public static final String CLASS_ATTRIBUTE_TYPE_MISMATCH = "class-attribute-type-mismatch";
    public static final String CLASS_ATTRIBUTE_REQUIRED = "class-attribute-required";
    public static final String CLASS_ATTRIBUTE_VALUE_MAXIMUM_LENGTH_EXCEEDED = "class-attribute-value-maximum-length-exceeded";
    public static final String CLASS_ATTRIBUTE_READONLY = "class-attribute-readonly";
    public static final String CLASS_COLLECTION_REQUIRED = "class-collection-required";
    public static final String COLLECTION_ITEM_NULL = "collection-item-null";
    public static final String COLLECTION_ITEM_TYPE_MISMATCH = "collection-item-type-mismatch";
    public static final String OBJECT_NOT_EXISTS = "object-not-exists";
    public static final String OBJECT_INVALID = "object-invalid";
    public static final String OBJECT_NOT_DELETABLE_REFERENCED = "object-not-deletable-referenced";
    public static final String METHOD_NOT_DEFINED = "method-not-defined";

    /**
     * Set of report messages.
     */
    private static final ReportSetMessages MESSAGES = new ReportSetMessages() {{
        addMessage(UNKNOWN_ERROR, new Report.UserType[]{}, Report.Language.ENGLISH, "Unknown error: ${description}");
        addMessage(UNKNOWN_ERROR, new Report.UserType[]{Report.UserType.USER}, Report.Language.ENGLISH, "Unknown error.");
        addMessage(TYPE_MISMATCH, new Report.UserType[]{}, Report.Language.ENGLISH, "Type mismatch. Present type ${presentType} doesn't match required type ${requiredType}.");
        addMessage(TYPE_ILLEGAL_VALUE, new Report.UserType[]{}, Report.Language.ENGLISH, "Value ${value} is illegal for type ${type}.");
        addMessage(VALUE_MAXIMUM_LENGTH_EXCEEDED, new Report.UserType[]{}, Report.Language.ENGLISH, "Value '${value}' exceeds the maximum length ${maximumLength}.");
        addMessage(CLASS_UNDEFINED, new Report.UserType[]{}, Report.Language.ENGLISH, "Class ${class} is not defined.");
        addMessage(CLASS_INSTANTIATION_ERROR, new Report.UserType[]{}, Report.Language.ENGLISH, "Class ${class} cannot be instanced.");
        addMessage(CLASS_ATTRIBUTE_UNDEFINED, new Report.UserType[]{}, Report.Language.ENGLISH, "Attribute ${attribute} is not defined in class ${class}.");
        addMessage(CLASS_ATTRIBUTE_TYPE_MISMATCH, new Report.UserType[]{}, Report.Language.ENGLISH, "Type mismatch of value in attribute ${attribute} in class ${class}. Present type ${presentType} doesn't match required type ${requiredType}.");
        addMessage(CLASS_ATTRIBUTE_REQUIRED, new Report.UserType[]{}, Report.Language.ENGLISH, "Attribute ${attribute} in class ${class} wasn't present but it is required.");
        addMessage(CLASS_ATTRIBUTE_VALUE_MAXIMUM_LENGTH_EXCEEDED, new Report.UserType[]{}, Report.Language.ENGLISH, "Value for attribute ${attribute} in class ${class} exceeds the maximum length ${maximumLength}.");
        addMessage(CLASS_ATTRIBUTE_READONLY, new Report.UserType[]{}, Report.Language.ENGLISH, "Value for attribute ${attribute} in class ${class} was present but the attribute is read-only.");
        addMessage(CLASS_COLLECTION_REQUIRED, new Report.UserType[]{}, Report.Language.ENGLISH, "Collection ${collection} in class ${class} wasn't present or was empty but it is required.");
        addMessage(COLLECTION_ITEM_NULL, new Report.UserType[]{}, Report.Language.ENGLISH, "Null item cannot be present in collection ${collection}.");
        addMessage(COLLECTION_ITEM_TYPE_MISMATCH, new Report.UserType[]{}, Report.Language.ENGLISH, "Collection ${collection} contains item of type ${presentType} which doesn't match the required type ${requiredType}.");
        addMessage(OBJECT_NOT_EXISTS, new Report.UserType[]{}, Report.Language.ENGLISH, "Object ${objectType} with identifier ${objectId} doesn't exist.");
        addMessage(OBJECT_INVALID, new Report.UserType[]{}, Report.Language.ENGLISH, "Object ${objectId} validation failed: ${reason}");
        addMessage(OBJECT_NOT_DELETABLE_REFERENCED, new Report.UserType[]{}, Report.Language.ENGLISH, "Object ${objectType} with identifier ${objectId} cannot be deleted because it is still referenced.");
        addMessage(METHOD_NOT_DEFINED, new Report.UserType[]{}, Report.Language.ENGLISH, "Method ${method} is not defined.");
    }};

    public static String getMessage(String reportId, Report.UserType userType, Report.Language language, org.joda.time.DateTimeZone timeZone, java.util.Map<String, Object> parameters)
    {
        return MESSAGES.getMessage(reportId, userType, language, timeZone, parameters);
    }

    /**
     * Unknown error: {@link #description}
     */
    public static class UnknownErrorReport extends AbstractReport implements ApiFault
    {
        protected String description;

        public UnknownErrorReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "unknown-error";
        }

        public UnknownErrorReport(String description)
        {
            setDescription(description);
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return UNKNOWN_ERROR_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new UnknownErrorException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            description = (String) reportSerializer.getParameter("description", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("description", description);
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
            parameters.put("description", description);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("unknown-error", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link UnknownErrorReport}.
     */
    public static class UnknownErrorException extends ReportRuntimeException implements ApiFaultException
    {
        public UnknownErrorException(UnknownErrorReport report)
        {
            this.report = report;
        }

        public UnknownErrorException(Throwable throwable, UnknownErrorReport report)
        {
            super(throwable);
            this.report = report;
        }

        public UnknownErrorException(String description)
        {
            UnknownErrorReport report = new UnknownErrorReport();
            report.setDescription(description);
            this.report = report;
        }

        public UnknownErrorException(Throwable throwable, String description)
        {
            super(throwable);
            UnknownErrorReport report = new UnknownErrorReport();
            report.setDescription(description);
            this.report = report;
        }

        public String getDescription()
        {
            return getReport().getDescription();
        }

        @Override
        public UnknownErrorReport getReport()
        {
            return (UnknownErrorReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (UnknownErrorReport) report;
        }
    }

    /**
     * Type mismatch. Present type {@link #presentType} doesn't match required type {@link #requiredType}.
     */
    public static class TypeMismatchReport extends AbstractReport implements ApiFault
    {
        protected String requiredType;

        protected String presentType;

        public TypeMismatchReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "type-mismatch";
        }

        public TypeMismatchReport(String requiredType, String presentType)
        {
            setRequiredType(requiredType);
            setPresentType(presentType);
        }

        public String getRequiredType()
        {
            return requiredType;
        }

        public void setRequiredType(String requiredType)
        {
            this.requiredType = requiredType;
        }

        public String getPresentType()
        {
            return presentType;
        }

        public void setPresentType(String presentType)
        {
            this.presentType = presentType;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return TYPE_MISMATCH_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new TypeMismatchException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            requiredType = (String) reportSerializer.getParameter("requiredType", String.class);
            presentType = (String) reportSerializer.getParameter("presentType", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("requiredType", requiredType);
            reportSerializer.setParameter("presentType", presentType);
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
            parameters.put("requiredType", requiredType);
            parameters.put("presentType", presentType);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("type-mismatch", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link TypeMismatchReport}.
     */
    public static class TypeMismatchException extends ReportRuntimeException implements ApiFaultException
    {
        public TypeMismatchException(TypeMismatchReport report)
        {
            this.report = report;
        }

        public TypeMismatchException(Throwable throwable, TypeMismatchReport report)
        {
            super(throwable);
            this.report = report;
        }

        public TypeMismatchException(String requiredType, String presentType)
        {
            TypeMismatchReport report = new TypeMismatchReport();
            report.setRequiredType(requiredType);
            report.setPresentType(presentType);
            this.report = report;
        }

        public TypeMismatchException(Throwable throwable, String requiredType, String presentType)
        {
            super(throwable);
            TypeMismatchReport report = new TypeMismatchReport();
            report.setRequiredType(requiredType);
            report.setPresentType(presentType);
            this.report = report;
        }

        public String getRequiredType()
        {
            return getReport().getRequiredType();
        }

        public String getPresentType()
        {
            return getReport().getPresentType();
        }

        @Override
        public TypeMismatchReport getReport()
        {
            return (TypeMismatchReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (TypeMismatchReport) report;
        }
    }

    /**
     * Value {@link #value} is illegal for type {@link #typeName}.
     */
    public static class TypeIllegalValueReport extends AbstractReport implements ApiFault
    {
        protected String typeName;

        protected String value;

        public TypeIllegalValueReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "type-illegal-value";
        }

        public TypeIllegalValueReport(String typeName, String value)
        {
            setTypeName(typeName);
            setValue(value);
        }

        public String getTypeName()
        {
            return typeName;
        }

        public void setTypeName(String typeName)
        {
            this.typeName = typeName;
        }

        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return TYPE_ILLEGAL_VALUE_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new TypeIllegalValueException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            typeName = (String) reportSerializer.getParameter("typeName", String.class);
            value = (String) reportSerializer.getParameter("value", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("typeName", typeName);
            reportSerializer.setParameter("value", value);
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
            parameters.put("type", typeName);
            parameters.put("value", value);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("type-illegal-value", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link TypeIllegalValueReport}.
     */
    public static class TypeIllegalValueException extends ReportRuntimeException implements ApiFaultException
    {
        public TypeIllegalValueException(TypeIllegalValueReport report)
        {
            this.report = report;
        }

        public TypeIllegalValueException(Throwable throwable, TypeIllegalValueReport report)
        {
            super(throwable);
            this.report = report;
        }

        public TypeIllegalValueException(String typeName, String value)
        {
            TypeIllegalValueReport report = new TypeIllegalValueReport();
            report.setTypeName(typeName);
            report.setValue(value);
            this.report = report;
        }

        public TypeIllegalValueException(Throwable throwable, String typeName, String value)
        {
            super(throwable);
            TypeIllegalValueReport report = new TypeIllegalValueReport();
            report.setTypeName(typeName);
            report.setValue(value);
            this.report = report;
        }

        public String getTypeName()
        {
            return getReport().getTypeName();
        }

        public String getValue()
        {
            return getReport().getValue();
        }

        @Override
        public TypeIllegalValueReport getReport()
        {
            return (TypeIllegalValueReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (TypeIllegalValueReport) report;
        }
    }

    /**
     * Value '{@link #value}' exceeds the maximum length {@link #maximumLength}.
     */
    public static class ValueMaximumLengthExceededReport extends AbstractReport implements ApiFault
    {
        protected String value;

        protected Integer maximumLength;

        public ValueMaximumLengthExceededReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "value-maximum-length-exceeded";
        }

        public ValueMaximumLengthExceededReport(String value, Integer maximumLength)
        {
            setValue(value);
            setMaximumLength(maximumLength);
        }

        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }

        public Integer getMaximumLength()
        {
            return maximumLength;
        }

        public void setMaximumLength(Integer maximumLength)
        {
            this.maximumLength = maximumLength;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return VALUE_MAXIMUM_LENGTH_EXCEEDED_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ValueMaximumLengthExceededException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            value = (String) reportSerializer.getParameter("value", String.class);
            maximumLength = (Integer) reportSerializer.getParameter("maximumLength", Integer.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("value", value);
            reportSerializer.setParameter("maximumLength", maximumLength);
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
            parameters.put("value", value);
            parameters.put("maximumLength", maximumLength);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("value-maximum-length-exceeded", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ValueMaximumLengthExceededReport}.
     */
    public static class ValueMaximumLengthExceededException extends ReportRuntimeException implements ApiFaultException
    {
        public ValueMaximumLengthExceededException(ValueMaximumLengthExceededReport report)
        {
            this.report = report;
        }

        public ValueMaximumLengthExceededException(Throwable throwable, ValueMaximumLengthExceededReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ValueMaximumLengthExceededException(String value, Integer maximumLength)
        {
            ValueMaximumLengthExceededReport report = new ValueMaximumLengthExceededReport();
            report.setValue(value);
            report.setMaximumLength(maximumLength);
            this.report = report;
        }

        public ValueMaximumLengthExceededException(Throwable throwable, String value, Integer maximumLength)
        {
            super(throwable);
            ValueMaximumLengthExceededReport report = new ValueMaximumLengthExceededReport();
            report.setValue(value);
            report.setMaximumLength(maximumLength);
            this.report = report;
        }

        public String getValue()
        {
            return getReport().getValue();
        }

        public Integer getMaximumLength()
        {
            return getReport().getMaximumLength();
        }

        @Override
        public ValueMaximumLengthExceededReport getReport()
        {
            return (ValueMaximumLengthExceededReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ValueMaximumLengthExceededReport) report;
        }
    }

    /**
     * Class {@link #className} is not defined.
     */
    public static class ClassUndefinedReport extends AbstractReport implements ApiFault
    {
        protected String className;

        public ClassUndefinedReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "class-undefined";
        }

        public ClassUndefinedReport(String className)
        {
            setClassName(className);
        }

        public String getClassName()
        {
            return className;
        }

        public void setClassName(String className)
        {
            this.className = className;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return CLASS_UNDEFINED_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ClassUndefinedException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            className = (String) reportSerializer.getParameter("className", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("className", className);
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
            parameters.put("class", className);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("class-undefined", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ClassUndefinedReport}.
     */
    public static class ClassUndefinedException extends ReportRuntimeException implements ApiFaultException
    {
        public ClassUndefinedException(ClassUndefinedReport report)
        {
            this.report = report;
        }

        public ClassUndefinedException(Throwable throwable, ClassUndefinedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ClassUndefinedException(String className)
        {
            ClassUndefinedReport report = new ClassUndefinedReport();
            report.setClassName(className);
            this.report = report;
        }

        public ClassUndefinedException(Throwable throwable, String className)
        {
            super(throwable);
            ClassUndefinedReport report = new ClassUndefinedReport();
            report.setClassName(className);
            this.report = report;
        }

        public String getClassName()
        {
            return getReport().getClassName();
        }

        @Override
        public ClassUndefinedReport getReport()
        {
            return (ClassUndefinedReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ClassUndefinedReport) report;
        }
    }

    /**
     * Class {@link #className} cannot be instanced.
     */
    public static class ClassInstantiationErrorReport extends AbstractReport implements ApiFault
    {
        protected String className;

        public ClassInstantiationErrorReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "class-instantiation-error";
        }

        public ClassInstantiationErrorReport(String className)
        {
            setClassName(className);
        }

        public String getClassName()
        {
            return className;
        }

        public void setClassName(String className)
        {
            this.className = className;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return CLASS_INSTANTIATION_ERROR_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ClassInstantiationErrorException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            className = (String) reportSerializer.getParameter("className", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("className", className);
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
            parameters.put("class", className);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("class-instantiation-error", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ClassInstantiationErrorReport}.
     */
    public static class ClassInstantiationErrorException extends ReportRuntimeException implements ApiFaultException
    {
        public ClassInstantiationErrorException(ClassInstantiationErrorReport report)
        {
            this.report = report;
        }

        public ClassInstantiationErrorException(Throwable throwable, ClassInstantiationErrorReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ClassInstantiationErrorException(String className)
        {
            ClassInstantiationErrorReport report = new ClassInstantiationErrorReport();
            report.setClassName(className);
            this.report = report;
        }

        public ClassInstantiationErrorException(Throwable throwable, String className)
        {
            super(throwable);
            ClassInstantiationErrorReport report = new ClassInstantiationErrorReport();
            report.setClassName(className);
            this.report = report;
        }

        public String getClassName()
        {
            return getReport().getClassName();
        }

        @Override
        public ClassInstantiationErrorReport getReport()
        {
            return (ClassInstantiationErrorReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ClassInstantiationErrorReport) report;
        }
    }

    /**
     * Attribute {@link #attribute} is not defined in class {@link #className}.
     */
    public static class ClassAttributeUndefinedReport extends AbstractReport implements ApiFault
    {
        protected String className;

        protected String attribute;

        public ClassAttributeUndefinedReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "class-attribute-undefined";
        }

        public ClassAttributeUndefinedReport(String className, String attribute)
        {
            setClassName(className);
            setAttribute(attribute);
        }

        public String getClassName()
        {
            return className;
        }

        public void setClassName(String className)
        {
            this.className = className;
        }

        public String getAttribute()
        {
            return attribute;
        }

        public void setAttribute(String attribute)
        {
            this.attribute = attribute;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return CLASS_ATTRIBUTE_UNDEFINED_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ClassAttributeUndefinedException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            className = (String) reportSerializer.getParameter("className", String.class);
            attribute = (String) reportSerializer.getParameter("attribute", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("className", className);
            reportSerializer.setParameter("attribute", attribute);
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
            parameters.put("class", className);
            parameters.put("attribute", attribute);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("class-attribute-undefined", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ClassAttributeUndefinedReport}.
     */
    public static class ClassAttributeUndefinedException extends ReportRuntimeException implements ApiFaultException
    {
        public ClassAttributeUndefinedException(ClassAttributeUndefinedReport report)
        {
            this.report = report;
        }

        public ClassAttributeUndefinedException(Throwable throwable, ClassAttributeUndefinedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ClassAttributeUndefinedException(String className, String attribute)
        {
            ClassAttributeUndefinedReport report = new ClassAttributeUndefinedReport();
            report.setClassName(className);
            report.setAttribute(attribute);
            this.report = report;
        }

        public ClassAttributeUndefinedException(Throwable throwable, String className, String attribute)
        {
            super(throwable);
            ClassAttributeUndefinedReport report = new ClassAttributeUndefinedReport();
            report.setClassName(className);
            report.setAttribute(attribute);
            this.report = report;
        }

        public String getClassName()
        {
            return getReport().getClassName();
        }

        public String getAttribute()
        {
            return getReport().getAttribute();
        }

        @Override
        public ClassAttributeUndefinedReport getReport()
        {
            return (ClassAttributeUndefinedReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ClassAttributeUndefinedReport) report;
        }
    }

    /**
     * Type mismatch of value in attribute {@link #attribute} in class {@link #className}. Present type {@link #presentType} doesn't match required type {@link #requiredType}.
     */
    public static class ClassAttributeTypeMismatchReport extends AbstractReport implements ApiFault
    {
        protected String className;

        protected String attribute;

        protected String requiredType;

        protected String presentType;

        public ClassAttributeTypeMismatchReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "class-attribute-type-mismatch";
        }

        public ClassAttributeTypeMismatchReport(String className, String attribute, String requiredType, String presentType)
        {
            setClassName(className);
            setAttribute(attribute);
            setRequiredType(requiredType);
            setPresentType(presentType);
        }

        public String getClassName()
        {
            return className;
        }

        public void setClassName(String className)
        {
            this.className = className;
        }

        public String getAttribute()
        {
            return attribute;
        }

        public void setAttribute(String attribute)
        {
            this.attribute = attribute;
        }

        public String getRequiredType()
        {
            return requiredType;
        }

        public void setRequiredType(String requiredType)
        {
            this.requiredType = requiredType;
        }

        public String getPresentType()
        {
            return presentType;
        }

        public void setPresentType(String presentType)
        {
            this.presentType = presentType;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return CLASS_ATTRIBUTE_TYPE_MISMATCH_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ClassAttributeTypeMismatchException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            className = (String) reportSerializer.getParameter("className", String.class);
            attribute = (String) reportSerializer.getParameter("attribute", String.class);
            requiredType = (String) reportSerializer.getParameter("requiredType", String.class);
            presentType = (String) reportSerializer.getParameter("presentType", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("className", className);
            reportSerializer.setParameter("attribute", attribute);
            reportSerializer.setParameter("requiredType", requiredType);
            reportSerializer.setParameter("presentType", presentType);
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
            parameters.put("class", className);
            parameters.put("attribute", attribute);
            parameters.put("requiredType", requiredType);
            parameters.put("presentType", presentType);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("class-attribute-type-mismatch", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ClassAttributeTypeMismatchReport}.
     */
    public static class ClassAttributeTypeMismatchException extends ReportRuntimeException implements ApiFaultException
    {
        public ClassAttributeTypeMismatchException(ClassAttributeTypeMismatchReport report)
        {
            this.report = report;
        }

        public ClassAttributeTypeMismatchException(Throwable throwable, ClassAttributeTypeMismatchReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ClassAttributeTypeMismatchException(String className, String attribute, String requiredType, String presentType)
        {
            ClassAttributeTypeMismatchReport report = new ClassAttributeTypeMismatchReport();
            report.setClassName(className);
            report.setAttribute(attribute);
            report.setRequiredType(requiredType);
            report.setPresentType(presentType);
            this.report = report;
        }

        public ClassAttributeTypeMismatchException(Throwable throwable, String className, String attribute, String requiredType, String presentType)
        {
            super(throwable);
            ClassAttributeTypeMismatchReport report = new ClassAttributeTypeMismatchReport();
            report.setClassName(className);
            report.setAttribute(attribute);
            report.setRequiredType(requiredType);
            report.setPresentType(presentType);
            this.report = report;
        }

        public String getClassName()
        {
            return getReport().getClassName();
        }

        public String getAttribute()
        {
            return getReport().getAttribute();
        }

        public String getRequiredType()
        {
            return getReport().getRequiredType();
        }

        public String getPresentType()
        {
            return getReport().getPresentType();
        }

        @Override
        public ClassAttributeTypeMismatchReport getReport()
        {
            return (ClassAttributeTypeMismatchReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ClassAttributeTypeMismatchReport) report;
        }
    }

    /**
     * Attribute {@link #attribute} in class {@link #className} wasn't present but it is required.
     */
    public static class ClassAttributeRequiredReport extends AbstractReport implements ApiFault
    {
        protected String className;

        protected String attribute;

        public ClassAttributeRequiredReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "class-attribute-required";
        }

        public ClassAttributeRequiredReport(String className, String attribute)
        {
            setClassName(className);
            setAttribute(attribute);
        }

        public String getClassName()
        {
            return className;
        }

        public void setClassName(String className)
        {
            this.className = className;
        }

        public String getAttribute()
        {
            return attribute;
        }

        public void setAttribute(String attribute)
        {
            this.attribute = attribute;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return CLASS_ATTRIBUTE_REQUIRED_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ClassAttributeRequiredException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            className = (String) reportSerializer.getParameter("className", String.class);
            attribute = (String) reportSerializer.getParameter("attribute", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("className", className);
            reportSerializer.setParameter("attribute", attribute);
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
            parameters.put("class", className);
            parameters.put("attribute", attribute);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("class-attribute-required", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ClassAttributeRequiredReport}.
     */
    public static class ClassAttributeRequiredException extends ReportRuntimeException implements ApiFaultException
    {
        public ClassAttributeRequiredException(ClassAttributeRequiredReport report)
        {
            this.report = report;
        }

        public ClassAttributeRequiredException(Throwable throwable, ClassAttributeRequiredReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ClassAttributeRequiredException(String className, String attribute)
        {
            ClassAttributeRequiredReport report = new ClassAttributeRequiredReport();
            report.setClassName(className);
            report.setAttribute(attribute);
            this.report = report;
        }

        public ClassAttributeRequiredException(Throwable throwable, String className, String attribute)
        {
            super(throwable);
            ClassAttributeRequiredReport report = new ClassAttributeRequiredReport();
            report.setClassName(className);
            report.setAttribute(attribute);
            this.report = report;
        }

        public String getClassName()
        {
            return getReport().getClassName();
        }

        public String getAttribute()
        {
            return getReport().getAttribute();
        }

        @Override
        public ClassAttributeRequiredReport getReport()
        {
            return (ClassAttributeRequiredReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ClassAttributeRequiredReport) report;
        }
    }

    /**
     * Value for attribute {@link #attribute} in class {@link #className} exceeds the maximum length {@link #maximumLength}.
     */
    public static class ClassAttributeValueMaximumLengthExceededReport extends AbstractReport implements ApiFault
    {
        protected String className;

        protected String attribute;

        protected Integer maximumLength;

        public ClassAttributeValueMaximumLengthExceededReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "class-attribute-value-maximum-length-exceeded";
        }

        public ClassAttributeValueMaximumLengthExceededReport(String className, String attribute, Integer maximumLength)
        {
            setClassName(className);
            setAttribute(attribute);
            setMaximumLength(maximumLength);
        }

        public String getClassName()
        {
            return className;
        }

        public void setClassName(String className)
        {
            this.className = className;
        }

        public String getAttribute()
        {
            return attribute;
        }

        public void setAttribute(String attribute)
        {
            this.attribute = attribute;
        }

        public Integer getMaximumLength()
        {
            return maximumLength;
        }

        public void setMaximumLength(Integer maximumLength)
        {
            this.maximumLength = maximumLength;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return CLASS_ATTRIBUTE_VALUE_MAXIMUM_LENGTH_EXCEEDED_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ClassAttributeValueMaximumLengthExceededException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            className = (String) reportSerializer.getParameter("className", String.class);
            attribute = (String) reportSerializer.getParameter("attribute", String.class);
            maximumLength = (Integer) reportSerializer.getParameter("maximumLength", Integer.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("className", className);
            reportSerializer.setParameter("attribute", attribute);
            reportSerializer.setParameter("maximumLength", maximumLength);
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
            parameters.put("class", className);
            parameters.put("attribute", attribute);
            parameters.put("maximumLength", maximumLength);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("class-attribute-value-maximum-length-exceeded", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ClassAttributeValueMaximumLengthExceededReport}.
     */
    public static class ClassAttributeValueMaximumLengthExceededException extends ReportRuntimeException implements ApiFaultException
    {
        public ClassAttributeValueMaximumLengthExceededException(ClassAttributeValueMaximumLengthExceededReport report)
        {
            this.report = report;
        }

        public ClassAttributeValueMaximumLengthExceededException(Throwable throwable, ClassAttributeValueMaximumLengthExceededReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ClassAttributeValueMaximumLengthExceededException(String className, String attribute, Integer maximumLength)
        {
            ClassAttributeValueMaximumLengthExceededReport report = new ClassAttributeValueMaximumLengthExceededReport();
            report.setClassName(className);
            report.setAttribute(attribute);
            report.setMaximumLength(maximumLength);
            this.report = report;
        }

        public ClassAttributeValueMaximumLengthExceededException(Throwable throwable, String className, String attribute, Integer maximumLength)
        {
            super(throwable);
            ClassAttributeValueMaximumLengthExceededReport report = new ClassAttributeValueMaximumLengthExceededReport();
            report.setClassName(className);
            report.setAttribute(attribute);
            report.setMaximumLength(maximumLength);
            this.report = report;
        }

        public String getClassName()
        {
            return getReport().getClassName();
        }

        public String getAttribute()
        {
            return getReport().getAttribute();
        }

        public Integer getMaximumLength()
        {
            return getReport().getMaximumLength();
        }

        @Override
        public ClassAttributeValueMaximumLengthExceededReport getReport()
        {
            return (ClassAttributeValueMaximumLengthExceededReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ClassAttributeValueMaximumLengthExceededReport) report;
        }
    }

    /**
     * Value for attribute {@link #attribute} in class {@link #className} was present but the attribute is read-only.
     */
    public static class ClassAttributeReadonlyReport extends AbstractReport implements ApiFault
    {
        protected String className;

        protected String attribute;

        public ClassAttributeReadonlyReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "class-attribute-readonly";
        }

        public ClassAttributeReadonlyReport(String className, String attribute)
        {
            setClassName(className);
            setAttribute(attribute);
        }

        public String getClassName()
        {
            return className;
        }

        public void setClassName(String className)
        {
            this.className = className;
        }

        public String getAttribute()
        {
            return attribute;
        }

        public void setAttribute(String attribute)
        {
            this.attribute = attribute;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return CLASS_ATTRIBUTE_READONLY_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ClassAttributeReadonlyException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            className = (String) reportSerializer.getParameter("className", String.class);
            attribute = (String) reportSerializer.getParameter("attribute", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("className", className);
            reportSerializer.setParameter("attribute", attribute);
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
            parameters.put("class", className);
            parameters.put("attribute", attribute);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("class-attribute-readonly", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ClassAttributeReadonlyReport}.
     */
    public static class ClassAttributeReadonlyException extends ReportRuntimeException implements ApiFaultException
    {
        public ClassAttributeReadonlyException(ClassAttributeReadonlyReport report)
        {
            this.report = report;
        }

        public ClassAttributeReadonlyException(Throwable throwable, ClassAttributeReadonlyReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ClassAttributeReadonlyException(String className, String attribute)
        {
            ClassAttributeReadonlyReport report = new ClassAttributeReadonlyReport();
            report.setClassName(className);
            report.setAttribute(attribute);
            this.report = report;
        }

        public ClassAttributeReadonlyException(Throwable throwable, String className, String attribute)
        {
            super(throwable);
            ClassAttributeReadonlyReport report = new ClassAttributeReadonlyReport();
            report.setClassName(className);
            report.setAttribute(attribute);
            this.report = report;
        }

        public String getClassName()
        {
            return getReport().getClassName();
        }

        public String getAttribute()
        {
            return getReport().getAttribute();
        }

        @Override
        public ClassAttributeReadonlyReport getReport()
        {
            return (ClassAttributeReadonlyReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ClassAttributeReadonlyReport) report;
        }
    }

    /**
     * Collection {@link #collection} in class {@link #className} wasn't present or was empty but it is required.
     */
    public static class ClassCollectionRequiredReport extends AbstractReport implements ApiFault
    {
        protected String className;

        protected String collection;

        public ClassCollectionRequiredReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "class-collection-required";
        }

        public ClassCollectionRequiredReport(String className, String collection)
        {
            setClassName(className);
            setCollection(collection);
        }

        public String getClassName()
        {
            return className;
        }

        public void setClassName(String className)
        {
            this.className = className;
        }

        public String getCollection()
        {
            return collection;
        }

        public void setCollection(String collection)
        {
            this.collection = collection;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return CLASS_COLLECTION_REQUIRED_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ClassCollectionRequiredException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            className = (String) reportSerializer.getParameter("className", String.class);
            collection = (String) reportSerializer.getParameter("collection", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("className", className);
            reportSerializer.setParameter("collection", collection);
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
            parameters.put("class", className);
            parameters.put("collection", collection);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("class-collection-required", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ClassCollectionRequiredReport}.
     */
    public static class ClassCollectionRequiredException extends ReportRuntimeException implements ApiFaultException
    {
        public ClassCollectionRequiredException(ClassCollectionRequiredReport report)
        {
            this.report = report;
        }

        public ClassCollectionRequiredException(Throwable throwable, ClassCollectionRequiredReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ClassCollectionRequiredException(String className, String collection)
        {
            ClassCollectionRequiredReport report = new ClassCollectionRequiredReport();
            report.setClassName(className);
            report.setCollection(collection);
            this.report = report;
        }

        public ClassCollectionRequiredException(Throwable throwable, String className, String collection)
        {
            super(throwable);
            ClassCollectionRequiredReport report = new ClassCollectionRequiredReport();
            report.setClassName(className);
            report.setCollection(collection);
            this.report = report;
        }

        public String getClassName()
        {
            return getReport().getClassName();
        }

        public String getCollection()
        {
            return getReport().getCollection();
        }

        @Override
        public ClassCollectionRequiredReport getReport()
        {
            return (ClassCollectionRequiredReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ClassCollectionRequiredReport) report;
        }
    }

    /**
     * Null item cannot be present in collection {@link #collection}.
     */
    public static class CollectionItemNullReport extends AbstractReport implements ApiFault
    {
        protected String collection;

        public CollectionItemNullReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "collection-item-null";
        }

        public CollectionItemNullReport(String collection)
        {
            setCollection(collection);
        }

        public String getCollection()
        {
            return collection;
        }

        public void setCollection(String collection)
        {
            this.collection = collection;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return COLLECTION_ITEM_NULL_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new CollectionItemNullException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            collection = (String) reportSerializer.getParameter("collection", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("collection", collection);
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
            parameters.put("collection", collection);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("collection-item-null", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link CollectionItemNullReport}.
     */
    public static class CollectionItemNullException extends ReportRuntimeException implements ApiFaultException
    {
        public CollectionItemNullException(CollectionItemNullReport report)
        {
            this.report = report;
        }

        public CollectionItemNullException(Throwable throwable, CollectionItemNullReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CollectionItemNullException(String collection)
        {
            CollectionItemNullReport report = new CollectionItemNullReport();
            report.setCollection(collection);
            this.report = report;
        }

        public CollectionItemNullException(Throwable throwable, String collection)
        {
            super(throwable);
            CollectionItemNullReport report = new CollectionItemNullReport();
            report.setCollection(collection);
            this.report = report;
        }

        public String getCollection()
        {
            return getReport().getCollection();
        }

        @Override
        public CollectionItemNullReport getReport()
        {
            return (CollectionItemNullReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (CollectionItemNullReport) report;
        }
    }

    /**
     * Collection {@link #collection} contains item of type {@link #presentType} which doesn't match the required type {@link #requiredType}.
     */
    public static class CollectionItemTypeMismatchReport extends AbstractReport implements ApiFault
    {
        protected String collection;

        protected String requiredType;

        protected String presentType;

        public CollectionItemTypeMismatchReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "collection-item-type-mismatch";
        }

        public CollectionItemTypeMismatchReport(String collection, String requiredType, String presentType)
        {
            setCollection(collection);
            setRequiredType(requiredType);
            setPresentType(presentType);
        }

        public String getCollection()
        {
            return collection;
        }

        public void setCollection(String collection)
        {
            this.collection = collection;
        }

        public String getRequiredType()
        {
            return requiredType;
        }

        public void setRequiredType(String requiredType)
        {
            this.requiredType = requiredType;
        }

        public String getPresentType()
        {
            return presentType;
        }

        public void setPresentType(String presentType)
        {
            this.presentType = presentType;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return COLLECTION_ITEM_TYPE_MISMATCH_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new CollectionItemTypeMismatchException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            collection = (String) reportSerializer.getParameter("collection", String.class);
            requiredType = (String) reportSerializer.getParameter("requiredType", String.class);
            presentType = (String) reportSerializer.getParameter("presentType", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("collection", collection);
            reportSerializer.setParameter("requiredType", requiredType);
            reportSerializer.setParameter("presentType", presentType);
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
            parameters.put("collection", collection);
            parameters.put("requiredType", requiredType);
            parameters.put("presentType", presentType);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("collection-item-type-mismatch", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link CollectionItemTypeMismatchReport}.
     */
    public static class CollectionItemTypeMismatchException extends ReportRuntimeException implements ApiFaultException
    {
        public CollectionItemTypeMismatchException(CollectionItemTypeMismatchReport report)
        {
            this.report = report;
        }

        public CollectionItemTypeMismatchException(Throwable throwable, CollectionItemTypeMismatchReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CollectionItemTypeMismatchException(String collection, String requiredType, String presentType)
        {
            CollectionItemTypeMismatchReport report = new CollectionItemTypeMismatchReport();
            report.setCollection(collection);
            report.setRequiredType(requiredType);
            report.setPresentType(presentType);
            this.report = report;
        }

        public CollectionItemTypeMismatchException(Throwable throwable, String collection, String requiredType, String presentType)
        {
            super(throwable);
            CollectionItemTypeMismatchReport report = new CollectionItemTypeMismatchReport();
            report.setCollection(collection);
            report.setRequiredType(requiredType);
            report.setPresentType(presentType);
            this.report = report;
        }

        public String getCollection()
        {
            return getReport().getCollection();
        }

        public String getRequiredType()
        {
            return getReport().getRequiredType();
        }

        public String getPresentType()
        {
            return getReport().getPresentType();
        }

        @Override
        public CollectionItemTypeMismatchReport getReport()
        {
            return (CollectionItemTypeMismatchReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (CollectionItemTypeMismatchReport) report;
        }
    }

    /**
     * Object {@link #objectType} with identifier {@link #objectId} doesn't exist.
     */
    public static class ObjectNotExistsReport extends AbstractReport implements ApiFault
    {
        protected String objectType;

        protected String objectId;

        public ObjectNotExistsReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "object-not-exists";
        }

        public ObjectNotExistsReport(String objectType, String objectId)
        {
            setObjectType(objectType);
            setObjectId(objectId);
        }

        public String getObjectType()
        {
            return objectType;
        }

        public void setObjectType(String objectType)
        {
            this.objectType = objectType;
        }

        public String getObjectId()
        {
            return objectId;
        }

        public void setObjectId(String objectId)
        {
            this.objectId = objectId;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return OBJECT_NOT_EXISTS_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ObjectNotExistsException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            objectType = (String) reportSerializer.getParameter("objectType", String.class);
            objectId = (String) reportSerializer.getParameter("objectId", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("objectType", objectType);
            reportSerializer.setParameter("objectId", objectId);
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
            parameters.put("objectId", objectId);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("object-not-exists", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ObjectNotExistsReport}.
     */
    public static class ObjectNotExistsException extends ReportRuntimeException implements ApiFaultException
    {
        public ObjectNotExistsException(ObjectNotExistsReport report)
        {
            this.report = report;
        }

        public ObjectNotExistsException(Throwable throwable, ObjectNotExistsReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ObjectNotExistsException(String objectType, String objectId)
        {
            ObjectNotExistsReport report = new ObjectNotExistsReport();
            report.setObjectType(objectType);
            report.setObjectId(objectId);
            this.report = report;
        }

        public ObjectNotExistsException(Throwable throwable, String objectType, String objectId)
        {
            super(throwable);
            ObjectNotExistsReport report = new ObjectNotExistsReport();
            report.setObjectType(objectType);
            report.setObjectId(objectId);
            this.report = report;
        }

        public String getObjectType()
        {
            return getReport().getObjectType();
        }

        public String getObjectId()
        {
            return getReport().getObjectId();
        }

        @Override
        public ObjectNotExistsReport getReport()
        {
            return (ObjectNotExistsReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ObjectNotExistsReport) report;
        }
    }

    /**
     * Object {@link #objectId} validation failed: {@link #reason}
     */
    public static class ObjectInvalidReport extends AbstractReport implements ApiFault
    {
        protected String objectId;

        protected String reason;

        public ObjectInvalidReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "object-invalid";
        }

        public ObjectInvalidReport(String objectId, String reason)
        {
            setObjectId(objectId);
            setReason(reason);
        }

        public String getObjectId()
        {
            return objectId;
        }

        public void setObjectId(String objectId)
        {
            this.objectId = objectId;
        }

        public String getReason()
        {
            return reason;
        }

        public void setReason(String reason)
        {
            this.reason = reason;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return OBJECT_INVALID_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ObjectInvalidException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            objectId = (String) reportSerializer.getParameter("objectId", String.class);
            reason = (String) reportSerializer.getParameter("reason", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("objectId", objectId);
            reportSerializer.setParameter("reason", reason);
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
            parameters.put("objectId", objectId);
            parameters.put("reason", reason);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("object-invalid", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ObjectInvalidReport}.
     */
    public static class ObjectInvalidException extends ReportRuntimeException implements ApiFaultException
    {
        public ObjectInvalidException(ObjectInvalidReport report)
        {
            this.report = report;
        }

        public ObjectInvalidException(Throwable throwable, ObjectInvalidReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ObjectInvalidException(String objectId, String reason)
        {
            ObjectInvalidReport report = new ObjectInvalidReport();
            report.setObjectId(objectId);
            report.setReason(reason);
            this.report = report;
        }

        public ObjectInvalidException(Throwable throwable, String objectId, String reason)
        {
            super(throwable);
            ObjectInvalidReport report = new ObjectInvalidReport();
            report.setObjectId(objectId);
            report.setReason(reason);
            this.report = report;
        }

        public String getObjectId()
        {
            return getReport().getObjectId();
        }

        public String getReason()
        {
            return getReport().getReason();
        }

        @Override
        public ObjectInvalidReport getReport()
        {
            return (ObjectInvalidReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ObjectInvalidReport) report;
        }
    }

    /**
     * Object {@link #objectType} with identifier {@link #objectId} cannot be deleted because it is still referenced.
     */
    public static class ObjectNotDeletableReferencedReport extends AbstractReport implements ApiFault
    {
        protected String objectType;

        protected String objectId;

        public ObjectNotDeletableReferencedReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "object-not-deletable-referenced";
        }

        public ObjectNotDeletableReferencedReport(String objectType, String objectId)
        {
            setObjectType(objectType);
            setObjectId(objectId);
        }

        public String getObjectType()
        {
            return objectType;
        }

        public void setObjectType(String objectType)
        {
            this.objectType = objectType;
        }

        public String getObjectId()
        {
            return objectId;
        }

        public void setObjectId(String objectId)
        {
            this.objectId = objectId;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return OBJECT_NOT_DELETABLE_REFERENCED_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new ObjectNotDeletableReferencedException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            objectType = (String) reportSerializer.getParameter("objectType", String.class);
            objectId = (String) reportSerializer.getParameter("objectId", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("objectType", objectType);
            reportSerializer.setParameter("objectId", objectId);
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
            parameters.put("objectId", objectId);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("object-not-deletable-referenced", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ObjectNotDeletableReferencedReport}.
     */
    public static class ObjectNotDeletableReferencedException extends ReportRuntimeException implements ApiFaultException
    {
        public ObjectNotDeletableReferencedException(ObjectNotDeletableReferencedReport report)
        {
            this.report = report;
        }

        public ObjectNotDeletableReferencedException(Throwable throwable, ObjectNotDeletableReferencedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ObjectNotDeletableReferencedException(String objectType, String objectId)
        {
            ObjectNotDeletableReferencedReport report = new ObjectNotDeletableReferencedReport();
            report.setObjectType(objectType);
            report.setObjectId(objectId);
            this.report = report;
        }

        public ObjectNotDeletableReferencedException(Throwable throwable, String objectType, String objectId)
        {
            super(throwable);
            ObjectNotDeletableReferencedReport report = new ObjectNotDeletableReferencedReport();
            report.setObjectType(objectType);
            report.setObjectId(objectId);
            this.report = report;
        }

        public String getObjectType()
        {
            return getReport().getObjectType();
        }

        public String getObjectId()
        {
            return getReport().getObjectId();
        }

        @Override
        public ObjectNotDeletableReferencedReport getReport()
        {
            return (ObjectNotDeletableReferencedReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (ObjectNotDeletableReferencedReport) report;
        }
    }

    /**
     * Method {@link #method} is not defined.
     */
    public static class MethodNotDefinedReport extends AbstractReport implements ApiFault
    {
        protected String method;

        public MethodNotDefinedReport()
        {
        }

        @Override
        public String getUniqueId()
        {
            return "method-not-defined";
        }

        public MethodNotDefinedReport(String method)
        {
            setMethod(method);
        }

        public String getMethod()
        {
            return method;
        }

        public void setMethod(String method)
        {
            this.method = method;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public int getFaultCode()
        {
            return METHOD_NOT_DEFINED_CODE;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(UserType.USER, Language.ENGLISH);
        }

        @Override
        public Exception getException()
        {
            return new MethodNotDefinedException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            method = (String) reportSerializer.getParameter("method", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("method", method);
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
            parameters.put("method", method);
            return parameters;
        }

        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return MESSAGES.getMessage("method-not-defined", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link MethodNotDefinedReport}.
     */
    public static class MethodNotDefinedException extends ReportRuntimeException implements ApiFaultException
    {
        public MethodNotDefinedException(MethodNotDefinedReport report)
        {
            this.report = report;
        }

        public MethodNotDefinedException(Throwable throwable, MethodNotDefinedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public MethodNotDefinedException(String method)
        {
            MethodNotDefinedReport report = new MethodNotDefinedReport();
            report.setMethod(method);
            this.report = report;
        }

        public MethodNotDefinedException(Throwable throwable, String method)
        {
            super(throwable);
            MethodNotDefinedReport report = new MethodNotDefinedReport();
            report.setMethod(method);
            this.report = report;
        }

        public String getMethod()
        {
            return getReport().getMethod();
        }

        @Override
        public MethodNotDefinedReport getReport()
        {
            return (MethodNotDefinedReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (MethodNotDefinedReport) report;
        }
    }

    @Override
    protected void fillReportClasses()
    {
        addReportClass(UnknownErrorReport.class);
        addReportClass(TypeMismatchReport.class);
        addReportClass(TypeIllegalValueReport.class);
        addReportClass(ValueMaximumLengthExceededReport.class);
        addReportClass(ClassUndefinedReport.class);
        addReportClass(ClassInstantiationErrorReport.class);
        addReportClass(ClassAttributeUndefinedReport.class);
        addReportClass(ClassAttributeTypeMismatchReport.class);
        addReportClass(ClassAttributeRequiredReport.class);
        addReportClass(ClassAttributeValueMaximumLengthExceededReport.class);
        addReportClass(ClassAttributeReadonlyReport.class);
        addReportClass(ClassCollectionRequiredReport.class);
        addReportClass(CollectionItemNullReport.class);
        addReportClass(CollectionItemTypeMismatchReport.class);
        addReportClass(ObjectNotExistsReport.class);
        addReportClass(ObjectInvalidReport.class);
        addReportClass(ObjectNotDeletableReferencedReport.class);
        addReportClass(MethodNotDefinedReport.class);
    }
}
