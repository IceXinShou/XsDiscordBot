package com.xs.kick;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.PermissionERROR.permissionCheck;
import static com.xs.loader.util.SlashCommandOption.REASON;
import static com.xs.loader.util.SlashCommandOption.USER_TAG;
import static net.dv8tion.jda.api.Permission.KICK_MEMBERS;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

public class Main extends PluginEvent {

    private JSONObject lang_register;
    private JSONObject lang_register_options;
    private JSONObject lang_runtime;
    private JSONObject lang_runtime_errors;
    private final String[] LANG_DEFAULT = {"en_US", "zh_TW"};

    private FileGetter getter;
    private Logger logger;
    private LangGetter langGetter;
    private static final String TAG = "Kick";
    private static final String VERSION = "1.0";
    private final String PATH_FOLDER_NAME = "plugins/Kick";

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
                new CommandDataImpl(lang_register.getString("cmd"), lang_register.getString("description")).addOptions(
                        new OptionData(USER, USER_TAG, lang_register_options.getString("user"), true),
                        new OptionData(STRING, REASON, lang_register_options.getString("reason"))
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
        langGetter.exportDefaultLang();
        JSONObject lang = langGetter.getLangFileData();
        lang_register = lang.getJSONObject("register");
        lang_register_options = lang_register.getJSONObject("options");
        lang_runtime = lang.getJSONObject("runtime");
        lang_runtime_errors = lang_runtime.getJSONObject("errors");

    }


    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals(lang_register.getString("cmd"))) return;
        if (!permissionCheck(KICK_MEMBERS, event))
            return;

        Member selfMember = event.getGuild().getSelfMember();
        Member member = event.getOption(USER_TAG).getAsMember();
        String reason = event.getOption(REASON) == null ? "null" : event.getOption(REASON).getAsString();

        if (!selfMember.hasPermission(KICK_MEMBERS)) {
            event.getHook().editOriginalEmbeds(createEmbed(lang_runtime_errors.getString("no_permission"), 0xFF0000)).queue();
            return;
        }

        if (!selfMember.canInteract(member)) {
            event.getHook().editOriginalEmbeds(createEmbed(lang_runtime_errors.getString("permission_denied"), 0xFF0000)).queue();
            return;
        }

        event.getGuild().kick(member).reason(reason).queue(
                success -> {
                    event.getHook().editOriginalEmbeds(createEmbed(lang_runtime.getString("success") + ' ' + member.getEffectiveName(), 0xffd2c5)).queue();
                },
                error -> {
                    event.getHook().editOriginalEmbeds(createEmbed(lang_runtime_errors.getString("unknown"), 0xFF0000)).queue();
                }
        );
    }

}