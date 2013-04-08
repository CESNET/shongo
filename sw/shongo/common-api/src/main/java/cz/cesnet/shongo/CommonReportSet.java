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
    public static final int CLASS_UNDEFINED_REPORT = 0;
    public static final int CLASS_INSTANTIATION_ERROR_REPORT = 0;
    public static final int CLASS_ATTRIBUTE_UNDEFINED_REPORT = 0;
    public static final int CLASS_ATTRIBUTE_TYPE_MISMATCH_REPORT = 0;
    public static final int CLASS_ATTRIBUTE_REQUIRED_REPORT = 0;
    public static final int CLASS_ATTRIBUTE_READONLY_REPORT = 0;
    public static final int CLASS_COLLECTION_REQUIRED_REPORT = 0;
    public static final int COLLECTION_ITEM_NULL_REPORT = 0;
    public static final int COLLECTION_ITEM_TYPE_MISMATCH_REPORT = 0;
    public static final int ENTITY_NOT_FOUND_REPORT = 0;
    public static final int ENTITY_INVALID_REPORT = 0;
    public static final int ENTITY_NOT_DELETABLE_REFERENCED_REPORT = 0;
    /**
     * Unknown error: {@link #description}
     */
    public static class UnknownErrorReport implements Report, ApiFault
    {
        private String description;

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
    public static class UnknownErrorException extends AbstractReportException implements ApiFault
    {
        private UnknownErrorReport report;

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
            report = new UnknownErrorReport();
            report.setDescription(description);
        }

        public UnknownErrorException(Throwable throwable, String description)
        {
            super(throwable);
            report = new UnknownErrorReport();
            report.setDescription(description);
        }

        public String getDescription()
        {
            return report.getDescription();
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
    }
    /**
     * Value {@link #value} is illegal for type ${type}.
     */
    public static class TypeIllegalValueReport implements Report
    {
        private String typeName;
        private String value;

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
        public String getMessage()
        {
            String message = "Value ${value} is illegal for type ${type}.";
            message = message.replace("${typeName}", (typeName == null ? "" : typeName));
            message = message.replace("${value}", (value == null ? "" : value));
            return message;
        }
    }

    /**
     * Exception for {@link TypeIllegalValueReport}.
     */
    public static class TypeIllegalValueException extends AbstractReportException
    {
        private TypeIllegalValueReport report;

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
            report = new TypeIllegalValueReport();
            report.setTypeName(typeName);
            report.setValue(value);
        }

        public TypeIllegalValueException(Throwable throwable, String typeName, String value)
        {
            super(throwable);
            report = new TypeIllegalValueReport();
            report.setTypeName(typeName);
            report.setValue(value);
        }

        public String getTypeName()
        {
            return report.getTypeName();
        }

        public String getValue()
        {
            return report.getValue();
        }

        @Override
        public TypeIllegalValueReport getReport()
        {
            return report;
        }
    }
    /**
     * Class {@link #className} is not defined.
     */
    public static class ClassUndefinedReport implements Report, ApiFault
    {
        private String className;

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
        public String getMessage()
        {
            String message = "Class ${class} is not defined.";
            message = message.replace("${className}", (className == null ? "" : className));
            return message;
        }
    }

    /**
     * Exception for {@link ClassUndefinedReport}.
     */
    public static class ClassUndefinedException extends AbstractReportException implements ApiFault
    {
        private ClassUndefinedReport report;

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
            report = new ClassUndefinedReport();
            report.setClassName(className);
        }

        public ClassUndefinedException(Throwable throwable, String className)
        {
            super(throwable);
            report = new ClassUndefinedReport();
            report.setClassName(className);
        }

        public String getClassName()
        {
            return report.getClassName();
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
    }
    /**
     * Class {@link #className} cannot be instanced.
     */
    public static class ClassInstantiationErrorReport implements Report, ApiFault
    {
        private String className;

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
        public String getMessage()
        {
            String message = "Class ${class} cannot be instanced.";
            message = message.replace("${className}", (className == null ? "" : className));
            return message;
        }
    }

    /**
     * Exception for {@link ClassInstantiationErrorReport}.
     */
    public static class ClassInstantiationErrorException extends AbstractReportException implements ApiFault
    {
        private ClassInstantiationErrorReport report;

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
            report = new ClassInstantiationErrorReport();
            report.setClassName(className);
        }

        public ClassInstantiationErrorException(Throwable throwable, String className)
        {
            super(throwable);
            report = new ClassInstantiationErrorReport();
            report.setClassName(className);
        }

        public String getClassName()
        {
            return report.getClassName();
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
    }
    /**
     * Attribute {@link #attribute} is not defined in class ${class}.
     */
    public static class ClassAttributeUndefinedReport implements Report, ApiFault
    {
        private String className;
        private String attribute;

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
        public String getMessage()
        {
            String message = "Attribute ${attribute} is not defined in class ${class}.";
            message = message.replace("${className}", (className == null ? "" : className));
            message = message.replace("${attribute}", (attribute == null ? "" : attribute));
            return message;
        }
    }

    /**
     * Exception for {@link ClassAttributeUndefinedReport}.
     */
    public static class ClassAttributeUndefinedException extends AbstractReportException implements ApiFault
    {
        private ClassAttributeUndefinedReport report;

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
            report = new ClassAttributeUndefinedReport();
            report.setClassName(className);
            report.setAttribute(attribute);
        }

        public ClassAttributeUndefinedException(Throwable throwable, String className, String attribute)
        {
            super(throwable);
            report = new ClassAttributeUndefinedReport();
            report.setClassName(className);
            report.setAttribute(attribute);
        }

        public String getClassName()
        {
            return report.getClassName();
        }

        public String getAttribute()
        {
            return report.getAttribute();
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
    }
    /**
     * Type mismatch of value in attribute {@link #attribute} in class ${class}. Present type ${given-type} doesn't match required type ${required-type}.
     */
    public static class ClassAttributeTypeMismatchReport implements Report, ApiFault
    {
        private String className;
        private String attribute;
        private String requiredType;
        private String presentType;

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
        public String getMessage()
        {
            String message = "Type mismatch of value in attribute ${attribute} in class ${class}. Present type ${given-type} doesn't match required type ${required-type}.";
            message = message.replace("${className}", (className == null ? "" : className));
            message = message.replace("${attribute}", (attribute == null ? "" : attribute));
            message = message.replace("${required-type}", (requiredType == null ? "" : requiredType));
            message = message.replace("${present-type}", (presentType == null ? "" : presentType));
            return message;
        }
    }

    /**
     * Exception for {@link ClassAttributeTypeMismatchReport}.
     */
    public static class ClassAttributeTypeMismatchException extends AbstractReportException implements ApiFault
    {
        private ClassAttributeTypeMismatchReport report;

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
            report = new ClassAttributeTypeMismatchReport();
            report.setClassName(className);
            report.setAttribute(attribute);
            report.setRequiredType(requiredType);
            report.setPresentType(presentType);
        }

        public ClassAttributeTypeMismatchException(Throwable throwable, String className, String attribute, String requiredType, String presentType)
        {
            super(throwable);
            report = new ClassAttributeTypeMismatchReport();
            report.setClassName(className);
            report.setAttribute(attribute);
            report.setRequiredType(requiredType);
            report.setPresentType(presentType);
        }

        public String getClassName()
        {
            return report.getClassName();
        }

        public String getAttribute()
        {
            return report.getAttribute();
        }

        public String getRequiredType()
        {
            return report.getRequiredType();
        }

        public String getPresentType()
        {
            return report.getPresentType();
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
    }
    /**
     * Attribute {@link #attribute} in class ${class} wasn't present but it is required.
     */
    public static class ClassAttributeRequiredReport implements Report, ApiFault
    {
        private String className;
        private String attribute;

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
        public String getMessage()
        {
            String message = "Attribute ${attribute} in class ${class} wasn't present but it is required.";
            message = message.replace("${className}", (className == null ? "" : className));
            message = message.replace("${attribute}", (attribute == null ? "" : attribute));
            return message;
        }
    }

    /**
     * Exception for {@link ClassAttributeRequiredReport}.
     */
    public static class ClassAttributeRequiredException extends AbstractReportException implements ApiFault
    {
        private ClassAttributeRequiredReport report;

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
            report = new ClassAttributeRequiredReport();
            report.setClassName(className);
            report.setAttribute(attribute);
        }

        public ClassAttributeRequiredException(Throwable throwable, String className, String attribute)
        {
            super(throwable);
            report = new ClassAttributeRequiredReport();
            report.setClassName(className);
            report.setAttribute(attribute);
        }

        public String getClassName()
        {
            return report.getClassName();
        }

        public String getAttribute()
        {
            return report.getAttribute();
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
    }
    /**
     * Value for attribute {@link #attribute} in class ${class} was present but the attribute is read-only.
     */
    public static class ClassAttributeReadonlyReport implements Report, ApiFault
    {
        private String className;
        private String attribute;

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
        public String getMessage()
        {
            String message = "Value for attribute ${attribute} in class ${class} was present but the attribute is read-only.";
            message = message.replace("${className}", (className == null ? "" : className));
            message = message.replace("${attribute}", (attribute == null ? "" : attribute));
            return message;
        }
    }

    /**
     * Exception for {@link ClassAttributeReadonlyReport}.
     */
    public static class ClassAttributeReadonlyException extends AbstractReportException implements ApiFault
    {
        private ClassAttributeReadonlyReport report;

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
            report = new ClassAttributeReadonlyReport();
            report.setClassName(className);
            report.setAttribute(attribute);
        }

        public ClassAttributeReadonlyException(Throwable throwable, String className, String attribute)
        {
            super(throwable);
            report = new ClassAttributeReadonlyReport();
            report.setClassName(className);
            report.setAttribute(attribute);
        }

        public String getClassName()
        {
            return report.getClassName();
        }

        public String getAttribute()
        {
            return report.getAttribute();
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
    }
    /**
     * Collection {@link #collection} in class ${class} wasn't present or was empty but it is required.
     */
    public static class ClassCollectionRequiredReport implements Report, ApiFault
    {
        private String className;
        private String collection;

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
        public String getMessage()
        {
            String message = "Collection ${collection} in class ${class} wasn't present or was empty but it is required.";
            message = message.replace("${className}", (className == null ? "" : className));
            message = message.replace("${collection}", (collection == null ? "" : collection));
            return message;
        }
    }

    /**
     * Exception for {@link ClassCollectionRequiredReport}.
     */
    public static class ClassCollectionRequiredException extends AbstractReportException implements ApiFault
    {
        private ClassCollectionRequiredReport report;

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
            report = new ClassCollectionRequiredReport();
            report.setClassName(className);
            report.setCollection(collection);
        }

        public ClassCollectionRequiredException(Throwable throwable, String className, String collection)
        {
            super(throwable);
            report = new ClassCollectionRequiredReport();
            report.setClassName(className);
            report.setCollection(collection);
        }

        public String getClassName()
        {
            return report.getClassName();
        }

        public String getCollection()
        {
            return report.getCollection();
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
    }
    /**
     * Null item cannot be present in collection {@link #collection}.
     */
    public static class CollectionItemNullReport implements Report, ApiFault
    {
        private String collection;

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
    public static class CollectionItemNullException extends AbstractReportException implements ApiFault
    {
        private CollectionItemNullReport report;

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
            report = new CollectionItemNullReport();
            report.setCollection(collection);
        }

        public CollectionItemNullException(Throwable throwable, String collection)
        {
            super(throwable);
            report = new CollectionItemNullReport();
            report.setCollection(collection);
        }

        public String getCollection()
        {
            return report.getCollection();
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
    }
    /**
     * Collection {@link #collection} contains item of type ${present-type} which dosn't match the required type ${required-type}.
     */
    public static class CollectionItemTypeMismatchReport implements Report, ApiFault
    {
        private String collection;
        private String requiredType;
        private String presentType;

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
    public static class CollectionItemTypeMismatchException extends AbstractReportException implements ApiFault
    {
        private CollectionItemTypeMismatchReport report;

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
            report = new CollectionItemTypeMismatchReport();
            report.setCollection(collection);
            report.setRequiredType(requiredType);
            report.setPresentType(presentType);
        }

        public CollectionItemTypeMismatchException(Throwable throwable, String collection, String requiredType, String presentType)
        {
            super(throwable);
            report = new CollectionItemTypeMismatchReport();
            report.setCollection(collection);
            report.setRequiredType(requiredType);
            report.setPresentType(presentType);
        }

        public String getCollection()
        {
            return report.getCollection();
        }

        public String getRequiredType()
        {
            return report.getRequiredType();
        }

        public String getPresentType()
        {
            return report.getPresentType();
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
    }
    /**
     * Entity {@link #entity} with identifier ${id} was not found.
     */
    public static class EntityNotFoundReport implements Report, ApiFault
    {
        private String entity;
        private String id;

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
    public static class EntityNotFoundException extends AbstractReportException implements ApiFault
    {
        private EntityNotFoundReport report;

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
            report = new EntityNotFoundReport();
            report.setEntity(entity);
            report.setId(id);
        }

        public EntityNotFoundException(Throwable throwable, String entity, String id)
        {
            super(throwable);
            report = new EntityNotFoundReport();
            report.setEntity(entity);
            report.setId(id);
        }

        public String getEntity()
        {
            return report.getEntity();
        }

        public String getId()
        {
            return report.getId();
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
    }
    /**
     * Entity {@link #entity} validation failed: ${reason}
     */
    public static class EntityInvalidReport implements Report, ApiFault
    {
        private String entity;
        private String reason;

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
    public static class EntityInvalidException extends AbstractReportException implements ApiFault
    {
        private EntityInvalidReport report;

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
            report = new EntityInvalidReport();
            report.setEntity(entity);
            report.setReason(reason);
        }

        public EntityInvalidException(Throwable throwable, String entity, String reason)
        {
            super(throwable);
            report = new EntityInvalidReport();
            report.setEntity(entity);
            report.setReason(reason);
        }

        public String getEntity()
        {
            return report.getEntity();
        }

        public String getReason()
        {
            return report.getReason();
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
    }
    /**
     * Entity {@link #entity} with identifier ${id} cannot be deleted because it is still referenced.
     */
    public static class EntityNotDeletableReferencedReport implements Report, ApiFault
    {
        private String entity;
        private String id;

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
    public static class EntityNotDeletableReferencedException extends AbstractReportException implements ApiFault
    {
        private EntityNotDeletableReferencedReport report;

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
            report = new EntityNotDeletableReferencedReport();
            report.setEntity(entity);
            report.setId(id);
        }

        public EntityNotDeletableReferencedException(Throwable throwable, String entity, String id)
        {
            super(throwable);
            report = new EntityNotDeletableReferencedReport();
            report.setEntity(entity);
            report.setId(id);
        }

        public String getEntity()
        {
            return report.getEntity();
        }

        public String getId()
        {
            return report.getId();
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
    }
}
