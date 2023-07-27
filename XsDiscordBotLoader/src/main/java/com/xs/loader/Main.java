package com.xs.loader;

import asg.cliche.ShellFactory;
import com.xs.loader.base.CLICommands;
import com.xs.loader.base.Loader;
import com.xs.loader.util.Arguments;
import org.apache.commons.cli.ParseException;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;

public class Main {
    public static Loader loader;
    public static Arguments arg;

    public static void main(String[] args) throws IOException, ParseException {
        AnsiConsole.systemInstall();

        arg = new Arguments(args);
        loader = new Loader();
        ShellFactory.createConsoleShell("", null, new CLICommands()).commandLoop();

        AnsiConsole.systemUninstall();
    }
}