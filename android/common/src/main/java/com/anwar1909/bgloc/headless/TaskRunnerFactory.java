package com.anwar1909.bgloc.headless;

public class TaskRunnerFactory {

    public TaskRunner getTaskRunner(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (TaskRunner) Class.forName(className).newInstance();
    }
}
