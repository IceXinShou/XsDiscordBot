package tw.xserver.poll;

import tw.xserver.loader.lang.ExampleType.BasicCommand;
import tw.xserver.loader.lang.ExampleType.Command_t;
import tw.xserver.loader.lang.LocaleData;

public class Language {
    Register_t register = new Register_t();
    Embed_t embed = new Embed_t();
    RunTime_t runtime = new RunTime_t();

    static class Register_t extends BasicCommand {
        Options_t options = new Options_t();

        static class Options_t {
            Command_t question = new Command_t();
            Command_t a = new Command_t();
            Command_t b = new Command_t();
            Command_t c = new Command_t();
            Command_t d = new Command_t();
            Command_t e = new Command_t();
            Command_t f = new Command_t();
            Command_t g = new Command_t();
            Command_t h = new Command_t();
            Command_t i = new Command_t();
            Command_t j = new Command_t();
        }
    }

    static class Embed_t {
        LocaleData footer;
    }

    static class RunTime_t {
        LocaleData success;
    }
}
