package com.xs.botinfo;

import com.xs.loader.PluginEvent;
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
import java.util.Map;

import static com.xs.loader.util.EmbedCreator.createEmbed;

public class Main extends PluginEvent {

    private JSONObject config;
    private Map<String, String> lang;
    private final String[] LANG_DEFAULT = {"en_US", "zh_TW"};
    private final String[] LANG_PARAMETERS_DEFAULT = {
            "REGISTER_NAME", "GUILD_TOTAL_COUNT", "MEMBER_TOTAL_COUNT", "INFORMATION"
    };

    FileGetter getter;
    Logger logger;
    final String TAG = "BotInfo";
    final String PATH_FOLDER_NAME = "BotInfo";

    @Override
    public void initLoad() {
        super.initLoad();
        getter = new FileGetter(TAG, PATH_FOLDER_NAME, Main.class.getClassLoader());
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
    public CommandData[] globalCommands() {
        return new CommandData[]{
                new CommandDataImpl("botinfo", lang.get("REGISTER_NAME"))
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
        // expert files
        getter.exportLang(LANG_DEFAULT, LANG_PARAMETERS_DEFAULT);
        lang = getter.getLangFileData(config.getString("Lang"));
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("botinfo")) return;

        int members = 0;
        for (int i = 0; i < event.getJDA().getGuilds().size(); i++)
            members = members + event.getJDA().getGuilds().get(i).getMemberCount();

        List<MessageEmbed.Field> fields = new ArrayList<>();
        if (event.getGuild() != null) {
            fields.add(new MessageEmbed.Field(lang.get("GUILD_TOTAL_COUNT"), String.valueOf((long) event.getJDA().getGuilds().size()), false));
            fields.add(new MessageEmbed.Field(lang.get("MEMBER_TOTAL_COUNT"), String.valueOf(members), false));
            event.getHook().editOriginalEmbeds(createEmbed(lang.get("INFORMATION"), "", "", "", "", fields, OffsetDateTime.now(), 0x00FFFF)).queue();
        } else {
            fields.add(new MessageEmbed.Field("Guild Count ", String.valueOf((long) event.getJDA().getGuilds().size()), false));
            fields.add(new MessageEmbed.Field("Member Count ", String.valueOf(members), false));
            event.getHook().editOriginalEmbeds(createEmbed("Bot Info", "", "", "", "", fields, OffsetDateTime.now(), 0x00FFFF)).queue();
        }
    }
}