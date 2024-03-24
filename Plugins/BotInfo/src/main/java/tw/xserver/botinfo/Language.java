package tw.xserver.botinfo;

import tw.xserver.loader.lang.LocaleData;

import static tw.xserver.loader.lang.ExampleType.Command_t;

public class Language {
    final Command_t register = new Command_t();
    final RunTime_t runtime = new RunTime_t();

    static class RunTime_t {
        final Successes_t successes = new Successes_t();

        static class Successes_t {
            LocaleData title;
            LocaleData guild_count;
            LocaleData member_count;
        }
    }
}
