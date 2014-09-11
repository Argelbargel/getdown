package com.threerings.getdown.data;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResourceTest {

   @Test
   public void testIsValidArchive() throws Exception {
      assertTrue(Resource.isValidArchive(new File("C:\test.jar")));
      assertTrue(Resource.isValidArchive(new File("C:\test.zip")));
      assertFalse(Resource.isValidArchive(new File("C:\test.jar.txt")));
   }
}