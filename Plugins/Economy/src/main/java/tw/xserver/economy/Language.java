package tw.xserver.economy;

import tw.xserver.loader.lang.ExampleType.BasicCommand;
import tw.xserver.loader.lang.ExampleType.Command_t;
import tw.xserver.loader.lang.ExampleType.Integer_t;
import tw.xserver.loader.lang.ExampleType.Member_t;
import tw.xserver.loader.lang.LocaleData;

public class Language {
    Register_t register = new Register_t();
    RunTime_t runtime = new RunTime_t();

    static class Register_t {
        CommandWithMember_t get_money = new CommandWithMember_t();
        CommandWithMember_t get_money_history = new CommandWithMember_t();
        CommandWithMemberValue_t add_money = new CommandWithMemberValue_t();
        CommandWithMemberValue_t remove_money = new CommandWithMemberValue_t();
        CommandWithMemberValue_t set_money = new CommandWithMemberValue_t();
        CommandWithMemberValue_t add_money_history = new CommandWithMemberValue_t();
        CommandWithMemberValue_t remove_money_history = new CommandWithMemberValue_t();
        CommandWithMemberValue_t set_money_history = new CommandWithMemberValue_t();
        Command_t money_board = new Command_t();
        Command_t money_history_board = new Command_t();

        static class CommandWithMember_t extends BasicCommand {
            Options_t options = new Options_t();

            static class Options_t {
                Member_t member = new Member_t();
            }
        }

        static class CommandWithMemberValue_t extends BasicCommand {
            Options_t options = new Options_t();

            static class Options_t {
                Member_t member = new Member_t();
                Integer_t value = new Integer_t();
            }
        }

    }

    static class RunTime_t {
        Successes_t successes = new Successes_t();
        Errors_t errors = new Errors_t();

        static class Successes_t {
            LocaleData money_board_title;
            LocaleData money_history_board_title;
            LocaleData current_money;
            LocaleData current_money_history;
        }

        static class Errors_t {
            LocaleData no_permission;
        }
    }
}
