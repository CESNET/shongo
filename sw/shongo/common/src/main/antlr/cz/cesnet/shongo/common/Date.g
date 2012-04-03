grammar Date;

@header {
package cz.cesnet.shongo.common;
}

@lexer::header {
package cz.cesnet.shongo.common;
}

@members {
    private Date date;

    public void setDate(Date date)
    {
        this.date = date;
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
    :   (   year=INT '-' month=INT ( '-' day=INT { date.setDay(Integer.parseInt($day.text)); } )?
            {
                date.setYear(Integer.parseInt($year.text));
                date.setMonth(Integer.parseInt($month.text));
            }
        )
    |   (   INT
            {
                if ( $INT.text.length() == 8 ) {
                    date.setYear(Integer.parseInt($INT.text.substring(0, 4)));
                    date.setMonth(Integer.parseInt($INT.text.substring(4, 6)));
                    date.setDay(Integer.parseInt($INT.text.substring(6, 8)));
                }
                else if ( $INT.text.length() == 6 ) {
                    date.setYear(Integer.parseInt($INT.text.substring(0, 4)));
                    date.setMonth(Integer.parseInt($INT.text.substring(4, 6)));
                }
                else {
                    date.setYear(Integer.parseInt($INT.text));
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