package cz.cesnet.shongo;

import cz.cesnet.shongo.report.*;

/**
 * Auto-generated implementation of {@link AbstractReportSet}.
 *
 * @author cz.cesnet.shongo.tool-report-generator
 */
public class CommonReportSet extends AbstractReportSet
{
    public static final int UNKNOWN_ERROR_REPORT = 0;
    public static final int TYPE_MISMATCH_REPORT = 1;
    public static final int TYPE_ILLEGAL_VALUE_REPORT = 2;
    public static final int CLASS_UNDEFINED_REPORT = 3;
    public static final int CLASS_INSTANTIATION_ERROR_REPORT = 4;
    public static final int CLASS_ATTRIBUTE_UNDEFINED_REPORT = 5;
    public static final int CLASS_ATTRIBUTE_TYPE_MISMATCH_REPORT = 6;
    public static final int CLASS_ATTRIBUTE_REQUIRED_REPORT = 7;
    public static final int CLASS_ATTRIBUTE_READONLY_REPORT = 8;
    public static final int CLASS_COLLECTION_REQUIRED_REPORT = 9;
    public static final int COLLECTION_ITEM_NULL_REPORT = 10;
    public static final int COLLECTION_ITEM_TYPE_MISMATCH_REPORT = 11;
    public static final int ENTITY_NOT_FOUND_REPORT = 12;
    public static final int ENTITY_INVALID_REPORT = 13;
    public static final int ENTITY_NOT_DELETABLE_REFERENCED_REPORT = 14;
    public static final int METHOD_NOT_DEFINED_REPORT = 15;

    /**
     * Unknown error: {@link #description}
     */
    public static class UnknownErrorReport extends Report implements ApiFault
    {
        protected String description;

        public UnknownErrorReport()
        {
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
            return UNKNOWN_ERROR_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
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
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                case USER:
                    message.append("Unknown error.");
                    break;
                default:
                    message.append("Unknown error: ");
                    message.append((description == null ? "null" : description));
                    break;
            }
            return message.toString();
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
    public static class TypeMismatchReport extends Report implements ApiFault
    {
        protected String requiredType;

        protected String presentType;

        public TypeMismatchReport()
        {
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
            return TYPE_MISMATCH_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
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
                    message.append("Type mismatch. Present type ");
                    message.append((presentType == null ? "null" : presentType));
                    message.append(" doesn't match required type ");
                    message.append((requiredType == null ? "null" : requiredType));
                    message.append(".");
                    break;
            }
            return message.toString();
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
    public static class TypeIllegalValueReport extends Report implements ApiFault
    {
        protected String typeName;

        protected String value;

        public TypeIllegalValueReport()
        {
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
            return TYPE_ILLEGAL_VALUE_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
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
                    message.append("Value ");
                    message.append((value == null ? "null" : value));
                    message.append(" is illegal for type ");
                    message.append((typeName == null ? "null" : typeName));
                    message.append(".");
                    break;
            }
            return message.toString();
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
     * Class {@link #className} is not defined.
     */
    public static class ClassUndefinedReport extends Report implements ApiFault
    {
        protected String className;

        public ClassUndefinedReport()
        {
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
            return CLASS_UNDEFINED_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
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
                    message.append("Class ");
                    message.append((className == null ? "null" : className));
                    message.append(" is not defined.");
                    break;
            }
            return message.toString();
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
    public static class ClassInstantiationErrorReport extends Report implements ApiFault
    {
        protected String className;

        public ClassInstantiationErrorReport()
        {
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
            return CLASS_INSTANTIATION_ERROR_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
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
                    message.append("Class ");
                    message.append((className == null ? "null" : className));
                    message.append(" cannot be instanced.");
                    break;
            }
            return message.toString();
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
    public static class ClassAttributeUndefinedReport extends Report implements ApiFault
    {
        protected String className;

        protected String attribute;

        public ClassAttributeUndefinedReport()
        {
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
            return CLASS_ATTRIBUTE_UNDEFINED_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
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
                    message.append("Attribute ");
                    message.append((attribute == null ? "null" : attribute));
                    message.append(" is not defined in class ");
                    message.append((className == null ? "null" : className));
                    message.append(".");
                    break;
            }
            return message.toString();
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
    public static class ClassAttributeTypeMismatchReport extends Report implements ApiFault
    {
        protected String className;

        protected String attribute;

        protected String requiredType;

        protected String presentType;

        public ClassAttributeTypeMismatchReport()
        {
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
            return CLASS_ATTRIBUTE_TYPE_MISMATCH_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
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
                    message.append("Type mismatch of value in attribute ");
                    message.append((attribute == null ? "null" : attribute));
                    message.append(" in class ");
                    message.append((className == null ? "null" : className));
                    message.append(". Present type ");
                    message.append((presentType == null ? "null" : presentType));
                    message.append(" doesn't match required type ");
                    message.append((requiredType == null ? "null" : requiredType));
                    message.append(".");
                    break;
            }
            return message.toString();
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
    public static class ClassAttributeRequiredReport extends Report implements ApiFault
    {
        protected String className;

        protected String attribute;

        public ClassAttributeRequiredReport()
        {
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
            return CLASS_ATTRIBUTE_REQUIRED_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
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
                    message.append("Attribute ");
                    message.append((attribute == null ? "null" : attribute));
                    message.append(" in class ");
                    message.append((className == null ? "null" : className));
                    message.append(" wasn't present but it is required.");
                    break;
            }
            return message.toString();
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
     * Value for attribute {@link #attribute} in class {@link #className} was present but the attribute is read-only.
     */
    public static class ClassAttributeReadonlyReport extends Report implements ApiFault
    {
        protected String className;

        protected String attribute;

        public ClassAttributeReadonlyReport()
        {
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
            return CLASS_ATTRIBUTE_READONLY_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
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
                    message.append("Value for attribute ");
                    message.append((attribute == null ? "null" : attribute));
                    message.append(" in class ");
                    message.append((className == null ? "null" : className));
                    message.append(" was present but the attribute is read-only.");
                    break;
            }
            return message.toString();
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
    public static class ClassCollectionRequiredReport extends Report implements ApiFault
    {
        protected String className;

        protected String collection;

        public ClassCollectionRequiredReport()
        {
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
            return CLASS_COLLECTION_REQUIRED_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
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
                    message.append("Collection ");
                    message.append((collection == null ? "null" : collection));
                    message.append(" in class ");
                    message.append((className == null ? "null" : className));
                    message.append(" wasn't present or was empty but it is required.");
                    break;
            }
            return message.toString();
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
    public static class CollectionItemNullReport extends Report implements ApiFault
    {
        protected String collection;

        public CollectionItemNullReport()
        {
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
            return COLLECTION_ITEM_NULL_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
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
                    message.append("Null item cannot be present in collection ");
                    message.append((collection == null ? "null" : collection));
                    message.append(".");
                    break;
            }
            return message.toString();
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
    public static class CollectionItemTypeMismatchReport extends Report implements ApiFault
    {
        protected String collection;

        protected String requiredType;

        protected String presentType;

        public CollectionItemTypeMismatchReport()
        {
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
            return COLLECTION_ITEM_TYPE_MISMATCH_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
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
                    message.append("Collection ");
                    message.append((collection == null ? "null" : collection));
                    message.append(" contains item of type ");
                    message.append((presentType == null ? "null" : presentType));
                    message.append(" which doesn't match the required type ");
                    message.append((requiredType == null ? "null" : requiredType));
                    message.append(".");
                    break;
            }
            return message.toString();
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
     * Entity {@link #entity} with identifier {@link #id} was not found.
     */
    public static class EntityNotFoundReport extends Report implements ApiFault
    {
        protected String entity;

        protected String id;

        public EntityNotFoundReport()
        {
        }

        public EntityNotFoundReport(String entity, String id)
        {
            setEntity(entity);
            setId(id);
        }

        public String getEntity()
        {
            return entity;
        }

        public void setEntity(String entity)
        {
            this.entity = entity;
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
            return ENTITY_NOT_FOUND_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
        }

        @Override
        public Exception getException()
        {
            return new EntityNotFoundException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            entity = (String) reportSerializer.getParameter("entity", String.class);
            id = (String) reportSerializer.getParameter("id", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("entity", entity);
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
                    message.append("Entity ");
                    message.append((entity == null ? "null" : entity));
                    message.append(" with identifier ");
                    message.append((id == null ? "null" : id));
                    message.append(" was not found.");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link EntityNotFoundReport}.
     */
    public static class EntityNotFoundException extends ReportRuntimeException implements ApiFaultException
    {
        public EntityNotFoundException(EntityNotFoundReport report)
        {
            this.report = report;
        }

        public EntityNotFoundException(Throwable throwable, EntityNotFoundReport report)
        {
            super(throwable);
            this.report = report;
        }

        public EntityNotFoundException(String entity, String id)
        {
            EntityNotFoundReport report = new EntityNotFoundReport();
            report.setEntity(entity);
            report.setId(id);
            this.report = report;
        }

        public EntityNotFoundException(Throwable throwable, String entity, String id)
        {
            super(throwable);
            EntityNotFoundReport report = new EntityNotFoundReport();
            report.setEntity(entity);
            report.setId(id);
            this.report = report;
        }

        public String getEntity()
        {
            return getReport().getEntity();
        }

        public String getId()
        {
            return getReport().getId();
        }

        @Override
        public EntityNotFoundReport getReport()
        {
            return (EntityNotFoundReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (EntityNotFoundReport) report;
        }
    }

    /**
     * Entity {@link #entity} validation failed: {@link #reason}
     */
    public static class EntityInvalidReport extends Report implements ApiFault
    {
        protected String entity;

        protected String reason;

        public EntityInvalidReport()
        {
        }

        public EntityInvalidReport(String entity, String reason)
        {
            setEntity(entity);
            setReason(reason);
        }

        public String getEntity()
        {
            return entity;
        }

        public void setEntity(String entity)
        {
            this.entity = entity;
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
            return ENTITY_INVALID_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
        }

        @Override
        public Exception getException()
        {
            return new EntityInvalidException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            entity = (String) reportSerializer.getParameter("entity", String.class);
            reason = (String) reportSerializer.getParameter("reason", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("entity", entity);
            reportSerializer.setParameter("reason", reason);
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
                    message.append("Entity ");
                    message.append((entity == null ? "null" : entity));
                    message.append(" validation failed: ");
                    message.append((reason == null ? "null" : reason));
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link EntityInvalidReport}.
     */
    public static class EntityInvalidException extends ReportRuntimeException implements ApiFaultException
    {
        public EntityInvalidException(EntityInvalidReport report)
        {
            this.report = report;
        }

        public EntityInvalidException(Throwable throwable, EntityInvalidReport report)
        {
            super(throwable);
            this.report = report;
        }

        public EntityInvalidException(String entity, String reason)
        {
            EntityInvalidReport report = new EntityInvalidReport();
            report.setEntity(entity);
            report.setReason(reason);
            this.report = report;
        }

        public EntityInvalidException(Throwable throwable, String entity, String reason)
        {
            super(throwable);
            EntityInvalidReport report = new EntityInvalidReport();
            report.setEntity(entity);
            report.setReason(reason);
            this.report = report;
        }

        public String getEntity()
        {
            return getReport().getEntity();
        }

        public String getReason()
        {
            return getReport().getReason();
        }

        @Override
        public EntityInvalidReport getReport()
        {
            return (EntityInvalidReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (EntityInvalidReport) report;
        }
    }

    /**
     * Entity {@link #entity} with identifier {@link #id} cannot be deleted because it is still referenced.
     */
    public static class EntityNotDeletableReferencedReport extends Report implements ApiFault
    {
        protected String entity;

        protected String id;

        public EntityNotDeletableReferencedReport()
        {
        }

        public EntityNotDeletableReferencedReport(String entity, String id)
        {
            setEntity(entity);
            setId(id);
        }

        public String getEntity()
        {
            return entity;
        }

        public void setEntity(String entity)
        {
            this.entity = entity;
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
            return ENTITY_NOT_DELETABLE_REFERENCED_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
        }

        @Override
        public Exception getException()
        {
            return new EntityNotDeletableReferencedException(this);
        }

        @Override
        public void readParameters(ReportSerializer reportSerializer)
        {
            entity = (String) reportSerializer.getParameter("entity", String.class);
            id = (String) reportSerializer.getParameter("id", String.class);
        }

        @Override
        public void writeParameters(ReportSerializer reportSerializer)
        {
            reportSerializer.setParameter("entity", entity);
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
                    message.append("Entity ");
                    message.append((entity == null ? "null" : entity));
                    message.append(" with identifier ");
                    message.append((id == null ? "null" : id));
                    message.append(" cannot be deleted because it is still referenced.");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link EntityNotDeletableReferencedReport}.
     */
    public static class EntityNotDeletableReferencedException extends ReportRuntimeException implements ApiFaultException
    {
        public EntityNotDeletableReferencedException(EntityNotDeletableReferencedReport report)
        {
            this.report = report;
        }

        public EntityNotDeletableReferencedException(Throwable throwable, EntityNotDeletableReferencedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public EntityNotDeletableReferencedException(String entity, String id)
        {
            EntityNotDeletableReferencedReport report = new EntityNotDeletableReferencedReport();
            report.setEntity(entity);
            report.setId(id);
            this.report = report;
        }

        public EntityNotDeletableReferencedException(Throwable throwable, String entity, String id)
        {
            super(throwable);
            EntityNotDeletableReferencedReport report = new EntityNotDeletableReferencedReport();
            report.setEntity(entity);
            report.setId(id);
            this.report = report;
        }

        public String getEntity()
        {
            return getReport().getEntity();
        }

        public String getId()
        {
            return getReport().getId();
        }

        @Override
        public EntityNotDeletableReferencedReport getReport()
        {
            return (EntityNotDeletableReferencedReport) report;
        }
        @Override
        public ApiFault getApiFault()
        {
            return (EntityNotDeletableReferencedReport) report;
        }
    }

    /**
     * Method {@link #method} is not defined.
     */
    public static class MethodNotDefinedReport extends Report implements ApiFault
    {
        protected String method;

        public MethodNotDefinedReport()
        {
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
            return METHOD_NOT_DEFINED_REPORT;
        }

        @Override
        public String getFaultString()
        {
            return getMessage(MessageType.USER);
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
                    message.append("Method ");
                    message.append((method == null ? "null" : method));
                    message.append(" is not defined.");
                    break;
            }
            return message.toString();
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
        addReportClass(ClassUndefinedReport.class);
        addReportClass(ClassInstantiationErrorReport.class);
        addReportClass(ClassAttributeUndefinedReport.class);
        addReportClass(ClassAttributeTypeMismatchReport.class);
        addReportClass(ClassAttributeRequiredReport.class);
        addReportClass(ClassAttributeReadonlyReport.class);
        addReportClass(ClassCollectionRequiredReport.class);
        addReportClass(CollectionItemNullReport.class);
        addReportClass(CollectionItemTypeMismatchReport.class);
        addReportClass(EntityNotFoundReport.class);
        addReportClass(EntityInvalidReport.class);
        addReportClass(EntityNotDeletableReferencedReport.class);
        addReportClass(MethodNotDefinedReport.class);
    }
}
