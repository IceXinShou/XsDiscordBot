package tw.xserver.checkin;

import tw.xserver.loader.lang.ExampleType.BasicCommand;
import tw.xserver.loader.lang.ExampleType.Command_t;
import tw.xserver.loader.lang.LocaleData;

public class Language {
    Register_t register = new Register_t();
    RunTime_t runtime = new RunTime_t();

    static class Register_t {
        Announcement_t announcement = new Announcement_t();
        Check_t check = new Check_t();

        static class Announcement_t extends BasicCommand {
            Options_t options = new Options_t();

            static class Options_t extends BasicCommand {
                Command_t id = new Command_t();
            }
        }

        static class Check_t extends BasicCommand {
            Options_t options = new Options_t();

            static class Options_t {
                Command_t id = new Command_t();
                Command_t content = new Command_t();
            }
        }
    }

    static class RunTime_t {
        Successes_t successes = new Successes_t();
        Errors_t errors = new Errors_t();

        static class Successes_t {
            LocaleData announce_success;
            LocaleData checkin_success;
        }

        static class Errors_t {
            LocaleData wrong_guild;
            LocaleData already_announced;
            LocaleData wrong_id_input;
            LocaleData check_message_get_failed;
            LocaleData not_announced_message;
            LocaleData no_permission;
        }
    }
}
