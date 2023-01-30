package com.xs.chatlogger;

import com.sun.jmx.remote.util.ClassLogger;
import com.xs.loader.ClassLoader;
import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;

import java.io.File;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static com.xs.loader.MainLoader.ROOT_PATH;
import static com.xs.loader.MainLoader.jdaBot;

public class Main extends PluginEvent {
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "ChatLogger";
    private final String PATH_FOLDER_NAME = "./plugins/ChatLogger";
    private Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content
    private final Map<Long, Connection> dbConns = new HashMap<>();
    private ScheduledExecutorService threadPool;

    public Main() {
        super(true);
    }


    @Override
    public void initLoad() {
        super.initLoad();
        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class.getClassLoader());
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
                logger.warn(e.getMessage());
            }
        }
    }

    @Override
    public void unload() {
        super.unload();

        dbConns.forEach((i, j) -> {
            try {
                j.close();
            } catch (SQLException e) {
                logger.warn(e.getMessage());
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

//    @Override
//    public CommandData[] guildCommands() {
//        return new SlashCommandData[]{
//                Commands.slash("chatlogger", "commands about dynamic voice chat ")
//                        .setNameLocalizations(lang.get("register;cmd"))
//                        .setDescriptionLocalizations(lang.get("register;description"))
//                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
//                        .addSubcommands(
//                        new SubcommandData("createbychannel", "create a dynamic voice chat by channel")
//                                .setNameLocalizations(lang.get("register;subcommand;create;cmd"))
//                                .setDescriptionLocalizations(lang.get("register;subcommand;create;description"))
//                                .addOptions(
//                                        new OptionData(CHANNEL, "detect", "the channel be detected", true)
//                                                .setDescriptionLocalizations(lang.get("register;subcommand;create;options;detect"))
//                                )
//                )
//        };
//    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("chatlogger")) return;
        if (!event.getSubcommandName().equals("createbychannel")) return;
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {

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

            String getMessageSql = String.format("SELECT message_id, user_id, message FROM \"%d\" WHERE message_id = ?", channelID);
            PreparedStatement getMessage = conn.prepareStatement(getMessageSql);
            getMessage.setLong(1, messageID);
            ResultSet rs = getMessage.executeQuery();

            User messageSender = jdaBot.retrieveUserById(rs.getLong("user_id")).complete();
            String message = rs.getString("message");

            logger.log(String.format(
                    "Deleted message: %s (%s : %d)",
                    message, messageSender.getAsTag(), messageSender.getIdLong()
            ));

            String removeMessageSql = String.format("DELETE FROM \"%d\" WHERE message_id = ?", channelID);
            PreparedStatement removeMessage = conn.prepareStatement(removeMessageSql);
            removeMessage.setLong(1, messageID);
            removeMessage.executeUpdate();


            stmt.close();
        } catch (SQLException e) {
            logger.warn(e.getClass().getName() + ": " + e.getMessage() + '\n' +
                    "\tat " + Arrays.stream(e.getStackTrace())
                    .filter(i -> !i.getClassName().startsWith("org.sqlite"))
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n\tat "))
            );
        }


    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().equals(jdaBot.getSelfUser())) return;

        long guildID = event.getGuild().getIdLong();
        long channelID = event.getChannel().getIdLong();
        long userID = event.getAuthor().getIdLong();
        long messageID = event.getMessageIdLong();
        String message = event.getMessage().getContentRaw();

        try {
            Connection conn = dbConns.getOrDefault(guildID, DriverManager.getConnection(
                    "jdbc:sqlite:" + ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data/" + guildID + ".db"
            ));

            Statement stmt = conn.createStatement();
            String create = "";
            if (!stmt.executeQuery(String.format("SELECT name FROM sqlite_master WHERE type='table' AND name='%d';", channelID)).next()) {
                // create a table
                create = String.format("" +
                                "CREATE TABLE \"%d\" (" +
                                "message_id INT PRIMARY KEY  NOT NULL   ON CONFLICT FAIL, " +
                                "user_id                INT  NOT NULL, " +
                                "message               TEXT  NOT NULL  " +
                                ")",
                        channelID
                );
            }

            if (!create.equals("")) stmt.executeUpdate(create);
            String insert = String.format("INSERT INTO \"%d\" VALUES (?, ?, ?)", channelID);
            PreparedStatement createMessage = conn.prepareStatement(insert);
            createMessage.setLong(1, messageID);
            createMessage.setLong(2, userID);
            createMessage.setString(3, message);
            createMessage.executeUpdate();

            stmt.close();
        } catch (SQLException e) {
            logger.warn(e.getClass().getName() + ": " + e.getMessage() + '\n' +
                    "\tat " + Arrays.stream(e.getStackTrace())
                    .filter(i -> !i.getClassName().startsWith("org.sqlite"))
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n\tat "))
            );
        }
    }
}