package tw.xserver.poll;

import tw.xserver.loader.lang.ExampleType.BasicCommand;
import tw.xserver.loader.lang.ExampleType.Command_t;
import tw.xserver.loader.lang.LocaleData;

public class Language {
    final Register_t register = new Register_t();
    final Embed_t embed = new Embed_t();
    final RunTime_t runtime = new RunTime_t();

    static class Register_t extends BasicCommand {
        final Options_t options = new Options_t();

        static class Options_t {
            final Command_t question = new Command_t();
            final Command_t a = new Command_t();
            final Command_t b = new Command_t();
            final Command_t c = new Command_t();
            final Command_t d = new Command_t();
            final Command_t e = new Command_t();
            final Command_t f = new Command_t();
            final Command_t g = new Command_t();
            final Command_t h = new Command_t();
            final Command_t i = new Command_t();
            final Command_t j = new Command_t();
        }
    }

    static class Embed_t {
        LocaleData footer;
    }

    static class RunTime_t {
        LocaleData success;
    }
}
