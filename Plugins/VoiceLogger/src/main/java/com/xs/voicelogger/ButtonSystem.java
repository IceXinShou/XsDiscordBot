package com.xs.voicelogger;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static net.dv8tion.jda.api.entities.channel.ChannelType.*;


public class ButtonSystem {
    private final JsonManager manager;

    private final Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content

    public ButtonSystem(Map<String, Map<DiscordLocale, String>> lang, JsonManager manager) {
        this.lang = lang;
        this.manager = manager;
    }

    public void setting(SlashCommandInteractionEvent event, DiscordLocale local) {
        if (event.getGuild() == null) return;
        ChannelSetting setting = manager.getOrDefault(event.getGuild().getIdLong(), event.getChannel().getIdLong());

        event.getHook()
                .editOriginalEmbeds(getEmbed(setting, local).build())
                .setActionRow(getButtons(event, local))
                .queue();
    }

    public void toggle(ButtonInteractionEvent event, String[] args, DiscordLocale local) {
        if (!args[3].equals(event.getUser().getId())) return;
        if (event.getGuild() == null) return;

        ChannelSetting setting = manager.toggle(event.getGuild().getIdLong(), event.getChannel().getIdLong());

        event.getHook()
                .editOriginalEmbeds(getEmbed(setting, local).build())
                .setActionRow(getButtons(event, local))
                .queue();
        event.deferEdit().queue();
    }


    public void delete(ButtonInteractionEvent event, String[] args, DiscordLocale local) {
        if (event.getGuild() == null) return;
        if (!args[3].equals(event.getUser().getId())) return;
        manager.delete(event.getGuild().getIdLong(), event.getChannel().getIdLong());

        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;setting;delete_success").get(local), 0x00FFFF)).setComponents(Collections.emptyList()).queue();
        event.deferEdit().queue();
    }

    public void select(EntitySelectInteractionEvent event, String[] args, DiscordLocale local) {
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
                channelIDs, String.valueOf(args[2]).equals("white")
        );

        if (setting == null) {
            System.out.println("WTF");
            return;
        }

        event.getHook()
                .editOriginalEmbeds(getEmbed(setting, local).build())
                .setActionRow(getButtons(event, local))
                .queue();

        event.deferEdit().queue();
    }

    public void createSel(ButtonInteractionEvent event, String[] args, DiscordLocale local) {
        if (!args[3].equals(event.getUser().getId())) return;

        EntitySelectMenu menu = EntitySelectMenu
                .create("xs:voicelogger:" + args[2] + ':' + event.getUser().getId() + ':' + event.getChannel().getId(), EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(VOICE, CATEGORY)
                .setPlaceholder(lang.get("runtime;setting;select_menu;placeholder").get(local))
                .setRequiredRange(1, 25)
                .build();

        Button button = new ButtonImpl("xs:voicelogger:toggle:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                "返回", ButtonStyle.SECONDARY, false, null);

        event.getHook().editOriginalEmbeds(Collections.emptyList()).setComponents(ActionRow.of(menu), ActionRow.of(button)).queue();
        event.deferEdit().queue();
    }

    private EmbedBuilder getEmbed(ChannelSetting setting, DiscordLocale local) {
        StringBuilder whiteBuilder = new StringBuilder();
        if (setting.white.size() > 0) {
            for (long i : setting.white) {
                whiteBuilder.append("<#").append(i).append(">\n");
            }
        } else {
            whiteBuilder.append(lang.get("runtime;setting;embed;empty").get(local));
        }

        StringBuilder blackBuilder = new StringBuilder();
        if (setting.black.size() > 0) {
            for (long i : setting.black) {
                blackBuilder.append("<#").append(i).append(">\n");
            }
        } else {
            blackBuilder.append(lang.get("runtime;setting;embed;empty").get(local));
        }

        return new EmbedBuilder()
                .setTitle(lang.get("runtime;setting;embed;channel_setting").get(local))
                .setColor(0x00FFFF)
                .addField(lang.get("runtime;setting;embed;now_status").get(local),
                        setting.whitelistStat ?
                                lang.get("runtime;setting;embed;white_list").get(local) :
                                lang.get("runtime;setting;embed;black_list").get(local),
                        false)
                .addField(lang.get("runtime;setting;embed;white_channel").get(local), whiteBuilder.toString(), false)
                .addField(lang.get("runtime;setting;embed;black_channel").get(local), blackBuilder.toString(), false);
    }

    public List<Button> getButtons(GenericInteractionCreateEvent event, DiscordLocale local) {
        if (event.getChannel() == null) return Collections.emptyList();
        List<Button> buttons = new ArrayList<>();
        buttons.add(
                new ButtonImpl("xs:voicelogger:toggle:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        lang.get("runtime;setting;button;toggle_status").get(local),
                        ButtonStyle.PRIMARY, false, null
                )
        );

        buttons.add(
                new ButtonImpl("xs:voicelogger:white:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        lang.get("runtime;setting;button;set_white").get(local),
                        ButtonStyle.SUCCESS, false, null
                )
        );

        buttons.add(
                new ButtonImpl("xs:voicelogger:black:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        lang.get("runtime;setting;button;set_black").get(local),
                        ButtonStyle.SECONDARY, false, null
                )
        );

        buttons.add(
                new ButtonImpl("xs:voicelogger:delete:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        lang.get("runtime;setting;button;delete").get(local),
                        ButtonStyle.DANGER, false, null
                )
        );

        return buttons;
    }

}
