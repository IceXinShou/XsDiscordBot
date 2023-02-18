package com.xs.privatechannelmanager;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.xs.loader.MainLoader.jdaBot;

public class Main extends PluginEvent {
    private MainConfig configFile;
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "PrivateChannelManager";
    private final String PATH_FOLDER_NAME = "./plugins/PrivateChannelManager";
    private Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {

        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        loadLang();
        loadConfigFile();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }

    @Override
    public void loadLang() {
        LangGetter langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, this.getClass());

        // expert files
        langGetter.exportDefaultLang();
        lang = langGetter.readLangFileData();
    }

    @Override
    public CommandData[] guildCommands() {
        return null;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

    }

    @Override
    public void loadConfigFile() {
        InputStream inputStream = getter.readYmlInputStream("config.yml", PATH_FOLDER_NAME);
        if (inputStream == null) return;

        try {
            configFile = new Yaml(new Constructor(MainConfig.class)).load(inputStream);
            inputStream.close();

            logger.log("Setting File Loaded Successfully");
        } catch (IOException e) {
            logger.warn("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromType(ChannelType.PRIVATE)) return;

        long authorID = event.getAuthor().getIdLong();
        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(event.getAuthor().getAsTag() + " (" + authorID + ")",
                        null, event.getAuthor().getAvatarUrl())
                .setDescription(event.getMessage().getContentRaw())
                .setTimestamp(OffsetDateTime.now())
                .setFooter("收到訊息")
                .setColor(Color.GREEN);

        Button reply = new ButtonImpl("xs:pcm:reply:" + authorID + ":" + event.getMessageId(), "回覆", ButtonStyle.SUCCESS, false, null);

        for (Long i : configFile.OwnerID) {
            PrivateChannel channel = jdaBot.retrieveUserById(i).complete().openPrivateChannel().complete();
            channel.sendMessageEmbeds(builder.build()).addActionRow(reply).queue();
        }

        event.getMessage().reply("已收到").queueAfter(1, TimeUnit.SECONDS, i -> i.delete().queue());
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromType(ChannelType.PRIVATE)) return;


    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("pcm")) return;

        switch (args[2]) {
            case "reply": {
                createReplyForm(event, args);
                break;
            }
        }

    }

    private void createReplyForm(ButtonInteractionEvent event, String[] args) {
        TextInput titleInp = TextInput.create("title", "標題", TextInputStyle.SHORT)
                .setPlaceholder("標題")
                .setMinLength(1)
                .build();

        TextInput contentInp = TextInput.create("content", "內容", TextInputStyle.PARAGRAPH)
                .setPlaceholder("內容")
                .setMinLength(1)
                .setRequired(false)
                .build();

        event.replyModal(
                Modal.create("xs:pcm:reply:" + args[3] + ':' + args[4], "回覆")
                        .addActionRows(ActionRow.of(titleInp), ActionRow.of(contentInp))
                        .build()
        ).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String[] args = event.getModalId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("pcm")) return;
        switch (args[2]) {
            case "reply": {
                sendReply(event, args);
                break;
            }
        }
    }

    private void sendReply(ModalInteractionEvent event, String[] args) {
        User user = jdaBot.retrieveUserById(args[3]).complete();
        if (user == null) {
            event.deferReply(true).setContent("找不到此使用者").queue();
            return;
        }

        PrivateChannel channel = user.openPrivateChannel().complete();
        if (channel == null) {
            event.deferReply(true).setContent("無法對此使用者開起私聊對話").queue();
            return;
        }

        ModalMapping titleMap = event.getValue("title");
        ModalMapping contentMap = event.getValue("content");
        if (titleMap == null) {
            event.deferReply(true).setContent("無標題").queue();
            return;
        }

        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(event.getUser().getAsTag() + " (" + event.getUser().getId() + ')',
                        null, event.getUser().getAvatarUrl())
                .setTitle(titleMap.getAsString())
                .setTimestamp(OffsetDateTime.now())
                .setFooter("回覆訊息")
                .setColor(Color.ORANGE);

        if (contentMap != null) builder.setDescription(contentMap.getAsString());

        Message message = channel.retrieveMessageById(args[4]).complete();
        if (message != null) {
            message.replyEmbeds(builder.build()).queue();
        } else {
            channel.sendMessageEmbeds(builder.build()).queue();
        }

        event.getChannel().sendMessageEmbeds(builder.setFooter("回覆備份").build()).queue();
        event.deferEdit().queue();
    }
}