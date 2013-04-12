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
    public static final int TYPE_ILLEGAL_VALUE_REPORT = 1;
    public static final int CLASS_UNDEFINED_REPORT = 2;
    public static final int CLASS_INSTANTIATION_ERROR_REPORT = 3;
    public static final int CLASS_ATTRIBUTE_UNDEFINED_REPORT = 4;
    public static final int CLASS_ATTRIBUTE_TYPE_MISMATCH_REPORT = 5;
    public static final int CLASS_ATTRIBUTE_REQUIRED_REPORT = 6;
    public static final int CLASS_ATTRIBUTE_READONLY_REPORT = 7;
    public static final int CLASS_COLLECTION_REQUIRED_REPORT = 8;
    public static final int COLLECTION_ITEM_NULL_REPORT = 9;
    public static final int COLLECTION_ITEM_TYPE_MISMATCH_REPORT = 10;
    public static final int ENTITY_NOT_FOUND_REPORT = 11;
    public static final int ENTITY_INVALID_REPORT = 12;
    public static final int ENTITY_NOT_DELETABLE_REFERENCED_REPORT = 13;

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
        public int getCode()
        {
            return UNKNOWN_ERROR_REPORT;
        }

        @Override
        public Exception getException()
        {
            return new UnknownErrorException(this);
        }

        @Override
        public String getMessage()
        {
            String message = "Unknown error: ${description}";
            message = message.replace("${description}", (description == null ? "" : description));
            return message;
        }
    }

    /**
     * Exception for {@link UnknownErrorReport}.
     */
    public static class UnknownErrorException extends ReportRuntimeException implements ApiFault
    {
        protected UnknownErrorReport report;

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
            return report;
        }
        @Override
        public int getCode()
        {
            return report.getCode();
        }

        @Override
        public Exception getException()
        {
            return this;
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
        public int getCode()
        {
            return TYPE_ILLEGAL_VALUE_REPORT;
        }

        @Override
        public Exception getException()
        {
            return new TypeIllegalValueException(this);
        }

        @Override
        public String getMessage()
        {
            String message = "Value ${value} is illegal for type ${type}.";
            message = message.replace("${type-name}", (typeName == null ? "" : typeName));
            message = message.replace("${value}", (value == null ? "" : value));
            return message;
        }
    }

    /**
     * Exception for {@link TypeIllegalValueReport}.
     */
    public static class TypeIllegalValueException extends ReportRuntimeException implements ApiFault
    {
        protected TypeIllegalValueReport report;

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
            return report;
        }
        @Override
        public int getCode()
        {
            return report.getCode();
        }

        @Override
        public Exception getException()
        {
            return this;
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
        public int getCode()
        {
            return CLASS_UNDEFINED_REPORT;
        }

        @Override
        public Exception getException()
        {
            return new ClassUndefinedException(this);
        }

        @Override
        public String getMessage()
        {
            String message = "Class ${class} is not defined.";
            message = message.replace("${class-name}", (className == null ? "" : className));
            return message;
        }
    }

    /**
     * Exception for {@link ClassUndefinedReport}.
     */
    public static class ClassUndefinedException extends ReportRuntimeException implements ApiFault
    {
        protected ClassUndefinedReport report;

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
            return report;
        }
        @Override
        public int getCode()
        {
            return report.getCode();
        }

        @Override
        public Exception getException()
        {
            return this;
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
        public int getCode()
        {
            return CLASS_INSTANTIATION_ERROR_REPORT;
        }

        @Override
        public Exception getException()
        {
            return new ClassInstantiationErrorException(this);
        }

        @Override
        public String getMessage()
        {
            String message = "Class ${class} cannot be instanced.";
            message = message.replace("${class-name}", (className == null ? "" : className));
            return message;
        }
    }

    /**
     * Exception for {@link ClassInstantiationErrorReport}.
     */
    public static class ClassInstantiationErrorException extends ReportRuntimeException implements ApiFault
    {
        protected ClassInstantiationErrorReport report;

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
            return report;
        }
        @Override
        public int getCode()
        {
            return report.getCode();
        }

        @Override
        public Exception getException()
        {
            return this;
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
        public int getCode()
        {
            return CLASS_ATTRIBUTE_UNDEFINED_REPORT;
        }

        @Override
        public Exception getException()
        {
            return new ClassAttributeUndefinedException(this);
        }

        @Override
        public String getMessage()
        {
            String message = "Attribute ${attribute} is not defined in class ${class}.";
            message = message.replace("${class-name}", (className == null ? "" : className));
            message = message.replace("${attribute}", (attribute == null ? "" : attribute));
            return message;
        }
    }

    /**
     * Exception for {@link ClassAttributeUndefinedReport}.
     */
    public static class ClassAttributeUndefinedException extends ReportRuntimeException implements ApiFault
    {
        protected ClassAttributeUndefinedReport report;

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
            return report;
        }
        @Override
        public int getCode()
        {
            return report.getCode();
        }

        @Override
        public Exception getException()
        {
            return this;
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
        public int getCode()
        {
            return CLASS_ATTRIBUTE_TYPE_MISMATCH_REPORT;
        }

        @Override
        public Exception getException()
        {
            return new ClassAttributeTypeMismatchException(this);
        }

        @Override
        public String getMessage()
        {
            String message = "Type mismatch of value in attribute ${attribute} in class ${class}. Present type ${present-type} doesn't match required type ${required-type}.";
            message = message.replace("${class-name}", (className == null ? "" : className));
            message = message.replace("${attribute}", (attribute == null ? "" : attribute));
            message = message.replace("${required-type}", (requiredType == null ? "" : requiredType));
            message = message.replace("${present-type}", (presentType == null ? "" : presentType));
            return message;
        }
    }

    /**
     * Exception for {@link ClassAttributeTypeMismatchReport}.
     */
    public static class ClassAttributeTypeMismatchException extends ReportRuntimeException implements ApiFault
    {
        protected ClassAttributeTypeMismatchReport report;

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
            return report;
        }
        @Override
        public int getCode()
        {
            return report.getCode();
        }

        @Override
        public Exception getException()
        {
            return this;
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
        public int getCode()
        {
            return CLASS_ATTRIBUTE_REQUIRED_REPORT;
        }

        @Override
        public Exception getException()
        {
            return new ClassAttributeRequiredException(this);
        }

        @Override
        public String getMessage()
        {
            String message = "Attribute ${attribute} in class ${class} wasn't present but it is required.";
            message = message.replace("${class-name}", (className == null ? "" : className));
            message = message.replace("${attribute}", (attribute == null ? "" : attribute));
            return message;
        }
    }

    /**
     * Exception for {@link ClassAttributeRequiredReport}.
     */
    public static class ClassAttributeRequiredException extends ReportRuntimeException implements ApiFault
    {
        protected ClassAttributeRequiredReport report;

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
            return report;
        }
        @Override
        public int getCode()
        {
            return report.getCode();
        }

        @Override
        public Exception getException()
        {
            return this;
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
        public int getCode()
        {
            return CLASS_ATTRIBUTE_READONLY_REPORT;
        }

        @Override
        public Exception getException()
        {
            return new ClassAttributeReadonlyException(this);
        }

        @Override
        public String getMessage()
        {
            String message = "Value for attribute ${attribute} in class ${class} was present but the attribute is read-only.";
            message = message.replace("${class-name}", (className == null ? "" : className));
            message = message.replace("${attribute}", (attribute == null ? "" : attribute));
            return message;
        }
    }

    /**
     * Exception for {@link ClassAttributeReadonlyReport}.
     */
    public static class ClassAttributeReadonlyException extends ReportRuntimeException implements ApiFault
    {
        protected ClassAttributeReadonlyReport report;

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
            return report;
        }
        @Override
        public int getCode()
        {
            return report.getCode();
        }

        @Override
        public Exception getException()
        {
            return this;
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
        public int getCode()
        {
            return CLASS_COLLECTION_REQUIRED_REPORT;
        }

        @Override
        public Exception getException()
        {
            return new ClassCollectionRequiredException(this);
        }

        @Override
        public String getMessage()
        {
            String message = "Collection ${collection} in class ${class} wasn't present or was empty but it is required.";
            message = message.replace("${class-name}", (className == null ? "" : className));
            message = message.replace("${collection}", (collection == null ? "" : collection));
            return message;
        }
    }

    /**
     * Exception for {@link ClassCollectionRequiredReport}.
     */
    public static class ClassCollectionRequiredException extends ReportRuntimeException implements ApiFault
    {
        protected ClassCollectionRequiredReport report;

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
            return report;
        }
        @Override
        public int getCode()
        {
            return report.getCode();
        }

        @Override
        public Exception getException()
        {
            return this;
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
        public int getCode()
        {
            return COLLECTION_ITEM_NULL_REPORT;
        }

        @Override
        public Exception getException()
        {
            return new CollectionItemNullException(this);
        }

        @Override
        public String getMessage()
        {
            String message = "Null item cannot be present in collection ${collection}.";
            message = message.replace("${collection}", (collection == null ? "" : collection));
            return message;
        }
    }

    /**
     * Exception for {@link CollectionItemNullReport}.
     */
    public static class CollectionItemNullException extends ReportRuntimeException implements ApiFault
    {
        protected CollectionItemNullReport report;

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
            return report;
        }
        @Override
        public int getCode()
        {
            return report.getCode();
        }

        @Override
        public Exception getException()
        {
            return this;
        }
    }

    /**
     * Collection {@link #collection} contains item of type {@link #presentType} which dosn't match the required type {@link #requiredType}.
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
        public int getCode()
        {
            return COLLECTION_ITEM_TYPE_MISMATCH_REPORT;
        }

        @Override
        public Exception getException()
        {
            return new CollectionItemTypeMismatchException(this);
        }

        @Override
        public String getMessage()
        {
            String message = "Collection ${collection} contains item of type ${present-type} which dosn't match the required type ${required-type}.";
            message = message.replace("${collection}", (collection == null ? "" : collection));
            message = message.replace("${required-type}", (requiredType == null ? "" : requiredType));
            message = message.replace("${present-type}", (presentType == null ? "" : presentType));
            return message;
        }
    }

    /**
     * Exception for {@link CollectionItemTypeMismatchReport}.
     */
    public static class CollectionItemTypeMismatchException extends ReportRuntimeException implements ApiFault
    {
        protected CollectionItemTypeMismatchReport report;

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
            return report;
        }
        @Override
        public int getCode()
        {
            return report.getCode();
        }

        @Override
        public Exception getException()
        {
            return this;
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
        public int getCode()
        {
            return ENTITY_NOT_FOUND_REPORT;
        }

        @Override
        public Exception getException()
        {
            return new EntityNotFoundException(this);
        }

        @Override
        public String getMessage()
        {
            String message = "Entity ${entity} with identifier ${id} was not found.";
            message = message.replace("${entity}", (entity == null ? "" : entity));
            message = message.replace("${id}", (id == null ? "" : id));
            return message;
        }
    }

    /**
     * Exception for {@link EntityNotFoundReport}.
     */
    public static class EntityNotFoundException extends ReportRuntimeException implements ApiFault
    {
        protected EntityNotFoundReport report;

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
            return report;
        }
        @Override
        public int getCode()
        {
            return report.getCode();
        }

        @Override
        public Exception getException()
        {
            return this;
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
        public int getCode()
        {
            return ENTITY_INVALID_REPORT;
        }

        @Override
        public Exception getException()
        {
            return new EntityInvalidException(this);
        }

        @Override
        public String getMessage()
        {
            String message = "Entity ${entity} validation failed: ${reason}";
            message = message.replace("${entity}", (entity == null ? "" : entity));
            message = message.replace("${reason}", (reason == null ? "" : reason));
            return message;
        }
    }

    /**
     * Exception for {@link EntityInvalidReport}.
     */
    public static class EntityInvalidException extends ReportRuntimeException implements ApiFault
    {
        protected EntityInvalidReport report;

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
            return report;
        }
        @Override
        public int getCode()
        {
            return report.getCode();
        }

        @Override
        public Exception getException()
        {
            return this;
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
        public int getCode()
        {
            return ENTITY_NOT_DELETABLE_REFERENCED_REPORT;
        }

        @Override
        public Exception getException()
        {
            return new EntityNotDeletableReferencedException(this);
        }

        @Override
        public String getMessage()
        {
            String message = "Entity ${entity} with identifier ${id} cannot be deleted because it is still referenced.";
            message = message.replace("${entity}", (entity == null ? "" : entity));
            message = message.replace("${id}", (id == null ? "" : id));
            return message;
        }
    }

    /**
     * Exception for {@link EntityNotDeletableReferencedReport}.
     */
    public static class EntityNotDeletableReferencedException extends ReportRuntimeException implements ApiFault
    {
        protected EntityNotDeletableReferencedReport report;

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
            return report;
        }
        @Override
        public int getCode()
        {
            return report.getCode();
        }

        @Override
        public Exception getException()
        {
            return this;
        }
    }

    @Override
    protected void fillReportClasses()
    {
        addReportClass(UnknownErrorReport.class);
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
    }
}
