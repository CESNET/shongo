package cz.cesnet.shongo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class TestChildEntity
{
    private Long id;

    private String attribute;

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
}
