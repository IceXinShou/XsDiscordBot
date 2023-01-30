package com.xs.memberpoint;

import com.xs.googlesheetapi.SheetRequest;
import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.Pair;
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
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.xs.googlesheetapi.Main.sheet;
import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.SlashCommandOption.USER_TAG;
import static com.xs.loader.util.SlashCommandOption.VALUE;
import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

public class Main extends PluginEvent {
    private MainConfig configFile;
    private final Map<Long, Integer> userData = new HashMap<>();
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "MemberPoint";
    private final String PATH_FOLDER_NAME = "./plugins/MemberPoint";
    private final List<Long> adminID = new ArrayList<>();
    private final List<Role> adminRoles = new ArrayList<>();
    private static Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {
        super.initLoad();
        logger = new Logger(TAG);
        new File("C:\\Users\\icexi\\IdeaProjects\\XsDiscordBot\\Server\\plugins\\MemberPoint").deleteOnExit();
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class.getClassLoader());
        loadConfigFile();
        loadLang();
        loadSheet();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        super.unload();
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
//                Commands.slash("refreshp", "refresh point data")
//                        .setNameLocalizations(lang.get("register;refresh;cmd"))
//                        .setDescriptionLocalizations(lang.get("register;refresh;description")),

                Commands.slash("point", "get current point from user")
                        .setNameLocalizations(lang.get("register;get_point;cmd"))
                        .setDescriptionLocalizations(lang.get("register;get_point;description"))
                        .addOptions(
                                new OptionData(USER, USER_TAG, "user")
                                        .setDescriptionLocalizations(lang.get("register;get_point;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

                Commands.slash("addpoint", "add point to user")
                        .setNameLocalizations(lang.get("register;add_point;cmd"))
                        .setDescriptionLocalizations(lang.get("register;add_point;description"))
                        .addOptions(
                        new OptionData(INTEGER, VALUE, "value", true)
                                .setDescriptionLocalizations(lang.get("register;add_point;options;value")),
                        new OptionData(USER, USER_TAG, "user")
                                .setDescriptionLocalizations(lang.get("register;add_point;options;user"))),

                Commands.slash("removepoint", "remove point from user")
                        .setNameLocalizations(lang.get("register;remove_point;cmd"))
                        .setDescriptionLocalizations(lang.get("register;remove_point;description"))
                        .addOptions(
                        new OptionData(INTEGER, VALUE, "value", true)
                                .setDescriptionLocalizations(lang.get("register;remove_point;options;value")),
                        new OptionData(USER, USER_TAG, "user")
                                .setDescriptionLocalizations(lang.get("register;remove_point;options;user"))),

                Commands.slash("setpoint", "set point to user")
                        .setNameLocalizations(lang.get("register;set_point;cmd"))
                        .setDescriptionLocalizations(lang.get("register;set_point;description"))
                        .addOptions(
                        new OptionData(INTEGER, VALUE, "value", true)
                                .setDescriptionLocalizations(lang.get("register;set_point;options;value")),
                        new OptionData(USER, USER_TAG, "user")
                                .setDescriptionLocalizations(lang.get("register;set_point;options;user"))),
        };
    }

    @Override
    public void loadConfigFile() {
        configFile = new Yaml(new Constructor(MainConfig.class))
                .load(getter.readYmlInputStream("config.yml", this.getClass(), PATH_FOLDER_NAME));


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
            final MessageEmbed noPermissionEmbed = createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000);

            switch (event.getName()) {
//                case "refreshp": {
//                    if (permissionCheck(event.getMember())) {
//                        event.getHook().editOriginalEmbeds(noPermissionEmbed).queue();
//                        return;
//                    }
//
//                    loadSheet();
//                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;refresh_success").get(local), 0x00FFFF)).queue();
//                    break;
//                }

                case "point": {
                    User user = getUser(event);
                    loadSheet();
                    event.getHook().editOriginalEmbeds(createEmbed(user.getAsTag(),
                            String.valueOf(userData.get(user.getIdLong()) == null ? 0 : userData.get(user.getIdLong())), 0x00FFFF)).queue();
                    break;
                }

                case "addpoint": {
                    if (permissionCheck(event.getMember())) {
                        event.getHook().editOriginalEmbeds(noPermissionEmbed).queue();
                        return;
                    }
                    loadSheet();
                    User user = getUser(event);
                    int value = event.getOption(VALUE).getAsInt();

                    value += userData.getOrDefault(user.getIdLong(), 0);
                    userData.put(user.getIdLong(), value);
                    update(user.getIdLong(), value);
                    event.getHook().editOriginalEmbeds(createEmbed(user.getAsTag(),
                            lang.get("runtime;current_point").get(local).replace("%point%", String.valueOf(value)), 0x00FFFF)).queue();
                    break;
                }

                case "removepoint": {
                    if (permissionCheck(event.getMember())) {
                        event.getHook().editOriginalEmbeds(noPermissionEmbed).queue();
                        return;
                    }
                    loadSheet();
                    User user = getUser(event);
                    int value = event.getOption(VALUE).getAsInt();

                    value = userData.getOrDefault(user.getIdLong(), 0) - value;
                    userData.put(user.getIdLong(), value);
                    update(user.getIdLong(), value);

                    event.getHook().editOriginalEmbeds(createEmbed(user.getAsTag(),
                            lang.get("runtime;current_point").get(local).replace("%point%", String.valueOf(value)), 0x00FFFF)).queue();

                    break;
                }

                case "setpoint": {
                    if (permissionCheck(event.getMember())) {
                        event.getHook().editOriginalEmbeds(noPermissionEmbed).queue();
                        return;
                    }
                    loadSheet();
                    User user = getUser(event);
                    int value = event.getOption(VALUE).getAsInt();

                    userData.put(user.getIdLong(), value);
                    update(user.getIdLong(), value);

                    event.getHook().editOriginalEmbeds(createEmbed(user.getAsTag(),
                            lang.get("runtime;current_point").get(local).replace("%point%", String.valueOf(value)), 0x00FFFF)).queue();
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

        if (tmp.size() > 0)
            return false;

        return true;
    }

    private void loadSheet() {
        try {
            userData.clear();
            sheet.refresh(configFile.sheetID, configFile.pointSheetLabel + "!A2:B");
            List<List<Object>> data = sheet.getData();
            for (List<Object> datum : data) {
                userData.put(Long.parseLong((String) datum.get(0)), Integer.parseInt((String) datum.get(1)));
            }

        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
    }

    private void update(final long userID, final int totalPoint) {
        try {
            Pair<Integer, Integer> pos = sheet.where(configFile.pointSheetLabel + "!A2:A", String.valueOf(userID));

            List<List<Object>> tmp = new ArrayList<>();


            if (pos != null) {
                tmp.add(Collections.singletonList(totalPoint));
                sheet.write(tmp, configFile.pointSheetLabel + "!B" + (pos.getValue() + 2) + ":B", SheetRequest.ValueInputOption.RAW);
            } else {
                tmp.add(Arrays.asList(String.valueOf(userID), totalPoint));
                sheet.append_down(tmp, configFile.pointSheetLabel + "!A2:A2",
                        SheetRequest.ValueInputOption.RAW
                );
            }
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
    }

    private User getUser(SlashCommandInteractionEvent event) {
        if (event.getOption(USER_TAG) != null)
            if (adminID.contains(event.getUser().getIdLong()))
                return event.getOption(USER_TAG).getAsUser();

        return event.getUser();
    }


}