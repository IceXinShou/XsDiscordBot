package tw.xserver.notifier;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.lang.LangManager;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN;
import static tw.xserver.loader.base.Loader.ROOT_PATH;

public class JoinNotifier extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(JoinNotifier.class);
    private static final String PATH_FOLDER_NAME = "plugins/JoinNotifier";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private Language lang;

    private final Map<String, Integer> invitesUsages = new HashMap<>();

    public JoinNotifier() {
        super(true);

        reloadAll();
        LOGGER.info("loaded JoinNotifier");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded JoinNotifier");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, JoinNotifier.class);
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
        return null;
    }

    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        // TODO: command listener
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        event.getGuild().retrieveInvites().queue(
                i -> i.forEach(invite -> invitesUsages.put(invite.getCode(), invite.getUses())
                )
        );
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        event.getGuild().retrieveInvites().queue(
                invites -> {
                    for (Invite i : invites) {
                        if (i.getUses() == invitesUsages.get(i.getCode())) continue;


                        return;

                    }
                }
        );
        guild.retrieveInvites().complete();
    }
}