package com.xs.economy;

import com.google.gson.JsonObject;
import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateGlobalNameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
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
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.xs.loader.base.Loader.ROOT_PATH;
import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.GlobalUtil.getUserById;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

public class Main extends Event {
    private static final String TAG = "Economy";
    private final Map<Long, String> nameCache = new HashMap<>();
    private final Map<Long, UserData> userData = new HashMap<>();
    private final List<UserData> moneyBoard = new ArrayList<>();
    private final List<UserData> totalBoard = new ArrayList<>();
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private final String PATH_FOLDER_NAME = "plugins/Economy";
    private final List<Long> ownerIDs = new ArrayList<>();
    private MainConfig configFile;
    private LangManager langManager;
    private FileGetter getter;
    private Logger logger;
    private JsonFileManager manager;
    private int boardUserShowLimit;
    private Map<String, Map<DiscordLocale, String>> langMap; // Label, Local, Content

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {

        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        loadConfigFile();
        loadLang();
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
                Commands.slash("money", "get current money from user")
                        .setNameLocalizations(langMap.get("register;get_money;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;get_money;description"))
                        .addOptions(
                                new OptionData(USER, "user", "user", true)
                                        .setDescriptionLocalizations(langMap.get("register;get_money;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

                Commands.slash("money_log", "get money log from user")
                        .setNameLocalizations(langMap.get("register;get_money_history;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;get_money_history;description"))
                        .addOptions(
                                new OptionData(USER, "user", "user", true)
                                        .setDescriptionLocalizations(langMap.get("register;get_money_history;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

                Commands.slash("add_money", "add money to user")
                        .setNameLocalizations(langMap.get("register;add_money;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;add_money;description"))
                        .addOptions(
                                new OptionData(INTEGER, "value", "value", true)
                                        .setDescriptionLocalizations(langMap.get("register;add_money;options;value")),
                                new OptionData(USER, "user", "user", true)
                                        .setDescriptionLocalizations(langMap.get("register;add_money;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("remove_money", "remove money from user")
                        .setNameLocalizations(langMap.get("register;remove_money;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;remove_money;description"))
                        .addOptions(
                                new OptionData(INTEGER, "value", "value", true)
                                        .setDescriptionLocalizations(langMap.get("register;remove_money;options;value")),
                                new OptionData(USER, "user", "user", true)
                                        .setDescriptionLocalizations(langMap.get("register;remove_money;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("set_money", "set money to user")
                        .setNameLocalizations(langMap.get("register;set_money;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;set_money;description"))
                        .addOptions(
                                new OptionData(INTEGER, "value", "value", true)
                                        .setDescriptionLocalizations(langMap.get("register;set_money;options;value")),
                                new OptionData(USER, "user", "user", true)
                                        .setDescriptionLocalizations(langMap.get("register;set_money;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("add_money_log", "add money log to user")
                        .setNameLocalizations(langMap.get("register;add_money_history;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;add_money_history;description"))
                        .addOptions(
                                new OptionData(INTEGER, "value", "value", true)
                                        .setDescriptionLocalizations(langMap.get("register;add_money_history;options;value")),
                                new OptionData(USER, "user", "user", true)
                                        .setDescriptionLocalizations(langMap.get("register;add_money_history;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("remove_money_log", "remove money log from user")
                        .setNameLocalizations(langMap.get("register;remove_money_history;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;remove_money_history;description"))
                        .addOptions(
                                new OptionData(INTEGER, "value", "value", true)
                                        .setDescriptionLocalizations(langMap.get("register;remove_money_history;options;value")),
                                new OptionData(USER, "user", "user", true)
                                        .setDescriptionLocalizations(langMap.get("register;remove_money_history;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("set_money_log", "set money log to user")
                        .setNameLocalizations(langMap.get("register;set_money_history;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;set_money_history;description"))
                        .addOptions(
                                new OptionData(INTEGER, "value", "value", true)
                                        .setDescriptionLocalizations(langMap.get("register;set_money_history;options;value")),
                                new OptionData(USER, "user", "user", true)
                                        .setDescriptionLocalizations(langMap.get("register;set_money_history;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("money_top", "get board from money")
                        .setNameLocalizations(langMap.get("register;money_board;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;money_board;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("money_top_log", "get board from log money")
                        .setNameLocalizations(langMap.get("register;money_history_board;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;money_history_board;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),
        };
    }

    @Override
    public void loadConfigFile() {
        try (InputStream inputStream = getter.readInputStreamOrDefaultFromSource("config.yml")) {
            if (inputStream == null) return;
            configFile = new Yaml(new Constructor(MainConfig.class)).load(inputStream);
            logger.log("Setting File Loaded Successfully");
        } catch (IOException e) {
            logger.warn("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
            throw new RuntimeException(e);
        }

        ownerIDs.addAll(Arrays.asList(configFile.OwnerID));

        boardUserShowLimit = configFile.BoardUserShowLimit;
        if (boardUserShowLimit < 0)
            boardUserShowLimit = 0;

        new File(ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data").mkdirs();
        manager = new JsonFileManager("/" + PATH_FOLDER_NAME + "/data/data.json", TAG, true);

        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void onReady(ReadyEvent event) {
        for (String i : manager.getObj().keySet()) {
            User user;
            JsonObject object = manager.getOrDefault(i);
            userData.put(Long.parseLong(i), new UserData(Long.parseLong(i), object.get("money").getAsInt(), object.get("total").getAsInt()));
            try {
                user = getUserById(Long.parseLong(i));
                nameCache.put(Long.parseLong(i), user.getName());
            } catch (ErrorResponseException e) {
                nameCache.put(Long.parseLong(i), "unknown (" + i + ')');
            }
        }

        updateMoney();
        updateTotal();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.isFromGuild() && event.getGuild() != null) {
            DiscordLocale local = event.getUserLocale();
            switch (event.getName()) {
                case "money": {
                    User user = getUserID(event);
                    checkData(user.getIdLong(), user.getName());
                    event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                            userData.get(user.getIdLong()).get() + " $", 0x00FFFF)).queue();
                    break;
                }

                case "money_log": {
                    User user = getUserID(event);
                    checkData(user.getIdLong(), user.getName());
                    event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                            userData.get(user.getIdLong()).getTotal() + " $", 0x00FFFF)).queue();
                    break;
                }

                case "money_top": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;no_permission", local), 0xFF0000)).queue();
                        return;
                    }

                    EmbedBuilder builder = fieldGetter(moneyBoard, true, event.getGuild());
                    event.getHook().editOriginalEmbeds(builder
                            .setTitle(langManager.get("runtime;money_board_title", local))
                            .setColor(0x00FFFF)
                            .build()
                    ).queue();
                    break;
                }

                case "money_top_log": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;no_permission", local), 0xFF0000)).queue();
                        return;
                    }

                    EmbedBuilder builder = fieldGetter(totalBoard, false, event.getGuild());
                    event.getHook().editOriginalEmbeds(builder
                            .setTitle(langManager.get("runtime;money_history_board_title", local))
                            .setColor(0x00FFFF)
                            .build()
                    ).queue();
                    break;
                }

                case "add_money": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;no_permission", local), 0xFF0000)).queue();
                        return;
                    }

                    User user = getUserID(event);
                    int value = event.getOption("value").getAsInt();
                    checkData(user.getIdLong(), user.getName());
                    userData.get(user.getIdLong()).add(value);
                    event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                            langManager.get("runtime;current_money", local).replace("%money%", userData.get(user.getIdLong()).get() + " $"), 0x00FFFF)).queue();

                    JsonObject object = manager.getOrDefault(user.getId());
                    if (object.has("money"))
                        object.addProperty("money", object.get("money").getAsInt() + value);
                    else
                        object.addProperty("money", value);

                    if (object.has("total"))
                        object.addProperty("total", object.get("total").getAsInt() + value);
                    else
                        object.addProperty("total", value);

                    manager.save();
                    updateMoney();
                    updateTotal();
                    break;
                }

                case "remove_money": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;no_permission", local), 0xFF0000)).queue();
                        return;
                    }

                    User user = getUserID(event);
                    int value = event.getOption("value").getAsInt();
                    checkData(user.getIdLong(), user.getName());
                    userData.get(user.getIdLong()).remove(value);
                    event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                            langManager.get("runtime;current_money", local).replace("%money%", userData.get(user.getIdLong()).get() + " $"), 0x00FFFF)).queue();

                    JsonObject object = manager.getOrDefault(user.getId());
                    if (object.has("money"))
                        object.addProperty("money", object.get("money").getAsInt() - value);
                    else
                        object.addProperty("money", -value);

                    manager.save();
                    updateMoney();
                    break;
                }

                case "set_money": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;no_permission", local), 0xFF0000)).queue();
                        return;
                    }

                    User user = getUserID(event);
                    int value = event.getOption("value").getAsInt();
                    checkData(user.getIdLong(), user.getName());
                    userData.get(user.getIdLong()).set(value);
                    event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                            langManager.get("runtime;current_money", local).replace("%money%", userData.get(user.getIdLong()).get() + " $"), 0x00FFFF)).queue();

                    manager.getOrDefault(user.getId()).addProperty("money", value);
                    manager.save();
                    updateMoney();
                    break;
                }
                case "add_money_log": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;no_permission", local), 0xFF0000)).queue();
                        return;
                    }

                    User user = getUserID(event);
                    int value = event.getOption("value").getAsInt();
                    checkData(user.getIdLong(), user.getName());
                    userData.get(user.getIdLong()).addTotal(value);
                    event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                            langManager.get("runtime;current_money_history", local).replace("%log_money%", userData.get(user.getIdLong()).getTotal() + " $"), 0x00FFFF)).queue();

                    JsonObject object = manager.getOrDefault(user.getId());
                    if (object.has("total"))
                        object.addProperty("total", object.get("total").getAsInt() + value);
                    else
                        object.addProperty("total", value);

                    manager.save();
                    updateTotal();
                    break;
                }

                case "remove_money_log": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;no_permission", local), 0xFF0000)).queue();
                        return;
                    }

                    User user = getUserID(event);
                    int value = event.getOption("value").getAsInt();
                    checkData(user.getIdLong(), user.getName());
                    userData.get(user.getIdLong()).removeTotal(value);
                    event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                            langManager.get("runtime;current_money_history", local).replace("%log_money%",
                                    userData.get(user.getIdLong()).getTotal() + " $"), 0x00FFFF)).queue();

                    JsonObject object = manager.getOrDefault(user.getId());

                    if (object.has("total"))
                        object.addProperty("total", object.get("total").getAsInt() - value);
                    else
                        object.addProperty("total", -value);

                    manager.save();
                    updateTotal();
                    break;
                }

                case "set_money_log": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;no_permission", local), 0xFF0000)).queue();
                        return;
                    }

                    User user = getUserID(event);
                    int value = event.getOption("value").getAsInt();
                    checkData(user.getIdLong(), user.getName());
                    userData.get(user.getIdLong()).setTotal(value);
                    event.getHook().editOriginalEmbeds(createEmbed(user.getName(),
                            langManager.get("runtime;current_money_history", local).replace("%log_money%", userData.get(user.getIdLong()).getTotal() + " $"), 0x00FFFF)).queue();

                    manager.getOrDefault(user.getId()).addProperty("total", value);
                    manager.save();
                    updateTotal();
                    break;
                }
            }
        }
    }

    @Override
    public void onUserUpdateGlobalName(UserUpdateGlobalNameEvent event) {
        nameCache.put(event.getUser().getIdLong(), event.getUser().getName());
    }

    @Override
    public void onUserUpdateName(UserUpdateNameEvent event) {
        nameCache.put(event.getUser().getIdLong(), event.getUser().getName());
    }

    User getUserID(SlashCommandInteractionEvent event) {
        if (event.getOption("user") != null)
            if (ownerIDs.contains(event.getUser().getIdLong()))
                return event.getOption("user").getAsUser();

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

    EmbedBuilder fieldGetter(List<UserData> board, boolean money, Guild guild) {
        EmbedBuilder builder = new EmbedBuilder();
        int count = Math.min(board.size(), boardUserShowLimit);
        for (int i = 0; i < count; ++i) {
            UserData data = board.get(i);
            String name;
            long id = data.getID();
            if (nameCache.containsKey(id)) {
                name = nameCache.get(id);
            } else {
                Member member = guild.retrieveMemberById(id).complete();
                if (member != null) {
                    nameCache.put(id, (name = member.getUser().getName()));
                } else {
                    logger.warn("cannot found member by id: " + id);
                    name = "unknown";
                }
            }

            builder.addField(
                    (i + 1) + ". " + name,
                    (money ? data.get() : data.getTotal()) + " $", false
            );
        }
        return builder;
    }

    void checkData(long id, String name) {
        if (!userData.containsKey(id)) {
            userData.put(id, new UserData(id));
            JsonObject object = manager.getOrDefault(String.valueOf(id));
            object.addProperty("money", 0);
            object.addProperty("total", 0);
            manager.save();
            nameCache.put(id, name);
            updateMoney();
            updateTotal();
        }
    }
}