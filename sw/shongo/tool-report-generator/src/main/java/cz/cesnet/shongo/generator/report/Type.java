package cz.cesnet.shongo.generator.report;

import cz.cesnet.shongo.generator.GeneratorException;

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

    public List<String> getPersistenceAnnotations(String columnName)
    {
        return new LinkedList<String>();
    }

    public String getPersistentGetterContent(String getterContent)
    {
        return getterContent;
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

    public static Type getType(String name, String elementName)
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
            public List<String> getPersistenceAnnotations(String columnName)
            {
                List<String> annotations = super.getPersistenceAnnotations(columnName);
                for (String annotation : annotations) {
                    if (annotation.contains("@javax.persistence.Column")) {
                        annotations.remove(annotation);
                        annotations.add(0, "@org.hibernate.annotations.Columns(columns={"
                                + "@javax.persistence.Column(name=\"" + columnName + "_start\"),"
                                + "@javax.persistence.Column(name=\"" + columnName + "_end\")})");
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
        public List<String> getPersistenceAnnotations(String columnName)
        {
            List<String> persistenceAnnotations = super.getPersistenceAnnotations(columnName);
            persistenceAnnotations.add("@javax.persistence.Column");
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
        public List<String> getPersistenceAnnotations(String columnName)
        {
            List<String> persistenceAnnotations = super.getPersistenceAnnotations(columnName);
            persistenceAnnotations.add("@javax.persistence.Enumerated(javax.persistence.EnumType.STRING)");
            return persistenceAnnotations;
        }
    }

    private static class CollectionType extends Type
    {
        private Type elementType;

        public CollectionType(String className, String elementName)
        {
            super(className);
            elementType = getType(elementName, null);
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
        public List<String> getPersistenceAnnotations(String columnName)
        {
            List<String> persistenceAnnotations = super.getPersistenceAnnotations(columnName);
            if (elementType instanceof EntityType) {
                persistenceAnnotations.add("@javax.persistence.OneToMany("
                        + "cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true)");
            }
            else {
                persistenceAnnotations.add("@javax.persistence.ElementCollection");
            }
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
        public List<String> getPersistenceAnnotations(String columnName)
        {
            List<String> persistenceAnnotations = super.getPersistenceAnnotations(columnName);
            persistenceAnnotations.add("@org.hibernate.annotations.Type(type = \"" + persistentType + "\")");
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
        public List<String> getPersistenceAnnotations(String columnName)
        {
            List<String> persistenceAnnotations = super.getPersistenceAnnotations(columnName);
            String params = null;
            if (hasFlag(CASCADE_ALL)) {
                params = "cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true";
            }
            else if (hasFlag(CASCADE_PERSIST)) {
                params = "cascade = javax.persistence.CascadeType.PERSIST";
            }
            if (params != null) {
                params = "(" + params + ", ";
            }
            else {
                params = "(";
            }
            params = params + "fetch = javax.persistence.FetchType.LAZY)";
            persistenceAnnotations.add("@javax.persistence.OneToOne" + params);
            persistenceAnnotations.add("@javax.persistence.Access(javax.persistence.AccessType.FIELD)");
            persistenceAnnotations.add("@javax.persistence.JoinColumn(name = \"" + columnName + "_id\")");
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
