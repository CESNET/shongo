grammar Identifier;

@header {
package cz.cesnet.shongo.common;
}

@lexer::header {
package cz.cesnet.shongo.common;
}

@members {
    String domain;
    String id;

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
    :   'shongo'
        ':' domain     { domain = $domain.text; }
        ':' id         { id = $id.text; }
    ;

id
    :   DIGIT+
    ;

domain
    :   domainPart ('.' domainPart)*
    ;

domainPart
    :   LETTER (DIGIT | LETTER)*
    ;

number
    :
    ;

DIGIT
    :   '0'..'9'
    ;

LETTER
    :   'a'..'z' | 'A'..'Z' | '-' | '_'
    ;
