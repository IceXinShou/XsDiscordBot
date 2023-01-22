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
import org.jetbrains.annotations.NotNull;
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
import static com.xs.loader.util.Tag.getNameByID;
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
    private static final String VERSION = "2.0";
    private final String PATH_FOLDER_NAME = "plugins/Economy";
    private JsonFileManager manager;
    private final List<Long> ownerIDs = new ArrayList<>();
    private int boardUserShowLimit;
    private Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content

    public Main() {
        super(TAG, VERSION);
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
        LangGetter langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT);

        // expert files
        langGetter.exportDefaultLang();
        lang = langGetter.readLangFileData();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("money", "get current money of user")
                        .setNameLocalizations(lang.get("register;get_money;cmd"))
                        .setDescriptionLocalizations(lang.get("register;get_money;description"))
                        .addOptions(
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;get_money;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

                Commands.slash("moneylog", "get money log of user")
                        .setNameLocalizations(lang.get("register;get_money_history;cmd"))
                        .setDescriptionLocalizations(lang.get("register;get_money_history;description"))
                        .addOptions(
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;get_money_history;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

                Commands.slash("addmoney", "add money to user")
                        .setNameLocalizations(lang.get("register;add_money;cmd"))
                        .setDescriptionLocalizations(lang.get("register;add_money;description"))
                        .addOptions(
                                new OptionData(INTEGER, VALUE, "value", true)
                                        .setDescriptionLocalizations(lang.get("register;add_money;options;value")),
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;add_money;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("removemoney", "add money from user")
                        .setNameLocalizations(lang.get("register;remove_money;cmd"))
                        .setDescriptionLocalizations(lang.get("register;remove_money;description"))
                        .addOptions(
                                new OptionData(INTEGER, VALUE, "value", true)
                                        .setDescriptionLocalizations(lang.get("register;remove_money;options;value")),
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;remove_money;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("setmoney", "set money from user")
                        .setNameLocalizations(lang.get("register;set_money;cmd"))
                        .setDescriptionLocalizations(lang.get("register;set_money;description"))
                        .addOptions(
                                new OptionData(INTEGER, VALUE, "value", true)
                                        .setDescriptionLocalizations(lang.get("register;set_money;options;value")),
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;set_money;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("addmoneylog", "add money log to user")
                        .setNameLocalizations(lang.get("register;add_money_history;cmd"))
                        .setDescriptionLocalizations(lang.get("register;add_money_history;description"))
                        .addOptions(
                                new OptionData(INTEGER, VALUE, "value", true)
                                        .setDescriptionLocalizations(lang.get("register;add_money_history;options;value")),
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;add_money_history;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("removemoneylog", "remove money log to user")
                        .setNameLocalizations(lang.get("register;remove_money_history;cmd"))
                        .setDescriptionLocalizations(lang.get("register;remove_money_history;description"))
                        .addOptions(
                                new OptionData(INTEGER, VALUE, "value", true)
                                        .setDescriptionLocalizations(lang.get("register;remove_money_history;options;value")),
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;remove_money_history;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("setmoneylog", "set money log to user")
                        .setNameLocalizations(lang.get("register;set_money_history;cmd"))
                        .setDescriptionLocalizations(lang.get("register;set_money_history;description"))
                        .addOptions(
                                new OptionData(INTEGER, VALUE, "value", true)
                                        .setDescriptionLocalizations(lang.get("register;set_money_history;options;value")),
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;set_money_history;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("moneytop", "get board of money")
                        .setNameLocalizations(lang.get("register;money_board;cmd"))
                        .setDescriptionLocalizations(lang.get("register;money_board;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("moneytoplog", "get board of log money")
                        .setNameLocalizations(lang.get("register;money_history_board;cmd"))
                        .setDescriptionLocalizations(lang.get("register;money_history_board;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),
        };
    }

    @Override
    public void loadConfigFile() {
        config = new JSONObject(getter.readYml("config.yml", PATH_FOLDER_NAME));

        JSONArray array = config.getJSONArray("OwnerID");
        for (int i = 0; i < array.length(); ++i) {
            ownerIDs.add(array.getLong(i));
        }

        boardUserShowLimit = config.getInt("BoardUserShowLimit");
        if (boardUserShowLimit < 0)
            boardUserShowLimit = 0;

        new File(ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data").mkdirs();
        manager = new JsonFileManager("/" + PATH_FOLDER_NAME + "/data/data.json", TAG);

        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        for (String i : manager.get().keySet()) {
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
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.isFromGuild() && event.getGuild() != null) {
            DiscordLocale local = event.getUserLocale();
            switch (event.getName()) {
                case "money": {
                    long id = getUserID(event);
                    checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            userData.get(id).get() + " $", 0x00FFFF)).queue();
                    break;
                }

                case "moneylog": {
                    long id = getUserID(event);
                    checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            userData.get(id).getTotal() + " $", 0x00FFFF)).queue();
                    break;
                }

                case "moneytop": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;money_board_title").get(local),
                            fieldGetter(moneyBoard, true, event.getGuild()), 0x00FFFF)).queue();
                    break;
                }

                case "moneytoplog": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;money_history_board_title").get(local),
                            fieldGetter(totalBoard, false, event.getGuild()), 0x00FFFF)).queue();
                    break;
                }

                case "addmoney": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    long id = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                    userData.get(id).add(value);
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            lang.get("runtime;current_money").get(local).replace("%money%", userData.get(id).get() + " $"), 0x00FFFF)).queue();

                    JSONObject object = manager.getOrDefault(String.valueOf(id));
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

                case "removemoney": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    long id = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                    userData.get(id).remove(value);
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            lang.get("runtime;current_money").get(local).replace("%money%", userData.get(id).get() + " $"), 0x00FFFF)).queue();

                    JSONObject object = manager.getOrDefault(String.valueOf(id));
                    if (object.has("money"))
                        object.put("money", object.getInt("money") - value);
                    else
                        object.put("money", -value);

                    manager.save();
                    updateMoney();
                    break;
                }

                case "setmoney": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    long id = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                    userData.get(id).set(value);
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            lang.get("runtime;current_money").get(local).replace("%money%", userData.get(id).get() + " $"), 0x00FFFF)).queue();

                    manager.getOrDefault(String.valueOf(id)).put("money", value);
                    manager.save();
                    updateMoney();
                    break;
                }
                case "addmoneylog": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    long id = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                    userData.get(id).addTotal(value);
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            lang.get("runtime;current_money_history").get(local).replace("%log_money%", userData.get(id).getTotal() + " $"), 0x00FFFF)).queue();

                    JSONObject object = manager.getOrDefault(String.valueOf(id));
                    if (object.has("total"))
                        object.put("total", object.getInt("total") + value);
                    else
                        object.put("total", value);

                    manager.save();
                    updateTotal();
                    break;
                }

                case "removemoneylog": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    long id = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                    userData.get(id).removeTotal(value);
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            lang.get("runtime;current_money_history").get(local).replace("%log_money%",
                                    userData.get(id).getTotal() + " $"), 0x00FFFF)).queue();

                    JSONObject object = manager.getOrDefault(String.valueOf(id));

                    if (object.has("total"))
                        object.put("total", object.getInt("total") - value);
                    else
                        object.put("total", -value);

                    manager.save();
                    updateTotal();
                    break;
                }

                case "setmoneylog": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    long id = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                    userData.get(id).setTotal(value);
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            lang.get("runtime;current_money_history").get(local).replace("%log_money%", userData.get(id).getTotal() + " $"), 0x00FFFF)).queue();

                    manager.getOrDefault(String.valueOf(id)).put("total", value);
                    manager.save();
                    updateTotal();
                    break;
                }
            }
        }
    }

    @Override
    public void onUserUpdateName(@NotNull UserUpdateNameEvent event) {
        nameCache.put(event.getUser().getIdLong(), event.getUser().getAsTag());
    }

    @Override
    public void onUserUpdateDiscriminator(@NotNull UserUpdateDiscriminatorEvent event) {
        nameCache.put(event.getUser().getIdLong(), event.getUser().getAsTag());
    }

    long getUserID(SlashCommandInteractionEvent event) {
        if (event.getOption(USER_TAG) != null)
            if (ownerIDs.contains(event.getUser().getIdLong()))
                return event.getOption(USER_TAG).getAsLong();

        return event.getUser().getIdLong();
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
                    logger.error("cannot found member by id: " + id);
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