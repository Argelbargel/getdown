package com.threerings.getdown.data;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class ResourceTest {

   @Test
   public void testIsValidArchive() throws Exception {
      assertTrue(Resource.isValidArchive(new File("C:\test.jar")));
      assertTrue(Resource.isValidArchive(new File("C:\test.zip")));
      assertFalse(Resource.isValidArchive(new File("C:\test.jar.txt")));
   }
}