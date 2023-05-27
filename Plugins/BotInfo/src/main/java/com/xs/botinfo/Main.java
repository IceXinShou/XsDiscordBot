package com.xs.botinfo;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.time.OffsetDateTime;
import java.util.Map;

public class Main extends PluginEvent {
    private LangManager langManager;
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};

    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "BotInfo";
    private final String PATH_FOLDER_NAME = "plugins/BotInfo";
    private Map<String, Map<DiscordLocale, String>> langMap; // Label, Local, Content

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {

        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        loadLang();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }

    @Override
    public void loadLang() {
        langManager = new LangManager(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, DiscordLocale.CHINESE_TAIWAN, this.getClass());
        langMap = langManager.readLangFileDataMap();
    }

    @Override
    public CommandData[] globalCommands() {
        return new SlashCommandData[]{
                Commands.slash("botinfo", "show about the bot data")
                        .setNameLocalizations(langMap.get("register;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
        };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("botinfo")) return;

        DiscordLocale local = event.getUserLocale();

        int members = 0;
        for (int i = 0; i < event.getJDA().getGuilds().size(); i++)
            members = members + event.getJDA().getGuilds().get(i).getMemberCount();

        EmbedBuilder builder = new EmbedBuilder();
        builder.addField(
                langManager.get("runtime;guild_count", local),
                String.valueOf((long) event.getJDA().getGuilds().size()), false
        );
        builder.addField(
                langManager.get("runtime;member_count", local),
                String.valueOf(members), false
        );

        event.getHook().editOriginalEmbeds(builder
                .setTitle(langManager.get("runtime;title", local))
                .setTimestamp(OffsetDateTime.now())
                .setColor(0x00FFFF)
                .build()
        ).queue();
    }
}