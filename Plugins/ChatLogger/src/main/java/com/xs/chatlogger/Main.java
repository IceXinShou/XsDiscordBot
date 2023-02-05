package com.xs.chatlogger;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.File;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.xs.loader.MainLoader.ROOT_PATH;
import static com.xs.loader.MainLoader.jdaBot;
import static com.xs.loader.util.UserUtil.getUserById;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;

public class Main extends PluginEvent {
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "ChatLogger";
    private final String PATH_FOLDER_NAME = "./plugins/ChatLogger";
    private Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content
    private final Map<Long, Connection> dbConns = new HashMap<>();

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
            try {
                dbConns.put(Long.parseLong(i.getName().substring(0, i.getName().indexOf('.'))), DriverManager.getConnection(
                        "jdbc:sqlite:" + ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data/" + i.getName()
                ));
            } catch (SQLException e) {
                sqlErrorPrinter(e);
            }
        }
    }

    @Override
    public void unload() {

        dbConns.forEach((i, j) -> {
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
        LangGetter langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, this.getClass());

        // expert files
        langGetter.exportDefaultLang();
        lang = langGetter.readLangFileData();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("chatlogger", "commands about dynamic voice chat")
                        .setNameLocalizations(lang.get("register;cmd"))
                        .setDescriptionLocalizations(lang.get("register;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        new SubcommandData("create", "create chat log in the channel")
                                .setNameLocalizations(lang.get("register;subcommand;create;cmd"))
                                .setDescriptionLocalizations(lang.get("register;subcommand;create;description"))
                )
        };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("chatlogger")) return;
        if (!event.getSubcommandName().equals("create")) return;

        long channelID = event.getChannel().getIdLong();


    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().equals(jdaBot.getSelfUser())) return;

        long guildID = event.getGuild().getIdLong();
        long channelID = event.getChannel().getIdLong();
        long userID = event.getAuthor().getIdLong();
        long messageID = event.getMessageIdLong();
        String messageStr;

        messageStr = getMessageOrEmbed(event.getMessage());

        try {
            Connection conn = dbConns.getOrDefault(guildID, DriverManager.getConnection(
                    "jdbc:sqlite:" + ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data/" + guildID + ".db"
            ));

            Statement stmt = conn.createStatement();
            if (!stmt.executeQuery(String.format("SELECT name FROM sqlite_master WHERE type='table' AND name='%d';", channelID)).next()) {
                // create a table
                stmt.executeUpdate(String.format("" +
                                "CREATE TABLE \"%d\" (" +
                                "message_id INT PRIMARY KEY  NOT NULL   ON CONFLICT FAIL, " +
                                "user_id                INT  NOT NULL, " +
                                "message               TEXT  NOT NULL  " +
                                ")",
                        channelID
                ));
            }

            String insert = String.format("INSERT INTO \"%d\" VALUES (?, ?, ?)", channelID);
            PreparedStatement createMessage = conn.prepareStatement(insert);
            createMessage.setLong(1, messageID);
            createMessage.setLong(2, userID);
            createMessage.setString(3, messageStr);
            createMessage.executeUpdate();

            stmt.close();
        } catch (SQLException e) {
            sqlErrorPrinter(e);
        }
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        long guildID = event.getGuild().getIdLong();
        long messageID = event.getMessageIdLong();
        long channelID = event.getChannel().getIdLong();

        try {
            Connection conn = dbConns.getOrDefault(guildID, DriverManager.getConnection(
                    "jdbc:sqlite:" + ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data/" + guildID + ".db"
            ));

            Statement stmt = conn.createStatement();

            ResultSet rs = findDataInTable(stmt, channelID, messageID);
            if (rs == null) return;

            User messageSender = event.getAuthor();

            logger.log(String.format(
                    "Updated message: (%s : %d) \n%s -> \n%s",
                    messageSender.getAsTag(), messageSender.getIdLong(),
                    rs.getString("message"), getMessageOrEmbed(event.getMessage())
            ));


        } catch (Exception e) {
            sqlErrorPrinter(e);
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        long guildID = event.getGuild().getIdLong();
        long messageID = event.getMessageIdLong();
        long channelID = event.getChannel().getIdLong();

        try {
            Connection conn = dbConns.getOrDefault(guildID, DriverManager.getConnection(
                    "jdbc:sqlite:" + ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data/" + guildID + ".db"
            ));

            Statement stmt = conn.createStatement();

            ResultSet rs = findDataInTable(stmt, channelID, messageID);
            if (rs == null) return;

            User messageSender = getUserById(rs.getLong("user_id"));

            String messageStr = rs.getString("message");

            logger.log(String.format(
                    "Deleted message: %s (%s : %d)",
                    messageStr, messageSender.getAsTag(), messageSender.getIdLong()
            ));

            String removeMessageSql = String.format("DELETE FROM \"%d\" WHERE message_id = ?", channelID);
            PreparedStatement removeMessage = conn.prepareStatement(removeMessageSql);
            removeMessage.setLong(1, messageID);
            removeMessage.executeUpdate();


            stmt.close();
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

    ResultSet findDataInTable(Statement stmt, long channelID, long messageID) {
        ResultSet rs;
        try {
            rs = stmt.executeQuery(
                    String.format("SELECT message_id, user_id, message FROM \"%d\" WHERE message_id = \"%d\"", channelID, messageID)
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