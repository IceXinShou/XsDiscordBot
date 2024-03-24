package tw.xserver.poll;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import tw.xserver.loader.lang.LangManager;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.dv8tion.jda.api.Permission.MANAGE_EVENTS;
import static net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;
import static net.dv8tion.jda.internal.utils.Checks.notNull;
import static tw.xserver.loader.base.Loader.ROOT_PATH;
import static tw.xserver.loader.base.Loader.jdaBot;
import static tw.xserver.loader.util.EmbedCreator.createEmbed;
import static tw.xserver.loader.util.GlobalUtil.checkCommand;

public class Poll extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(Poll.class);
    private static final String PATH_FOLDER_NAME = "plugins/Poll";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private final List<Emoji> votes = new ArrayList<>();
    private MainConfig configFile;
    private Language lang;

    public Poll() {
        super(true);

        reloadAll();
        LOGGER.info("loaded Poll");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded Poll");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, Poll.class);

        try (InputStream inputStream = getter.readInputStream("config.yml")) {
            if (inputStream == null) return;
            configFile = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader(), new LoaderOptions()))
                    .loadAs(inputStream, MainConfig.class);
            LOGGER.info("setting file loaded successfully");
        } catch (IOException e) {
            LOGGER.error("please configure /" + PATH_FOLDER_NAME + "/config.yml");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reloadLang() {
        try {
            lang = new LangManager<>(getter, PATH_FOLDER_NAME, CHINESE_TAIWAN, Language.class).get();
        } catch (IOException | IllegalAccessException | InstantiationException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("poll", "make a poll")
                        .setNameLocalizations(lang.register.name)
                        .setDescriptionLocalizations(lang.register.description)
                        .addOptions(
                                new OptionData(STRING, "question", "user", true)
                                        .setNameLocalizations(lang.register.options.question.name)
                                        .setDescriptionLocalizations(lang.register.options.question.description),
                                new OptionData(STRING, "choice_a", "reason")
                                        .setNameLocalizations(lang.register.options.a.name)
                                        .setDescriptionLocalizations(lang.register.options.a.description),
                                new OptionData(STRING, "choice_b", "reason")
                                        .setNameLocalizations(lang.register.options.b.name)
                                        .setDescriptionLocalizations(lang.register.options.b.description),
                                new OptionData(STRING, "choice_c", "reason")
                                        .setNameLocalizations(lang.register.options.c.name)
                                        .setDescriptionLocalizations(lang.register.options.c.description),
                                new OptionData(STRING, "choice_d", "reason")
                                        .setNameLocalizations(lang.register.options.d.name)
                                        .setDescriptionLocalizations(lang.register.options.d.description),
                                new OptionData(STRING, "choice_e", "reason")
                                        .setNameLocalizations(lang.register.options.e.name)
                                        .setDescriptionLocalizations(lang.register.options.e.description),
                                new OptionData(STRING, "choice_f", "reason")
                                        .setNameLocalizations(lang.register.options.f.name)
                                        .setDescriptionLocalizations(lang.register.options.f.description),
                                new OptionData(STRING, "choice_g", "reason")
                                        .setNameLocalizations(lang.register.options.g.name)
                                        .setDescriptionLocalizations(lang.register.options.g.description),
                                new OptionData(STRING, "choice_h", "reason")
                                        .setNameLocalizations(lang.register.options.h.name)
                                        .setDescriptionLocalizations(lang.register.options.h.description),
                                new OptionData(STRING, "choice_i", "reason")
                                        .setNameLocalizations(lang.register.options.i.name)
                                        .setDescriptionLocalizations(lang.register.options.i.description),
                                new OptionData(STRING, "choice_j", "reason")
                                        .setNameLocalizations(lang.register.options.j.name)
                                        .setDescriptionLocalizations(lang.register.options.j.description)
                        ).setDefaultPermissions(DefaultMemberPermissions.enabledFor(MANAGE_EVENTS))
        };
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        for (Map.Entry<Long, List<String>> i : configFile.Emojis.entrySet()) {
            Guild guild = jdaBot.getGuildById(i.getKey());
            if (guild == null) {
                LOGGER.warn("Cannot found guild by id: " + i.getKey());
                return;
            }

            guild.retrieveEmojis().queue(es -> {
                for (RichCustomEmoji e : es) {
                    i.getValue().forEach(j -> {
                        if (e.getName().equals(j)) {
                            votes.add(e);
                            LOGGER.info("Added " + e.getName());
                        }
                    });
                }
            });
        }
    }


    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        if (checkCommand(event, "poll")) return;

        DiscordLocale locale = event.getUserLocale();
        Member member = event.getMember();

        notNull(member, "Member Executor");

        if (!member.hasPermission(Permission.MANAGE_CHANNEL)) {
            event.getHook().editOriginalEmbeds(createEmbed("你沒有權限", 0xFF0000)).queue();
            return;
        }

        EmbedBuilder builder = new EmbedBuilder();

        for (int i = 0; i < event.getOptions().size() - 1; i++) {
            builder.addField(
                    votes.get(i).getFormatted() + event.getOptions().get(i + 1).getAsString(),
                    "", false
            );
        }

        event.getChannel().sendMessageEmbeds(builder
                .setAuthor(event.getMember().getEffectiveName(), null, event.getUser().getAvatarUrl())
                .setTitle(event.getOption("question", "empty", OptionMapping::getAsString))
                .setFooter(lang.embed.footer.get(locale))
                .setColor(0x87E5CF)
                .setTimestamp(OffsetDateTime.now())
                .build()
        ).queue(m -> {
            for (int i = 0; i < event.getOptions().size() - 1; i++)
                m.addReaction(votes.get(i)).queue();
        });

        event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.success.get(locale), 0x9740b9)).queue();
    }

}