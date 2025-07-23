package com.anwar1909.bgloc.service;

import com.anwar1909.bgloc.Config;

public interface LocationService {
    void start();
    void startForegroundService();
    void stop();
    void startForeground();
    void stopForeground();
    void configure(Config config);
    void registerHeadlessTask(String jsFunction);
    void startHeadlessTask();
    void stopHeadlessTask();
    void executeProviderCommand(int command, int arg);
}
