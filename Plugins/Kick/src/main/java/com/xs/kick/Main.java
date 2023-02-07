package com.xs.kick;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.Map;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static net.dv8tion.jda.api.Permission.KICK_MEMBERS;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

public class Main extends PluginEvent {
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "Kick";
    private final String PATH_FOLDER_NAME = "./plugins/Kick";
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
        LangGetter langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, this.getClass());

        // expert files
        langGetter.exportDefaultLang();
        lang = langGetter.readLangFileData();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("kick", "kick a member from your server")
                        .setNameLocalizations(lang.get("register;cmd"))
                        .setDescriptionLocalizations(lang.get("register;description"))
                        .addOptions(
                                new OptionData(USER, "user", "user", true)
                                        .setDescriptionLocalizations(lang.get("register;options;user")),
                                new OptionData(STRING, "reason", "reason")
                                        .setDescriptionLocalizations(lang.get("register;options;reason")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(KICK_MEMBERS))
        };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("kick")) return;
        if (!event.getMember().hasPermission(KICK_MEMBERS)) {
            event.getHook().editOriginalEmbeds(createEmbed("你沒有權限", 0xFF0000)).queue();
            return;
        }

        DiscordLocale local = event.getUserLocale();
        Member selfMember = event.getGuild().getSelfMember();
        Member member = event.getOption("user").getAsMember();

        if (member == null) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_user").get(local), 0xFF0000)).queue();
            return;
        }

        String reason = event.getOption("reason") == null ? "null" : event.getOption("reason").getAsString();

        if (!selfMember.hasPermission(KICK_MEMBERS)) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
            return;
        }

        if (!selfMember.canInteract(member)) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;permission_denied").get(local), 0xFF0000)).queue();
            return;
        }

        event.getGuild().kick(member).reason(reason).queue(
                success -> {
                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;success").get(local) + ' ' + member.getEffectiveName(), 0xffd2c5)).queue();
                },
                error -> {
                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;unknown").get(local), 0xFF0000)).queue();
                }
        );
    }

}