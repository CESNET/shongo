grammar Time;

@header {
package cz.cesnet.shongo.common;
}

@lexer::header {
package cz.cesnet.shongo.common;
}

@members {
    public Integer hour;
    public Integer minute;
    public Integer second;

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
    :   (   tokenHour=INT ':' tokenMinute=INT ( ':' tokenSecond=INT { second = Integer.parseInt($tokenSecond.text); } )?
            {
                hour = Integer.parseInt($tokenHour.text);
                minute = Integer.parseInt($tokenMinute.text);
            }
        )
    |   (   INT
            {
                if ( $INT.text.length() == 6 ) {
                    hour = Integer.parseInt($INT.text.substring(0, 2));
                    minute = Integer.parseInt($INT.text.substring(2, 4));
                    second = Integer.parseInt($INT.text.substring(4, 6));
                }
                else if ( $INT.text.length() == 4 ) {
                    hour = Integer.parseInt($INT.text.substring(0, 2));
                    minute = Integer.parseInt($INT.text.substring(2, 4));
                }
                else {
                    hour = Integer.parseInt($INT.text);
                }
            }
        )
    ;

fragment DIGIT
    :   '0'..'9'
    ;

INT :   DIGIT+
    ;

WS  :   (' '|'\t')+ { skip(); }
    ;