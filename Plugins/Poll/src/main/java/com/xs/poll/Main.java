package com.xs.poll;

import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.EmbedBuilder;
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
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.xs.loader.base.Loader.jdaBot;
import static com.xs.loader.util.EmbedCreator.createEmbed;
import static net.dv8tion.jda.api.Permission.MANAGE_EVENTS;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class Main extends Event {

    private static final String TAG = "Poll";
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private final String PATH_FOLDER_NAME = "plugins/Poll";
    private final List<Emoji> votes = new ArrayList<>();
    private MainConfig configFile;
    private LangManager langManager;
    private FileGetter getter;
    private Logger logger;
    private Map<String, Map<DiscordLocale, String>> langMap; // Label, Local, Content
    private Map<Long, List<String>> emojiData;
    private boolean setup = false;

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {

        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        loadConfigFile();
        loadLang();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }

    @Override
    public void loadLang() {
        langManager = new LangManager(logger, getter, PATH_FOLDER_NAME, LANG_DEFAULT, DiscordLocale.CHINESE_TAIWAN);

        langMap = langManager.getMap();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("poll", "make a poll")
                        .setNameLocalizations(langMap.get("register;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;description"))
                        .addOptions(
                                new OptionData(STRING, "question", "user", true)
                                        .setDescriptionLocalizations(langMap.get("register;options;name")),
                                new OptionData(STRING, "choice_a", "reason")
                                        .setDescriptionLocalizations(langMap.get("register;options;a")),
                                new OptionData(STRING, "choice_b", "reason")
                                        .setDescriptionLocalizations(langMap.get("register;options;b")),
                                new OptionData(STRING, "choice_c", "reason")
                                        .setDescriptionLocalizations(langMap.get("register;options;c")),
                                new OptionData(STRING, "choice_d", "reason")
                                        .setDescriptionLocalizations(langMap.get("register;options;d")),
                                new OptionData(STRING, "choice_e", "reason")
                                        .setDescriptionLocalizations(langMap.get("register;options;e")),
                                new OptionData(STRING, "choice_f", "reason")
                                        .setDescriptionLocalizations(langMap.get("register;options;f")),
                                new OptionData(STRING, "choice_g", "reason")
                                        .setDescriptionLocalizations(langMap.get("register;options;g")),
                                new OptionData(STRING, "choice_h", "reason")
                                        .setDescriptionLocalizations(langMap.get("register;options;h")),
                                new OptionData(STRING, "choice_i", "reason")
                                        .setDescriptionLocalizations(langMap.get("register;options;i")),
                                new OptionData(STRING, "choice_j", "reason")
                                        .setDescriptionLocalizations(langMap.get("register;options;j")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(MANAGE_EVENTS))
        };
    }

    @Override
    public void loadConfigFile() {
        try (InputStream inputStream = getter.readInputStreamOrDefaultFromSource("config.yml")) {
            if (inputStream == null) return;
            configFile = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader()))
                    .loadAs(inputStream, MainConfig.class);
            logger.log("Setting File Loaded Successfully");
        } catch (IOException e) {
            logger.warn("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        if (setup) {
            for (Map.Entry<Long, List<String>> i : configFile.Emojis.entrySet()) {
                Guild guild = jdaBot.getGuildById(i.getKey());
                if (guild == null) {
                    logger.warn("Cannot found guild by id: " + i.getKey());
                } else {
                    for (RichCustomEmoji e : guild.retrieveEmojis().complete()) {
                        i.getValue().forEach(j -> {
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
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("poll")) return;
        if (!event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
            event.getHook().editOriginalEmbeds(createEmbed("你沒有權限", 0xFF0000)).queue();
            return;
        }

        DiscordLocale local = event.getUserLocale();
        List<MessageEmbed.Field> fields = new ArrayList<>();
        EmbedBuilder builder = new EmbedBuilder();

        for (int i = 0; i < event.getOptions().size() - 1; i++) {
            builder.addField(
                    votes.get(i).getFormatted() + event.getOptions().get(i + 1).getAsString(),
                    "", false
            );
        }

        event.getChannel().sendMessageEmbeds(builder
                .setAuthor(event.getMember().getEffectiveName(), null, event.getUser().getAvatarUrl())
                .setTitle(event.getOption("question").getAsString())
                .setFooter(langManager.get("embed;footer", local))
                .setColor(0x87E5CF)
                .setTimestamp(OffsetDateTime.now())
                .build()
        ).queue(m -> {
            for (int i = 0; i < event.getOptions().size() - 1; i++)
                m.addReaction(votes.get(i)).queue();
        });

        event.getHook().editOriginalEmbeds(createEmbed(langManager.get("command;success", local), 0x9740b9)).queue();
    }

}