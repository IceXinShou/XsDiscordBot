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
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
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
import static net.dv8tion.jda.api.Permission.MANAGE_EVENTS;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class Main extends PluginEvent {
    private JSONObject config;
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "Poll";
    private static final String VERSION = "2.0";
    private final String PATH_FOLDER_NAME = "plugins/Poll";
    private Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content
    private JSONObject emojiData;
    private final List<Emoji> votes = new ArrayList<>();
    private boolean setup = false;

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
    public void loadLang() {
        LangGetter langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT);

        // expert files
        langGetter.exportDefaultLang();
        lang = langGetter.readLangFileData();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("poll", "make a poll")
                        .setNameLocalizations(lang.get("register;cmd"))
                        .setDescriptionLocalizations(lang.get("register;description"))
                        .addOptions(
                                new OptionData(STRING, QUESTION, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;options;name")),
                                new OptionData(STRING, CHOICE_A, "reason")
                                        .setDescriptionLocalizations(lang.get("register;options;a")),
                                new OptionData(STRING, CHOICE_B, "reason")
                                        .setDescriptionLocalizations(lang.get("register;options;b")),
                                new OptionData(STRING, CHOICE_C, "reason")
                                        .setDescriptionLocalizations(lang.get("register;options;c")),
                                new OptionData(STRING, CHOICE_D, "reason")
                                        .setDescriptionLocalizations(lang.get("register;options;d")),
                                new OptionData(STRING, CHOICE_E, "reason")
                                        .setDescriptionLocalizations(lang.get("register;options;e")),
                                new OptionData(STRING, CHOICE_F, "reason")
                                        .setDescriptionLocalizations(lang.get("register;options;f")),
                                new OptionData(STRING, CHOICE_G, "reason")
                                        .setDescriptionLocalizations(lang.get("register;options;g")),
                                new OptionData(STRING, CHOICE_H, "reason")
                                        .setDescriptionLocalizations(lang.get("register;options;h")),
                                new OptionData(STRING, CHOICE_I, "reason")
                                        .setDescriptionLocalizations(lang.get("register;options;i")),
                                new OptionData(STRING, CHOICE_J, "reason")
                                        .setDescriptionLocalizations(lang.get("register;options;j"))
                        )
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(MANAGE_EVENTS))
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
            setup = true;
        } else {
            logger.error("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
        }
    }


    @SuppressWarnings("unchecked warning")
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        if (setup) {
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
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("poll")) return;
        if (!permissionCheck(Permission.MANAGE_CHANNEL, event))
            return;

        DiscordLocale local = event.getUserLocale();
        List<MessageEmbed.Field> fields = new ArrayList<>();
        for (int i = 0; i < event.getOptions().size() - 1; i++) {
            fields.add(new MessageEmbed.Field(
                    votes.get(i).getFormatted() + event.getOptions().get(i + 1).getAsString(),
                    "", false)
            );
        }
        event.getChannel().sendMessageEmbeds(createEmbed(
                event.getOption(QUESTION).getAsString(), null,
                lang.get("embed;footer").get(local),
                getMemberNick(event), event.getUser().getAvatarUrl(),
                fields,
                OffsetDateTime.now(), 0x87E5CF
        )).queue(m -> {
            for (int i = 0; i < event.getOptions().size() - 1; i++)
                m.addReaction(votes.get(i)).queue();

        });

        event.getHook().editOriginalEmbeds(createEmbed(lang.get("command;success").get(local), 0x9740b9)).queue();
    }

}