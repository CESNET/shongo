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

    public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
        throw new RuntimeException(getErrorMessage(e, tokenNames));
    }
}

parse
    :   'P' dateFragment* ('T' timeFragment*)?
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

INT :   '0'..'9'+ ;
WS  :   (' '|'\t')+ {skip();} ;