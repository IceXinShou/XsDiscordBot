package com.xs.botinfo;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.xs.loader.util.EmbedCreator.createEmbed;

public class Main extends PluginEvent {

    private JSONObject lang_register;
    private JSONObject lang_runtime;

    private LangGetter langGetter;
    private final String[] LANG_DEFAULT = {"en_US", "zh_TW"};

    private FileGetter getter;
    private Logger logger;
    final String TAG = "BotInfo";
    final String PATH_FOLDER_NAME = "plugins/BotInfo";

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
    public CommandData[] globalCommands() {
        return new CommandData[]{
                new CommandDataImpl(lang_register.getString("cmd"), lang_register.getString("description"))
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
        JSONObject lang = langGetter.getLangFileData();
        lang_register = lang.getJSONObject("register");
        lang_runtime = lang.getJSONObject("runtime");
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals(lang_register.getString("cmd"))) return;

        int members = 0;
        for (int i = 0; i < event.getJDA().getGuilds().size(); i++)
            members = members + event.getJDA().getGuilds().get(i).getMemberCount();

        List<MessageEmbed.Field> fields = new ArrayList<>();
        if (event.getGuild() != null) {
            fields.add(new MessageEmbed.Field(lang_runtime.getString("guild_count"), String.valueOf((long) event.getJDA().getGuilds().size()), false));
            fields.add(new MessageEmbed.Field(lang_runtime.getString("member_count"), String.valueOf(members), false));
            event.getHook().editOriginalEmbeds(createEmbed(lang_runtime.getString("title"), "", "", "", "", fields, OffsetDateTime.now(), 0x00FFFF)).queue();
        } else {
            fields.add(new MessageEmbed.Field("Guild Count ", String.valueOf((long) event.getJDA().getGuilds().size()), false));
            fields.add(new MessageEmbed.Field("Member Count ", String.valueOf(members), false));
            event.getHook().editOriginalEmbeds(createEmbed("Bot Info", "", "", "", "", fields, OffsetDateTime.now(), 0x00FFFF)).queue();
        }
    }
}