package tw.xserver.checkin;

import tw.xserver.loader.lang.ExampleType.BasicCommand;
import tw.xserver.loader.lang.ExampleType.Command_t;
import tw.xserver.loader.lang.LocaleData;

public class Language {
    final Register_t register = new Register_t();
    final RunTime_t runtime = new RunTime_t();

    static class Register_t {
        final Announcement_t announcement = new Announcement_t();
        final Check_t check = new Check_t();

        static class Announcement_t extends BasicCommand {
            final Options_t options = new Options_t();

            static class Options_t extends BasicCommand {
                final Command_t id = new Command_t();
            }
        }

        static class Check_t extends BasicCommand {
            final Options_t options = new Options_t();

            static class Options_t {
                Command_t id = new Command_t();
                final Command_t content = new Command_t();
            }
        }
    }

    static class RunTime_t {
        final Successes_t successes = new Successes_t();
        final Errors_t errors = new Errors_t();

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
