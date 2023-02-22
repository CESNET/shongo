package cz.cesnet.shongo.client.web.support.tags;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.tags.Param;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import javax.servlet.jsp.JspException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Extends {@link org.springframework.web.servlet.tags.UrlTag}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UrlTag extends org.springframework.web.servlet.tags.UrlTag
{
    private boolean empty;

    @Override
    public void setValue(String value)
    {
        if (value == null || value.isEmpty()) {
            empty = true;
        }
        else {
            empty = false;
            super.setValue(value);
        }
    }

    @Override
    public int doEndTag() throws JspException {

        if (!empty) {
            super.doEndTag();
        }
        return EVAL_PAGE;
    }

    @Override
    protected String createQueryString(List<Param> params, Set<String> usedParams, boolean includeQueryStringDelimiter)
    {
        String encoding = pageContext.getResponse().getCharacterEncoding();
        StringBuilder qs = new StringBuilder();
        for (Param param : params) {
            if (!usedParams.contains(param.getName()) && StringUtils.hasLength(param.getName())) {
                if (includeQueryStringDelimiter && qs.length() == 0) {
                    qs.append("?");
                }
                else {
                    qs.append("&");
                }
                if (param instanceof ParamTag.NotEscapedParam) {
                    qs.append(param.getName());
                    if (param.getValue() != null) {
                        qs.append("=");
                        qs.append(param.getValue());
                    }
                }
                else {
                    qs.append(UriUtils.encodeQueryParam(param.getName(), encoding).replace("+", "%2B"));
                    if (param.getValue() != null) {
                        qs.append("=");
                        qs.append(UriUtils.encodeQueryParam(param.getValue(), encoding).replace("+", "%2B"));
                    }
                }
            }
        }
        return qs.toString();
    }

    @Override
    protected String replaceUriTemplateParams(String uri, List<Param> params, Set<String> usedParams)
    {
        String encoding = pageContext.getResponse().getCharacterEncoding();
        for (Param param : params) {
            String template = "{" + param.getName();
            int startIndex = uri.indexOf(template);
            if (startIndex != -1) {
                int endIndex = uri.indexOf("}", startIndex);
                if (endIndex != -1) {
                    usedParams.add(param.getName());
                    String value;
                    if (param instanceof ParamTag.NotEscapedParam) {
                        value = param.getValue();
                    }
                    else {
                        value = UriUtils.encodePath(param.getValue(), encoding).replace("+", "%2B");
                    }
                    uri = uri.substring(0, startIndex) + value + uri.substring(endIndex + 1);
                }
            }
        }
        return uri;
    }
}
