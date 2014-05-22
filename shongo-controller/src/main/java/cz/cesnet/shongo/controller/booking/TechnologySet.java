package cz.cesnet.shongo.controller.booking;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.AbstractComplexType;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Represents a set of technologies which can be persisted.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class TechnologySet extends SimplePersistentObject implements Collection<Technology>
{
    /**
     * Set of technologies.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Constructor.
     */
    public TechnologySet()
    {
    }

    /**
     * Constructor.
     *
     * @param technologies sets the {@link #technologies}
     */
    public TechnologySet(Set<Technology> technologies)
    {
        setTechnologies(technologies);
    }

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Column(length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<Technology> getTechnologies()
    {
        return technologies;
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies.clear();
        this.technologies.addAll(technologies);
    }

    @Override
    public String toString()
    {
        return Technology.formatTechnologies(technologies);
    }

    @Transient
    @Override
    public int size()
    {
        return technologies.size();
    }

    @Transient
    @Override
    public boolean isEmpty()
    {
        return technologies.isEmpty();
    }

    @Transient
    @Override
    public boolean contains(Object o)
    {
        return technologies.contains(o);
    }

    @Transient
    @Override
    public Iterator iterator()
    {
        return technologies.iterator();
    }

    @Transient
    @Override
    public Object[] toArray()
    {
        return technologies.toArray();
    }

    @Transient
    @Override
    public boolean add(Technology o)
    {
        return technologies.add(o);
    }

    @Transient
    @Override
    public boolean remove(Object o)
    {
        return technologies.remove(o);
    }

    @Transient
    @Override
    public boolean containsAll(Collection<?> c)
    {
        return technologies.containsAll(c);
    }

    @Transient
    @Override
    public boolean addAll(Collection c)
    {
        return technologies.addAll(c);
    }

    @Transient
    @Override
    public boolean removeAll(Collection<?> c)
    {
        return technologies.removeAll(c);
    }

    @Transient
    @Override
    public boolean retainAll(Collection<?> c)
    {
        return technologies.retainAll(c);
    }

    @Transient
    @Override
    public void clear()
    {
        technologies.clear();
    }

    @Transient
    @Override
    public Object[] toArray(Object[] a)
    {
        return technologies.toArray(a);
    }
}
