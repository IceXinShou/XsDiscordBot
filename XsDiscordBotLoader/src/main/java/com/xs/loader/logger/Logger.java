package com.xs.loader.logger;

public class Logger {
    public final String TAG, ERRTAG;

    public Logger(final String TAG) {
        this.TAG = Color.GREEN + '[' + Color.RESET + TAG + Color.GREEN + ']' + Color.RESET + ' ';
        this.ERRTAG = Color.RED + '[' + Color.RESET + TAG + Color.RED + ']' + Color.RESET + ' ';
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
