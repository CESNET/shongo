package cz.cesnet.shongo.tests.jade_ontologies;

/**
 * A utility class for parsing an address with an optional port specification.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Address
{
    private String host;
    private int port;

    /**
     * Given an address, possibly with a port specification, constructs the address.
     *
     * @param address     <code>host[:port]</code>
     * @param defaultPort port to use when no port is specified within the address
     */
    public Address(String address, int defaultPort)
    {
        host = address;
        port = defaultPort;

        if (host.indexOf(':') != -1) {
            int colonPos = host.indexOf(':');
            port = Integer.parseInt(host.substring(colonPos + 1));
            host = host.substring(0, colonPos);
        }
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    @Override
    public String toString()
    {
        return host + ":" + port;
    }
}
