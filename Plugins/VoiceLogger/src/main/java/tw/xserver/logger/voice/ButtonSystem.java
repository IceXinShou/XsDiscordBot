package tw.xserver.logger.voice;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.entities.channel.ChannelType.CATEGORY;
import static net.dv8tion.jda.api.entities.channel.ChannelType.VOICE;
import static tw.xserver.loader.util.EmbedCreator.createEmbed;


public class ButtonSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(ButtonSystem.class);
    private final JsonManager manager;
    private final Language lang;

    public ButtonSystem(Language lang, JsonManager manager) {
        this.lang = lang;
        this.manager = manager;
    }

    public void setting(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;

        ChannelSetting setting = manager.getOrDefault(event.getGuild().getIdLong(), event.getChannel().getIdLong());
        DiscordLocale locale = event.getUserLocale();
        event.getHook()
                .editOriginalEmbeds(getEmbed(setting, locale).build())
                .setActionRow(getButtons(event, locale))
                .queue();
    }

    public void toggle(ButtonInteractionEvent event, String[] args, DiscordLocale locale) {
        if (!args[3].equals(event.getUser().getId())) return;
        if (event.getGuild() == null) return;

        ChannelSetting setting = manager.toggle(event.getGuild().getIdLong(), event.getChannel().getIdLong());

        event.getHook()
                .editOriginalEmbeds(getEmbed(setting, locale).build())
                .setActionRow(getButtons(event, locale))
                .queue();
        event.deferEdit().queue();
    }


    public void delete(ButtonInteractionEvent event, String[] args, DiscordLocale locale) {
        if (event.getGuild() == null) return;
        if (!args[3].equals(event.getUser().getId())) return;

        manager.delete(event.getGuild().getIdLong(), event.getChannel().getIdLong());

        event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.setting.delete_success.get(locale), 0x00FFFF)).setComponents(Collections.emptyList()).queue();
        event.deferEdit().queue();
    }

    public void select(EntitySelectInteractionEvent event, String[] args, DiscordLocale locale) {
        if (!args[3].equals(event.getUser().getId())) return;
        if (event.getGuild() == null) return;

        List<Long> channelIDs = event.getValues().stream().map(ISnowflake::getIdLong).collect(Collectors.toList());

        // add categorizes channel
        event.getGuild().getCategories().forEach(i -> {
            if (channelIDs.contains(i.getIdLong())) {
                channelIDs.remove(i.getIdLong());
                channelIDs.addAll(i.getVoiceChannels().stream().map(ISnowflake::getIdLong).collect(Collectors.toList()));
            }
        });

        ChannelSetting setting = manager.addChannels(
                event.getGuild().getIdLong(),
                Long.parseLong(args[4]),
                channelIDs,
                String.valueOf(args[2]).equals("white")
        );

        if (setting == null) {
            LOGGER.error("impossible");
            return;
        }

        event.getHook()
                .editOriginalEmbeds(getEmbed(setting, locale).build())
                .setActionRow(getButtons(event, locale))
                .queue();

        event.deferEdit().queue();
    }

    public void createSel(ButtonInteractionEvent event, String[] args, DiscordLocale locale) {
        if (!args[3].equals(event.getUser().getId())) return;

        EntitySelectMenu menu = EntitySelectMenu
                .create("xs:voice-logger:" + args[2] + ':' + event.getUser().getId() + ':' + event.getChannel().getId(), EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(VOICE, CATEGORY)
                .setPlaceholder(lang.runtime.setting.menu.placeholder.get(locale))
                .setRequiredRange(1, 25)
                .build();

        event.getHook().editOriginalEmbeds(Collections.emptyList())
                .setComponents(
                        ActionRow.of(menu),
                        ActionRow.of(
                                new ButtonImpl("xs:voice-logger:toggle:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                                        lang.runtime.setting.button.go_back.get(locale), ButtonStyle.SECONDARY, false, null)
                        )).queue();
        event.deferEdit().queue();
    }

    private EmbedBuilder getEmbed(ChannelSetting setting, DiscordLocale locale) {
        StringBuilder whiteBuilder = new StringBuilder();
        if (!setting.white.isEmpty()) {
            setting.white.stream()
                    .map(i -> ("<#" + i + ">\n"))
                    .forEach(whiteBuilder::append);
        } else {
            whiteBuilder.append(lang.runtime.setting.embed.empty.get(locale));
        }

        StringBuilder blackBuilder = new StringBuilder();
        if (!setting.black.isEmpty()) {
            setting.black.stream()
                    .map(i -> ("<#" + i + ">\n"))
                    .forEach(blackBuilder::append);
        } else {
            blackBuilder.append(lang.runtime.setting.embed.empty.get(locale));
        }

        return new EmbedBuilder()
                .setTitle(lang.runtime.setting.embed.channel_setting.get(locale))
                .setColor(0x00FFFF)
                .addField(lang.runtime.setting.embed.now_status.get(locale),
                        setting.whitelistStat ?
                                lang.runtime.setting.embed.white_list.get(locale) :
                                lang.runtime.setting.embed.black_list.get(locale),
                        false)
                .addField(lang.runtime.setting.embed.white_channel.get(locale), whiteBuilder.toString(), false)
                .addField(lang.runtime.setting.embed.black_channel.get(locale), blackBuilder.toString(), false);
    }

    public List<Button> getButtons(GenericInteractionCreateEvent event, DiscordLocale locale) {
        if (event.getChannel() == null) return Collections.emptyList();

        return Arrays.asList(
                new ButtonImpl("xs:voice-logger:toggle:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        lang.runtime.setting.button.toggle_status.get(locale),
                        ButtonStyle.PRIMARY, false, null
                ),
                new ButtonImpl("xs:voice-logger:white:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        lang.runtime.setting.button.set_white.get(locale),
                        ButtonStyle.SUCCESS, false, null
                ),
                new ButtonImpl("xs:voice-logger:black:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        lang.runtime.setting.button.set_black.get(locale),
                        ButtonStyle.SECONDARY, false, null
                ),
                new ButtonImpl("xs:voice-logger:delete:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        lang.runtime.setting.button.delete.get(locale),
                        ButtonStyle.DANGER, false, null
                )
        );
    }
}
