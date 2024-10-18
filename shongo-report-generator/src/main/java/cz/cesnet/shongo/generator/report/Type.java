package cz.cesnet.shongo.generator.report;

import cz.cesnet.shongo.generator.GeneratorException;
import org.hibernate.usertype.UserTypeLegacyBridge;

import java.util.*;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Type
{
    private final String className;

    public Type(String className)
    {
        this.className = className;
    }

    public String getClassName()
    {
        return className;
    }

    public String getMessage(String value)
    {
        return value + ".toString()";
    }

    public List<String> getPersistenceAnnotations(String reportName, String columnName)
    {
        return new LinkedList<String>();
    }

    public String getPersistentGetterContent(String getterContent)
    {
        return getterContent;
    }

    public String getPersistentSetterContent(String setterContent)
    {
        return setterContent;
    }

    public Collection<String> getPersistencePreRemove(String variableName)
    {
        return new LinkedList<String>();
    }

    public boolean isReport()
    {
        return false;
    }

    public boolean isCollection()
    {
        return false;
    }

    public String getCollectionClassName()
    {
        return null;
    }

    public String getElementTypeClassName()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "(" + className + ")";
    }

    private static final Map<String, Type> types = new HashMap<String, Type>();

    public static Type getType(String name, String keyName, String elementName)
    {
        Type type;
        if (elementName == null) {
            type = types.get(name);
        }
        else {
            String fullName = name + ":" + elementName;
            type = types.get(fullName);
            if (type == null) {
                if (name.equals("Set") || name.equals("List")) {
                    type = new CollectionType("java.util." + name, elementName);
                    types.put(fullName, type);
                }
                else if (name.equals("Map")) {
                    type = new MapType("java.util.Map", keyName, elementName);
                    types.put(fullName, type);
                }
                else {
                    throw new GeneratorException("Type '%s' not defined.", name);
                }
            }
        }

        if (type == null) {
            throw new GeneratorException("Type '%s' not defined.", name);
        }
        return type;
    }

    static {
        types.put("Boolean", new AtomicType("Boolean"));
        types.put("Integer", new AtomicType("Integer"));
        types.put("String", new AtomicType("String")
        {
            @Override
            public String getMessage(String value)
            {
                return value;
            }
        });
        types.put("DateTime", new PersistentAtomicType("org.joda.time.DateTime", "DateTime"));
        types.put("Interval", new PersistentAtomicType("org.joda.time.Interval", "Interval") {
            @Override
            public List<String> getPersistenceAnnotations(String reportName, String columnName)
            {
                List<String> annotations = super.getPersistenceAnnotations(reportName, columnName);
                for (String annotation : annotations) {
                    if (annotation.contains("@jakarta.persistence.Column")) {
                        annotations.remove(annotation);
                        annotations.add(0, "@org.hibernate.annotations.Columns(columns={"
                                + "@jakarta.persistence.Column(name=\"" + columnName + "_start\"),"
                                + "@jakarta.persistence.Column(name=\"" + columnName + "_end\")})");
                        break;
                    }
                }
                return annotations;
            }
        });
        types.put("Period", new PersistentAtomicType("org.joda.time.Period", "Period"));
        types.put("Technology", new EnumAtomicType("cz.cesnet.shongo.Technology"));
        types.put("AliasType", new EnumAtomicType("cz.cesnet.shongo.AliasType"));

        types.put("Resource", new EntityType("cz.cesnet.shongo.controller.booking.resource.Resource"));
        types.put("Capability", new EntityType("cz.cesnet.shongo.controller.booking.resource.Capability"));
        types.put("Specification", new EntityType("cz.cesnet.shongo.controller.booking.specification.Specification"));
        types.put("Reservation", new EntityType("cz.cesnet.shongo.controller.booking.reservation.Reservation"));
        types.put("AbstractReservationRequest", new EntityType("cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest"));
        types.put("ReservationRequest", new EntityType("cz.cesnet.shongo.controller.booking.request.ReservationRequest"));
        types.put("Executable", new EntityType("cz.cesnet.shongo.controller.booking.executable.Executable"));
        types.put("Endpoint", new EntityType("cz.cesnet.shongo.controller.booking.executable.Endpoint",
                EntityType.CASCADE_PERSIST)
        {
            @Override
            public Collection<String> getPersistencePreRemove(String variableName)
            {
                Collection<String> persistencePreRemove = super.getPersistencePreRemove(variableName);
                persistencePreRemove.add("if (" + variableName + ".getState() == " +
                        "cz.cesnet.shongo.controller.booking.executable.Executable.State.NOT_ALLOCATED) {");
                persistencePreRemove.add("    " + variableName + ".setState(" +
                        "cz.cesnet.shongo.controller.booking.executable.Executable.State.TO_DELETE);");
                persistencePreRemove.add("}");
                return persistencePreRemove;
            }
        });
        types.put("TechnologySet", new EntityType("cz.cesnet.shongo.controller.booking.TechnologySet",
                EntityType.CASCADE_ALL));
        types.put("JadeReport", new EntityType("cz.cesnet.shongo.JadeReport",
                EntityType.IS_REPORT | EntityType.CASCADE_ALL));
    }

    private static class AtomicType extends Type
    {
        public AtomicType(String className)
        {
            super(className);
        }

        @Override
        public List<String> getPersistenceAnnotations(String reportName, String columnName)
        {
            List<String> persistenceAnnotations = super.getPersistenceAnnotations(reportName, columnName);
            if (getClassName().equals("String")) {
                persistenceAnnotations.add("@jakarta.persistence.Column(length = cz.cesnet.shongo.api.AbstractComplexType.DEFAULT_COLUMN_LENGTH)");

            }
            else {
                persistenceAnnotations.add("@jakarta.persistence.Column");
            }
            return persistenceAnnotations;
        }
    }

    private static class EnumAtomicType extends AtomicType
    {
        public EnumAtomicType(String className)
        {
            super(className);
        }

        @Override
        public List<String> getPersistenceAnnotations(String reportName, String columnName)
        {
            List<String> persistenceAnnotations = super.getPersistenceAnnotations(reportName, columnName);
            persistenceAnnotations.clear();
            persistenceAnnotations.add("@jakarta.persistence.Column(length = cz.cesnet.shongo.api.AbstractComplexType.ENUM_COLUMN_LENGTH)");
            persistenceAnnotations.add("@jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)");
            return persistenceAnnotations;
        }
    }

    private static class CollectionType extends Type
    {
        private Type elementType;

        public CollectionType(String className, String elementName)
        {
            super(className);
            elementType = getType(elementName, null, null);
        }

        @Override
        public String getClassName()
        {
            return super.getClassName() + "<" + elementType.getClassName() + ">";
        }

        public String getCollectionClassName()
        {
            return super.getClassName();
        }

        public String getElementTypeClassName()
        {
            return elementType.getClassName();
        }

        @Override
        public List<String> getPersistenceAnnotations(String reportName, String columnName)
        {
            List<String> persistenceAnnotations = super.getPersistenceAnnotations(reportName, columnName);
            String tableName = reportName + "_" + columnName;
            String joinColumn = reportName + "_id";
            if (elementType instanceof EntityType) {
                persistenceAnnotations.add("@jakarta.persistence.JoinTable(name = \"" + tableName + "\", "
                        + "joinColumns = @jakarta.persistence.JoinColumn(name = \"" + joinColumn + "\"))");
                persistenceAnnotations.add("@jakarta.persistence.OneToMany("
                        + "cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)");
            }
            else {
                persistenceAnnotations.add("@jakarta.persistence.CollectionTable(name = \"" + tableName + "\", "
                        + "joinColumns = @jakarta.persistence.JoinColumn(name = \"" + joinColumn + "\"))");
                persistenceAnnotations.add("@jakarta.persistence.ElementCollection");
                if (elementType instanceof EnumAtomicType) {
                    persistenceAnnotations.add("@jakarta.persistence.Column(length = cz.cesnet.shongo.api.AbstractComplexType.ENUM_COLUMN_LENGTH)");
                    persistenceAnnotations.add("@jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)");
                }
                else if (elementType instanceof AtomicType && elementType.getClassName().equals("String")) {
                    persistenceAnnotations.add("@jakarta.persistence.Column(length = cz.cesnet.shongo.api.AbstractComplexType.DEFAULT_COLUMN_LENGTH)");
                }
            }
            return persistenceAnnotations;
        }

        @Override
        public boolean isCollection()
        {
            return true;
        }
    }

    private static class MapType extends Type
    {
        private Type keyType;

        private Type elementType;

        public MapType(String className, String keyName, String elementName)
        {
            super(className);
            keyType = getType(keyName, null, null);
            elementType = getType(elementName, null, null);
        }

        @Override
        public String getClassName()
        {
            return super.getClassName() + "<" + keyType.getClassName() + ", " + elementType.getClassName() + ">";
        }

        public String getCollectionClassName()
        {
            return super.getClassName();
        }

        public String getElementTypeClassName()
        {
            return elementType.getClassName();
        }

        @Override
        public List<String> getPersistenceAnnotations(String reportName, String columnName)
        {
            String tableName = reportName + "_" + columnName;
            String joinColumn = reportName + "_id";
            List<String> persistenceAnnotations = super.getPersistenceAnnotations(reportName, columnName);
            persistenceAnnotations.add("@jakarta.persistence.CollectionTable(name = \"" + tableName + "\", "
                    + "joinColumns = @jakarta.persistence.JoinColumn(name = \"" + joinColumn + "\"))");
            persistenceAnnotations.add("@jakarta.persistence.ElementCollection");
            return persistenceAnnotations;
        }

        @Override
        public boolean isCollection()
        {
            return true;
        }
    }

    private static class PersistentAtomicType extends AtomicType
    {
        private String persistentType;

        public PersistentAtomicType(String className, String persistentType)
        {
            super(className);
            this.persistentType = persistentType;
        }

        @Override
        public List<String> getPersistenceAnnotations(String reportName, String columnName)
        {
            List<String> persistenceAnnotations = super.getPersistenceAnnotations(reportName, columnName);
            if (!persistentType.equals("DateTime")) {
                persistenceAnnotations.clear();
                persistenceAnnotations.add("@jakarta.persistence.Column(length = cz.cesnet.shongo.hibernate.Persistent" + persistentType + ".LENGTH)");
            }
            persistenceAnnotations.add("@Type(cz.cesnet.shongo.hibernate.Persistent" + persistentType + ".class, parameters = {@Parameter(name = \"" + UserTypeLegacyBridge.TYPE_NAME_PARAM_KEY + "\", value = \"" + persistentType + "\")})");
            return persistenceAnnotations;
        }
    }

    private static class EntityType extends Type
    {
        public static final int IS_REPORT = 1;
        public static final int CASCADE_PERSIST = 2;
        public static final int CASCADE_ALL = 4;

        private int flags = 0;

        public EntityType(String className)
        {
            super(className);
        }

        public EntityType(String className, int flags)
        {
            super(className);
            this.flags = flags;
        }

        public boolean hasFlag(int flag)
        {
            return (flags & flag) == flag;
        }

        @Override
        public String getMessage(String value)
        {
            return value + ".getReportDescription(messageType)";
        }

        @Override
        public List<String> getPersistenceAnnotations(String reportName, String columnName)
        {
            List<String> persistenceAnnotations = super.getPersistenceAnnotations(reportName, columnName);
            String params = null;
            if (hasFlag(CASCADE_ALL)) {
                params = "cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true";
            }
            else if (hasFlag(CASCADE_PERSIST)) {
                params = "cascade = jakarta.persistence.CascadeType.PERSIST";
            }
            if (params != null) {
                params = "(" + params + ", ";
            }
            else {
                params = "(";
            }
            params = params + "fetch = jakarta.persistence.FetchType.LAZY)";
            persistenceAnnotations.add("@jakarta.persistence.OneToOne" + params);
            persistenceAnnotations.add("@jakarta.persistence.Access(jakarta.persistence.AccessType.FIELD)");
            persistenceAnnotations.add("@jakarta.persistence.JoinColumn(name = \"" + columnName + "_id\")");
            return persistenceAnnotations;
        }

        @Override
        public String getPersistentGetterContent(String getterContent)
        {
            return "cz.cesnet.shongo.PersistentObject.getLazyImplementation(" + getterContent + ")";
        }

        @Override
        public boolean isReport()
        {
            return hasFlag(IS_REPORT);
        }
    }
}
