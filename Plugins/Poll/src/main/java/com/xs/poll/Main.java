package com.xs.poll;

import com.wavjaby.json.Item;
import com.wavjaby.json.JsonArray;
import com.wavjaby.json.JsonObject;
import com.xs.loader.PluginEvent;
import com.xs.loader.util.BasicUtil;
import com.xs.loader.util.FileGetter;
import kotlin.Pair;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.xs.loader.MainLoader.jdaBot;
import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.PermissionERROR.permissionCheck;
import static com.xs.loader.util.SlashCommandOption.*;
import static com.xs.loader.util.Tag.getMemberNick;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class Main extends PluginEvent {
    public static Map<String, Object> config = new HashMap<>();
    public static Map<String, String> lang = new HashMap<>();
    private final String[] LANG_DEFAULT = {"en_US", "zh_TW"};
    private final String[] LANG_PARAMETERS_DEFAULT = {
            "REGISTER_NAME", "REGISTER_QUESTION"
            , "REGISTER_OPTION_A", "REGISTER_OPTION_B", "REGISTER_OPTION_C"
            , "REGISTER_OPTION_D", "REGISTER_OPTION_E", "REGISTER_OPTION_F"
            , "REGISTER_OPTION_G", "REGISTER_OPTION_H", "REGISTER_OPTION_I"
            , "REGISTER_OPTION_J", "FOOTER", "SUCCESS"
    };
    FileGetter getter;
    BasicUtil util;
    final String TAG = "[Poll]";
    final String PATH_FOLDER_NAME = "Poll";
    List<Emoji> votes = new ArrayList<>();

    @Override
    public void initLoad() {
        getter = new FileGetter(TAG, PATH_FOLDER_NAME, Main.class.getClassLoader());
        util = new BasicUtil(TAG);
        loadConfigFile();
        loadVariables();
        loadLang();
        util.println("Loaded");
    }

    @Override
    public void unload() {
        util.println("UnLoaded");
    }

    @Override
    public CommandData[] guildCommands() {
        return new CommandData[]{
                new CommandDataImpl("poll", lang.get("REGISTER_NAME")).addOptions(
                        new OptionData(STRING, QUESTION, lang.get("REGISTER_QUESTION"), true),
                        new OptionData(STRING, CHOICE_A, lang.get("REGISTER_OPTION_A"), true),
                        new OptionData(STRING, CHOICE_B, lang.get("REGISTER_OPTION_B")),
                        new OptionData(STRING, CHOICE_C, lang.get("REGISTER_OPTION_C")),
                        new OptionData(STRING, CHOICE_D, lang.get("REGISTER_OPTION_D")),
                        new OptionData(STRING, CHOICE_E, lang.get("REGISTER_OPTION_E")),
                        new OptionData(STRING, CHOICE_F, lang.get("REGISTER_OPTION_F")),
                        new OptionData(STRING, CHOICE_G, lang.get("REGISTER_OPTION_G")),
                        new OptionData(STRING, CHOICE_H, lang.get("REGISTER_OPTION_H")),
                        new OptionData(STRING, CHOICE_I, lang.get("REGISTER_OPTION_I")),
                        new OptionData(STRING, CHOICE_J, lang.get("REGISTER_OPTION_J"))
                )
        };
    }

    @Override
    public void loadConfigFile() {
        config = getter.readYml("config.yml", "plugins/" + PATH_FOLDER_NAME);
        util.println("Setting File Loaded Successfully");
    }

    @Override
    public void loadVariables() {
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        for (Object guild : new JsonArray(config.get("Emojis").toString())) {
            for (Item emojis : ((JsonObject) guild).Items()) {
                List<RichCustomEmoji> e = jdaBot.getGuildById(emojis.getKey()).retrieveEmojis().complete();
                for (Object emoji : (JsonArray) emojis.getValue()) {
                    JsonObject object = ((JsonObject) emoji);
                    for (var i : e) {
                        if (i.getName().equals(object.getString("name")) && (i.isAnimated() == object.getBoolean("a"))) {
                            votes.add(i);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void loadLang() {
        // expert files
        getter.exportLang(LANG_DEFAULT, LANG_PARAMETERS_DEFAULT);
        lang = getter.getLangFileData((String) config.get("Lang"));
    }


    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("poll")) return;
        if (!permissionCheck(Permission.MANAGE_CHANNEL, event))
            return;

        List<MessageEmbed.Field> fields = new ArrayList<>();
        for (int i = 0; i < event.getOptions().size() - 1; i++) {
            fields.add(new MessageEmbed.Field(votes.get(i).getFormatted() + event.getOptions().get(i + 1).getAsString(), "", false));
        }
        event.getChannel().sendMessageEmbeds(createEmbed(
                event.getOption(QUESTION).getAsString(), null,
                lang.get("FOOTER"),
                getMemberNick(event), event.getUser().getAvatarUrl(),
                fields,
                OffsetDateTime.now(), 0x87E5CF
        )).queue(m -> {
            for (int i = 0; i < event.getOptions().size() - 1; i++)
                m.addReaction(votes.get(i)).queue();

        });

        event.getHook().editOriginalEmbeds(createEmbed(lang.get("SUCCESS"), 0x9740b9)).queue();
    }

}