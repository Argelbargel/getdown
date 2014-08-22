package com.threerings.getdown.util;

import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.assertEquals;

public class RuntimeVersionParserTest {
   @Test
   public void testParseJavaVersionValidValues() throws ParseException {
      assertEquals(1060045, new RuntimeVersionParser().parse("1.6.0_45"));
      assertEquals(1060045, new RuntimeVersionParser().parse("1060045"));
   }

   @Test(expected = ParseException.class)
   public void testParseJavaVersionInvalidIDTooShort() throws ParseException {
      new RuntimeVersionParser().parse("100000");
   }

   @Test(expected = ParseException.class)
   public void testParseJavaVersionInvalidIDTooLong() throws ParseException {
      new RuntimeVersionParser().parse("10000000");
   }

   @Test(expected = ParseException.class)
   public void testParseJavaVersionInvalidIDNegative() throws ParseException {
      new RuntimeVersionParser().parse("-100000");
   }
}