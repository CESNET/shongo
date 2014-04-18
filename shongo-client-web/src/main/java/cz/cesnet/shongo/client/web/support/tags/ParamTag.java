package cz.cesnet.shongo.client.web.support.tags;

import org.springframework.web.servlet.tags.Param;
import org.springframework.web.servlet.tags.ParamAware;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * Extends {@link org.springframework.web.servlet.tags.ParamTag}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ParamTag extends BodyTagSupport
{
    private String name;

    private String value;

    private Param param;

    private boolean escape = true;

    @Override
    public int doEndTag() throws JspException {
        param = (escape ? new Param() : new NotEscapedParam());
        param.setName(name);
        if (value != null) {
            param.setValue(value);
        }
        else if (getBodyContent() != null) {
            // get the value from the tag body
            param.setValue(getBodyContent().getString().trim());
        }

        // find a param aware ancestor
        ParamAware paramAwareTag = (ParamAware) findAncestorWithClass(this,
                ParamAware.class);
        if (paramAwareTag == null) {
            throw new JspException(
                    "The param tag must be a descendant of a tag that supports parameters");
        }

        paramAwareTag.addParam(param);

        return EVAL_PAGE;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setEscape(boolean escape)
    {
        this.escape = escape;
    }

    public static class NotEscapedParam extends Param
    {
    }
}
