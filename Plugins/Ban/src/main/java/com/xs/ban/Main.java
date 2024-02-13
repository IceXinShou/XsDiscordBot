package com.xs.ban;

import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static net.dv8tion.jda.api.Permission.BAN_MEMBERS;
import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class Main extends Event {
    private static final String TAG = "Ban";
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private final String PATH_FOLDER_NAME = "plugins/Ban";
    private LangManager langManager;
    private FileGetter getter;
    private Logger logger;
    private Map<String, Map<DiscordLocale, String>> langMap; // Label, Local, Content

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {

        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        loadLang();
        logger.logln("Loaded");
    }

    @Override
    public void reload() {
        loadLang();
    }

    @Override
    public void unload() {
        logger.logln("UnLoaded");
    }

    @Override
    public void loadLang() {
        langManager = new LangManager(logger, getter, PATH_FOLDER_NAME, LANG_DEFAULT, DiscordLocale.CHINESE_TAIWAN);

        langMap = langManager.getMap();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("ban", "ban a member from your server")
                        .setNameLocalizations(langMap.get("register;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;description"))
                        .addOptions(
                                new OptionData(USER, "user", "user", true)
                                        .setDescriptionLocalizations(langMap.get("register;options;user")),
                                new OptionData(INTEGER, "days", "day")
                                        .setDescriptionLocalizations(langMap.get("register;options;day")),
                                new OptionData(STRING, "reason", "reason")
                                        .setDescriptionLocalizations(langMap.get("register;options;reason")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(BAN_MEMBERS))
        };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("ban")) return;
        if (!event.getMember().hasPermission(BAN_MEMBERS)) {
            event.getHook().editOriginalEmbeds(createEmbed("你沒有權限", 0xFF0000)).queue();
            return;
        }

        DiscordLocale local = event.getUserLocale();
        Member selfMember = event.getGuild().getSelfMember();
        Member member = event.getOption("user").getAsMember();

        if (member == null) {
            event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;no_user", local), 0xFF0000)).queue();
            return;
        }

        String reason = ((event.getOption("reason") == null) ? "null" : event.getOption("reason").getAsString());

        if (!selfMember.hasPermission(BAN_MEMBERS)) {
            event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;no_permission", local), 0xFF0000)).queue();
            return;
        }

        if (!selfMember.canInteract(member)) {
            event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;permission_denied", local), 0xFF0000)).queue();
            return;
        }

        int delDays = 0;
        OptionMapping option = event.getOption("days");
        if (option != null)
            delDays = (int) Math.max(0, Math.min(7, option.getAsLong()));

        String userName = member.getEffectiveName();
        event.getGuild().ban(member, delDays, TimeUnit.DAYS).reason(reason).queue(
                success -> event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;success", local) + ' ' + userName, 0xffb1b3)).queue(),
                error -> event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;unknown", local), 0xFF0000)).queue()
        );
    }
}