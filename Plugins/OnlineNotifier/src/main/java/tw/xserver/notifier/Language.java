package tw.xserver.notifier;

import tw.xserver.loader.lang.ExampleType.BasicCommand;
import tw.xserver.loader.lang.ExampleType.Boolean_t;
import tw.xserver.loader.lang.ExampleType.Member_t;
import tw.xserver.loader.lang.LocaleData;

public class Language {
    final Register_t register = new Register_t();
    final RunTime_t runtime = new RunTime_t();

    static class Register_t extends BasicCommand {
        final Options_t options = new Options_t();

        static class Options_t {
            final Member_t member = new Member_t();
            final Boolean_t infinity = new Boolean_t();
        }
    }

    static class RunTime_t {
        final Successes_t successes = new Successes_t();
        final Errors_t errors = new Errors_t();

        static class Successes_t {
            LocaleData test;
            LocaleData done;
            LocaleData show_up;
        }

        static class Errors_t {
            LocaleData no_dm_able;
            LocaleData already_online;
        }
    }
}
