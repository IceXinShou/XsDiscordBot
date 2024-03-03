package tw.xserver.bank;

import tw.xserver.loader.lang.ExampleType.BasicCommand;
import tw.xserver.loader.lang.ExampleType.Integer_t;
import tw.xserver.loader.lang.ExampleType.Member_t;
import tw.xserver.loader.lang.LocaleData;

public class Language {
    Register_t register = new Register_t();
    RunTime_t runtime = new RunTime_t();

    static class Register_t {
        CommandWithMember_t check_balance = new CommandWithMember_t();
        CommandWithMemberValue_t add_money = new CommandWithMemberValue_t();
        CommandWithMemberValue_t remove_money = new CommandWithMemberValue_t();
        CommandWithMemberValue_t transfer_money = new CommandWithMemberValue_t();

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
            LocaleData add_success;
            LocaleData remove_success;
            LocaleData transfer_success;
            LocaleData check_balance_title;
            LocaleData check_balance_description;
            LocaleData transferring;
            LocaleData transfer_done;
        }

        static class Errors_t {
            LocaleData transfer_self;
            LocaleData no_such_money;
        }
    }
}
