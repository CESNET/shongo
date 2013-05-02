package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.jade.Container;
import org.junit.Test;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReallocationTest extends AbstractControllerTest
{

    @Override
    protected void onStart()
    {
        super.onStart();

        getController().startJade();
    }

    @Test
    public void test1() throws Exception
    {
    }

    @Test
    public void test2() throws Exception
    {
    }
}
