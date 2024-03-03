package tw.xserver.general.commands;

import tw.xserver.loader.lang.ExampleType.BasicCommand;
import tw.xserver.loader.lang.ExampleType.Integer_t;
import tw.xserver.loader.lang.ExampleType.Member_t;
import tw.xserver.loader.lang.ExampleType.String_t;
import tw.xserver.loader.lang.LocaleData;

public class Language {
    public Ban_t ban = new Ban_t();
    public Kick_t kick = new Kick_t();

    public static class Ban_t {
        Register_t register = new Register_t();
        RunTime_t runtime = new RunTime_t();

        static class Register_t extends BasicCommand {
            Options_t options = new Options_t();

            static class Options_t {
                Member_t member = new Member_t();
                Integer_t day = new Integer_t();
                String_t reason = new String_t();
            }
        }

        static class RunTime_t {
            Successes_t successes = new Successes_t();
            Errors_t errors = new Errors_t();

            static class Successes_t {
                LocaleData done;
            }

            static class Errors_t {
                LocaleData no_user;
                LocaleData no_permission;
                LocaleData permission_denied;
                LocaleData unknown;
            }
        }
    }


    public static class Kick_t {
        Register_t register = new Register_t();
        RunTime_t runtime = new RunTime_t();

        static class Register_t extends BasicCommand {
            Options_t options = new Options_t();

            static class Options_t {
                Member_t member = new Member_t();
                String_t reason = new String_t();
            }
        }

        static class RunTime_t {
            Successes_t successes = new Successes_t();
            Errors_t errors = new Errors_t();

            static class Successes_t {
                LocaleData done;
            }

            static class Errors_t {
                LocaleData no_user;
                LocaleData no_permission;
                LocaleData permission_denied;
                LocaleData unknown;
            }
        }
    }
}
