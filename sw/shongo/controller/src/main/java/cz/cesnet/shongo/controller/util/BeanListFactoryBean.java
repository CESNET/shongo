package cz.cesnet.shongo.controller.util;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link AbstractFactoryBean} which list all beans of specified <code>beanType</code>.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class BeanListFactoryBean<T> extends AbstractFactoryBean<List<T>>
{
    private Class<T> beanType;

    @Autowired
    private ListableBeanFactory beanFactory;

    @Required
    public void setBeanType(Class<T> beanType)
    {
        this.beanType = beanType;
    }

    @Override
    protected List<T> createInstance() throws Exception
    {
        return new ArrayList<T>(beanFactory.getBeansOfType(beanType).values());
    }

    @Override
    public Class<?> getObjectType()
    {
        return List.class;
    }
}
