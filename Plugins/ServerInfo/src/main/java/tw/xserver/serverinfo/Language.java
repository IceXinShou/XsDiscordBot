package tw.xserver.serverinfo;

import tw.xserver.loader.lang.LocaleData;

import static tw.xserver.loader.lang.ExampleType.Command_t;

public class Language {
    Command_t register = new Command_t();
    RunTime_t runtime = new RunTime_t();

    static class RunTime_t {
        LocaleData loading;
        LocaleData footer;
        LocaleData error;
        LocaleData no_permission;
        Fields_t fields = new Fields_t();

        static class Fields_t {
            LocaleData roles;
            LocaleData emoji;
            LocaleData sticker;
            LocaleData language;
            Members_t members = new Members_t();
            MembersStatus_t members_status = new MembersStatus_t();
            Channels_t channels = new Channels_t();
            ChannelsStatus_t channels_status = new ChannelsStatus_t();
            Boost_t boost = new Boost_t();

            static class Members_t {
                LocaleData title;
                LocaleData total;
                LocaleData human;
                LocaleData bot;
                LocaleData admin;
            }

            static class MembersStatus_t {
                LocaleData title;
                LocaleData online;
                LocaleData working;
                LocaleData idle;
                LocaleData offline;
            }

            static class Channels_t {
                LocaleData title;
                LocaleData total;
                LocaleData text;
                LocaleData voice;
                LocaleData stage;
            }

            static class ChannelsStatus_t {
                LocaleData title;
                LocaleData connect;
                LocaleData voice;
                LocaleData stage;
            }


            static class Boost_t {
                LocaleData title;
                LocaleData level;
                LocaleData amount;
            }
        }
    }
}
