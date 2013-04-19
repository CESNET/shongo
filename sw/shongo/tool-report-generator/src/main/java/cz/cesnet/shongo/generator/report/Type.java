package cz.cesnet.shongo.generator.report;

import cz.cesnet.shongo.generator.GeneratorException;

import java.util.HashMap;
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

    public String getClassName(String typeElementName)
    {
        return className;
    }

    public String getString(String value)
    {
        return value + ".toString()";
    }

    public abstract String getPersistenceAnnotation(String columnName);

    public boolean isReport()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "(" + className + ")";
    }

    private static final Map<String, Type> types = new HashMap<String, Type>();

    public static Type getType(String name)
    {
        Type type = types.get(name);
        if (type == null) {
            throw new GeneratorException("Type '%s' not defined.", name);
        }
        return type;
    }

    static {
        types.put("Integer", new AtomicType("Integer"));
        types.put("String", new AtomicType("String") {
            @Override
            public String getString(String value)
            {
                return value;
            }
        });
        types.put("Set", new CollectionType("java.util.Set"));
        types.put("List", new CollectionType("java.util.List"));

        types.put("DateTime", new PersistentAtomicType("org.joda.time.DateTime", "DateTime"));
        types.put("Period", new PersistentAtomicType("org.joda.time.Period", "Period"));
        types.put("Technology", new EnumAtomicType("cz.cesnet.shongo.Technology"));
        types.put("AliasType", new EnumAtomicType("cz.cesnet.shongo.AliasType"));

        types.put("Resource", new EntityType("cz.cesnet.shongo.controller.resource.Resource"));
        types.put("Capability", new EntityType("cz.cesnet.shongo.controller.resource.Capability"));
        types.put("Specification", new EntityType("cz.cesnet.shongo.controller.request.Specification"));
        types.put("Reservation", new EntityType("cz.cesnet.shongo.controller.reservation.Reservation"));
        types.put("Executable", new EntityType("cz.cesnet.shongo.controller.executor.Executable"));
        types.put("Endpoint", new EntityType("cz.cesnet.shongo.controller.executor.Endpoint"));
        types.put("TechnologySet", new EntityType("cz.cesnet.shongo.controller.scheduler.TechnologySet"));

        types.put("JadeReport", new EntityType("cz.cesnet.shongo.JadeReport", true, true));
    }

    private static class AtomicType extends Type
    {
        public AtomicType(String className)
        {
            super(className);
        }

        @Override
        public String getPersistenceAnnotation(String columnName)
        {
            return "@javax.persistence.Column";
        }
    }

    private static class EnumAtomicType extends AtomicType
    {
        public EnumAtomicType(String className)
        {
            super(className);
        }

        @Override
        public String getPersistenceAnnotation(String columnName)
        {
            return super.getPersistenceAnnotation(columnName) + " @javax.persistence.Enumerated(javax.persistence.EnumType.STRING)";
        }
    }

    private static class CollectionType extends Type
    {
        public CollectionType(String className)
        {
            super(className);
        }

        @Override
        public String getClassName(String typeElementName)
        {
            if (typeElementName == null) {
                throw new GeneratorException("Collection element type is empty.");
            }
            return super.getClassName(typeElementName) + "<" + getType(typeElementName).getClassName(null) + ">";
        }

        @Override
        public String getPersistenceAnnotation(String columnName)
        {
            return "@javax.persistence.ElementCollection";
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
        public String getPersistenceAnnotation(String columnName)
        {
            return super.getPersistenceAnnotation(columnName) + " @org.hibernate.annotations.Type(type = \"" + persistentType + "\")";
        }
    }

    private static class EntityType extends Type
    {
        private final boolean persistenceCascade;

        private final boolean report;

        public EntityType(String className)
        {
            this(className, false, false);
        }

        public EntityType(String className, boolean persistenceCascade, boolean report)
        {
            super(className);
            this.persistenceCascade = persistenceCascade;
            this.report = report;
        }

        @Override
        public String getPersistenceAnnotation(String columnName)
        {
            String params = "";
            if (persistenceCascade) {
                params = "cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true";
            }
            return "@javax.persistence.OneToOne(" + params + ") @javax.persistence.JoinColumn(name = \"" + columnName + "_id\")";
        }

        @Override
        public boolean isReport()
        {
            return report;
        }
    }
}
