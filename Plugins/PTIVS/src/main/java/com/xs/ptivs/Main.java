package com.xs.ptivs;

import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.Map;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;

public class Main extends Event {
    private static final String TAG = "PTIVS";
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private final String PATH_FOLDER_NAME = "./plugins/PTIVS";
    private FileGetter getter;
    private Logger logger;
    private Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content

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
        LangManager langManager = new LangManager(logger, getter, PATH_FOLDER_NAME, LANG_DEFAULT, DiscordLocale.CHINESE_TAIWAN);

        // expert files
        lang = langManager.getMap();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("ntihs", "commands about ")
                        .setNameLocalizations(lang.get("register;cmd"))
                        .setDescriptionLocalizations(lang.get("register;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        new SubcommandData("setting", "set chat log in this channel")
                                .setNameLocalizations(lang.get("register;subcommand;setting;cmd"))
                                .setDescriptionLocalizations(lang.get("register;subcommand;setting;description"))
                )
        };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("ntihs")) return;
        if (!event.getMember().hasPermission(ADMINISTRATOR)) {
            event.getHook().editOriginalEmbeds(createEmbed("你沒有權限", 0xFF0000)).queue();
            return;
        }

        DiscordLocale local = event.getUserLocale();
    }
}