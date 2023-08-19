package com.xs.memberpoint;

import com.xs.googlesheetapi.SheetRequest;
import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import javafx.util.Pair;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.*;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

public class Main extends Event {
    private static final String TAG = "MemberPoint";
    private final Map<Long, Integer> userData = new HashMap<>();
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private final String PATH_FOLDER_NAME = "plugins/MemberPoint";
    private final List<Long> adminID = new ArrayList<>();
    private final List<Role> adminRoles = new ArrayList<>();
    private MainConfig configFile;
    private LangManager langManager;
    private FileGetter getter;
    private Logger logger;
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
        loadSheet();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }

    @Override
    public void loadLang() {
        langManager = new LangManager(logger, getter, PATH_FOLDER_NAME, LANG_DEFAULT, DiscordLocale.CHINESE_TAIWAN);

        langMap = langManager.getMap();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
//                Commands.slash("refreshp", "refresh point data")
//                        .setNameLocalizations(langMap.get("register;refresh;cmd"))
//                        .setDescriptionLocalizations(langMap.get("register;refresh;description")),

                Commands.slash("point", "get current point from user")
                        .setNameLocalizations(langMap.get("register;get_point;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;get_point;description"))
                        .addOptions(
                                new OptionData(USER, "user", "user")
                                        .setDescriptionLocalizations(langMap.get("register;get_point;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

                Commands.slash("add_point", "add point to user")
                        .setNameLocalizations(langMap.get("register;add_point;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;add_point;description"))
                        .addOptions(
                        new OptionData(INTEGER, "value", "value", true)
                                .setDescriptionLocalizations(langMap.get("register;add_point;options;value")),
                        new OptionData(USER, "user", "user")
                                .setDescriptionLocalizations(langMap.get("register;add_point;options;user"))),

                Commands.slash("remove_point", "remove point from user")
                        .setNameLocalizations(langMap.get("register;remove_point;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;remove_point;description"))
                        .addOptions(
                        new OptionData(INTEGER, "value", "value", true)
                                .setDescriptionLocalizations(langMap.get("register;remove_point;options;value")),
                        new OptionData(USER, "user", "user")
                                .setDescriptionLocalizations(langMap.get("register;remove_point;options;user"))),

                Commands.slash("set_point", "set point to user")
                        .setNameLocalizations(langMap.get("register;set_point;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;set_point;description"))
                        .addOptions(
                        new OptionData(INTEGER, "value", "value", true)
                                .setDescriptionLocalizations(langMap.get("register;set_point;options;value")),
                        new OptionData(USER, "user", "user")
                                .setDescriptionLocalizations(langMap.get("register;set_point;options;user"))),
        };
    }

    @Override
    public void loadConfigFile() {
        try (InputStream inputStream = getter.readInputStreamOrDefaultFromSource("config.yml")) {
            if (inputStream == null) return;
            configFile = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader()))
                    .loadAs(inputStream, MainConfig.class);
            logger.log("Setting File Loaded Successfully");
        } catch (IOException e) {
            logger.warn("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
            throw new RuntimeException(e);
        }

        adminID.addAll(Arrays.asList(configFile.adminID));

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
//                case "refreshp": {
//                    if (permissionCheck(event.getMember())) {
//                        event.getHook().editOriginalEmbeds(noPermissionEmbed).queue();
//                        return;
//                    }
//
//                    loadSheet();
//                    event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;refresh_success", local), 0x00FFFF)).queue();
//                    break;
//                }

                case "point": {
                    User user = getUser(event);
                    loadSheet();
                    event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                            String.valueOf(userData.get(user.getIdLong()) == null ? 0 : userData.get(user.getIdLong())), 0x00FFFF)).queue();
                    break;
                }

                case "add_point": {
                    if (permissionCheck(event.getMember())) {
                        event.getHook().editOriginalEmbeds(noPermissionEmbed).queue();
                        return;
                    }
                    loadSheet();
                    User user = getUser(event);
                    int value = event.getOption("value").getAsInt();

                    value += userData.getOrDefault(user.getIdLong(), 0);
                    userData.put(user.getIdLong(), value);
                    update(user.getIdLong(), value);
                    event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                            langManager.get("runtime;current_point", local).replace("%point%", String.valueOf(value)), 0x00FFFF)).queue();
                    break;
                }

                case "remove_point": {
                    if (permissionCheck(event.getMember())) {
                        event.getHook().editOriginalEmbeds(noPermissionEmbed).queue();
                        return;
                    }
                    loadSheet();
                    User user = getUser(event);
                    int value = event.getOption("value").getAsInt();

                    value = userData.getOrDefault(user.getIdLong(), 0) - value;
                    userData.put(user.getIdLong(), value);
                    update(user.getIdLong(), value);

                    event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                            langManager.get("runtime;current_point", local).replace("%point%", String.valueOf(value)), 0x00FFFF)).queue();

                    break;
                }

                case "set_point": {
                    if (permissionCheck(event.getMember())) {
                        event.getHook().editOriginalEmbeds(noPermissionEmbed).queue();
                        return;
                    }

                    loadSheet();
                    User user = getUser(event);
                    int value = event.getOption("value").getAsInt();

                    userData.put(user.getIdLong(), value);
                    update(user.getIdLong(), value);

                    event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                            langManager.get("runtime;current_point", local).replace("%point%", String.valueOf(value)), 0x00FFFF)).queue();
                    break;
                }
            }
        }
    }

    private boolean permissionCheck(Member member) {
        if (adminID.contains(member.getIdLong()))
            return false;

        List<Role> tmp = new ArrayList<>(member.getRoles());
        tmp.retainAll(adminRoles);

        return tmp.isEmpty();
    }

    private void loadSheet() {
        try {
            userData.clear();
            sheet.refresh(configFile.sheetID, configFile.sheetLabel + "!A2:B");
            List<List<Object>> data = sheet.getData();
            if (data != null)
                for (List<Object> datum : data) {
                    userData.put(Long.parseLong((String) datum.get(0)), Integer.parseInt((String) datum.get(1)));
                }

        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
    }

    private void update(final long userID, final int totalPoint) {
        try {
            Pair<Integer, Integer> pos = sheet.where(configFile.sheetLabel + "!A2:A", String.valueOf(userID));

            List<List<Object>> tmp = new ArrayList<>();


            if (pos != null) {
                tmp.add(Collections.singletonList(totalPoint));
                sheet.write(tmp, configFile.sheetLabel + "!B" + (pos.getValue() + 2) + ":B", SheetRequest.ValueInputOption.RAW);
            } else {
                tmp.add(Arrays.asList(String.valueOf(userID), totalPoint));
                sheet.append_down(tmp, configFile.sheetLabel + "!A2:A2",
                        SheetRequest.ValueInputOption.RAW
                );
            }
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
    }

    private User getUser(SlashCommandInteractionEvent event) {
        if (event.getOption("user") != null)
            if (adminID.contains(event.getUser().getIdLong()))
                return event.getOption("user").getAsUser();

        return event.getUser();
    }
}