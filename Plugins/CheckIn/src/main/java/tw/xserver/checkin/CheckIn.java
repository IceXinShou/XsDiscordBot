package tw.xserver.checkin;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import tw.xserver.api.google.sheet.SheetRequest;
import tw.xserver.loader.lang.LangManager;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.*;

import static net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;
import static tw.xserver.loader.base.Loader.ROOT_PATH;
import static tw.xserver.loader.util.EmbedCreator.createEmbed;

public class CheckIn extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckIn.class);
    private static final String PATH_FOLDER_NAME = "plugins/CheckIn";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private Language lang;

    private final List<Long> adminID = new ArrayList<>();
    private final List<Role> adminRoles = new ArrayList<>();
    private final List<Pair<Long, Long>> announcements = new ArrayList<>();
    private MainConfig configFile;
    private SheetRequest sheet;

    public CheckIn() {
        super(true);

        try {
            sheet = new SheetRequest();
        } catch (IOException | GeneralSecurityException e) {
            LOGGER.error("sheet init failed");
            LOGGER.error(e.getMessage());
        }

        reloadAll();
        LOGGER.info("loaded CheckIn");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded CheckIn");
    }

    @Override
    public void reloadAll() {
        reloadConfigFile();
        reloadLang();
        loadAnnouncements();
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, CheckIn.class);

        try (InputStream inputStream = getter.readInputStream("config.yml")) {
            if (inputStream == null) return;
            configFile = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader(), new LoaderOptions()))
                    .loadAs(inputStream, MainConfig.class);
            LOGGER.info("setting file loaded successfully");
        } catch (IOException e) {
            LOGGER.error("please configure /" + PATH_FOLDER_NAME + "/config.yml");
            throw new RuntimeException(e);
        }

        adminID.addAll(Arrays.asList(configFile.adminID));

        LOGGER.info("setting file loaded successfully");
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

            LOGGER.error(e.getMessage());
        }
    }

    private Pair<Long, Long> getDataFromSheet(String s) {
        String[] tmp = s.substring(s.lastIndexOf(' ') + 2, s.length() - 1).split(";");
        return new Pair<>(Long.parseLong(tmp[0]), Long.parseLong(tmp[1]));
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("announcement", "mark a message which will be checked in")
                        .setNameLocalizations(lang.register.announcement.name)
                        .setDescriptionLocalizations(lang.register.announcement.description)
                        .addOptions(
                        new OptionData(STRING, "id", "message id", true)
                                .setNameLocalizations(lang.register.announcement.options.id.name)
                                .setDescriptionLocalizations(lang.register.announcement.options.id.description)),

                Commands.slash("check", "checkin for a announced message")
                        .setNameLocalizations(lang.register.check.name)
                        .setDescriptionLocalizations(lang.register.check.description)
                        .addOptions(
                                new OptionData(STRING, "id", "what message you would checkin?", true)
                                        .setNameLocalizations(lang.register.check.options.content.name)
                                        .setDescriptionLocalizations(lang.register.check.options.content.description),
                                new OptionData(STRING, "content", "what would you want?", true)
                                        .setNameLocalizations(lang.register.check.options.content.name)
                                        .setDescriptionLocalizations(lang.register.check.options.content.description))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),
        };
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
            final DiscordLocale locale = event.getUserLocale();

            if (event.getGuild().getIdLong() != configFile.guildID) {
                event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.wrong_guild.get(locale), 0xFF0000)).queue();
                return;
            }

            final MessageEmbed noPermissionEmbed = createEmbed(lang.runtime.errors.no_permission.get(locale), 0xFF0000);

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
                        event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.wrong_id_input.get(locale), 0xFF0000)).queue();
                        return;
                    }

                    try {
                        message = event.getChannel().retrieveMessageById(messageID).complete();
                    } catch (Exception e) {
                        event.getHook().editOriginalEmbeds(createEmbed(
                                lang.runtime.errors.check_message_get_failed.get(locale).replace("%id%", String.valueOf(messageID)),
                                0xFF0000)).queue();
                        return;
                    }

                    loadAnnouncements();
                    if (announcements.stream().anyMatch(i -> i.equals(new Pair<>(event.getChannel().getIdLong(), messageID)))) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.already_announced.get(locale), 0xFF0000)).queue();
                        return;
                    }
                    addAnnouncement(event.getChannel().getIdLong(), messageID, message.getContentDisplay());
                    event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.successes.announce_success.get(locale), 0x00FFFF)).queue();
                    break;
                }

                case "check": {
                    loadAnnouncements();
                    long messageID;
                    try {
                        messageID = event.getOption("id").getAsLong();
                    } catch (Exception e) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.wrong_id_input.get(locale), 0xFF0000)).queue();
                        return;
                    }

                    try {
                        event.getChannel().retrieveMessageById(messageID).complete();
                    } catch (Exception e) {
                        event.getHook().editOriginalEmbeds(createEmbed(
                                lang.runtime.errors.check_message_get_failed.get(locale).replace("%id%", String.valueOf(messageID)),
                                0xFF0000)).queue();
                        return;
                    }

                    if (announcements.stream().noneMatch(i -> i.equals(new Pair<>(event.getChannel().getIdLong(), messageID)))) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.not_announced_message.get(locale), 0xFF0000)).queue();
                        return;
                    }

                    check(event.getUser().getIdLong(), event.getChannel().getIdLong(), messageID, event.getOption("content").getAsString());
                    event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.successes.checkin_success.get(locale), 0x00FFFF)).queue();
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
            LOGGER.error("sheet write failed");
            LOGGER.error(e.getMessage());
        }
    }

    private void check(long userID, long channelID, long messageID, String content) {
        try {
            List<List<Object>> tmp = new ArrayList<>();
            Pair<Integer, Integer> userPos = sheet.where(configFile.sheetLabel + "!A2:A", String.valueOf(userID));

            int annPos = announcements.indexOf(new Pair<>(channelID, messageID));
            char annChr;
            if (annPos == -1) {
                LOGGER.error("cannot found announcement index in java data");
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
            LOGGER.error(e.getMessage());
        }
    }


    private boolean permissionCheck(Member member) {
        if (adminID.contains(member.getIdLong()))
            return false;

        List<Role> tmp = new ArrayList<>(member.getRoles());
        tmp.retainAll(adminRoles);

        return tmp.isEmpty();
    }
}