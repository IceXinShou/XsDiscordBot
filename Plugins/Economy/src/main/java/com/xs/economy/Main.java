package com.xs.economy;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateDiscriminatorEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.xs.loader.MainLoader.ROOT_PATH;
import static com.xs.loader.MainLoader.jdaBot;
import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.SlashCommandOption.USER_TAG;
import static com.xs.loader.util.SlashCommandOption.VALUE;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

public class Main extends PluginEvent {
    private JSONObject config;
    private final Map<Long, String> nameCache = new HashMap<>();
    private final Map<Long, UserData> userData = new HashMap<>();
    private final List<UserData> moneyBoard = new ArrayList<>();
    private final List<UserData> totalBoard = new ArrayList<>();
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "Economy";
    private final String PATH_FOLDER_NAME = "./plugins/Economy";
    private JsonFileManager manager;
    private final List<Long> ownerIDs = new ArrayList<>();
    private int boardUserShowLimit;
    private Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {
        super.initLoad();
        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class.getClassLoader());
        loadConfigFile();
        loadLang();
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
                Commands.slash("money", "get current money from user")
                        .setNameLocalizations(lang.get("register;get_money;cmd"))
                        .setDescriptionLocalizations(lang.get("register;get_money;description"))
                        .addOptions(
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;get_money;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

                Commands.slash("money_log", "get money log from user")
                        .setNameLocalizations(lang.get("register;get_money_history;cmd"))
                        .setDescriptionLocalizations(lang.get("register;get_money_history;description"))
                        .addOptions(
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;get_money_history;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

                Commands.slash("add_money", "add money to user")
                        .setNameLocalizations(lang.get("register;add_money;cmd"))
                        .setDescriptionLocalizations(lang.get("register;add_money;description"))
                        .addOptions(
                                new OptionData(INTEGER, VALUE, "value", true)
                                        .setDescriptionLocalizations(lang.get("register;add_money;options;value")),
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;add_money;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("remove_money", "remove money from user")
                        .setNameLocalizations(lang.get("register;remove_money;cmd"))
                        .setDescriptionLocalizations(lang.get("register;remove_money;description"))
                        .addOptions(
                                new OptionData(INTEGER, VALUE, "value", true)
                                        .setDescriptionLocalizations(lang.get("register;remove_money;options;value")),
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;remove_money;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("set_money", "set money to user")
                        .setNameLocalizations(lang.get("register;set_money;cmd"))
                        .setDescriptionLocalizations(lang.get("register;set_money;description"))
                        .addOptions(
                                new OptionData(INTEGER, VALUE, "value", true)
                                        .setDescriptionLocalizations(lang.get("register;set_money;options;value")),
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;set_money;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("add_money_log", "add money log to user")
                        .setNameLocalizations(lang.get("register;add_money_history;cmd"))
                        .setDescriptionLocalizations(lang.get("register;add_money_history;description"))
                        .addOptions(
                                new OptionData(INTEGER, VALUE, "value", true)
                                        .setDescriptionLocalizations(lang.get("register;add_money_history;options;value")),
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;add_money_history;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("remove_money_log", "remove money log from user")
                        .setNameLocalizations(lang.get("register;remove_money_history;cmd"))
                        .setDescriptionLocalizations(lang.get("register;remove_money_history;description"))
                        .addOptions(
                                new OptionData(INTEGER, VALUE, "value", true)
                                        .setDescriptionLocalizations(lang.get("register;remove_money_history;options;value")),
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;remove_money_history;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("set_money_log", "set money log to user")
                        .setNameLocalizations(lang.get("register;set_money_history;cmd"))
                        .setDescriptionLocalizations(lang.get("register;set_money_history;description"))
                        .addOptions(
                                new OptionData(INTEGER, VALUE, "value", true)
                                        .setDescriptionLocalizations(lang.get("register;set_money_history;options;value")),
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;set_money_history;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("money_top", "get board from money")
                        .setNameLocalizations(lang.get("register;money_board;cmd"))
                        .setDescriptionLocalizations(lang.get("register;money_board;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("money_top_log", "get board from log money")
                        .setNameLocalizations(lang.get("register;money_history_board;cmd"))
                        .setDescriptionLocalizations(lang.get("register;money_history_board;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),
        };
    }

    @Override
    public void loadConfigFile() {
        config = new JSONObject(getter.readYml("config.yml", this.getClass(), PATH_FOLDER_NAME));

        JSONArray array = config.getJSONArray("OwnerID");
        for (int i = 0; i < array.length(); ++i) {
            ownerIDs.add(array.getLong(i));
        }

        boardUserShowLimit = config.getInt("BoardUserShowLimit");
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
            JSONObject object = manager.getOrDefault(i);
            userData.put(Long.parseLong(i), new UserData(Long.parseLong(i), object.getInt("money"), object.getInt("total")));
            try {
                user = jdaBot.retrieveUserById(Long.parseLong(i)).complete();
                nameCache.put(Long.parseLong(i), user.getAsTag());
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
                    checkData(user.getIdLong(), user.getAsTag());
                    event.getHook().editOriginalEmbeds(createEmbed(user.getAsTag(),
                            userData.get(user.getIdLong()).get() + " $", 0x00FFFF)).queue();
                    break;
                }

                case "money_log": {
                    User user = getUserID(event);
                    checkData(user.getIdLong(), user.getAsTag());
                    event.getHook().editOriginalEmbeds(createEmbed(user.getAsTag(),
                            userData.get(user.getIdLong()).getTotal() + " $", 0x00FFFF)).queue();
                    break;
                }

                case "money_top": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;money_board_title").get(local),
                            fieldGetter(moneyBoard, true, event.getGuild()), 0x00FFFF)).queue();
                    break;
                }

                case "money_top_log": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;money_history_board_title").get(local),
                            fieldGetter(totalBoard, false, event.getGuild()), 0x00FFFF)).queue();
                    break;
                }

                case "add_money": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    User user = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(user.getIdLong(), user.getAsTag());
                    userData.get(user.getIdLong()).add(value);
                    event.getHook().editOriginalEmbeds(createEmbed(user.getAsTag(),
                            lang.get("runtime;current_money").get(local).replace("%money%", userData.get(user.getIdLong()).get() + " $"), 0x00FFFF)).queue();

                    JSONObject object = manager.getOrDefault(user.getId());
                    if (object.has("money"))
                        object.put("money", object.getInt("money") + value);
                    else
                        object.put("money", value);

                    if (object.has("total"))
                        object.put("total", object.getInt("total") + value);
                    else
                        object.put("total", value);

                    manager.save();
                    updateMoney();
                    updateTotal();
                    break;
                }

                case "remove_money": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    User user = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(user.getIdLong(), user.getAsTag());
                    userData.get(user.getIdLong()).remove(value);
                    event.getHook().editOriginalEmbeds(createEmbed(user.getAsTag(),
                            lang.get("runtime;current_money").get(local).replace("%money%", userData.get(user.getIdLong()).get() + " $"), 0x00FFFF)).queue();

                    JSONObject object = manager.getOrDefault(user.getId());
                    if (object.has("money"))
                        object.put("money", object.getInt("money") - value);
                    else
                        object.put("money", -value);

                    manager.save();
                    updateMoney();
                    break;
                }

                case "set_money": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    User user = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(user.getIdLong(), user.getAsTag());
                    userData.get(user.getIdLong()).set(value);
                    event.getHook().editOriginalEmbeds(createEmbed(user.getAsTag(),
                            lang.get("runtime;current_money").get(local).replace("%money%", userData.get(user.getIdLong()).get() + " $"), 0x00FFFF)).queue();

                    manager.getOrDefault(user.getId()).put("money", value);
                    manager.save();
                    updateMoney();
                    break;
                }
                case "add_money_log": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    User user = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(user.getIdLong(), user.getAsTag());
                    userData.get(user.getIdLong()).addTotal(value);
                    event.getHook().editOriginalEmbeds(createEmbed(user.getAsTag(),
                            lang.get("runtime;current_money_history").get(local).replace("%log_money%", userData.get(user.getIdLong()).getTotal() + " $"), 0x00FFFF)).queue();

                    JSONObject object = manager.getOrDefault(user.getId());
                    if (object.has("total"))
                        object.put("total", object.getInt("total") + value);
                    else
                        object.put("total", value);

                    manager.save();
                    updateTotal();
                    break;
                }

                case "remove_money_log": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    User user = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(user.getIdLong(), user.getAsTag());
                    userData.get(user.getIdLong()).removeTotal(value);
                    event.getHook().editOriginalEmbeds(createEmbed(user.getAsTag(),
                            lang.get("runtime;current_money_history").get(local).replace("%log_money%",
                                    userData.get(user.getIdLong()).getTotal() + " $"), 0x00FFFF)).queue();

                    JSONObject object = manager.getOrDefault(user.getId());

                    if (object.has("total"))
                        object.put("total", object.getInt("total") - value);
                    else
                        object.put("total", -value);

                    manager.save();
                    updateTotal();
                    break;
                }

                case "set_money_log": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    User user = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(user.getIdLong(), user.getAsTag());
                    userData.get(user.getIdLong()).setTotal(value);
                    event.getHook().editOriginalEmbeds(createEmbed(user.getAsTag(),
                            lang.get("runtime;current_money_history").get(local).replace("%log_money%", userData.get(user.getIdLong()).getTotal() + " $"), 0x00FFFF)).queue();

                    manager.getOrDefault(user.getId()).put("total", value);
                    manager.save();
                    updateTotal();
                    break;
                }
            }
        }
    }

    @Override
    public void onUserUpdateName(UserUpdateNameEvent event) {
        nameCache.put(event.getUser().getIdLong(), event.getUser().getAsTag());
    }

    @Override
    public void onUserUpdateDiscriminator(UserUpdateDiscriminatorEvent event) {
        nameCache.put(event.getUser().getIdLong(), event.getUser().getAsTag());
    }

    User getUserID(SlashCommandInteractionEvent event) {
        if (event.getOption(USER_TAG) != null)
            if (ownerIDs.contains(event.getUser().getIdLong()))
                return event.getOption(USER_TAG).getAsUser();

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

    List<MessageEmbed.Field> fieldGetter(List<UserData> board, boolean money, Guild guild) {
        List<MessageEmbed.Field> fields = new ArrayList<>();
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
                    nameCache.put(id, (name = member.getUser().getAsTag()));
                } else {
                    logger.warn("cannot found member by id: " + id);
                    name = "unknown";
                }
            }

            fields.add(new MessageEmbed.Field(
                            (i + 1) + ". " + name,
                            (money ? data.get() : data.getTotal()) + " $", false
                    )
            );
        }
        return fields;
    }

    void checkData(long id, String name) {
        if (!userData.containsKey(id)) {
            userData.put(id, new UserData(id));
            JSONObject object = manager.getOrDefault(String.valueOf(id));
            object.put("money", 0);
            object.put("total", 0);
            manager.save();
            nameCache.put(id, name);
            updateMoney();
            updateTotal();
        }
    }
}