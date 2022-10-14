package com.xs.poll;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.xs.loader.MainLoader.jdaBot;
import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.PermissionERROR.permissionCheck;
import static com.xs.loader.util.SlashCommandOption.*;
import static com.xs.loader.util.Tag.getMemberNick;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class Main extends PluginEvent {
    private JSONObject config;
    private JSONObject lang_register;
    private JSONObject lang_register_options;
    private JSONObject lang_embed;
    private JSONObject lang_command;

    private final String[] LANG_DEFAULT = {"en_US", "zh_TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "Poll";
    private static final String VERSION = "1.0";
    private final String PATH_FOLDER_NAME = "plugins/Poll";
    private JSONObject emojiData;
    private final List<Emoji> votes = new ArrayList<>();
    private boolean noSet = false;
    private LangGetter langGetter;

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
                        new OptionData(STRING, QUESTION, lang_register_options.getString("name"), true),
                        new OptionData(STRING, CHOICE_A, lang_register_options.getString("a"), true),
                        new OptionData(STRING, CHOICE_B, lang_register_options.getString("b")),
                        new OptionData(STRING, CHOICE_C, lang_register_options.getString("c")),
                        new OptionData(STRING, CHOICE_D, lang_register_options.getString("d")),
                        new OptionData(STRING, CHOICE_E, lang_register_options.getString("e")),
                        new OptionData(STRING, CHOICE_F, lang_register_options.getString("f")),
                        new OptionData(STRING, CHOICE_G, lang_register_options.getString("g")),
                        new OptionData(STRING, CHOICE_H, lang_register_options.getString("h")),
                        new OptionData(STRING, CHOICE_I, lang_register_options.getString("i")),
                        new OptionData(STRING, CHOICE_J, lang_register_options.getString("j"))
                )
        };
    }

    @Override
    public void loadConfigFile() {
        config = new JSONObject(getter.readYml("config.yml", PATH_FOLDER_NAME));
        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void loadVariables() {
        if (config.has("Emojis") && !config.getJSONObject("Emojis").isEmpty()) {
            emojiData = config.getJSONObject("Emojis");
        } else {
            logger.error("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
            noSet = true;
        }
        langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, config.getString("Lang"));
    }


    @SuppressWarnings("unchecked warning")
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        if (!noSet) {
            for (Map.Entry<String, Object> i : emojiData.toMap().entrySet()) {
                Guild guild = jdaBot.getGuildById(i.getKey());
                if (guild == null) {
                    logger.error("Cannot found guild by id: " + i.getKey());
                } else {
                    for (RichCustomEmoji e : guild.retrieveEmojis().complete()) {
                        ((List<Object>) (i.getValue())).forEach(j -> {
                            if (e.getName().equals(j)) {
                                votes.add(e);
                                logger.log("Added " + e.getName());
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public void loadLang() {
        // expert files
        langGetter.exportDefaultLang();
        final JSONObject lang = langGetter.getLangFileData();
        lang_register = lang.getJSONObject("register");
        lang_register_options = lang_register.getJSONObject("options");
        lang_embed = lang.getJSONObject("embed");
        lang_command = lang.getJSONObject("command");
    }


    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals(lang_register.getString("cmd"))) return;
        if (!permissionCheck(Permission.MANAGE_CHANNEL, event))
            return;

        List<MessageEmbed.Field> fields = new ArrayList<>();
        for (int i = 0; i < event.getOptions().size() - 1; i++) {
            fields.add(new MessageEmbed.Field(votes.get(i).getFormatted() + event.getOptions().get(i + 1).getAsString(), "", false));
        }
        event.getChannel().sendMessageEmbeds(createEmbed(
                event.getOption(QUESTION).getAsString(), null,
                lang_embed.getString("footer"),
                getMemberNick(event), event.getUser().getAvatarUrl(),
                fields,
                OffsetDateTime.now(), 0x87E5CF
        )).queue(m -> {
            for (int i = 0; i < event.getOptions().size() - 1; i++)
                m.addReaction(votes.get(i)).queue();

        });

        event.getHook().editOriginalEmbeds(createEmbed(lang_command.getString("success"), 0x9740b9)).queue();
    }

}