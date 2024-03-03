package tw.xserver.loader.lang;

import net.dv8tion.jda.api.interactions.DiscordLocale;

import javax.annotation.Nonnull;
import java.util.HashMap;

public class LocaleData extends HashMap<DiscordLocale, String> {
    private DiscordLocale defaultLocale;

    public void setDefaultLocale(@Nonnull DiscordLocale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    @Override
    public String get(Object key) {
        String ret = super.get(key);
        if (ret != null) return ret;

        return this.get(defaultLocale);
    }
}
