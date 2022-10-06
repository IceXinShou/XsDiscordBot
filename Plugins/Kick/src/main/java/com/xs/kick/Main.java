package com.xs.kick;

import com.xs.loader.PluginEvent;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.PermissionERROR.permissionCheck;
import static com.xs.loader.util.SlashCommandOption.REASON;
import static com.xs.loader.util.SlashCommandOption.USER_TAG;
import static net.dv8tion.jda.api.Permission.KICK_MEMBERS;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

public class Main extends PluginEvent {

    public static JSONObject config;
    public static Map<String, String> lang = new HashMap<>();
    private final String[] LANG_DEFAULT = {"en_US", "zh_TW"};
    private final String[] LANG_PARAMETERS_DEFAULT = {
            "REGISTER_NAME", "REGISTER_OPTION_MEMBER_YOU_CHOOSE",
            "REGISTER_OPTION_REASON", "NO_PERMISSION", "PERMISSION_DENIED", "SUCCESS", "UNKNOWN_ERROR"
    };
    FileGetter getter;
    Logger logger;
    final String TAG = "Kick";
    final String PATH_FOLDER_NAME = "Kick";

    @Override
    public void initLoad() {
        super.initLoad();
        getter = new FileGetter(TAG, PATH_FOLDER_NAME, LANG_DEFAULT, LANG_PARAMETERS_DEFAULT, Main.class.getClassLoader());
        logger = new Logger(TAG);
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
                new CommandDataImpl("kick", lang.get("REGISTER_NAME")).addOptions(
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_MEMBER_YOU_CHOOSE"), true),
                        new OptionData(STRING, REASON, lang.get("REGISTER_OPTION_REASON"))
                )
        };
    }

    @Override
    public void loadConfigFile() {
        config = new JSONObject(getter.readYml("config.yml", "plugins/" + PATH_FOLDER_NAME));
        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void loadVariables() {

    }

    @Override
    public void loadLang() {

        getter.exportDefaultLang();;
        lang = getter.getLangFileData(config.getString("Lang"));
    }


    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("kick")) return;
        if (!permissionCheck(KICK_MEMBERS, event))
            return;

        Member selfMember = event.getGuild().getSelfMember();
        Member member = event.getOption(USER_TAG).getAsMember();
        String reason = event.getOption(REASON) == null ? "null" : event.getOption(REASON).getAsString();

        if (!selfMember.hasPermission(KICK_MEMBERS)) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.get("NO_PERMISSION"), 0xFF0000)).queue();
            return;
        }

        if (!selfMember.canInteract(member)) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.get("PERMISSION_DENIED"), 0xFF0000)).queue();
            return;
        }

        event.getGuild().kick(member).reason(reason).queue(
                success -> {
                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("SUCCESS") + ' ' + member.getEffectiveName(), 0xffd2c5)).queue();
                },
                error -> {
                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("UNKNOWN_ERROR"), 0xFF0000)).queue();
                }
        );
    }

}