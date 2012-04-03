grammar Time;

@header {
package cz.cesnet.shongo.common;
}

@lexer::header {
package cz.cesnet.shongo.common;
}

@members {
    private Time time;

    public void setTime(Time time)
    {
        this.time = time;
    }

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
    :   (   hour=INT ':' minute=INT ( ':' second=INT { time.setSecond(Integer.parseInt($second.text)); } )?
            {
                time.setHour(Integer.parseInt($hour.text));
                time.setMinute(Integer.parseInt($minute.text));
            }
        )
    |   (   INT
            {
                if ( $INT.text.length() == 6 ) {
                    time.setHour(Integer.parseInt($INT.text.substring(0, 2)));
                    time.setMinute(Integer.parseInt($INT.text.substring(2, 4)));
                    time.setSecond(Integer.parseInt($INT.text.substring(4, 6)));
                }
                else if ( $INT.text.length() == 4 ) {
                    time.setHour(Integer.parseInt($INT.text.substring(0, 2)));
                    time.setMinute(Integer.parseInt($INT.text.substring(2, 4)));
                }
                else {
                    time.setHour(Integer.parseInt($INT.text));
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