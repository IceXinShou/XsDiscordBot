package com.xs.loader.logger;

public class Logger {
    public final String TAG, ERRTAG;

    public Logger(final String TAG) {
        this.TAG = Color.RESET + '[' + Color.GREEN + TAG + Color.RESET + ']' + ' ';
        this.ERRTAG = Color.RESET + '[' + Color.RED + TAG + Color.RESET + ']' + ' ';
    }

    public void log(final String msg) {
        System.out.println(TAG + msg);
    }

    public void print(final String msg) {
        System.out.print(TAG + msg);
    }

    public void error(final String msg) {
        System.err.println(TAG + msg);
    }

    public void printErr(final String msg) {
        System.err.print(TAG + msg);
    }
}
