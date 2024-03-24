package tw.xserver.logger.voice;

import tw.xserver.loader.lang.LocaleData;

import static tw.xserver.loader.lang.ExampleType.BasicCommand;

public class Language {
    final Register_t register = new Register_t();
    final RunTime_t runtime = new RunTime_t();

    static class Register_t extends BasicCommand {
        final SubCommand_t subcommand = new SubCommand_t();

        static class SubCommand_t {
            final Setting_t setting = new Setting_t();

            static class Setting_t extends BasicCommand {
            }
        }
    }

    static class RunTime_t {
        final Log_t log = new Log_t();
        final Setting_t setting = new Setting_t();
        Errors_t errors = new Errors_t();

        static class Log_t {
            final TitleFooter_t left = new TitleFooter_t();
            final TitleFooter_t join = new TitleFooter_t();
            final TitleFooter_t move = new TitleFooter_t();
            final TitleFooter_t status_remove = new TitleFooter_t();
            final TitleFooter_t status_add = new TitleFooter_t();
            final TitleFooter_t status_change = new TitleFooter_t();

            static class TitleFooter_t {
                LocaleData title;
                LocaleData footer;
            }
        }

        static class Setting_t {
            LocaleData delete_success;
            final Menu_t menu = new Menu_t();
            final Embed_t embed = new Embed_t();
            final Button_t button = new Button_t();

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
