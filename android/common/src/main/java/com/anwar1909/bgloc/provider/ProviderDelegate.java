package com.anwar1909.bgloc.provider;

import com.anwar1909.bgloc.PluginException;
import com.anwar1909.bgloc.data.BackgroundActivity;
import com.anwar1909.bgloc.data.BackgroundLocation;

public interface ProviderDelegate {
    void onLocation(BackgroundLocation location);
    void onStationary(BackgroundLocation location);
    void onActivity(BackgroundActivity activity);
    void onError(PluginException error);
}
