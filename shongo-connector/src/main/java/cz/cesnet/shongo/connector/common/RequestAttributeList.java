package cz.cesnet.shongo.connector.common;


import cz.cesnet.shongo.api.jade.CommandException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;

/**
 * @author opicak <pavelka@cesnet.cz>
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class RequestAttributeList extends LinkedList<RequestAttributeList.Entry>
{

    public String getAttributesQuery() throws UnsupportedEncodingException {
        String queryString = "";
        for (RequestAttributeList.Entry entry : this) {
            queryString += '&' + entry.getKey() + '=' + URLEncoder.encode(entry.getValue(),"UTF8");
        }
        return queryString;
    }

    public void add(String key, String value) throws CommandException
    {
        if (!add(new Entry(key, value))) {
            throw new CommandException("Failed to add attribute to attribute list.");
        }
    }

    public String getValue(String key)
    {
        for (Entry entry : this)
        {
            if (entry.getKey().equals(key))
            {
                return entry.getValue();
            }
        }

        return null;
    }

    public Entry getEntry(String key)
    {
        for (Entry entry : this)
        {
            if (entry.getKey().equals(key))
            {
                return entry;
            }
        }
        return null;
    }

    public static class Entry {
        private String key;
        private String value;

        public Entry(String key,String value)
        {
            setKey(key);
            setValue(value);
        }

        public String getKey()
        {
            return key;
        }

        public void setKey(String key)
        {
            this.key = key;
        }

        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }
    }
}

