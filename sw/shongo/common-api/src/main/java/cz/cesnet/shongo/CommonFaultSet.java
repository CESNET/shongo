package cz.cesnet.shongo;

import cz.cesnet.shongo.fault.Fault;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.jade.CommandFailure;
import cz.cesnet.shongo.fault.FaultSet;

public class CommonFaultSet extends FaultSet
{
    public static final int UNKNOWN_ERROR_FAULT = 0;
    public static final int TYPE_ILLEGAL_VALUE_FAULT = 1;
    public static final int CLASS_UNDEFINED_FAULT = 2;
    public static final int CLASS_INSTANTIATION_ERROR_FAULT = 3;
    public static final int CLASS_ATTRIBUTE_UNDEFINED_FAULT = 4;
    public static final int CLASS_ATTRIBUTE_TYPE_MISMATCH_FAULT = 5;
    public static final int CLASS_ATTRIBUTE_REQUIRED_FAULT = 6;
    public static final int CLASS_ATTRIBUTE_READONLY_FAULT = 7;
    public static final int CLASS_COLLECTION_REQUIRED_FAULT = 8;
    public static final int CLASS_COLLECTION_ITEM_NULL_FAULT = 9;
    public static final int CLASS_COLLECTION_ITEM_TYPE_MISMATCH_FAULT = 10;
    public static final int ENTITY_NOT_FOUND_FAULT = 11;
    public static final int ENTITY_INVALID_FAULT = 12;
    public static final int ENTITY_NOT_DELETABLE_REFERENCED_FAULT = 13;
    public static final int SECURITY_ERROR_FAULT = 14;

    /**
     * Unknown error: {@link #description}
     */
    public static class UnknownErrorFault implements Fault
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
        public int getCode()
        {
            return UNKNOWN_ERROR_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Unknown error: {description}";
            message = message.replace("{description}", description);
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link UnknownErrorFault}
     */
    public static UnknownErrorFault createUnknownErrorFault(String description)
    {
        UnknownErrorFault unknownErrorFault = new UnknownErrorFault();
        unknownErrorFault.setDescription(description);
        return unknownErrorFault;
    }

    /**
     * @return new instance of {@link UnknownErrorFault}
     */
    public static <T> T throwUnknownErrorFault(String description) throws FaultException
    {
        UnknownErrorFault unknownErrorFault = createUnknownErrorFault(description);
        throw unknownErrorFault.createException();
    }

    /**
     * Value {@link #value} is illegal for type {type}.
     */
    public static class TypeIllegalValueFault implements Fault
    {
        private String type;
        private String value;

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
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
        public int getCode()
        {
            return TYPE_ILLEGAL_VALUE_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Value {value} is illegal for type {type}.";
            message = message.replace("{type}", type);
            message = message.replace("{value}", value);
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link TypeIllegalValueFault}
     */
    public static TypeIllegalValueFault createTypeIllegalValueFault(String type, String value)
    {
        TypeIllegalValueFault typeIllegalValueFault = new TypeIllegalValueFault();
        typeIllegalValueFault.setType(type);
        typeIllegalValueFault.setValue(value);
        return typeIllegalValueFault;
    }

    /**
     * @return new instance of {@link TypeIllegalValueFault}
     */
    public static <T> T throwTypeIllegalValueFault(String type, String value) throws FaultException
    {
        TypeIllegalValueFault typeIllegalValueFault = createTypeIllegalValueFault(type, value);
        throw typeIllegalValueFault.createException();
    }

    /**
     * Class {@link #className} is not defined.
     */
    public static class ClassUndefinedFault implements Fault
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
        public int getCode()
        {
            return CLASS_UNDEFINED_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Class {class} is not defined.";
            message = message.replace("{class}", className);
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link ClassUndefinedFault}
     */
    public static ClassUndefinedFault createClassUndefinedFault(String className)
    {
        ClassUndefinedFault classUndefinedFault = new ClassUndefinedFault();
        classUndefinedFault.setClassName(className);
        return classUndefinedFault;
    }

    /**
     * @return new instance of {@link ClassUndefinedFault}
     */
    public static <T> T throwClassUndefinedFault(String className) throws FaultException
    {
        ClassUndefinedFault classUndefinedFault = createClassUndefinedFault(className);
        throw classUndefinedFault.createException();
    }

    /**
     * Class {@link #className} cannot be instanced.
     */
    public static class ClassInstantiationErrorFault implements Fault
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
        public int getCode()
        {
            return CLASS_INSTANTIATION_ERROR_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Class {class} cannot be instanced.";
            message = message.replace("{class}", className);
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link ClassInstantiationErrorFault}
     */
    public static ClassInstantiationErrorFault createClassInstantiationErrorFault(String className)
    {
        ClassInstantiationErrorFault classInstantiationErrorFault = new ClassInstantiationErrorFault();
        classInstantiationErrorFault.setClassName(className);
        return classInstantiationErrorFault;
    }

    /**
     * @return new instance of {@link ClassInstantiationErrorFault}
     */
    public static <T> T throwClassInstantiationErrorFault(String className) throws FaultException
    {
        ClassInstantiationErrorFault classInstantiationErrorFault = createClassInstantiationErrorFault(className);
        throw classInstantiationErrorFault.createException();
    }

    /**
     * Attribute {@link #attribute} is not defined in class {class}.
     */
    public static class ClassAttributeUndefinedFault implements Fault
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
        public int getCode()
        {
            return CLASS_ATTRIBUTE_UNDEFINED_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Attribute {attribute} is not defined in class {class}.";
            message = message.replace("{class}", className);
            message = message.replace("{attribute}", attribute);
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link ClassAttributeUndefinedFault}
     */
    public static ClassAttributeUndefinedFault createClassAttributeUndefinedFault(String className, String attribute)
    {
        ClassAttributeUndefinedFault classAttributeUndefinedFault = new ClassAttributeUndefinedFault();
        classAttributeUndefinedFault.setClassName(className);
        classAttributeUndefinedFault.setAttribute(attribute);
        return classAttributeUndefinedFault;
    }

    /**
     * @return new instance of {@link ClassAttributeUndefinedFault}
     */
    public static <T> T throwClassAttributeUndefinedFault(String className, String attribute) throws FaultException
    {
        ClassAttributeUndefinedFault classAttributeUndefinedFault = createClassAttributeUndefinedFault(className, attribute);
        throw classAttributeUndefinedFault.createException();
    }

    /**
     * Type mismatch of value in attribute {@link #attribute} in class {class}. Present type {given-type} doesn't match required type {required-type}.
     */
    public static class ClassAttributeTypeMismatchFault implements Fault
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
        public int getCode()
        {
            return CLASS_ATTRIBUTE_TYPE_MISMATCH_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Type mismatch of value in attribute {attribute} in class {class}. Present type {given-type} doesn't match required type {required-type}.";
            message = message.replace("{class}", className);
            message = message.replace("{attribute}", attribute);
            message = message.replace("{required-type}", requiredType);
            message = message.replace("{present-type}", presentType);
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link ClassAttributeTypeMismatchFault}
     */
    public static ClassAttributeTypeMismatchFault createClassAttributeTypeMismatchFault(String className, String attribute, String requiredType, String presentType)
    {
        ClassAttributeTypeMismatchFault classAttributeTypeMismatchFault = new ClassAttributeTypeMismatchFault();
        classAttributeTypeMismatchFault.setClassName(className);
        classAttributeTypeMismatchFault.setAttribute(attribute);
        classAttributeTypeMismatchFault.setRequiredType(requiredType);
        classAttributeTypeMismatchFault.setPresentType(presentType);
        return classAttributeTypeMismatchFault;
    }

    /**
     * @return new instance of {@link ClassAttributeTypeMismatchFault}
     */
    public static <T> T throwClassAttributeTypeMismatchFault(String className, String attribute, String requiredType, String presentType) throws FaultException
    {
        ClassAttributeTypeMismatchFault classAttributeTypeMismatchFault = createClassAttributeTypeMismatchFault(className, attribute, requiredType, presentType);
        throw classAttributeTypeMismatchFault.createException();
    }

    /**
     * Attribute {@link #attribute} in class {class} wasn't present but it is required.
     */
    public static class ClassAttributeRequiredFault implements Fault
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
        public int getCode()
        {
            return CLASS_ATTRIBUTE_REQUIRED_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Attribute {attribute} in class {class} wasn't present but it is required.";
            message = message.replace("{class}", className);
            message = message.replace("{attribute}", attribute);
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link ClassAttributeRequiredFault}
     */
    public static ClassAttributeRequiredFault createClassAttributeRequiredFault(String className, String attribute)
    {
        ClassAttributeRequiredFault classAttributeRequiredFault = new ClassAttributeRequiredFault();
        classAttributeRequiredFault.setClassName(className);
        classAttributeRequiredFault.setAttribute(attribute);
        return classAttributeRequiredFault;
    }

    /**
     * @return new instance of {@link ClassAttributeRequiredFault}
     */
    public static <T> T throwClassAttributeRequiredFault(String className, String attribute) throws FaultException
    {
        ClassAttributeRequiredFault classAttributeRequiredFault = createClassAttributeRequiredFault(className, attribute);
        throw classAttributeRequiredFault.createException();
    }

    /**
     * Value for attribute {@link #attribute} in class {class} was present but the attribute is read-only.
     */
    public static class ClassAttributeReadonlyFault implements Fault
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
        public int getCode()
        {
            return CLASS_ATTRIBUTE_READONLY_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Value for attribute {attribute} in class {class} was present but the attribute is read-only.";
            message = message.replace("{class}", className);
            message = message.replace("{attribute}", attribute);
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link ClassAttributeReadonlyFault}
     */
    public static ClassAttributeReadonlyFault createClassAttributeReadonlyFault(String className, String attribute)
    {
        ClassAttributeReadonlyFault classAttributeReadonlyFault = new ClassAttributeReadonlyFault();
        classAttributeReadonlyFault.setClassName(className);
        classAttributeReadonlyFault.setAttribute(attribute);
        return classAttributeReadonlyFault;
    }

    /**
     * @return new instance of {@link ClassAttributeReadonlyFault}
     */
    public static <T> T throwClassAttributeReadonlyFault(String className, String attribute) throws FaultException
    {
        ClassAttributeReadonlyFault classAttributeReadonlyFault = createClassAttributeReadonlyFault(className, attribute);
        throw classAttributeReadonlyFault.createException();
    }

    /**
     * Collection {@link #collection} in class {class} wasn't present or was empty but it is required.
     */
    public static class ClassCollectionRequiredFault implements Fault
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
        public int getCode()
        {
            return CLASS_COLLECTION_REQUIRED_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Collection {collection} in class {class} wasn't present or was empty but it is required.";
            message = message.replace("{class}", className);
            message = message.replace("{collection}", collection);
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link ClassCollectionRequiredFault}
     */
    public static ClassCollectionRequiredFault createClassCollectionRequiredFault(String className, String collection)
    {
        ClassCollectionRequiredFault classCollectionRequiredFault = new ClassCollectionRequiredFault();
        classCollectionRequiredFault.setClassName(className);
        classCollectionRequiredFault.setCollection(collection);
        return classCollectionRequiredFault;
    }

    /**
     * @return new instance of {@link ClassCollectionRequiredFault}
     */
    public static <T> T throwClassCollectionRequiredFault(String className, String collection) throws FaultException
    {
        ClassCollectionRequiredFault classCollectionRequiredFault = createClassCollectionRequiredFault(className, collection);
        throw classCollectionRequiredFault.createException();
    }

    /**
     * Null item cannot be present in collection {@link #collection} in class {class}.
     */
    public static class ClassCollectionItemNullFault implements Fault
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
        public int getCode()
        {
            return CLASS_COLLECTION_ITEM_NULL_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Null item cannot be present in collection {collection} in class {class}.";
            message = message.replace("{class}", className);
            message = message.replace("{collection}", collection);
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link ClassCollectionItemNullFault}
     */
    public static ClassCollectionItemNullFault createClassCollectionItemNullFault(String className, String collection)
    {
        ClassCollectionItemNullFault classCollectionItemNullFault = new ClassCollectionItemNullFault();
        classCollectionItemNullFault.setClassName(className);
        classCollectionItemNullFault.setCollection(collection);
        return classCollectionItemNullFault;
    }

    /**
     * @return new instance of {@link ClassCollectionItemNullFault}
     */
    public static <T> T throwClassCollectionItemNullFault(String className, String collection) throws FaultException
    {
        ClassCollectionItemNullFault classCollectionItemNullFault = createClassCollectionItemNullFault(className, collection);
        throw classCollectionItemNullFault.createException();
    }

    /**
     * Collection {@link #collection} in class {class} contains item of type {present-type} which dosn't match the required type {required-type}.
     */
    public static class ClassCollectionItemTypeMismatchFault implements Fault
    {
        private String className;
        private String collection;
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
        public int getCode()
        {
            return CLASS_COLLECTION_ITEM_TYPE_MISMATCH_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Collection {collection} in class {class} contains item of type {present-type} which dosn't match the required type {required-type}.";
            message = message.replace("{class}", className);
            message = message.replace("{collection}", collection);
            message = message.replace("{required-type}", requiredType);
            message = message.replace("{present-type}", presentType);
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link ClassCollectionItemTypeMismatchFault}
     */
    public static ClassCollectionItemTypeMismatchFault createClassCollectionItemTypeMismatchFault(String className, String collection, String requiredType, String presentType)
    {
        ClassCollectionItemTypeMismatchFault classCollectionItemTypeMismatchFault = new ClassCollectionItemTypeMismatchFault();
        classCollectionItemTypeMismatchFault.setClassName(className);
        classCollectionItemTypeMismatchFault.setCollection(collection);
        classCollectionItemTypeMismatchFault.setRequiredType(requiredType);
        classCollectionItemTypeMismatchFault.setPresentType(presentType);
        return classCollectionItemTypeMismatchFault;
    }

    /**
     * @return new instance of {@link ClassCollectionItemTypeMismatchFault}
     */
    public static <T> T throwClassCollectionItemTypeMismatchFault(String className, String collection, String requiredType, String presentType) throws FaultException
    {
        ClassCollectionItemTypeMismatchFault classCollectionItemTypeMismatchFault = createClassCollectionItemTypeMismatchFault(className, collection, requiredType, presentType);
        throw classCollectionItemTypeMismatchFault.createException();
    }

    /**
     * Entity {@link #entity} with identifier {id} was not found.
     */
    public static class EntityNotFoundFault implements Fault
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
        public int getCode()
        {
            return ENTITY_NOT_FOUND_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Entity {entity} with identifier {id} was not found.";
            message = message.replace("{entity}", entity);
            message = message.replace("{id}", id);
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link EntityNotFoundFault}
     */
    public static EntityNotFoundFault createEntityNotFoundFault(String entity, String id)
    {
        EntityNotFoundFault entityNotFoundFault = new EntityNotFoundFault();
        entityNotFoundFault.setEntity(entity);
        entityNotFoundFault.setId(id);
        return entityNotFoundFault;
    }

    /**
     * @return new instance of {@link EntityNotFoundFault}
     */
    public static <T> T throwEntityNotFoundFault(String entity, String id) throws FaultException
    {
        EntityNotFoundFault entityNotFoundFault = createEntityNotFoundFault(entity, id);
        throw entityNotFoundFault.createException();
    }

    /**
     * Entity {@link #entity} validation failed: {reason}
     */
    public static class EntityInvalidFault implements Fault
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
        public int getCode()
        {
            return ENTITY_INVALID_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Entity {entity} validation failed: {reason}";
            message = message.replace("{entity}", entity);
            message = message.replace("{reason}", reason);
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link EntityInvalidFault}
     */
    public static EntityInvalidFault createEntityInvalidFault(String entity, String reason)
    {
        EntityInvalidFault entityInvalidFault = new EntityInvalidFault();
        entityInvalidFault.setEntity(entity);
        entityInvalidFault.setReason(reason);
        return entityInvalidFault;
    }

    /**
     * @return new instance of {@link EntityInvalidFault}
     */
    public static <T> T throwEntityInvalidFault(String entity, String reason) throws FaultException
    {
        EntityInvalidFault entityInvalidFault = createEntityInvalidFault(entity, reason);
        throw entityInvalidFault.createException();
    }

    /**
     * Entity {@link #entity} with identifier {id} cannot be deleted because it is still referenced.
     */
    public static class EntityNotDeletableReferencedFault implements Fault
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
        public int getCode()
        {
            return ENTITY_NOT_DELETABLE_REFERENCED_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Entity {entity} with identifier {id} cannot be deleted because it is still referenced.";
            message = message.replace("{entity}", entity);
            message = message.replace("{id}", id);
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link EntityNotDeletableReferencedFault}
     */
    public static EntityNotDeletableReferencedFault createEntityNotDeletableReferencedFault(String entity, String id)
    {
        EntityNotDeletableReferencedFault entityNotDeletableReferencedFault = new EntityNotDeletableReferencedFault();
        entityNotDeletableReferencedFault.setEntity(entity);
        entityNotDeletableReferencedFault.setId(id);
        return entityNotDeletableReferencedFault;
    }

    /**
     * @return new instance of {@link EntityNotDeletableReferencedFault}
     */
    public static <T> T throwEntityNotDeletableReferencedFault(String entity, String id) throws FaultException
    {
        EntityNotDeletableReferencedFault entityNotDeletableReferencedFault = createEntityNotDeletableReferencedFault(entity, id);
        throw entityNotDeletableReferencedFault.createException();
    }

    /**
     * Authentication or authorization has failed: {@link #description}
     */
    public static class SecurityErrorFault implements Fault
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
        public int getCode()
        {
            return SECURITY_ERROR_FAULT;
        }

        @Override
        public String getMessage()
        {
            String message = "Authentication or authorization has failed: {description}";
            message = message.replace("{description}", description);
            return message;
        }

        @Override
        public FaultException createException()
        {
            return new FaultException(this);
        }
    }

    /**
     * @return new instance of {@link SecurityErrorFault}
     */
    public static SecurityErrorFault createSecurityErrorFault(String description)
    {
        SecurityErrorFault securityErrorFault = new SecurityErrorFault();
        securityErrorFault.setDescription(description);
        return securityErrorFault;
    }

    /**
     * @return new instance of {@link SecurityErrorFault}
     */
    public static <T> T throwSecurityErrorFault(String description) throws FaultException
    {
        SecurityErrorFault securityErrorFault = createSecurityErrorFault(description);
        throw securityErrorFault.createException();
    }

    @Override
    protected void fillFaults()
    {
        super.fillFaults();
        addFault(UNKNOWN_ERROR_FAULT, UnknownErrorFault.class);
        addFault(TYPE_ILLEGAL_VALUE_FAULT, TypeIllegalValueFault.class);
        addFault(CLASS_UNDEFINED_FAULT, ClassUndefinedFault.class);
        addFault(CLASS_INSTANTIATION_ERROR_FAULT, ClassInstantiationErrorFault.class);
        addFault(CLASS_ATTRIBUTE_UNDEFINED_FAULT, ClassAttributeUndefinedFault.class);
        addFault(CLASS_ATTRIBUTE_TYPE_MISMATCH_FAULT, ClassAttributeTypeMismatchFault.class);
        addFault(CLASS_ATTRIBUTE_REQUIRED_FAULT, ClassAttributeRequiredFault.class);
        addFault(CLASS_ATTRIBUTE_READONLY_FAULT, ClassAttributeReadonlyFault.class);
        addFault(CLASS_COLLECTION_REQUIRED_FAULT, ClassCollectionRequiredFault.class);
        addFault(CLASS_COLLECTION_ITEM_NULL_FAULT, ClassCollectionItemNullFault.class);
        addFault(CLASS_COLLECTION_ITEM_TYPE_MISMATCH_FAULT, ClassCollectionItemTypeMismatchFault.class);
        addFault(ENTITY_NOT_FOUND_FAULT, EntityNotFoundFault.class);
        addFault(ENTITY_INVALID_FAULT, EntityInvalidFault.class);
        addFault(ENTITY_NOT_DELETABLE_REFERENCED_FAULT, EntityNotDeletableReferencedFault.class);
        addFault(SECURITY_ERROR_FAULT, SecurityErrorFault.class);
    }
}
