package com.anwar1909.bgloc.headless;

import android.content.Context;

public abstract class AbstractTaskRunner implements TaskRunner {
    protected Context mContext;

    public AbstractTaskRunner() {}

    public abstract void runTask(Task task);

    public void setContext(Context context) {
        mContext = context;
    }
}
