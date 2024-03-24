package tw.xserver.officialguild;

import tw.xserver.loader.lang.LocaleData;

public class Language {
    final RunTime_t runtime = new RunTime_t();

    static class RunTime_t {
        final Steps_t steps = new Steps_t();
        final Errors_t errors = new Errors_t();

        static class Steps_t {
            LocaleData updating;
            final Chi_t chi = new Chi_t();
            final Eng_t eng = new Eng_t();


            static class Chi_t {
                LocaleData label;
                LocaleData placeholder;
                LocaleData title;
            }

            static class Eng_t {
                LocaleData org_label;
                LocaleData org_placeholder;
                LocaleData mc_label;
                LocaleData mc_placeholder;
                LocaleData title;
            }
        }

        static class Errors_t {
            LocaleData wrong_type_chi;
            LocaleData cannot_found_mc_acc;
        }

    }
}
