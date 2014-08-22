package com.threerings.getdown.util;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuntimeVersionParser {
   private static final Pattern JAVA_HOME_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(_\\d+)");
   private static final Pattern JAVA_VERSION_ID_PATTERN = Pattern.compile("^\\d{7}$");

   public int parse(String value) throws ParseException {
      Matcher m = JAVA_VERSION_ID_PATTERN.matcher(value);
      if (m.find()) {
         return Integer.parseInt(value);
      }

      m = JAVA_HOME_PATTERN.matcher(value);
      if (!m.find()) {
         throw new ParseException("the given version-string does not adhere to the format MAJ.MIN.REV_PATCH", 0);
      }

      int major = Integer.parseInt(m.group(1));
      int minor = Integer.parseInt(m.group(2));
      int revis = Integer.parseInt(m.group(3));
      int patch = m.group(4) == null ? 0 : Integer.parseInt(m.group(4).substring(1));
      return patch + 100 * (revis + 100 * (minor + 100 * major));
   }

}
