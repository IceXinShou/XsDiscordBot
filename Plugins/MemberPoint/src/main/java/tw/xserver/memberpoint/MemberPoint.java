package tw.xserver.memberpoint;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import tw.xserver.api.google.sheet.SheetRequest;
import tw.xserver.loader.Main;
import tw.xserver.loader.lang.LangManager;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.*;

import static net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN;
import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;
import static tw.xserver.loader.base.Loader.ROOT_PATH;
import static tw.xserver.loader.util.EmbedCreator.createEmbed;

public class MemberPoint extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final String PATH_FOLDER_NAME = "plugins/MemberPoint";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private Language lang;

    private final Map<Long, Integer> userData = new HashMap<>();
    private final List<Long> adminID = new ArrayList<>();
    private final List<Role> adminRoles = new ArrayList<>();
    private MainConfig configFile;
    private SheetRequest sheet;

    public MemberPoint() {
        super(true);

        reloadAll();
        LOGGER.info("loaded MemberPoint");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded MemberPoint");
    }

    @Override
    public void reloadAll() {
        reloadConfigFile();
        reloadLang();
        loadSheet();
    }

    @Override
    public void reloadConfigFile() {
        try {
            getter = new FileGetter(FOLDER, MemberPoint.class);
            sheet = new SheetRequest();
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }

        try (InputStream inputStream = getter.readInputStream("config.yml")) {
            if (inputStream == null) return;
            configFile = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader(), new LoaderOptions()))
                    .loadAs(inputStream, MainConfig.class);
            LOGGER.info("setting file loaded successfully");
        } catch (IOException e) {
            LOGGER.error("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
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
                 NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("point", "get current point from member")
                        .setNameLocalizations(lang.register.get_point.name)
                        .setDescriptionLocalizations(lang.register.get_point.description)
                        .addOptions(
                                new OptionData(USER, "member", "member")
                                        .setNameLocalizations(lang.register.get_point.options.member.name)
                                        .setDescriptionLocalizations(lang.register.get_point.options.member.description))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

                Commands.slash("add-point", "add point to member")
                        .setNameLocalizations(lang.register.add_point.name)
                        .setDescriptionLocalizations(lang.register.add_point.description)
                        .addOptions(
                        new OptionData(INTEGER, "value", "value", true)
                                .setNameLocalizations(lang.register.add_point.options.value.name)
                                .setDescriptionLocalizations(lang.register.add_point.options.value.description),
                        new OptionData(USER, "member", "member")
                                .setNameLocalizations(lang.register.add_point.options.member.name)
                                .setDescriptionLocalizations(lang.register.add_point.options.member.description)),

                Commands.slash("remove-point", "remove point from member")
                        .setNameLocalizations(lang.register.remove_point.name)
                        .setDescriptionLocalizations(lang.register.remove_point.description)
                        .addOptions(
                        new OptionData(INTEGER, "value", "value", true)
                                .setNameLocalizations(lang.register.remove_point.options.value.name)
                                .setDescriptionLocalizations(lang.register.remove_point.options.value.description),
                        new OptionData(USER, "member", "member")
                                .setNameLocalizations(lang.register.remove_point.options.member.name)
                                .setDescriptionLocalizations(lang.register.remove_point.options.member.description)),

                Commands.slash("set-point", "set point to member")
                        .setNameLocalizations(lang.register.set_point.name)
                        .setDescriptionLocalizations(lang.register.set_point.description)
                        .addOptions(
                        new OptionData(INTEGER, "value", "value", true)
                                .setNameLocalizations(lang.register.set_point.options.value.name)
                                .setDescriptionLocalizations(lang.register.set_point.options.value.description),
                        new OptionData(USER, "member", "member")
                                .setNameLocalizations(lang.register.set_point.options.member.name)
                                .setDescriptionLocalizations(lang.register.set_point.options.member.description)),
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
//                case "refreshp": {
//                    if (permissionCheck(event.getMember())) {
//                        event.getHook().editOriginalEmbeds(noPermissionEmbed).queue();
//                        return;
//                    }
//
//                    loadSheet();
//                    event.getHook().editOriginalEmbeds(createEmbed(lang.runtime;refresh_success.get(locale), 0x00FFFF)).queue();
//                    break;
//                }

                case "point": {
                    User user = getUser(event);
                    loadSheet();
                    event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                            String.valueOf(userData.get(user.getIdLong()) == null ? 0 : userData.get(user.getIdLong())), 0x00FFFF)).queue();
                    break;
                }

                case "add-point": {
                    if (permissionCheck(event.getMember())) {
                        event.getHook().editOriginalEmbeds(noPermissionEmbed).queue();
                        return;
                    }
                    loadSheet();
                    User user = getUser(event);
                    int value = userData.merge(user.getIdLong(), event.getOption("value").getAsInt(), Integer::sum);
                    update(user.getIdLong(), value);
                    event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                            lang.runtime.successes.current_point.get(locale).replace("%point%", String.valueOf(value)), 0x00FFFF)).queue();
                    break;
                }

                case "remove-point": {
                    if (permissionCheck(event.getMember())) {
                        event.getHook().editOriginalEmbeds(noPermissionEmbed).queue();
                        return;
                    }
                    loadSheet();
                    User user = getUser(event);

                    int value = userData.merge(user.getIdLong(), -(event.getOption("value").getAsInt()), Integer::sum);
                    update(user.getIdLong(), value);

                    event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                            lang.runtime.successes.current_point.get(locale).replace("%point%", String.valueOf(value)), 0x00FFFF)).queue();

                    break;
                }

                case "set-point": {
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
                            lang.runtime.successes.current_point.get(locale).replace("%point%", String.valueOf(value)), 0x00FFFF)).queue();
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
            LOGGER.error(e.getMessage());
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
            LOGGER.error(e.getMessage());
        }
    }

    private User getUser(SlashCommandInteractionEvent event) {
        if (event.getOption("member") != null)
            if (adminID.contains(event.getUser().getIdLong()))
                return event.getOption("user").getAsUser();

        return event.getUser();
    }
}