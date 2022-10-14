package com.xs.ban;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.PermissionERROR.permissionCheck;
import static com.xs.loader.util.SlashCommandOption.*;
import static net.dv8tion.jda.api.Permission.BAN_MEMBERS;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

public class Main extends PluginEvent {

    private JSONObject lang;
    private LangGetter langGetter;
    private final String[] LANG_DEFAULT = {"en_US", "zh_TW"};

    FileGetter getter;
    Logger logger;
    private static final String TAG = "Reurl";
    private static final String VERSION = "1.0";
    final String PATH_FOLDER_NAME = "plugins/Reurl";

    public Main() {
        super(TAG, VERSION);
    }

    @Override
    public void initLoad() {
        super.initLoad();
        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class.getClassLoader());
        loadConfigFile();
        loadVariables();
        loadLang();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        super.unload();
        logger.log("UnLoaded");
    }

    @Override
    public CommandData[] guildCommands() {
        return new CommandData[]{
                new CommandDataImpl("reurl", lang.get("REGISTER_NAME")).addOptions(
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_MEMBER_YOU_CHOOSE"), true)
                )
        };
    }

    @Override
    public void loadConfigFile() {
        JSONObject config = new JSONObject(getter.readYml("config.yml", PATH_FOLDER_NAME));
        langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, config.getString("Lang"));
        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void loadLang() {
        // expert files
        langGetter.exportDefaultLang();
        lang = langGetter.getLangFileData();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("ban")) return;
        if (!permissionCheck(BAN_MEMBERS, event))
            return;

        Member selfMember = event.getGuild().getSelfMember();
        Member member = event.getOption(USER_TAG).getAsMember();
        String reason = event.getOption(REASON) == null ? "null" : event.getOption(REASON).getAsString();

        if (!selfMember.hasPermission(BAN_MEMBERS)) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.get("NO_PERMISSION"), 0xFF0000)).queue();
            return;
        }

        if (!selfMember.canInteract(member)) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.get("PERMISSION_DENIED"), 0xFF0000)).queue();
            return;
        }

        int delDays = 0;
        OptionMapping option = event.getOption(DAYS);
        if (option != null)
            delDays = (int) Math.max(0, Math.min(7, option.getAsLong()));

        String userName = member.getEffectiveName();
        event.getGuild().ban(member, delDays, TimeUnit.DAYS).reason(reason).queue(
                success -> {
                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("SUCCESS") + ' ' + userName, 0xffb1b3)).queue();
                },
                error -> {
                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("UNKNOWN_ERROR"), 0xFF0000)).queue();
                }
        );
    }
}