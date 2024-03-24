package tw.xserver.economy;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateGlobalNameEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import tw.xserver.loader.lang.LangManager;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;
import tw.xserver.loader.util.json.JsonObjFileManager;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN;
import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;
import static net.dv8tion.jda.internal.utils.Checks.notNull;
import static tw.xserver.loader.base.Loader.ROOT_PATH;
import static tw.xserver.loader.util.EmbedCreator.createEmbed;
import static tw.xserver.loader.util.GlobalUtil.getUserById;

public class Economy extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(Economy.class);
    private static final String PATH_FOLDER_NAME = "plugins/Economy";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private Language lang;

    private final Map<Long, UserData> userData = new HashMap<>();
    private final List<UserData> moneyBoard = new ArrayList<>();
    private final List<UserData> totalBoard = new ArrayList<>();
    private MainConfig configFile;
    private JsonObjFileManager manager;

    public Economy() {
        super(true);

        reloadAll();
        LOGGER.info("loaded Economy");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded Economy");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, Economy.class);

        try (InputStream inputStream = getter.readInputStream("config.yml")) {
            if (inputStream == null) return;
            configFile = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader(), new LoaderOptions()))
                    .loadAs(inputStream, MainConfig.class);
            LOGGER.info("setting file loaded successfully");
        } catch (IOException e) {
            LOGGER.error("please configure /" + PATH_FOLDER_NAME + "/config.yml");
            throw new RuntimeException(e);
        }

        if (new File(ROOT_PATH + '/' + PATH_FOLDER_NAME + "/data").mkdirs()) {
            LOGGER.info("default data folder created");
        }

        manager = new JsonObjFileManager('/' + PATH_FOLDER_NAME + "/data/data.json");

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

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("money", "get current money from member")
                        .setNameLocalizations(lang.register.get_money.name)
                        .setDescriptionLocalizations(lang.register.get_money.description)
                        .addOptions(
                                new OptionData(USER, "member", "member")
                                        .setNameLocalizations(lang.register.get_money.options.member.name)
                                        .setDescriptionLocalizations(lang.register.get_money.options.member.description))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

                Commands.slash("money-log", "get money log from member")
                        .setNameLocalizations(lang.register.get_money_history.name)
                        .setDescriptionLocalizations(lang.register.get_money_history.description)
                        .addOptions(
                                new OptionData(USER, "member", "member")
                                        .setNameLocalizations(lang.register.get_money_history.options.member.name)
                                        .setDescriptionLocalizations(lang.register.get_money_history.options.member.description))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

                Commands.slash("add-money", "add money to member")
                        .setNameLocalizations(lang.register.add_money.name)
                        .setDescriptionLocalizations(lang.register.add_money.description)
                        .addOptions(
                                new OptionData(USER, "member", "member", true)
                                        .setNameLocalizations(lang.register.add_money.options.member.name)
                                        .setDescriptionLocalizations(lang.register.add_money.options.member.description),
                                new OptionData(INTEGER, "value", "value", true)
                                        .setNameLocalizations(lang.register.add_money.options.value.name)
                                        .setDescriptionLocalizations(lang.register.add_money.options.value.description))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("remove-money", "remove money from member")
                        .setNameLocalizations(lang.register.remove_money.name)
                        .setDescriptionLocalizations(lang.register.remove_money.description)
                        .addOptions(
                                new OptionData(USER, "member", "member", true)
                                        .setNameLocalizations(lang.register.remove_money.options.member.name)
                                        .setDescriptionLocalizations(lang.register.remove_money.options.member.description),
                                new OptionData(INTEGER, "value", "value", true)
                                        .setNameLocalizations(lang.register.remove_money.options.value.name)
                                        .setDescriptionLocalizations(lang.register.remove_money.options.value.description))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("set-money", "set money to member")
                        .setNameLocalizations(lang.register.set_money.name)
                        .setDescriptionLocalizations(lang.register.set_money.description)
                        .addOptions(
                                new OptionData(USER, "member", "member", true)
                                        .setNameLocalizations(lang.register.set_money.options.member.name)
                                        .setDescriptionLocalizations(lang.register.set_money.options.member.description),
                                new OptionData(INTEGER, "value", "value", true)
                                        .setNameLocalizations(lang.register.set_money.options.value.name)
                                        .setDescriptionLocalizations(lang.register.set_money.options.value.description))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("add-money-log", "add money log to member")
                        .setNameLocalizations(lang.register.add_money_history.name)
                        .setDescriptionLocalizations(lang.register.add_money_history.description)
                        .addOptions(
                                new OptionData(USER, "member", "member", true)
                                        .setNameLocalizations(lang.register.add_money_history.options.member.name)
                                        .setDescriptionLocalizations(lang.register.add_money_history.options.member.description),
                                new OptionData(INTEGER, "value", "value", true)
                                        .setNameLocalizations(lang.register.add_money_history.options.value.name)
                                        .setDescriptionLocalizations(lang.register.add_money_history.options.value.description))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("remove-money-log", "remove money log from member")
                        .setNameLocalizations(lang.register.remove_money_history.name)
                        .setDescriptionLocalizations(lang.register.remove_money_history.description)
                        .addOptions(
                                new OptionData(USER, "member", "member", true)
                                        .setNameLocalizations(lang.register.remove_money_history.options.member.name)
                                        .setDescriptionLocalizations(lang.register.remove_money_history.options.member.description),
                                new OptionData(INTEGER, "value", "value", true)
                                        .setNameLocalizations(lang.register.remove_money_history.options.value.name)
                                        .setDescriptionLocalizations(lang.register.remove_money_history.options.value.description))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("set-money-log", "set money log to member")
                        .setNameLocalizations(lang.register.set_money_history.name)
                        .setDescriptionLocalizations(lang.register.set_money_history.description)
                        .addOptions(
                                new OptionData(USER, "member", "member", true)
                                        .setNameLocalizations(lang.register.set_money_history.options.member.name)
                                        .setDescriptionLocalizations(lang.register.set_money_history.options.member.description),
                                new OptionData(INTEGER, "value", "value", true)
                                        .setNameLocalizations(lang.register.set_money_history.options.value.name)
                                        .setDescriptionLocalizations(lang.register.set_money_history.options.value.description))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("top-money", "get board from money")
                        .setNameLocalizations(lang.register.money_board.name)
                        .setDescriptionLocalizations(lang.register.money_board.description)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("top-money-log", "get board from log money")
                        .setNameLocalizations(lang.register.money_history_board.name)
                        .setDescriptionLocalizations(lang.register.money_history_board.description)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),
        };
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        for (String i : manager.get().keySet()) {
            User user;
            JsonObject object = manager.computeIfAbsent(i, new JsonObject()).getAsJsonObject();

            try {
                user = getUserById(Long.parseLong(i));
            } catch (ErrorResponseException e) {
                userData.put(Long.parseLong(i), new UserData("unknown (" + i + ')', Long.parseLong(i), object.get("money").getAsInt(), object.get("total").getAsInt()));
                continue;
            }

            userData.put(Long.parseLong(i), new UserData(user.getName(), Long.parseLong(i), object.get("money").getAsInt(), object.get("total").getAsInt()));
        }

        updateMoney();
        updateTotal();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        DiscordLocale locale = event.getUserLocale();
        Guild guild = event.getGuild();

        notNull(guild, "Guild");

        switch (event.getName()) {
            case "money": {
                User user = getUserID(event);

                initData(user.getIdLong(), user.getName());
                event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                        userData.get(user.getIdLong()).get() + " $", 0x00FFFF)).queue();
                break;
            }

            case "money-log": {
                User user = getUserID(event);
                initData(user.getIdLong(), user.getName());
                event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                        userData.get(user.getIdLong()).getTotal() + " $", 0x00FFFF)).queue();
                break;
            }

            case "top-money": {
                if (checkPermission(event)) {
                    return;
                }

                EmbedBuilder builder = fieldGetter(moneyBoard, true);
                event.getHook().editOriginalEmbeds(builder
                        .setTitle(lang.runtime.successes.money_board_title.get(locale))
                        .setColor(0x00FFFF)
                        .build()
                ).queue();
                break;
            }

            case "top-money-log": {
                if (checkPermission(event)) {
                    return;
                }

                EmbedBuilder builder = fieldGetter(totalBoard, false);
                event.getHook().editOriginalEmbeds(builder
                        .setTitle(lang.runtime.successes.money_history_board_title.get(locale))
                        .setColor(0x00FFFF)
                        .build()
                ).queue();
                break;
            }

            case "add-money": {
                if (checkPermission(event)) {
                    return;
                }

                User user = getUserID(event);
                int value = event.getOption("value", 0, OptionMapping::getAsInt);

                initData(user.getIdLong(), user.getName());
                int newValue = userData.get(user.getIdLong()).add(value);

                event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                        lang.runtime.successes.current_money.get(locale)
                                .replace("%money%", newValue + " $"), 0x00FFFF)).queue();

                JsonObject object = manager.getAsJsonObject(user.getId());
                object.addProperty("money", object.get("money").getAsInt() + value);
                object.addProperty("total", object.get("total").getAsInt() + value);

                manager.save();
                updateMoney();
                updateTotal();
                break;
            }

            case "remove-money": {
                if (checkPermission(event)) {
                    return;
                }

                User user = getUserID(event);
                int value = event.getOption("value", 0, OptionMapping::getAsInt);
                initData(user.getIdLong(), user.getName());
                int newValue = userData.get(user.getIdLong()).remove(value);

                event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                        lang.runtime.successes.current_money.get(locale)
                                .replace("%money%", newValue + " $"), 0x00FFFF)).queue();

                JsonObject object = manager.getAsJsonObject(user.getId());
                object.addProperty("money", object.get("money").getAsInt() - value);

                manager.save();
                updateMoney();
                break;
            }

            case "set-money": {
                if (checkPermission(event)) {
                    return;
                }

                User user = getUserID(event);
                int value = event.getOption("value", 0, OptionMapping::getAsInt);
                initData(user.getIdLong(), user.getName());
                int newValue = userData.get(user.getIdLong()).set(value);

                event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                        lang.runtime.successes.current_money.get(locale)
                                .replace("%money%", newValue + " $"), 0x00FFFF)).queue();

                manager.getAsJsonObject(user.getId())
                        .addProperty("money", value);

                manager.save();
                updateMoney();
                break;
            }
            case "add-money-log": {
                if (checkPermission(event)) {
                    return;
                }

                User user = getUserID(event);
                int value = event.getOption("value", 0, OptionMapping::getAsInt);
                initData(user.getIdLong(), user.getName());
                int newValue = userData.get(user.getIdLong()).addTotal(value);

                event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                        lang.runtime.successes.current_money_history.get(locale)
                                .replace("%log_money%", newValue + " $"), 0x00FFFF)).queue();

                JsonObject object = manager.getAsJsonObject(user.getId());
                object.addProperty("total", object.get("total").getAsInt() + value);

                manager.save();
                updateTotal();
                break;
            }

            case "remove-money-log": {
                if (checkPermission(event)) {
                    return;
                }

                User user = getUserID(event);
                int value = event.getOption("value", 0, OptionMapping::getAsInt);
                initData(user.getIdLong(), user.getName());
                int newValue = userData.get(user.getIdLong()).removeTotal(value);

                event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                        lang.runtime.successes.current_money_history.get(locale)
                                .replace("%log_money%", newValue + " $"), 0x00FFFF)).queue();

                JsonObject object = manager.getAsJsonObject(user.getId());
                object.addProperty("total", object.get("total").getAsInt() - value);

                manager.save();
                updateTotal();
                break;
            }

            case "set-money-log": {
                if (checkPermission(event)) {
                    return;
                }

                User user = getUserID(event);
                int value = event.getOption("value", 0, OptionMapping::getAsInt);
                initData(user.getIdLong(), user.getName());
                int newValue = userData.get(user.getIdLong()).setTotal(value);

                event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                        lang.runtime.successes.current_money_history.get(locale)
                                .replace("%log_money%", newValue + " $"), 0x00FFFF)).queue();

                manager.getAsJsonObject(user.getId())
                        .addProperty("total", value);

                manager.save();
                updateTotal();
                break;
            }
        }
    }

    @Override
    public void onUserUpdateGlobalName(UserUpdateGlobalNameEvent event) {
        UserData data = userData.get(event.getUser().getIdLong());
        if (data != null)
            data.setName(event.getUser().getName());
    }

    User getUserID(SlashCommandInteractionEvent event) {
        User targetUser = event.getOption("member", null, OptionMapping::getAsUser);
        if (targetUser == null) return event.getUser();

        if (Arrays.stream(configFile.OwnerID).anyMatch(i -> i.equals(event.getUser().getIdLong())))
            return targetUser;

        return event.getUser();
    }

    void updateMoney() {
        moneyBoard.clear();
        moneyBoard.addAll(userData.values().stream().sorted(Comparator.comparingInt(UserData::get).reversed()).collect(Collectors.toList()));
    }

    void updateTotal() {
        totalBoard.clear();
        totalBoard.addAll(userData.values().stream().sorted(Comparator.comparingInt(UserData::getTotal).reversed()).collect(Collectors.toList()));
    }

    EmbedBuilder fieldGetter(List<UserData> board, boolean money) {
        EmbedBuilder builder = new EmbedBuilder();
        int count = Math.min(board.size(), Math.min(configFile.BoardUserShowLimit, 25));

        for (int i = 0; i < count; ++i) {
            UserData data = board.get(i);

            builder.addField(
                    (i + 1) + ". " + data.getName(),
                    (money ? data.get() : data.getTotal()) + " $", false
            );
        }
        return builder;
    }

    private void initData(long id, String name) {
        if (!userData.containsKey(id)) {
            userData.put(id, new UserData(name, id));
            JsonObject object = manager.computeIfAbsent(String.valueOf(id), new JsonObject()).getAsJsonObject();
            object.addProperty("money", 0);
            object.addProperty("total", 0);
            manager.save();
            updateMoney();
            updateTotal();
        }
    }

    private boolean checkPermission(SlashCommandInteractionEvent event) {
        if (Arrays.stream(configFile.OwnerID).noneMatch(i -> i.equals(event.getUser().getIdLong()))) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.no_permission.get(event.getUserLocale()), 0xFF0000)).queue();
            return true;
        }

        return false;
    }
}