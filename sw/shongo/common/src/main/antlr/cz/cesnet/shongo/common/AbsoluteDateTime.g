grammar AbsoluteDateTime;

@header {
package cz.cesnet.shongo.common;
}

@lexer::header {
package cz.cesnet.shongo.common;
}

@members {
    private AbsoluteDateTime absoluteDateTime;

    public void setAbsoluteDateTime(AbsoluteDateTime absoluteDateTime)
    {
        this.absoluteDateTime = absoluteDateTime;
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
    :   parseDate ( 'T' parseTime )?
    ;

parseDate
    :   (   year=INT '-' month=INT ( '-' day=INT { absoluteDateTime.setDay(Integer.parseInt($day.text)); } )?
            {
                absoluteDateTime.setYear(Integer.parseInt($year.text));
                absoluteDateTime.setMonth(Integer.parseInt($month.text));
            }
        )
    |   (   INT
            {
                if ( $INT.text.length() == 8 ) {
                    absoluteDateTime.setYear(Integer.parseInt($INT.text.substring(0, 4)));
                    absoluteDateTime.setMonth(Integer.parseInt($INT.text.substring(4, 6)));
                    absoluteDateTime.setDay(Integer.parseInt($INT.text.substring(6, 8)));
                }
                else if ( $INT.text.length() == 6 ) {
                    absoluteDateTime.setYear(Integer.parseInt($INT.text.substring(0, 4)));
                    absoluteDateTime.setMonth(Integer.parseInt($INT.text.substring(4, 6)));
                }
                else {
                    absoluteDateTime.setYear(Integer.parseInt($INT.text));
                }
            }
        )
    ;

parseTime
    :   (   hour=INT ':' minute=INT ( ':' second=INT { absoluteDateTime.setSecond(Integer.parseInt($second.text)); } )?
            {
                absoluteDateTime.setHour(Integer.parseInt($hour.text));
                absoluteDateTime.setMinute(Integer.parseInt($minute.text));
            }
        )
    |   (   INT
            {
                if ( $INT.text.length() == 6 ) {
                    absoluteDateTime.setHour(Integer.parseInt($INT.text.substring(0, 2)));
                    absoluteDateTime.setMinute(Integer.parseInt($INT.text.substring(2, 4)));
                    absoluteDateTime.setSecond(Integer.parseInt($INT.text.substring(4, 6)));
                }
                else if ( $INT.text.length() == 4 ) {
                    absoluteDateTime.setHour(Integer.parseInt($INT.text.substring(0, 2)));
                    absoluteDateTime.setMinute(Integer.parseInt($INT.text.substring(2, 4)));
                }
                else {
                    absoluteDateTime.setHour(Integer.parseInt($INT.text));
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