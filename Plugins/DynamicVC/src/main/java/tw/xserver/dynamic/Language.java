package tw.xserver.dynamic;

import tw.xserver.loader.lang.ExampleType.BasicCommand;
import tw.xserver.loader.lang.ExampleType.Command_t;
import tw.xserver.loader.lang.LocaleData;

public class Language {
    final Register_t register = new Register_t();
    final RunTime_t runtime = new RunTime_t();

    static class Register_t extends BasicCommand {
        final SubCommand_t subcommand = new SubCommand_t();

        static class SubCommand_t {
            final Create_t create = new Create_t();
            final Remove_t remove = new Remove_t();

            static class Create_t extends BasicCommand {
                final Options_t options = new Options_t();

            }

            static class Remove_t extends BasicCommand {
                final Options_t options = new Options_t();
            }

            static class Options_t {
                final Command_t detect = new Command_t();
            }
        }
    }

    static class RunTime_t {
        final Successes_t successes = new Successes_t();
        final Errors_t errors = new Errors_t();

        static class Successes_t {
            LocaleData done;
            LocaleData remove_success;
            LocaleData no_remove_success;
        }

        static class Errors_t {
            LocaleData unknown;
        }
    }
}
