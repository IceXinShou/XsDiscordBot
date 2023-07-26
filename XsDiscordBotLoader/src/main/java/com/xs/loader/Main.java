package com.xs.loader;

import asg.cliche.ShellFactory;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;

public class Main {
    public static Loader loader;

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        try {
            loader = new Loader(args);
            ShellFactory.createConsoleShell("", null, new CLICommands()).commandLoop();
        } catch (IOException e) {
            e.printStackTrace();
        }
        AnsiConsole.systemUninstall();
    }
}