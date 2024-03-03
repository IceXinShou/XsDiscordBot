package tw.xserver.notifier;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.lang.LangManager;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN;
import static net.dv8tion.jda.api.interactions.commands.OptionType.BOOLEAN;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;
import static net.dv8tion.jda.internal.utils.Checks.notNull;
import static tw.xserver.loader.base.Loader.ROOT_PATH;
import static tw.xserver.loader.util.EmbedCreator.createEmbed;
import static tw.xserver.loader.util.GlobalUtil.checkCommand;

public class OnlineNotifier extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnlineNotifier.class);
    private static final String PATH_FOLDER_NAME = "plugins/OnlineNotifier";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private Language lang;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentLinkedQueue<NotifierType> notifyQueue = new ConcurrentLinkedQueue<>();


    public OnlineNotifier() {
        super(true);

        reloadAll();
        LOGGER.info("loadedOnlineNotifier");
    }

    @Override
    public void unload() {
        executorService.shutdown();
        LOGGER.info("unLoaded OnlineNotifier");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, OnlineNotifier.class);
    }

    @Override
    public void reloadLang() {
        try {
            lang = new LangManager<>(getter, PATH_FOLDER_NAME, CHINESE_TAIWAN, Language.class).get();
        } catch (IOException | IllegalAccessException | InstantiationException | InvocationTargetException |
                 NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CommandData[] globalCommands() {
        return new SlashCommandData[]{
                Commands.slash("online-notify", "bot will dm you while the other person online")
                        .setNameLocalizations(lang.register.name)
                        .setDescriptionLocalizations(lang.register.description)
                        .addOptions(
                        new OptionData(USER, "member", "member", true)
                                .setNameLocalizations(lang.register.options.member.name)
                                .setDescriptionLocalizations(lang.register.options.member.description),
                        new OptionData(BOOLEAN, "infinity", "notify once or infinity")
                                .setNameLocalizations(lang.register.options.infinity.name)
                                .setDescriptionLocalizations(lang.register.options.infinity.description)
                )
        };
    }

    @Override
    public void onReady() {
        executorService.scheduleWithFixedDelay(() -> {
            if (notifyQueue.isEmpty()) return;

            List<NotifierType> retryList = new ArrayList<>();
            NotifierType notifierType;
            while ((notifierType = notifyQueue.poll()) != null) {
                if (notifierType.targetMember.getOnlineStatus() == OnlineStatus.OFFLINE) continue;

                Member targetMember = notifierType.targetMember;
                User sourceUser = notifierType.sourceUser;

                final NotifierType finalNotifier = notifierType;
                sourceUser.openPrivateChannel()
                        .flatMap(channel -> channel.sendMessageEmbeds(createEmbed(
                                lang.runtime.successes.show_up.get(finalNotifier.locale)
                                        .replace("%member%", targetMember.getUser().getName())
                                        .replace("%status%", targetMember.getOnlineStatus().getKey())
                                , 0x00FFFF
                        )))
                        .queue(
                                success -> {
                                    if (finalNotifier.infinity)
                                        retryList.add(finalNotifier);
                                },
                                failure -> {
                                }
                        );

            }

            notifyQueue.addAll(retryList);
        }, 0, 30, TimeUnit.SECONDS);
    }

    /* ---------- Process Command ---------- */
    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        if (checkCommand(event, "online-notify")) return;

        DiscordLocale locale = event.getUserLocale();
        Guild guild = event.getGuild();
        User sourceUser = event.getUser();
        Member targetMember = event.getOption("member", OptionMapping::getAsMember);
        boolean infinity = event.getOption("event", false, OptionMapping::getAsBoolean);

        notNull(guild, "Guild");
        notNull(targetMember, "Target Member");

        if (isDmFail(sourceUser, locale)) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.no_dm_able.get(locale), 0xFF0000)).queue();
            return;
        }

        if (targetMember.getOnlineStatus() != OnlineStatus.OFFLINE && !infinity) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.already_online.get(locale), 0xFF0000)).queue();
            return;
        }

        notifyQueue.add(new NotifierType(targetMember, sourceUser, infinity, locale));
    }

    private boolean isDmFail(User sourceUser, DiscordLocale locale) {
        try {
            sourceUser.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessage(lang.runtime.successes.test.get(locale)))
                    .flatMap(Message::delete)
                    .complete();
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    static class NotifierType {
        public final Member targetMember;
        public final User sourceUser;
        public final DiscordLocale locale;
        public final boolean infinity;

        public NotifierType(Member targetMember, User soruceUser, boolean infinity, DiscordLocale locale) {
            this.targetMember = targetMember;
            this.sourceUser = soruceUser;
            this.infinity = infinity;
            this.locale = locale;
        }
    }
}