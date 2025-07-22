package com.marianhello.bgloc;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PermissionManager {
    private static final int REQUEST_CODE = 12345;
    private static PermissionManager instance;
    private final Context context;

    private PermissionManager(Context context) {
        this.context = context;
    }

    public static PermissionManager getInstance(Context context) {
        if (instance == null) {
            instance = new PermissionManager(context);
        }
        return instance;
    }

    public CompletableFuture<Boolean> checkPermissions(List<String> permissions) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            future.complete(true);
        } else {
            ActivityCompat.requestPermissions((Activity) context, permissions.toArray(new String[0]), REQUEST_CODE);
            // ⚠️ Tambahan: kamu butuh callback untuk handle hasil request permission di activity
            future.complete(false); // Untuk sementara dianggap gagal
        }

        return future;
    }
}
