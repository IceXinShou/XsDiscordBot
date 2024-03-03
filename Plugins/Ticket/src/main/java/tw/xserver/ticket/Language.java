package tw.xserver.ticket;

import tw.xserver.loader.lang.ExampleType.Command_t;

public class Language {
    Register_t register = new Register_t();

    static class Register_t {
        Command_t create = new Command_t();
        Command_t add = new Command_t();
    }
}
