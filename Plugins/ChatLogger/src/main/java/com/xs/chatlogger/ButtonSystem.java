package com.xs.chatlogger;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static net.dv8tion.jda.api.entities.channel.ChannelType.*;


public class ButtonSystem {
    public final JsonManager manager;

    public ButtonSystem(JsonManager manager) {
        this.manager = manager;
    }

    public void setting(SlashCommandInteractionEvent event, DiscordLocale local) {
        ChannelSetting setting = manager.getOrDefault(event.getChannel().getIdLong());

        event.getHook()
                .editOriginalEmbeds(getEmbed(setting, local).build())
                .setActionRow(getButtons(event, setting, local))
                .queue();
    }

    public void toggle(ButtonInteractionEvent event, String[] args, DiscordLocale local) {
        if (!args[3].equals(event.getUser().getId())) return;
        manager.toggle(event.getChannel().getId());

        ChannelSetting setting = manager.getOrDefault(event.getChannel().getIdLong());
        event.getHook()
                .editOriginalEmbeds(getEmbed(setting, local).build())
                .setActionRow(getButtons(event, setting, local))
                .queue();
        event.deferEdit().queue();
    }

    public void createSel(ButtonInteractionEvent event, String[] args, DiscordLocale local) {
        if (!args[3].equals(event.getUser().getId())) return;

        EntitySelectMenu menu = EntitySelectMenu
                .create("xs:chatlogger:" + args[2] + ":" + event.getUser().getId() + ':' + event.getChannel().getId(), EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(
                        TEXT, VOICE, VOICE, NEWS, FORUM,
                        GUILD_PRIVATE_THREAD, GUILD_PUBLIC_THREAD, GUILD_NEWS_THREAD
                )
                .setPlaceholder("請選擇頻道")
                .setRequiredRange(1, 25)
                .build();

        event.getHook().editOriginalEmbeds(Collections.emptyList()).setActionRow(menu).queue();
        event.deferEdit().queue();
    }

    public void delete(ButtonInteractionEvent event, String[] args, DiscordLocale local) {
        if (!args[3].equals(event.getUser().getId())) return;
        manager.delete(event.getChannel().getId());

        event.getHook().editOriginalEmbeds(createEmbed("已刪除", 0x00FFFF)).setComponents(Collections.emptyList()).queue();
        event.deferEdit().queue();
    }


    public void select(EntitySelectInteractionEvent event, String[] args, DiscordLocale local) {
        if (!args[3].equals(event.getUser().getId())) return;

        List<String> channelIDs = event.getValues().stream().map(ISnowflake::getId).collect(Collectors.toList());

        manager.addChannels(args[4], channelIDs, args[2]);

        ChannelSetting setting = manager.getOrDefault(event.getChannel().getIdLong());
        event.getHook()
                .editOriginalEmbeds(getEmbed(setting, local).build())
                .setActionRow(getButtons(event, setting, local))
                .queue();

        event.deferEdit().queue();
    }

    private EmbedBuilder getEmbed(ChannelSetting setting, DiscordLocale local) {
        StringBuilder whiteBuilder = new StringBuilder();
        if (setting.white.size() > 0) {
            for (ChannelSetting.ListData i : setting.white) {
                whiteBuilder.append("<#").append(i.id).append(">\n");
            }
        } else {
            whiteBuilder.append("無");
        }

        StringBuilder blackBuilder = new StringBuilder();
        if (setting.black.size() > 0) {
            for (ChannelSetting.ListData i : setting.black) {
                blackBuilder.append("<#").append(i.id).append(">\n");
            }
        } else {
            blackBuilder.append("無");
        }

        return new EmbedBuilder()
                .setTitle("頻道設定")
                .setColor(0x00FFFF)
                .addField("目前紀錄狀態", setting.whitelist ? "**<白名單>**" : "**<黑名單>**", false)
                .addField("白名單頻道", whiteBuilder.toString(), false)
                .addField("黑名單頻道", blackBuilder.toString(), false);
    }

    public List<Button> getButtons(GenericInteractionCreateEvent event, ChannelSetting setting, DiscordLocale local) {
        List<Button> buttons = new ArrayList<>();
        buttons.add(
                new ButtonImpl("xs:chatlogger:toggle:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        "切換狀態",
                        ButtonStyle.PRIMARY, false, null
                )
        );

        buttons.add(
                new ButtonImpl("xs:chatlogger:white:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        "設定白名單",
                        ButtonStyle.SUCCESS, false, null
                )
        );

        buttons.add(
                new ButtonImpl("xs:chatlogger:black:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        "設定黑名單",
                        ButtonStyle.SECONDARY, false, null
                )
        );

        buttons.add(
                new ButtonImpl("xs:chatlogger:delete:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        "刪除",
                        ButtonStyle.DANGER, false, null
                )
        );

        return buttons;
    }

}
