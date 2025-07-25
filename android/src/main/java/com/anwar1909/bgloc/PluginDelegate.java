package com.anwar1909.bgloc;

import com.anwar1909.bgloc.data.BackgroundActivity;
import com.anwar1909.bgloc.data.BackgroundLocation;

/**
 * Created by finch on 27.11.2017.
 */

public interface PluginDelegate {
    void onAuthorizationChanged(int authStatus);
    void onLocationChanged(BackgroundLocation location);
    void onStationaryChanged(BackgroundLocation location);
    void onActivityChanged(BackgroundActivity activity);
    void onServiceStatusChanged(int status);
    void onAbortRequested();
    void onHttpAuthorization();
    void onError(PluginException error);
}
