package com.applitools.eyes.images;

public class TestUtils {
    public final static boolean runOnCI = System.getenv("CI") != null;
    public final static boolean verboseLogs = !runOnCI || "true".equalsIgnoreCase(System.getenv("APPLITOOLS_VERBOSE_LOGS"));
}
