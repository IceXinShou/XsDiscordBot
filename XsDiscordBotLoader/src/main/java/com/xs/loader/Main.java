package com.xs.loader;

import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
//        for (String i : args) {
//            switch (i) {
//                case "-ignore-version-check": {
//                    ignore_version_check = true;
//                    break;
//                }
//            }
//        }

        AnsiConsole.systemInstall();
        try {
            new Loader(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
        AnsiConsole.systemUninstall();
    }
}