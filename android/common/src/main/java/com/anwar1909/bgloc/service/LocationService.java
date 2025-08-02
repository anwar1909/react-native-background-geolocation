package com.anwar1909.bgloc.service;

import com.anwar1909.bgloc.Config;
import com.anwar1909.bgloc.Setting;

public interface LocationService {
    void start();
    void startForegroundService();
    void stop();
    void startForeground();
    void stopForeground();
    void setting(Setting setting);
    void configure(Config config);
    void registerHeadlessTask(String jsFunction);
    void startHeadlessTask();
    void stopHeadlessTask();
    void executeProviderCommand(int command, int arg);
}
