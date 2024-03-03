package tw.xserver.loader.base;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.plugin.Event;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.List;

public class ListenerManager extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerManager.class);

    private final List<CommandData> guildCommands;
    private final LinkedHashMap<String, Event> plugin_queue;

    public ListenerManager(List<CommandData> guildCommands, LinkedHashMap<String, Event> plugin_queue) {
        this.guildCommands = guildCommands;
        this.plugin_queue = plugin_queue;
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        Guild guild = event.getGuild();
        if (!guildCommands.isEmpty()) {
            guild.updateCommands().addCommands(guildCommands).queue();
        }

        LOGGER.info("load guild: " + guild.getName() + " (" + guild.getId() + ')');
    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        LOGGER.info("[CMD] " + event.getUser().getName() + ": " + event.getCommandString());
    }

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        event.deferReply(true).queue();

        LOGGER.info(event.getUser().getName() + ": " + event.getCommandString());
    }

    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        LOGGER.info("[BTN] " + event.getUser().getName() + ": " + event.getComponentId());
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        for (Event plugin : plugin_queue.values()) plugin.onReady();
    }
}
