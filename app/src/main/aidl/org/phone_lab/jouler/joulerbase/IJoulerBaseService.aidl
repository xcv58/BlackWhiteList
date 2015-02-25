// IJoulerBaseService.aidl
package org.phone_lab.jouler.joulerbase;

// Declare any non-default types here with import statements

interface IJoulerBaseService {
    void test(String title, String text);
    boolean checkPermission();
    String getStatistics();
    void controlCpuMaxFrequency(int freq);
    int[] getAllCpuFrequencies();
    int getPriority(int uid);
    void resetPriority(int uid, int priority);
    void addRateLimitRule(int uid);
    void delRateLimitRule(int uid);
    void lowBrightness();
    void resetBrightness();
}
