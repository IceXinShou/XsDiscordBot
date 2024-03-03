package tw.xserver.dm;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;

import static tw.xserver.loader.base.Loader.ROOT_PATH;
import static tw.xserver.loader.base.Loader.jdaBot;

public class DirectMessage extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectMessage.class);
    private static final String PATH_FOLDER_NAME = "./plugins/PrivateChannelManager";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private MainConfig configFile;

    public DirectMessage() {
        super(true);

        reloadAll();
        LOGGER.info("loaded DirectMessage");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded DirectMessage");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, DirectMessage.class);

        try (InputStream inputStream = getter.readInputStream("config.yml")) {
            if (inputStream == null) return;
            configFile = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader(), new LoaderOptions()))
                    .loadAs(inputStream, MainConfig.class);
            LOGGER.info("setting file loaded successfully");
        } catch (IOException e) {
            LOGGER.error("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (!event.isFromType(ChannelType.PRIVATE)) return;
        if (event.getUser().isBot()) return;
        if (!event.getEmoji().asUnicode().equals(Emoji.fromUnicode("🗑"))) return;
        if (!configFile.OwnerID.contains(event.getUser().getIdLong())) return;

        event.retrieveMessage().queue(msg -> {
            String[] spl = msg.getContentRaw().split(" - ");
            if (spl.length != 3) return;

            jdaBot.openPrivateChannelById(spl[0]).queue(channel -> {
                channel.deleteMessageById(spl[2]).queue();
            });

            msg.delete().queue();
        });
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromType(ChannelType.PRIVATE)) return;
        if (event.getAuthor().isBot()) return;

        User author = event.getAuthor();
        Message msg = event.getMessage();

        if (configFile.OwnerID.contains(author.getIdLong())) {
            // author
            if (msg.getMessageReference() == null || !msg.getReferencedMessage().getAuthor().isBot())
                return; // (not reply to a message) or (not reply to a message which sent by bot)

            Message repliedMessage = msg.getReferencedMessage();
            if (repliedMessage.getEmbeds().isEmpty())
                return; // not a dm message from other person

            String[] footer = repliedMessage.getEmbeds().get(0).getFooter().getText().split(" - ");
            String userID = footer[0];
            String messageID = footer[1];

            jdaBot.openPrivateChannelById(userID).queue(channel -> {
                if (msg.getContentRaw().startsWith("[REPLY]")) {
                    channel.retrieveMessageById(messageID).queue(message -> {
                        message.reply(msg.getContentRaw().substring(7)).queue(newMsg -> {
                            msg.reply(userID + " - " + messageID + " - " + newMsg.getId()).queue(log -> {
                                log.addReaction(Emoji.fromUnicode("🗑")).queue();
                            });
                        });
                    });
                } else {
                    channel.sendMessage(msg.getContentRaw()).queue(newMsg -> {
                        msg.reply(userID + " - " + messageID + " - " + newMsg.getId()).queue(log -> {
                            log.addReaction(Emoji.fromUnicode("🗑")).queue();
                        });
                    });
                }
            });

            return;
        }

        boolean haveField = false;
        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(author.getGlobalName() + " (" + author.getName() + ')',
                        null,
                        author.getAvatarUrl())
                .setTimestamp(OffsetDateTime.now())
                .setFooter(author.getId() + " - " + msg.getId())
                .setColor(Color.GREEN);


        if (!msg.getContentRaw().isEmpty()) {
            builder.addField("文字內容", msg.getContentRaw(), false);
            haveField = true;
        }


        if (!event.getMessage().getAttachments().isEmpty()) {
            for (Message.Attachment i : event.getMessage().getAttachments()) {
                if (haveField) builder.addBlankField(false);
                else haveField = true;

                if (i.isImage()) {
                    // image
                    builder.addField("圖片" + (i.isSpoiler() ? "(暴雷訊息)" : ""),
                            (i.getDescription() == null ? "" : i.getDescription() + '\n') +
                                    '(' + i.getWidth() + 'x' + i.getHeight() + ')' +
                                    "\n[" + i.getFileName() + "](" + i.getProxyUrl() + ')',
                            false);
                } else if (i.isVideo()) {
                    // video
                    builder.addField("影片" + (i.isSpoiler() ? "(暴雷訊息)" : ""),
                            (i.getDescription() == null ? "" : i.getDescription() + '\n') +
                                    '(' + i.getWidth() + 'x' + i.getHeight() + ')' +
                                    "\n[" + i.getFileName() + "](" + i.getProxyUrl() + ')',
                            false);


                } else if (i.getDuration() != 0) {
                    // voice
                    builder.addField("音訊" + (i.isSpoiler() ? "(暴雷訊息)" : ""),
                            (i.getDescription() == null ? "" : i.getDescription() + '\n') +
                                    "\n[" + i.getFileName() + "](" + i.getProxyUrl() + ')',
                            false);

                } else {
                    // file
                    builder.addField("檔案" + (i.isSpoiler() ? "(暴雷訊息)" : ""),
                            (i.getDescription() == null ? "" : i.getDescription() + '\n') +
                                    "[" + i.getFileName() + "](" + i.getProxyUrl() + ')',
                            false);
                }
            }
        }

        // Button reply = new ButtonImpl("xs:pcm:reply:" + authorID + ":" + event.getMessageId(), "回覆", ButtonStyle.SUCCESS, false, null);

        for (Long i : configFile.OwnerID) {
            jdaBot.retrieveUserById(i).complete().openPrivateChannel().queue(c -> {
                c.sendMessageEmbeds(builder.build()).queue();
            });
        }

        event.getMessage().addReaction(Emoji.fromUnicode("✅")).queue();
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromType(ChannelType.PRIVATE)) {

        }
    }

//    @Override
//    public void onButtonInteraction(ButtonInteractionEvent event) {
//        String[] args = event.getComponentId().split(":");
//        if (!args[0].equals("xs") || !args[1].equals("pcm")) return;
//
//        switch (args[2]) {
//            case "reply": {
//                createReplyForm(event, args);
//                break;
//            }
//
//            case "delete": {
//                User user = jdaBot.retrieveUserById(args[3]).complete();
//                if (user == null) {
//                    event.getMessage().editMessageEmbeds(createEmbed("Cannot found the user", 0xFF0000)).queue();
//                    break;
//                }
//
//                Message message = user.openPrivateChannel().complete().retrieveMessageById(args[4]).complete();
//                if (message == null) {
//                    event.getMessage().editMessageEmbeds(createEmbed("Cannot found the message", 0xFF0000)).queue();
//                    break;
//                }
//
//                message.delete().queue();
//                event.getMessage().delete().queue();
//            }
//        }
//
//    }

//    private void createReplyForm(ButtonInteractionEvent event, String[] args) {
//        TextInput titleInp = TextInput.create("title", "標題", TextInputStyle.SHORT)
//                .setPlaceholder("標題")
//                .setMinLength(1)
//                .build();
//
//        TextInput contentInp = TextInput.create("content", "內容", TextInputStyle.PARAGRAPH)
//                .setPlaceholder("內容")
//                .setMinLength(1)
//                .setRequired(false)
//                .build();
//
//        event.replyModal(
//                Modal.create("xs:pcm:reply:" + args[3] + ':' + args[4], "回覆")
//                        .addActionRows(ActionRow.of(titleInp), ActionRow.of(contentInp))
//                        .build()
//        ).queue();
//    }

//    @Override
//    public void onModalInteraction(ModalInteractionEvent event) {
//        String[] args = event.getModalId().split(":");
//        if (!args[0].equals("xs") || !args[1].equals("pcm")) return;
//        if (args[2].equals("reply")) {
//            sendReply(event, args);
//        }
//    }

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
                .setAuthor(event.getUser().getName() + " (" + event.getUser().getId() + ')',
                        null, event.getUser().getAvatarUrl())
                .setTitle(titleMap.getAsString())
                .setTimestamp(OffsetDateTime.now())
                .setFooter("回覆訊息")
                .setColor(Color.ORANGE);

        if (contentMap != null) builder.setDescription(contentMap.getAsString());

        Message message = channel.retrieveMessageById(args[4]).complete();

        long messageID;
        if (message != null) {
            messageID = message.replyEmbeds(builder.build()).complete().getIdLong();
        } else {
            messageID = channel.sendMessageEmbeds(builder.build()).complete().getIdLong();
        }

        Button delete = new ButtonImpl("xs:pcm:delete:" + user.getId() + ':' + messageID, "刪除", ButtonStyle.DANGER, false, null);

        event.getChannel().sendMessageEmbeds(builder.setFooter("回覆備份").build()).setActionRow(delete).queue();
        event.deferEdit().queue();
    }
}