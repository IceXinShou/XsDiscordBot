package tw.xserver.logger.chat;

import tw.xserver.loader.lang.LocaleData;

import static tw.xserver.loader.lang.ExampleType.BasicCommand;

public class Language {
    Register_t register = new Register_t();
    RunTime_t runtime = new RunTime_t();

    static class Register_t extends BasicCommand {
        SubCommand_t subcommand = new SubCommand_t();

        static class SubCommand_t {
            Setting_t setting = new Setting_t();

            static class Setting_t extends BasicCommand {
            }
        }
    }

    static class RunTime_t {
        Log_t log = new Log_t();
        Setting_t setting = new Setting_t();
        Errors_t errors = new Errors_t();

        static class Log_t {
            Update_t update = new Update_t();
            Delete_t delete = new Delete_t();

            static class Update_t {
                LocaleData before;
                LocaleData after;
                LocaleData footer;
            }

            static class Delete_t {
                LocaleData footer;
            }
        }

        static class Setting_t {
            LocaleData delete_success;
            Menu_t menu = new Menu_t();
            Embed_t embed = new Embed_t();
            Button_t button = new Button_t();

            static class Menu_t {
                LocaleData placeholder;
            }

            static class Embed_t {
                LocaleData channel_setting;
                LocaleData now_status;
                LocaleData white_list;
                LocaleData black_list;
                LocaleData white_channel;
                LocaleData black_channel;
                LocaleData empty;
            }

            static class Button_t {
                LocaleData toggle_status;
                LocaleData set_white;
                LocaleData set_black;
                LocaleData delete;
                LocaleData go_back;
            }
        }

        static class Errors_t {
            LocaleData unknown;
        }
    }
}
