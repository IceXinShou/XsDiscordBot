package com.xs.chatlogger;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.xs.loader.MainLoader.ROOT_PATH;
import static com.xs.loader.MainLoader.jdaBot;
import static com.xs.loader.util.GlobalUtil.*;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;

public class Main extends PluginEvent {
    private LangManager langManager;
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "ChatLogger";
    private final String PATH_FOLDER_NAME = "plugins/ChatLogger";
    private Map<String, Map<DiscordLocale, String>> langMap; // Label, Local, Content
    private final Map<Long, Connection> DB_CONNS = new HashMap<>();
    private final JsonManager MANAGER = new JsonManager();
    private ButtonSystem buttonSystem;

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {
        logger = new Logger(TAG);
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            sqlErrorPrinter(e);
        }
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        loadLang();

        initData();
        logger.log("Loaded");
    }

    private void initData() {
        File f = new File(ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data");
        f.mkdirs();

        for (File i : f.listFiles()) {
            String extensionName = getExtensionName(i.getName());
            if (extensionName == null || !extensionName.equals("db")) continue;

            try {
                DB_CONNS.put(Long.parseLong(i.getName().substring(0, i.getName().indexOf('.'))), DriverManager.getConnection(
                        "jdbc:sqlite:" + ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data/" + i.getName()
                ));
            } catch (SQLException e) {
                sqlErrorPrinter(e);
            }
        }
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

        logger.log("UnLoaded");
    }

    @Override
    public void loadLang() {
        langManager = new LangManager(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, DiscordLocale.CHINESE_TAIWAN, this.getClass());

        langMap = langManager.readLangFileDataMap();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("chat_logger", "commands about chat logger")
                        .setNameLocalizations(langMap.get("register;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        new SubcommandData("setting", "set chat log in this channel")
                                .setNameLocalizations(langMap.get("register;subcommand;setting;cmd"))
                                .setDescriptionLocalizations(langMap.get("register;subcommand;setting;description"))
                )
        };
    }

    @Override
    public void onReady(ReadyEvent event) {
        MANAGER.init();
        buttonSystem = new ButtonSystem(langManager, MANAGER);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("chat_logger")) return;
        if (event.getSubcommandName() == null) return;

        if (event.getSubcommandName().equals("setting")) {
            buttonSystem.setting(event);
        }
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("chatlogger")) return;


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
        if (!args[0].equals("xs") || !args[1].equals("chatlogger")) return;
        DiscordLocale local = event.getUserLocale();

        switch (args[2]) {
            case "toggle": {
                buttonSystem.toggle(event, args);
                break;
            }

            case "black":
            case "white": {
                buttonSystem.createSel(event, args);
                break;
            }

            case "delete": {
                buttonSystem.delete(event, args);
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
            Connection conn = DB_CONNS.getOrDefault(guildID, DriverManager.getConnection(
                    "jdbc:sqlite:" + ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data/" + guildID + ".db"
            ));

            Statement stmt = conn.createStatement();
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
            stmt.close();

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
        long guildID = event.getGuild().getIdLong();
        long messageID = event.getMessageIdLong();
        long channelID = event.getChannel().getIdLong();

        try {
            Connection conn = DB_CONNS.getOrDefault(guildID, DriverManager.getConnection(
                    "jdbc:sqlite:" + ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data/" + guildID + ".db"
            ));

            Statement stmt = conn.createStatement();

            ResultSet rs = findDataInTable(stmt, channelID, messageID);
            if (rs == null) return;

            User sender = event.getAuthor();
            String beforeMessageStr = rs.getString("message");
            String afterMessageStr = getMessageOrEmbed(event.getMessage());

            String update = String.format("UPDATE '%d' SET message = ? WHERE message_id= ?", channelID);
            PreparedStatement createMessage = conn.prepareStatement(update);
            createMessage.setString(1, afterMessageStr);
            createMessage.setLong(2, messageID);
            createMessage.executeUpdate();


            rs.close();
            stmt.close();

            MANAGER.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                    (i, j) -> {
                        if (j.contains(channelID, ChannelSetting.DetectType.UPDATE)) {
                            GuildChannel sendChannel = event.getGuild().getGuildChannelById(i);

                            if (sendChannel != null) {
                                TextChannel removeChannel = event.getChannel().asTextChannel();
                                String title = removeChannel.getParentCategory() == null ?
                                        (removeChannel.getName()) :
                                        (removeChannel.getParentCategory().getName() + " > " + removeChannel.getName());
                                EmbedBuilder builder = new EmbedBuilder()
                                        .setAuthor(getNickOrTag(sender, event.getGuild()), null, sender.getAvatarUrl())
                                        .setTitle(title)
                                        .addField("Before", beforeMessageStr, false)
                                        .addBlankField(false)
                                        .addField("After", afterMessageStr, false)
                                        .setFooter("更改訊息")
                                        .setTimestamp(OffsetDateTime.now())
                                        .setColor(0xFFDB00);

                                if (sendChannel instanceof TextChannel) {
                                    ((TextChannel) sendChannel).sendMessageEmbeds(builder.build()).queue();
                                } else if (sendChannel instanceof VoiceChannel) {
                                    ((VoiceChannel) sendChannel).sendMessageEmbeds(builder.build()).queue();
                                } else {
                                    logger.warn("unknown chat type! : " + sendChannel.getType());
                                }
                            }
                        }
                    }
            );
        } catch (Exception e) {
            sqlErrorPrinter(e);
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (!event.isFromGuild()) return;
        long guildID = event.getGuild().getIdLong();
        long messageID = event.getMessageIdLong();
        long channelID = event.getChannel().getIdLong();

        try {
            Connection conn = DB_CONNS.getOrDefault(guildID, DriverManager.getConnection(
                    "jdbc:sqlite:" + ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data/" + guildID + ".db"
            ));

            Statement stmt = conn.createStatement();

            ResultSet rs = findDataInTable(stmt, channelID, messageID);
            if (rs == null) return;

            User messageSender = getUserById(rs.getLong("user_id"));
            if (messageSender.getIdLong() == jdaBot.getSelfUser().getIdLong()) {
                rs.close();
                stmt.close();
                return;
            }
            String messageStr = rs.getString("message");

//            String removeMessageSql = String.format("DELETE FROM '%d' WHERE message_id = ?", channelID);
//            PreparedStatement removeMessage = conn.prepareStatement(removeMessageSql);
//            removeMessage.setLong(1, messageID);
//            removeMessage.executeUpdate();

            rs.close();
            stmt.close();

            MANAGER.channelSettings.getOrDefault(guildID, new HashMap<>()).forEach(
                    (i, j) -> {
                        if (j.contains(channelID, ChannelSetting.DetectType.DELETE)) {
                            GuildChannel sendChannel = event.getGuild().getGuildChannelById(i);

                            if (sendChannel != null) {
                                TextChannel removeChannel = event.getChannel().asTextChannel();
                                String title = removeChannel.getParentCategory() == null ?
                                        (removeChannel.getName()) :
                                        (removeChannel.getParentCategory().getName() + " > " + removeChannel.getName());
                                EmbedBuilder builder = new EmbedBuilder()
                                        .setAuthor(getNickOrTag(messageSender, event.getGuild()), null, messageSender.getAvatarUrl())
                                        .setTitle(title)
                                        .setDescription(messageStr)
                                        .setFooter("刪除訊息")
                                        .setTimestamp(OffsetDateTime.now())
                                        .setColor(0xFF0000);

                                if (sendChannel instanceof TextChannel) {
                                    ((TextChannel) sendChannel).sendMessageEmbeds(builder.build()).queue();
                                } else if (sendChannel instanceof VoiceChannel) {
                                    ((VoiceChannel) sendChannel).sendMessageEmbeds(builder.build()).queue();
                                } else {
                                    logger.warn("unknown chat type! : " + sendChannel.getType());
                                }
                            }
                        }
                    }
            );
        } catch (SQLException e) {
            sqlErrorPrinter(e);
        }
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        long guildID = event.getGuild().getIdLong();

        File file = new File(ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data/" + guildID + ".db");
        if (file.exists())
            file.delete();
    }

    String getMessageOrEmbed(Message message) {

        if (message.getEmbeds().size() == 0) {
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
            logger.warn("the table which is named as channel_id is not exist");
            return null;
        }

        try {
            if (rs.getLong("user_id") == 0) {
                logger.warn("cannot get message history from table");
                return null;
            }
        } catch (SQLException e) {
            sqlErrorPrinter(e);
        }

        return rs;
    }

    private void sqlErrorPrinter(Exception e) {
        logger.warn(e.getClass().getName() + ": " + e.getMessage() + '\n' +
                "\tat " + Arrays.stream(e.getStackTrace())
                .filter(i -> !i.getClassName().startsWith("org.sqlite"))
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n\tat "))
        );
    }
}