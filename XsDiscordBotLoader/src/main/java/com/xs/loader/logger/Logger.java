package com.xs.loader.logger;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

public class Logger {
    private final String TAG, ERRTAG;

    public Logger(final String TAG) {
        this.TAG = Color.RESET + '[' + Color.GREEN + TAG + Color.RESET + ']' + ' ';
        this.ERRTAG = Color.RESET + '[' + Color.RED + TAG + Color.RESET + ']' + ' ';
    }

    public void log(final String msg) {
        System.out.println('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + TAG + msg);
    }

    public void print(final String msg) {
        System.out.print('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + TAG + msg);
    }

    public void warn(final String msg) {
        System.err.println('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + TAG + msg);
    }

    public void printWarn(final String msg) {
        System.err.print('[' + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + "] " + TAG + msg);
    }
}
