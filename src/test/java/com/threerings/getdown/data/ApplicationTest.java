package com.threerings.getdown.data;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ApplicationTest {
   @Test
   public void testParseJavaVersionValidValues() throws IOException {
      assertEquals(1060045, Configuration.parseJavaVersion("1.6.0_45", ""));
      assertEquals(1060045, Configuration.parseJavaVersion("1060045", ""));
   }

   @Test
   public void testParseJavaVersionInvalidIDs() throws IOException {
      try {
         Configuration.parseJavaVersion("100000", "m.invalid.version");
      } catch (IOException e) {
         assertEquals("m.invalid.version|~100000", e.getMessage());
      }

      try {
         Configuration.parseJavaVersion("1000000", "m.invalid.version");
      } catch (IOException e) {
         assertEquals("m.invalid.version|~1000000", e.getMessage());
      }


      try {
         Configuration.parseJavaVersion("-100000", "m.invalid.version");
      } catch (IOException e) {
         assertEquals("m.invalid.version|~-100000", e.getMessage());
      }
   }
}