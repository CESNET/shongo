grammar Period;

@header {
package cz.cesnet.shongo.common;
}

@lexer::header {
package cz.cesnet.shongo.common;
}

@members {
    private Period period;

    public void setPeriod(Period period)
    {
        this.period = period;
    }

    public void displayRecognitionError(String[] tokenNames, RecognitionException e)
    {
        throw new RuntimeException(getErrorMessage(e, tokenNames));
    }
}

parse
    :   (   'P' dateFragment* ('T' timeFragment*)? )
    |   (   'P' date=INT 'T' time=INT
            {
                if ( $date.text.length() != 8 )
                    throw new RuntimeException("Date part should be 8 characters long.");
                if ( $time.text.length() != 6 )
                    throw new RuntimeException("Time part should be 6 characters long.");
                period.setYear(Integer.parseInt($date.text.substring(0, 4)));
                period.setMonth(Integer.parseInt($date.text.substring(4, 6)));
                period.setDay(Integer.parseInt($date.text.substring(6, 8)));
                period.setHour(Integer.parseInt($time.text.substring(0, 2)));
                period.setMinute(Integer.parseInt($time.text.substring(2, 4)));
                period.setSecond(Integer.parseInt($time.text.substring(4, 6)));
            }
        )
    |   (   'P' year=INT '-' month=INT '-' day=INT 'T' hour=INT ':' minute=INT ':' second=INT
            {
                period.setYear(Integer.parseInt($year.text));
                period.setMonth(Integer.parseInt($month.text));
                period.setDay(Integer.parseInt($day.text));
                period.setHour(Integer.parseInt($hour.text));
                period.setMinute(Integer.parseInt($minute.text));
                period.setSecond(Integer.parseInt($second.text));
            }
        )
    ;

dateFragment
    :   INT 'Y' { period.setYear(Integer.parseInt($INT.text)); }
    |   INT 'M' { period.setMonth(Integer.parseInt($INT.text)); }
    |   INT 'D' { period.setDay(Integer.parseInt($INT.text)); }
    |   INT 'W' { period.setWeek(Integer.parseInt($INT.text)); }
    ;

timeFragment
    :   INT 'H' { period.setHour(Integer.parseInt($INT.text)); }
    |   INT 'M' { period.setMinute(Integer.parseInt($INT.text)); }
    |   INT 'S' { period.setSecond(Integer.parseInt($INT.text)); }
    ;

fragment DIGIT
    :   '0'..'9'
    ;

INT :   DIGIT+
    ;

WS  :   (' '|'\t')+ { skip(); }
    ;