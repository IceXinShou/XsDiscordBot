package tw.xserver.ticket;

import tw.xserver.loader.lang.ExampleType.Command_t;

public class Language {
    final Register_t register = new Register_t();

    static class Register_t {
        final Command_t create = new Command_t();
        final Command_t add = new Command_t();
    }
}
