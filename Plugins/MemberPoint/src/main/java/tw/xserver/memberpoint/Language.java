package tw.xserver.memberpoint;

import tw.xserver.loader.lang.ExampleType;
import tw.xserver.loader.lang.LocaleData;

import static tw.xserver.loader.lang.ExampleType.BasicCommand;
import static tw.xserver.loader.lang.ExampleType.Command_t;

public class Language {
    Register_t register = new Register_t();
    RunTime_t runtime = new RunTime_t();

    static class Register_t {
        Command_t refresh = new Command_t();
        CommandWithMember_t get_point = new CommandWithMember_t();
        CommandWithMemberValue_t add_point = new CommandWithMemberValue_t();
        CommandWithMemberValue_t remove_point = new CommandWithMemberValue_t();
        CommandWithMemberValue_t set_point = new CommandWithMemberValue_t();


        static class CommandWithMember_t extends BasicCommand {
            Options_t options = new Options_t();

            static class Options_t {
                ExampleType.Member_t member = new ExampleType.Member_t();
            }
        }

        static class CommandWithMemberValue_t extends BasicCommand {
            Options_t options = new Options_t();

            static class Options_t {
                ExampleType.Member_t member = new ExampleType.Member_t();
                ExampleType.Integer_t value = new ExampleType.Integer_t();
            }
        }
    }

    static class RunTime_t {
        Successes_t successes = new Successes_t();
        Errors_t errors = new Errors_t();

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
