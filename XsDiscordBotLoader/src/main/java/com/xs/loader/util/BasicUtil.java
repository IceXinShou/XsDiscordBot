package com.xs.loader.util;

public class BasicUtil {
    public final String TAG;

    public BasicUtil(final String TAG) {
        this.TAG = TAG;
    }

    public void println(final String msg) {
        System.out.println(TAG + ' ' + msg);
    }

    public void print(final String msg) {
        System.out.print(TAG + ' ' + msg);
    }

    public void printErrln(final String msg) {
        System.err.println(TAG + ' ' + msg);
    }

    public void printErr(final String msg) {
        System.err.print(TAG + ' ' + msg);
    }
}
