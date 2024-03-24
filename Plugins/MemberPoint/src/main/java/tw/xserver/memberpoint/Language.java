package tw.xserver.memberpoint;

import tw.xserver.loader.lang.ExampleType;
import tw.xserver.loader.lang.LocaleData;

import static tw.xserver.loader.lang.ExampleType.BasicCommand;
import static tw.xserver.loader.lang.ExampleType.Command_t;

public class Language {
    final Register_t register = new Register_t();
    final RunTime_t runtime = new RunTime_t();

    static class Register_t {
        Command_t refresh = new Command_t();
        final CommandWithMember_t get_point = new CommandWithMember_t();
        final CommandWithMemberValue_t add_point = new CommandWithMemberValue_t();
        final CommandWithMemberValue_t remove_point = new CommandWithMemberValue_t();
        final CommandWithMemberValue_t set_point = new CommandWithMemberValue_t();


        static class CommandWithMember_t extends BasicCommand {
            final Options_t options = new Options_t();

            static class Options_t {
                final ExampleType.Member_t member = new ExampleType.Member_t();
            }
        }

        static class CommandWithMemberValue_t extends BasicCommand {
            final Options_t options = new Options_t();

            static class Options_t {
                final ExampleType.Member_t member = new ExampleType.Member_t();
                final ExampleType.Integer_t value = new ExampleType.Integer_t();
            }
        }
    }

    static class RunTime_t {
        final Successes_t successes = new Successes_t();
        final Errors_t errors = new Errors_t();

        static class Successes_t {
            LocaleData current_point;
            LocaleData refresh_success;
        }

        static class Errors_t {
            LocaleData wrong_guild;
            LocaleData no_permission;
        }
    }
}
