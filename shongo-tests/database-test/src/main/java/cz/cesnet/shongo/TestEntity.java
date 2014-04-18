package cz.cesnet.shongo;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class TestEntity
{
    private Long id;

    private String attribute;

    private List<TestChildEntity> childEntities = new LinkedList<TestChildEntity>();

    @Id
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @Column
    public String getAttribute()
    {
        return attribute;
    }

    public void setAttribute(String attribute)
    {
        this.attribute = attribute;
    }

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<TestChildEntity> getChildEntities()
    {
        return childEntities;
    }

    public void setChildEntities(List<TestChildEntity> childEntities)
    {
        this.childEntities = childEntities;
    }

    public void addChildEntity(TestChildEntity testChildEntity)
    {
        childEntities.add(testChildEntity);
    }
}
