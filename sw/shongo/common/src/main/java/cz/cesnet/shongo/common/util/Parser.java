package cz.cesnet.shongo.common.util;

import org.antlr.runtime.Lexer;
import org.antlr.runtime.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser helper
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Parser
{
    private static Logger logger = LoggerFactory.getLogger(Parser.class);

    /**
     * Get token stream from text and lexer class
     *
     * @param text
     * @param lexerClass
     * @return token stream
     */
    public static TokenStream getTokenStream(String text, Class<? extends Lexer> lexerClass)
    {
        org.antlr.runtime.CommonTokenStream tokens = null;
        try {
            java.io.InputStream inputStream = new java.io.ByteArrayInputStream(text.getBytes());
            org.antlr.runtime.ANTLRInputStream input = new org.antlr.runtime.ANTLRInputStream(inputStream);
            Lexer lexer = lexerClass.newInstance();
            lexer.setCharStream(input);
            tokens = new org.antlr.runtime.CommonTokenStream(lexer);
        }
        catch (Exception exception) {
            logger.error("Failed create token stream for parsing.", exception);
        }
        return tokens;
    }
}
