package tw.xserver.logger.chat;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.lang.LangManager;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN;
import static tw.xserver.loader.base.Loader.ROOT_PATH;
import static tw.xserver.loader.base.Loader.jdaBot;
import static tw.xserver.loader.util.GlobalUtil.*;

public class ChatLogger extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatLogger.class);
    private static final String PATH_FOLDER_NAME = "plugins/ChatLogger";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private Language lang;

    private final Map<Long, Connection> DB_CONNS = new HashMap<>();
    private final JsonManager MANAGER = new JsonManager();
    private ButtonSystem buttonSystem;

    public ChatLogger() {
        super(true);

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            sqlErrorPrinter(e);
        }

        reloadAll();
        LOGGER.info("loaded ChatLogger");
    }

    @Override
    public void unload() {
        DB_CONNS.forEach((i, j) -> {
            try {
                j.close();
            } catch (SQLException e) {
                sqlErrorPrinter(e);
            }
        });

        LOGGER.info("unLoaded ChatLogger");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, ChatLogger.class);

        File f = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME + "/data");
        if (f.mkdirs()) {
            LOGGER.info("default data folder created");
        }


        File[] files = f.listFiles();
        if (files == null) return;
        for (File i : files) {
            String extensionName = getExtensionByName(i.getName());
            if (extensionName == null || !extensionName.equals("db")) continue;

            try {
                DB_CONNS.put(Long.parseLong(i.getName().substring(0, i.getName().indexOf('.'))), DriverManager.getConnection(
                        "jdbc:sqlite:" + ROOT_PATH + '/' + PATH_FOLDER_NAME + "/data/" + i.getName()
                ));
            } catch (SQLException e) {
                sqlErrorPrinter(e);
            }
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
                Commands.slash("chat-logger", "commands about chat logger")
                        .setNameLocalizations(lang.register.name)
                        .setDescriptionLocalizations(lang.register.description)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        new SubcommandData("setting", "set chat log in this channel")
                                .setNameLocalizations(lang.register.subcommand.setting.name)
                                .setDescriptionLocalizations(lang.register.subcommand.setting.description)
                )
        };
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        MANAGER.init();
        buttonSystem = new ButtonSystem(lang, MANAGER);
    }

    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        if (checkCommand(event, "chat-logger")) return;
        if (event.getSubcommandName() == null) return;

        if (event.getSubcommandName().equals("setting")) {
            buttonSystem.setting(event);
        }
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("chat-logger")) return;


        switch (args[2]) {
            case "white":
            case "black": {
                buttonSystem.select(event, args);
                break;
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("chat-logger")) return;
        DiscordLocale locale = event.getUserLocale();

        switch (args[2]) {
            case "toggle": {
                buttonSystem.toggle(event, args, locale);
                break;
            }

            case "black":
            case "white": {
                buttonSystem.createSel(event, args, locale);
                break;
            }

            case "delete": {
                buttonSystem.delete(event, args, locale);
                break;
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getAuthor().equals(jdaBot.getSelfUser())) return;

        long guildID = event.getGuild().getIdLong();
        long channelID = event.getChannel().getIdLong();
        long userID = event.getAuthor().getIdLong();
        long messageID = event.getMessageIdLong();
        String messageStr;

        messageStr = getMessageOrEmbed(event.getMessage());

        try {
            Connection conn;
            if (DB_CONNS.containsKey(guildID)) {
                conn = DB_CONNS.get(guildID);
            } else {
                conn = DriverManager.getConnection(
                        "jdbc:sqlite:" + ROOT_PATH + '/' + PATH_FOLDER_NAME + "/data/" + guildID + ".db"
                );
                DB_CONNS.put(guildID, conn);
            }


            try (Statement stmt = conn.createStatement()) {
                if (!stmt.executeQuery(String.format("SELECT name FROM sqlite_master WHERE type='table' AND name='%d';", channelID)).next()) {
                    // create a table
                    stmt.executeUpdate(String.format("CREATE TABLE '%d' (" +
                                    "message_id INT PRIMARY KEY  NOT NULL   ON CONFLICT FAIL, " +
                                    "user_id                INT  NOT NULL, " +
                                    "message               TEXT  NOT NULL  " +
                                    ")",
                            channelID
                    ));
                }
            }
            String insert = String.format("INSERT INTO '" +
                    "%d' VALUES (?, ?, ?)", channelID);
            PreparedStatement createMessage = conn.prepareStatement(insert);
            createMessage.setLong(1, messageID);
            createMessage.setLong(2, userID);
            createMessage.setString(3, messageStr);
            createMessage.executeUpdate();
        } catch (SQLException e) {
            sqlErrorPrinter(e);
        }
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getAuthor().equals(jdaBot.getSelfUser())) return;
        DiscordLocale locale = event.getGuild().getLocale();
        long guildID = event.getGuild().getIdLong();
        long messageID = event.getMessageIdLong();
        long channelID = event.getChannel().getIdLong();

        try {
            Connection conn = DB_CONNS.computeIfAbsent(guildID, k -> {
                try {
                    return DriverManager.getConnection(
                            "jdbc:sqlite:" + ROOT_PATH + '/' + PATH_FOLDER_NAME + "/data/" + guildID + ".db"
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });


            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = findDataInTable(stmt, channelID, messageID)) {
                    if (rs == null) return;

                    User sender = event.getAuthor();
                    String beforeMessageStr = rs.getString("message");
                    String afterMessageStr = getMessageOrEmbed(event.getMessage());

                    String update = String.format("UPDATE '%d' SET message = ? WHERE message_id= ?", channelID);
                    PreparedStatement createMessage = conn.prepareStatement(update);
                    createMessage.setString(1, afterMessageStr);
                    createMessage.setLong(2, messageID);
                    createMessage.executeUpdate();

                    MANAGER.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                            (i, j) -> {
                                if (j.contains(channelID, ChannelSetting.DetectType.UPDATE)) {
                                    GuildChannel sendChannel = event.getGuild().getGuildChannelById(i);

                                    if (sendChannel != null) {
                                        String title = getChannelTitle(event.getChannel());

                                        EmbedBuilder builder = new EmbedBuilder()
                                                .setAuthor(getNickOrName(sender, event.getGuild()), null, sender.getAvatarUrl())
                                                .setTitle(title)
                                                .addField(lang.runtime.log.update.before.get(locale), beforeMessageStr, false)
                                                .addBlankField(false)
                                                .addField(lang.runtime.log.update.after.get(locale), afterMessageStr, false)
                                                .setFooter(lang.runtime.log.update.footer.get(locale))
                                                .setTimestamp(OffsetDateTime.now())
                                                .setColor(0xFFDB00);

                                        if (sendChannel instanceof TextChannel) {
                                            ((TextChannel) sendChannel).sendMessageEmbeds(builder.build()).queue();
                                        } else if (sendChannel instanceof VoiceChannel) {
                                            ((VoiceChannel) sendChannel).sendMessageEmbeds(builder.build()).queue();
                                        } else {
                                            LOGGER.warn("unknown channel type! : " + sendChannel.getType());
                                        }
                                    }
                                }
                            }
                    );
                }
            }

        } catch (Exception e) {
            sqlErrorPrinter(e);
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (!event.isFromGuild()) return;

        DiscordLocale locale = event.getGuild().getLocale();
        long guildID = event.getGuild().getIdLong();
        long messageID = event.getMessageIdLong();
        long channelID = event.getChannel().getIdLong();

        Connection conn = DB_CONNS.computeIfAbsent(guildID, k -> {
            try {
                return DriverManager.getConnection(
                        "jdbc:sqlite:" + ROOT_PATH + '/' + PATH_FOLDER_NAME + "/data/" + guildID + ".db"
                );
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        try (Statement stmt = conn.createStatement()) {
            try (ResultSet rs = findDataInTable(stmt, channelID, messageID)) {
                if (rs == null) return;

                User messageSender;
                try {
                    messageSender = getUserById(rs.getLong("user_id"));
                } catch (ErrorResponseException e) {
                    LOGGER.warn("無法取得發送者 Id, 已跳過");
                    return;
                }

                if (messageSender.getIdLong() == jdaBot.getSelfUser().getIdLong()) {
                    return;
                }
                String messageStr = rs.getString("message");


//                    String removeMessageSql = String.format("DELETE FROM '%d' WHERE message_id = ?", channelID);
//                    PreparedStatement removeMessage = conn.prepareStatement(removeMessageSql);
//                    removeMessage.setLong(1, messageID);
//                    removeMessage.executeUpdate();
                MANAGER.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                        (i, j) -> {
                            if (j.contains(channelID, ChannelSetting.DetectType.DELETE)) {
                                GuildChannel sendChannel = event.getGuild().getGuildChannelById(i);

                                if (sendChannel != null) {
                                    String title = getChannelTitle(event.getChannel());

                                    EmbedBuilder builder = new EmbedBuilder()
                                            .setAuthor(getNickOrName(messageSender, event.getGuild()), null, messageSender.getAvatarUrl())
                                            .setTitle(title)
                                            .setDescription(messageStr)
                                            .setFooter(lang.runtime.log.delete.footer.get(locale))
                                            .setTimestamp(OffsetDateTime.now())
                                            .setColor(0xFF0000);

                                    if (sendChannel instanceof TextChannel) {
                                        ((TextChannel) sendChannel).sendMessageEmbeds(builder.build()).queue();
                                    } else if (sendChannel instanceof VoiceChannel) {
                                        ((VoiceChannel) sendChannel).sendMessageEmbeds(builder.build()).queue();
                                    } else {
                                        LOGGER.warn("unknown chat type! : " + sendChannel.getType());
                                    }
                                }
                            }
                        }
                );
            }
        } catch (SQLException e) {
            sqlErrorPrinter(e);
        }
    }

    @Nullable
    private String getChannelTitle(MessageChannelUnion eventChannel) {
        String channelCategoryName;
        String channelName;

        if (eventChannel.getType().isThread()) {
            ThreadChannel channel = eventChannel.asThreadChannel();
            channelCategoryName = channel.getParentChannel().getName();
            channelName = channel.getName();
        } else if (eventChannel.getType().isMessage()) {
            StandardGuildChannel channel = (StandardGuildChannel) eventChannel;
            channelCategoryName = channel.getParentCategory() == null ?
                    null : channel.getParentCategory().getName();
            channelName = channel.getName();
        } else {
            LOGGER.warn("unknown chat type! : " + eventChannel.getType());
            return null;
        }

        return channelCategoryName == null ?
                (channelName) :
                (channelCategoryName + " > " + channelName);
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        long guildID = event.getGuild().getIdLong();

        File file = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME + "/data/" + guildID + ".db");
        if (file.exists())
            file.delete();
    }

    String getMessageOrEmbed(Message message) {

        if (message.getEmbeds().isEmpty()) {
            // if it's default message
            return message.getContentRaw();
        } else {
            // if message is an embed
            StringBuilder builder = new StringBuilder("Deleted message: \n");
            for (MessageEmbed embed : message.getEmbeds()) {
                builder.append((embed.getAuthor() == null ? "" : embed.getAuthor().getName())).append('\n')
                        .append(embed.getTitle()).append(';').append(embed.getDescription());
                for (MessageEmbed.Field field : embed.getFields()) {
                    builder.append('\n').append(field.getName()).append(';').append(field.getValue());
                }
            }
            return builder.toString();
        }
    }

    @Nullable
    ResultSet findDataInTable(Statement stmt, long channelID, long messageID) {
        ResultSet rs;
        try {
            rs = stmt.executeQuery(
                    String.format("SELECT message_id, user_id, message FROM '%d' WHERE message_id = '%d'", channelID, messageID)
            );
        } catch (Exception e) {
            LOGGER.error("the table which is named as channel_id is not exist");
            return null;
        }

        try {
            if (rs.getLong("user_id") == 0) {
                LOGGER.error("cannot get message history from table");
                return null;
            }
        } catch (SQLException e) {
            sqlErrorPrinter(e);
        }

        return rs;
    }

    private void sqlErrorPrinter(Exception e) {
        LOGGER.error(e.getClass().getName() + ": " + e.getMessage() + '\n' +
                "\tat " + Arrays.stream(e.getStackTrace())
                .filter(i -> !i.getClassName().startsWith("org.sqlite"))
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n\tat "))
        );
    }
}