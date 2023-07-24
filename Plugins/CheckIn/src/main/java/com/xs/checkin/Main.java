package com.xs.checkin;

import com.xs.googlesheetapi.SheetRequest;
import com.xs.loader.plugin.Event;
import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import javafx.util.Pair;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;

public class Main extends Event {
    private MainConfig configFile;
    private LangManager langManager;
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "CheckIn";
    private final String PATH_FOLDER_NAME = "plugins/CheckIn";
    private final List<Long> adminID = new ArrayList<>();
    private final List<Role> adminRoles = new ArrayList<>();
    private final List<Pair<Long, Long>> announcements = new ArrayList<>();
    private Map<String, Map<DiscordLocale, String>> langMap; // Label, Local, Content
    private SheetRequest sheet;

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {


        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        try {
            sheet = new SheetRequest(logger);
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
        loadConfigFile();
        loadLang();
        loadAnnouncements();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }

    @Override
    public void loadLang() {
        langManager = new LangManager(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, DiscordLocale.CHINESE_TAIWAN, this.getClass());

        langMap = langManager.getMap();
    }

    private Pair<Long, Long> getDataFromSheet(String s) {
        String[] tmp = s.substring(s.lastIndexOf(' ') + 2, s.length() - 1).split(";");
        return new Pair<>(Long.parseLong(tmp[0]), Long.parseLong(tmp[1]));
    }

    private void loadAnnouncements() {
        try {
            announcements.clear();
            sheet.refresh(configFile.sheetID, configFile.sheetLabel);

            if (sheet.getData() == null)
                return;

            List<Object> tmp = sheet.getData().get(0);
            int tmpSize = tmp.size();

            for (int i = 1; i < tmpSize; ++i) {
                announcements.add(getDataFromSheet((String) tmp.get(i)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("announcement", "mark a message which will be checked in")
                        .setNameLocalizations(langMap.get("register;announcement;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;announcement;description"))
                        .addOptions(
                        new OptionData(STRING, "id", "message id", true)
                                .setDescriptionLocalizations(langMap.get("register;announcement;options;id"))),

                Commands.slash("check", "checkin for a announced message")
                        .setNameLocalizations(langMap.get("register;check;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;check;description"))
                        .addOptions(
                                new OptionData(STRING, "id", "what message you would checkin?", true)
                                        .setDescriptionLocalizations(langMap.get("register;check;options;content")),
                                new OptionData(STRING, "content", "what would you want?", true)
                                        .setDescriptionLocalizations(langMap.get("register;check;options;content")))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),
        };
    }

    @Override
    public void loadConfigFile() {

        InputStream inputStream = getter.readInputStreamOrDefaultFromSource("config.yml");
        if (inputStream == null) return;

        try {
            configFile = new Yaml(new Constructor(MainConfig.class)).load(inputStream);
            inputStream.close();
            logger.log("Setting File Loaded Successfully");
        } catch (IOException e) {
            logger.warn("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
            e.printStackTrace();
        }


        for (long i : configFile.adminID) adminID.add(i);

        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        if (event.getGuild().getIdLong() != configFile.guildID) return;

        Role role;
        for (long i : configFile.adminRoleID) {
            if ((role = event.getGuild().getRoleById(i)) != null) {
                adminRoles.add(role);
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.isFromGuild() && event.getGuild() != null && event.getMember() != null) {
            final DiscordLocale local = event.getUserLocale();

            if (event.getGuild().getIdLong() != configFile.guildID) {
                event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;wrong_guild", local), 0xFF0000)).queue();
                return;
            }

            final MessageEmbed noPermissionEmbed = createEmbed(langManager.get("runtime;errors;no_permission", local), 0xFF0000);

            switch (event.getName()) {
                case "announcement": {
                    if (permissionCheck(event.getMember())) {
                        event.getHook().editOriginalEmbeds(noPermissionEmbed).queue();
                        return;
                    }

                    Message message;

                    long messageID;

                    try {
                        messageID = event.getOption("id").getAsLong();
                    } catch (Exception e) {
                        event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;wrong_id_input", local), 0xFF0000)).queue();
                        return;
                    }

                    try {
                        message = event.getChannel().retrieveMessageById(messageID).complete();
                    } catch (Exception e) {
                        event.getHook().editOriginalEmbeds(createEmbed(
                                langManager.get("runtime;errors;check_message_get_failed", local).replace("%id%", String.valueOf(messageID)),
                                0xFF0000)).queue();
                        return;
                    }

                    loadAnnouncements();
                    if (announcements.stream().anyMatch(i -> i.equals(new Pair<>(event.getChannel().getIdLong(), messageID)))) {
                        event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;already_announced", local), 0xFF0000)).queue();
                        return;
                    }
                    addAnnouncement(event.getChannel().getIdLong(), messageID, message.getContentDisplay());
                    event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;announce_success", local), 0x00FFFF)).queue();
                    break;
                }

                case "check": {
                    loadAnnouncements();
                    long messageID;
                    try {
                        messageID = event.getOption("id").getAsLong();
                    } catch (Exception e) {
                        event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;wrong_id_input", local), 0xFF0000)).queue();
                        return;
                    }

                    try {
                        event.getChannel().retrieveMessageById(messageID).complete();
                    } catch (Exception e) {
                        event.getHook().editOriginalEmbeds(createEmbed(
                                langManager.get("runtime;errors;check_message_get_failed", local).replace("%id%", String.valueOf(messageID)),
                                0xFF0000)).queue();
                        return;
                    }

                    if (announcements.stream().noneMatch(i -> i.equals(new Pair<>(event.getChannel().getIdLong(), messageID)))) {
                        event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;not_announced_message", local), 0xFF0000)).queue();
                        return;
                    }

                    check(event.getUser().getIdLong(), event.getChannel().getIdLong(), messageID, event.getOption("content").getAsString());
                    event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;checkin_success", local), 0x00FFFF)).queue();
                    break;
                }
            }
        }
    }

    private void addAnnouncement(long channelID, long messageID, String displayContent) {
        announcements.add(new Pair<>(channelID, messageID));
        List<List<Object>> tmp = new ArrayList<>();
        tmp.add(Collections.singletonList(String.format("%s (%d;%d)", displayContent, channelID, messageID)));

        try {
            sheet.write(tmp,
                    configFile.sheetLabel + "!" + sheet.toUpperAlpha(sheet.getData().get(0).size()) + "1:1",
                    SheetRequest.ValueInputOption.RAW);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void check(long userID, long channelID, long messageID, String content) {
        try {
            List<List<Object>> tmp = new ArrayList<>();
            Pair<Integer, Integer> userPos = sheet.where(configFile.sheetLabel + "!A2:A", String.valueOf(userID));

            int annPos = announcements.indexOf(new Pair<>(channelID, messageID));
            char annChr;
            if (annPos == -1) {
                logger.warn("cannot found announcement index in java data");
                return;
            } else {
                annChr = sheet.toUpperAlpha(annPos + 1);
            }

            if (userPos == null) {
                // create user label
                tmp.add(Collections.singletonList(String.valueOf(userID)));
                sheet.append_down(tmp, configFile.sheetLabel + "!A2", SheetRequest.ValueInputOption.RAW);
                tmp.clear();
                userPos = sheet.where(configFile.sheetLabel + "!A2:A", String.valueOf(userID));
            }
            tmp.add(Collections.singletonList(content +
                    " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) + ")"));
            sheet.write(tmp, configFile.sheetLabel + "!" + annChr + (userPos.getValue() + 2), SheetRequest.ValueInputOption.RAW);

        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
    }


    private boolean permissionCheck(Member member) {
        if (adminID.contains(member.getIdLong()))
            return false;

        List<Role> tmp = new ArrayList<>(member.getRoles());
        tmp.retainAll(adminRoles);

        for (Role i : tmp) {
            System.out.println(i.getName());
        }

        return tmp.size() <= 0;
    }
}