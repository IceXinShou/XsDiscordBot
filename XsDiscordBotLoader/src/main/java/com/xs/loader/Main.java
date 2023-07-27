package com.xs.loader;

import asg.cliche.ShellFactory;
import com.xs.loader.base.Loader;
import com.xs.loader.cli.RootCLI;
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

        while (true) {
            ShellFactory.createConsoleShell("$", "type `?l` to get help page", new RootCLI()).commandLoop();
            System.err.println('\n' +
                    "If you wanna stop the program, \n" +
                    "You can type `close` or `stop`\n" +
                    "Or the program may be broken...\n"
            );
        }
    }
}