package cz.cesnet.shongo.common;

import org.junit.Test;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;

/**
 * Identifier tests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class IdentifierTest
{
    @Test
    public void testFromString() throws Exception
    {
        Identifier identifier = null;

        identifier = new Identifier("shongo:cz.cesnet:11");
        assertEquals("cz.cesnet", identifier.getDomain());
        assertEquals(11, identifier.getId());

        identifier = new Identifier("shongo:cz.muni.fi:22");
        assertEquals("cz.muni.fi", identifier.getDomain());
        assertEquals(22, identifier.getId());
    }

    @Test
    public void testToString() throws Exception
    {
        Identifier identifier = null;

        identifier = new Identifier("cz.cesnet", 11);
        assertEquals("shongo:cz.cesnet:11", identifier.toString());

        identifier = new Identifier("cz.muni.fi", 22);
        assertEquals("shongo:cz.muni.fi:22", identifier.toString());
    }
}
