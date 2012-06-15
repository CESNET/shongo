grammar Date;

@header {
package cz.cesnet.shongo.common;
}

@lexer::header {
package cz.cesnet.shongo.common;
}

@members {
    public Integer year;
    public Integer month;
    public Integer day;

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
    :   (   tokenYear=INT '-' tokenMonth=INT ( '-' tokenDay=INT { day = Integer.parseInt($tokenDay.text); } )?
            {
                year = Integer.parseInt($tokenYear.text);
                month = Integer.parseInt($tokenMonth.text);
            }
        )
    |   (   INT
            {
                if ( $INT.text.length() == 8 ) {
                    year = Integer.parseInt($INT.text.substring(0, 4));
                    month = Integer.parseInt($INT.text.substring(4, 6));
                    day = Integer.parseInt($INT.text.substring(6, 8));
                }
                else if ( $INT.text.length() == 6 ) {
                    year = Integer.parseInt($INT.text.substring(0, 4));
                    month = Integer.parseInt($INT.text.substring(4, 6));
                }
                else {
                    year = Integer.parseInt($INT.text);
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