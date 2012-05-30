grammar Identifier;

@header {
package cz.cesnet.shongo.common;
}

@lexer::header {
package cz.cesnet.shongo.common;
}

@members {
    String type;
    String domain;
    String uuid;

    @Override
    public void displayRecognitionError(String[] tokenNames, RecognitionException e)
    {
        throw new RuntimeException(getErrorMessage(e, tokenNames));
    }
}

@lexer::members {
    @Override
    public void reportError(RecognitionException exception)
    {
        throw new RuntimeException(getErrorMessage(exception, null));
    }
}

parse
    :   'shongo:' type { type = $type.text; }
        ':' domain     { domain = $domain.text; }
        ':' uuid       { uuid = $uuid.text; }
    ;

type
    :   'resource' | 'reservation'
    ;


uuid
    :   fourHexDigits fourHexDigits '-' fourHexDigits '-' fourHexDigits '-'
        fourHexDigits '-' fourHexDigits fourHexDigits fourHexDigits
    ;

domain
    :   domainPart ('.' domainPart)*
    ;

domainPart
    :   (HEX_DIGIT | OTHER_LETTER)+
    ;

fourHexDigits
    :   HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;

HEX_DIGIT
    :   'a'..'f' | 'A'..'F' | '0'..'9'
    ;

OTHER_LETTER
    :   'g'..'z' | 'G'..'Z' | '-' | '_'
    ;
