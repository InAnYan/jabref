package org.jabref.gui.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionsUtil {
    private ExceptionsUtil() {
        throw new UnsupportedOperationException("cannot instantiate utility class");
    }

    public static String generateExceptionMessage(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
