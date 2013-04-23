package cz.cesnet.shongo.generator.report;

import cz.cesnet.shongo.generator.GeneratorException;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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

    public Collection<String> getPersistenceAnnotations(String columnName)
    {
        return new LinkedList<String>();
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
        types.put("Period", new PersistentAtomicType("org.joda.time.Period", "Period"));
        types.put("Technology", new EnumAtomicType("cz.cesnet.shongo.Technology"));
        types.put("AliasType", new EnumAtomicType("cz.cesnet.shongo.AliasType"));

        types.put("Resource", new EntityType("cz.cesnet.shongo.controller.resource.Resource"));
        types.put("Capability", new EntityType("cz.cesnet.shongo.controller.resource.Capability"));
        types.put("Specification", new EntityType("cz.cesnet.shongo.controller.request.Specification"));
        types.put("Reservation", new EntityType("cz.cesnet.shongo.controller.reservation.Reservation"));
        types.put("Executable", new EntityType("cz.cesnet.shongo.controller.executor.Executable"));
        types.put("Endpoint", new EntityType("cz.cesnet.shongo.controller.executor.Endpoint",
                EntityType.CASCADE_PERSIST)
        {
            @Override
            public Collection<String> getPersistencePreRemove(String variableName)
            {
                Collection<String> persistencePreRemove = super.getPersistencePreRemove(variableName);
                persistencePreRemove.add("if (" + variableName + ".getState() == " +
                        "cz.cesnet.shongo.controller.executor.Executable.State.NOT_ALLOCATED) {");
                persistencePreRemove.add("    " + variableName + ".setState(" +
                        "cz.cesnet.shongo.controller.executor.Executable.State.TO_DELETE);");
                persistencePreRemove.add("}");
                return persistencePreRemove;
            }
        });
        types.put("TechnologySet", new EntityType("cz.cesnet.shongo.controller.scheduler.TechnologySet",
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
        public Collection<String> getPersistenceAnnotations(String columnName)
        {
            Collection<String> persistenceAnnotations = super.getPersistenceAnnotations(columnName);
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
        public Collection<String> getPersistenceAnnotations(String columnName)
        {
            Collection<String> persistenceAnnotations = super.getPersistenceAnnotations(columnName);
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

        @Override
        public Collection<String> getPersistenceAnnotations(String columnName)
        {
            Collection<String> persistenceAnnotations = super.getPersistenceAnnotations(columnName);
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
        public Collection<String> getPersistenceAnnotations(String columnName)
        {
            Collection<String> persistenceAnnotations = super.getPersistenceAnnotations(columnName);
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
            return value + ".getReportDescription()";
        }

        @Override
        public Collection<String> getPersistenceAnnotations(String columnName)
        {
            Collection<String> persistenceAnnotations = super.getPersistenceAnnotations(columnName);
            String params = "";
            if (hasFlag(CASCADE_ALL)) {
                params = "(cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true)";
            }
            else if (hasFlag(CASCADE_PERSIST)) {
                params = "(cascade = javax.persistence.CascadeType.PERSIST)";
            }
            persistenceAnnotations.add("@javax.persistence.OneToOne" + params);
            persistenceAnnotations.add("@javax.persistence.JoinColumn(name = \"" + columnName + "_id\")");
            return persistenceAnnotations;
        }

        @Override
        public boolean isReport()
        {
            return hasFlag(IS_REPORT);
        }
    }
}
