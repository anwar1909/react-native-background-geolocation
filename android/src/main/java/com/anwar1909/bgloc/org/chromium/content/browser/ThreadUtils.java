// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.anwar1909.bgloc.org.chromium.content.browser;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Helper methods to deal with threading related tasks.
 */
public class ThreadUtils {

    /**
     * Run the supplied Runnable on the main thread. The method will block until
     * the Runnable completes.
     *
     * @param r The Runnable to run.
     */
    public static void runOnUiThreadBlocking(final Runnable r) {
        if (runningOnUiThread()) {
            r.run();
        } else {
            FutureTask<Void> task = new FutureTask<Void>(r, null);
            postOnUiThread(task);
            try {
                task.get();
            } catch (Exception e) {
                throw new RuntimeException("Exception occured while waiting for runnable", e);
            }
        }
    }

    /**
     * Run the supplied Callable on the main thread, wrapping any exceptions in
     * a RuntimeException. The method will block until the Callable completes.
     *
     * @param c The Callable to run
     * @return The result of the callable
     */
    public static <T> T runOnUiThreadBlockingNoException(Callable<T> c) {
        try {
            return runOnUiThreadBlocking(c);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error occured waiting for callable", e);
        }
    }

    /**
     * Run the supplied Callable on the main thread, The method will block until
     * the Callable completes.
     *
     * @param c The Callable to run
     * @return The result of the callable
     * @throws ExecutionException c's exception
     */
    public static <T> T runOnUiThreadBlocking(Callable<T> c) throws ExecutionException {
        FutureTask<T> task = new FutureTask<T>(c);
        runOnUiThread(task);
        try {
            return task.get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted waiting for callable", e);
        }
    }

    /**
     * Run the supplied FutureTask on the main thread. The method will block
     * only if the current thread is the main thread.
     *
     * @param task The FutureTask to run
     * @return The queried task (to aid inline construction)
     */
    public static <T> FutureTask<T> runOnUiThread(FutureTask<T> task) {
        if (runningOnUiThread()) {
            task.run();
        } else {
            postOnUiThread(task);
        }
        return task;
    }

    /**
     * Run the supplied Callable on the main thread. The method will block
     * only if the current thread is the main thread.
     *
     * @param c The Callable to run
     * @return A FutureTask wrapping the callable to retrieve results
     */
    public static <T> FutureTask<T> runOnUiThread(Callable<T> c) {
        return runOnUiThread(new FutureTask<T>(c));
    }

    /**
     * Run the supplied Runnable on the main thread. The method will block
     * only if the current thread is the main thread.
     *
     * @param r The Runnable to run
     */
    public static void runOnUiThread(Runnable r) {
        runOnUiThread(new FutureTask<Void>(r, null));
    }

    /**
     * Post the supplied FutureTask to run on the main thread. The method will
     * not block, even if called on the UI thread.
     *
     * @param task The FutureTask to run
     * @return The queried task (to aid inline construction)
     */
    public static <T> FutureTask<T> postOnUiThread(FutureTask<T> task) {
        new Handler(Looper.getMainLooper()).post(task);
        return task;
    }

    /**
     * Asserts that the current thread is running on the main thread.
     */
    public static void assertOnUiThread() {
        assert runningOnUiThread();
    }

    /**
     * @return true iff the current thread is the main (UI) thread.
     */
    public static boolean runningOnUiThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }
}