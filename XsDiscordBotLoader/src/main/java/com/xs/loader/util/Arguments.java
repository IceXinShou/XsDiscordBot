package com.xs.loader.util;

import org.apache.commons.cli.*;

public class Arguments{
    public final boolean ignore_version_check;

    public Arguments(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("h", "help", false, "show help");
        options.addOption("ign", "ignore-version-check", false, "ignore version check from github");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("ApacheCommonsCLIWithTerminalInput", options);
            System.exit(0);
        }

        ignore_version_check = cmd.hasOption("ign");
    }
}
