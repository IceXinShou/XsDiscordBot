package com.xs.voicelogger;

import com.xs.loader.lang.LangManager;
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
import java.util.stream.Collectors;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static net.dv8tion.jda.api.entities.channel.ChannelType.CATEGORY;
import static net.dv8tion.jda.api.entities.channel.ChannelType.VOICE;


public class ButtonSystem {
    private final LangManager langManager;
    private final JsonManager manager;

    public ButtonSystem(LangManager langManager, JsonManager manager) {
        this.langManager = langManager;
        this.manager = manager;
    }

    public void setting(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        ChannelSetting setting = manager.getOrDefault(event.getGuild().getIdLong(), event.getChannel().getIdLong());
        DiscordLocale local = event.getUserLocale();
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

        event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;setting;delete_success", local), 0x00FFFF)).setComponents(Collections.emptyList()).queue();
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
                .setPlaceholder(langManager.get("runtime;setting;select_menu;placeholder", local))
                .setRequiredRange(1, 25)
                .build();

        Button button = new ButtonImpl("xs:voicelogger:toggle:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                "返回", ButtonStyle.SECONDARY, false, null);

        event.getHook().editOriginalEmbeds(Collections.emptyList()).setComponents(ActionRow.of(menu), ActionRow.of(button)).queue();
        event.deferEdit().queue();
    }

    private EmbedBuilder getEmbed(ChannelSetting setting, DiscordLocale local) {
        StringBuilder whiteBuilder = new StringBuilder();
        if (!setting.white.isEmpty()) {
            for (long i : setting.white) {
                whiteBuilder.append("<#").append(i).append(">\n");
            }
        } else {
            whiteBuilder.append(langManager.get("runtime;setting;embed;empty", local));
        }

        StringBuilder blackBuilder = new StringBuilder();
        if (!setting.black.isEmpty()) {
            for (long i : setting.black) {
                blackBuilder.append("<#").append(i).append(">\n");
            }
        } else {
            blackBuilder.append(langManager.get("runtime;setting;embed;empty", local));
        }

        return new EmbedBuilder()
                .setTitle(langManager.get("runtime;setting;embed;channel_setting", local))
                .setColor(0x00FFFF)
                .addField(langManager.get("runtime;setting;embed;now_status", local),
                        setting.whitelistStat ?
                                langManager.get("runtime;setting;embed;white_list", local) :
                                langManager.get("runtime;setting;embed;black_list", local),
                        false)
                .addField(langManager.get("runtime;setting;embed;white_channel", local), whiteBuilder.toString(), false)
                .addField(langManager.get("runtime;setting;embed;black_channel", local), blackBuilder.toString(), false);
    }

    public List<Button> getButtons(GenericInteractionCreateEvent event, DiscordLocale local) {
        if (event.getChannel() == null) return Collections.emptyList();
        List<Button> buttons = new ArrayList<>();
        buttons.add(
                new ButtonImpl("xs:voicelogger:toggle:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        langManager.get("runtime;setting;button;toggle_status", local),
                        ButtonStyle.PRIMARY, false, null
                )
        );

        buttons.add(
                new ButtonImpl("xs:voicelogger:white:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        langManager.get("runtime;setting;button;set_white", local),
                        ButtonStyle.SUCCESS, false, null
                )
        );

        buttons.add(
                new ButtonImpl("xs:voicelogger:black:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        langManager.get("runtime;setting;button;set_black", local),
                        ButtonStyle.SECONDARY, false, null
                )
        );

        buttons.add(
                new ButtonImpl("xs:voicelogger:delete:" + event.getUser().getId() + ':' + event.getChannel().getId(),
                        langManager.get("runtime;setting;button;delete", local),
                        ButtonStyle.DANGER, false, null
                )
        );

        return buttons;
    }

}
