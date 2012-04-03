package cz.cesnet.shongo.common;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author Ondrej Bouda
 */
public class PeriodTest
{
    private Period period;

    @Before
    public void setUp() throws Exception
    {
        period = new Period();
        period.setYear(3);
        period.setMonth(6);
        period.setDay(4);
        period.setHour(12);
        period.setMinute(30);
        period.setSecond(5);
    }

    @Test
    public void testFromString() throws Exception
    {
        assertEquals(period, new Period("P3Y6M4DT12H30M5S"));

        Period weeksPeriod = new Period("P2Y1W");
        assertEquals(2, weeksPeriod.getYear());
        assertEquals(0, weeksPeriod.getWeek());
        assertEquals(7, weeksPeriod.getDay());

        Period zeroPeriod = new Period("P");
        assertEquals(0, zeroPeriod.getYear());
        assertEquals(0, zeroPeriod.getMonth());
        assertEquals(0, zeroPeriod.getDay());
        assertEquals(0, zeroPeriod.getWeek());
        assertEquals(0, zeroPeriod.getHour());
        assertEquals(0, zeroPeriod.getMinute());
        assertEquals(0, zeroPeriod.getSecond());
    }

    @Test
    public void testToString() throws Exception
    {
        assertEquals("P3Y6M4DT12H30M5S", period.toString());

        Period p = new Period("P");
        p.setWeek(3);
        p.setDay(1);
        p.setYear(1);
        p.setSecond(4);
        assertEquals("P1Y22DT4S", p.toString());

        assertEquals("Ranges in the output string should be normalized.", new Period("P1W"), new Period("P7D"));

        Period zeroPeriod = new Period("P");
        assertEquals("PT0S", zeroPeriod.toString());
    }

    @Test
    public void testAdd() throws Exception
    {
        assertEquals("P3Y6M4DT13H30M5S", period.add(new Period("PT1H")).toString());
        assertEquals("P7D", new Period("P3D").add(new Period("P4D")).toString());
    }

    @Test
    public void testCarryOver() throws Exception
    {
        assertEquals("P1DT12H", new Period("PT36H").toString());

        assertEquals("P1Y1M", new Period("P10M").add(new Period("P3M")).toString());
    }

    @Test
    public void testEquals() throws Exception
    {
        assertEquals(period, period);

        Period p = new Period("P3W").add(new Period("P1D")).add(new Period("P1Y")).add(new Period("PT1S"));
        assertEquals(new Period("P1Y3W1DT1S"), p);
    }

    @Test
    public void testCompareTo() throws Exception
    {
        Period period1 = new Period("P7D");
        Period period2 = new Period("P1W");
        Period period3 = new Period("P12M");
        Period period4 = new Period("P1Y");

        assertEquals(0, period1.compareTo(period2));
        assertEquals(0, period3.compareTo(period4));
        assertTrue(period1.compareTo(period3) < 0);
        assertTrue(period3.compareTo(period1) > 0);
        assertTrue(period2.compareTo(period4) < 0);
        assertTrue(period4.compareTo(period2) > 0);
    }

    @Test
    public void testAlternativeSyntax() throws Exception
    {
        assertEquals("The short alternative syntax is not accepted correctly",
                new Period("P3Y6M4DT12H30M5S"), new Period("P00030604T123005"));

        assertEquals("The extended alternative syntax is not accepted correctly",
                new Period("P3Y6M4DT12H30M5S"), new Period("P0003-06-04T12:30:05"));
    }
}
