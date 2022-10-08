package com.xs.economy;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.Lang;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
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
import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

public class Main extends PluginEvent {
    private JSONObject config;
    private Map<String, String> lang;

    Lang langGetter;
    private final Map<Long, String> nameCache = new HashMap<>();
    private final Map<Long, UserData> userData = new HashMap<>();
    private final List<UserData> moneyBoard = new ArrayList<>();
    private final List<UserData> totalBoard = new ArrayList<>();
    private final String[] LANG_DEFAULT = {"en_US", "zh_TW"};
    private final String[] LANG_PARAMETERS_DEFAULT = {
            "REGISTER_GET_MONEY_NAME",
            "REGISTER_OPTION_USER_YOU_CHOOSE",
            "REGISTER_GET_MONEY_TOTAL_NAME",
            "REGISTER_ADD_MONEY_NAME",
            "REGISTER_OPTION_ADD_MONEY_VALUE",
            "REGISTER_REMOVE_MONEY_NAME",
            "REGISTER_OPTION_REMOVE_MONEY_VALUE",
            "REGISTER_SET_MONEY_NAME",
            "REGISTER_OPTION_SET_MONEY_VALUE",
            "REGISTER_ADD_MONEY_TOTAL_NAME",
            "REGISTER_OPTION_ADD_MONEY_TOTAL_VALUE",
            "REGISTER_REMOVE_MONEY_TOTAL_NAME",
            "REGISTER_OPTION_REMOVE_MONEY_TOTAL_VALUE",
            "REGISTER_SET_MONEY_TOTAL_NAME",
            "REGISTER_OPTION_SET_MONEY_TOTAL_VALUE",
            "REGISTER_GET_MONEY_TOP_NAME",
            "REGISTER_GET_MONEY_TOP_TOTAL_NAME",
            "MONEY_BOARD",
            "TOTAL_BOARD",
            "CURRENT_MONEY",
            "CURRENT_TOTAL_MONEY"
    };

    FileGetter getter;
    Logger logger;
    final String TAG = "Economy";
    final String PATH_FOLDER_NAME = "plugins/Economy";
    JsonFileManager manager;
    private final List<Long> ownerIDs = new ArrayList<>();
    private int boardUserShowLimit;

    @Override
    public void initLoad() {
        super.initLoad();
        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class.getClassLoader());
        loadConfigFile();
        loadVariables();
        loadLang();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        super.unload();
        logger.log("UnLoaded");
    }

    @Override
    public CommandData[] guildCommands() {
        return new CommandData[]{
                new CommandDataImpl("money", lang.get("REGISTER_GET_MONEY_NAME")).addOptions(
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_USER_YOU_CHOOSE"))),
                new CommandDataImpl("moneytotal", lang.get("REGISTER_GET_MONEY_TOTAL_NAME")).addOptions(
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_USER_YOU_CHOOSE"))),
                new CommandDataImpl("addmoney", lang.get("REGISTER_ADD_MONEY_NAME")).addOptions(
                        new OptionData(INTEGER, VALUE, lang.get("REGISTER_OPTION_ADD_MONEY_VALUE"), true),
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_USER_YOU_CHOOSE"))),
                new CommandDataImpl("removemoney", lang.get("REGISTER_REMOVE_MONEY_NAME")).addOptions(
                        new OptionData(INTEGER, VALUE, lang.get("REGISTER_OPTION_REMOVE_MONEY_VALUE"), true),
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_USER_YOU_CHOOSE"))),
                new CommandDataImpl("setmoney", lang.get("REGISTER_SET_MONEY_NAME")).addOptions(
                        new OptionData(INTEGER, VALUE, lang.get("REGISTER_OPTION_SET_MONEY_VALUE"), true),
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_USER_YOU_CHOOSE"))),
                new CommandDataImpl("addmoneytotal", lang.get("REGISTER_ADD_MONEY_TOTAL_NAME")).addOptions(
                        new OptionData(INTEGER, VALUE, lang.get("REGISTER_OPTION_ADD_MONEY_TOTAL_VALUE"), true),
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_USER_YOU_CHOOSE"))),
                new CommandDataImpl("removemoneytotal", lang.get("REGISTER_REMOVE_MONEY_TOTAL_NAME")).addOptions(
                        new OptionData(INTEGER, VALUE, lang.get("REGISTER_OPTION_REMOVE_MONEY_TOTAL_VALUE"), true),
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_USER_YOU_CHOOSE"))),
                new CommandDataImpl("setmoneytotal", lang.get("REGISTER_SET_MONEY_TOTAL_NAME")).addOptions(
                        new OptionData(INTEGER, VALUE, lang.get("REGISTER_OPTION_SET_MONEY_TOTAL_VALUE"), true),
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_USER_YOU_CHOOSE"))),
                new CommandDataImpl("moneytop", lang.get("REGISTER_GET_MONEY_TOP_NAME")),
                new CommandDataImpl("moneytoptotal", lang.get("REGISTER_GET_MONEY_TOP_TOTAL_NAME"))
        };
    }

    @Override
    public void loadConfigFile() {
        config = new JSONObject(getter.readYml("config.yml", PATH_FOLDER_NAME));
        langGetter = new Lang(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, LANG_PARAMETERS_DEFAULT, config.getString("Lang"));
        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void loadVariables() {
        JSONArray array = config.getJSONArray("OwnerID");
        for (int i = 0; i < array.length(); ++i) {
            ownerIDs.add(array.getLong(i));
        }

        boardUserShowLimit = config.getInt("BoardUserShowLimit");
        if (boardUserShowLimit < 0)
            boardUserShowLimit = 0;

        new File(ROOT_PATH + "/plugins/" + PATH_FOLDER_NAME + "/data").mkdirs();
        manager = new JsonFileManager("/plugins/" + PATH_FOLDER_NAME + "/data/data.json", TAG);
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        for (String i : manager.get().keySet()) {
            User user;
            if ((user = jdaBot.retrieveUserById(Long.parseLong(i)).complete()) != null) {
                JSONObject object = manager.getOrDefault(i);
                userData.put(Long.parseLong(i), new UserData(Long.parseLong(i), object.getInt("money"), object.getInt("total")));
                nameCache.put(Long.parseLong(i), user.getAsTag());
            }
        }

        updateMoney();
        updateTotal();
    }

    @Override
    public void loadLang() {
        langGetter.exportDefaultLang();
        lang = langGetter.getLangFileData();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.isFromGuild() && event.getGuild() != null) {
            switch (event.getName()) {
                case "money": {
                    long id = getUserID(event);
                    checkData(id, event.getUser().getAsTag());
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id), userData.get(id).get() + " $", 0x00FFFF)).queue();
                    break;
                }
                case "moneytotal": {
                    long id = getUserID(event);
                    checkData(id, event.getUser().getAsTag());
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id), userData.get(id).getTotal() + " $", 0x00FFFF)).queue();
                    break;
                }
                case "moneytop": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("NO_PERMISSION"), 0xFF0000)).queue();
                        return;
                    }
                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("MONEY_BOARD"), fieldGetter(moneyBoard, true, event.getGuild()), 0x00FFFF)).queue();
                    break;
                }
                case "moneytoptotal": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("NO_PERMISSION"), 0xFF0000)).queue();
                        return;
                    }
                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("TOTAL_BOARD"), fieldGetter(totalBoard, false, event.getGuild()), 0x00FFFF)).queue();
                    break;
                }
                case "addmoney": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("NO_PERMISSION"), 0xFF0000)).queue();
                        return;
                    }
                    long id = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(id, event.getUser().getAsTag());
                    userData.get(id).add(value);
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            lang.get("CURRENT_MONEY").replace("%money%", userData.get(id).get() + " $"), 0x00FFFF)).queue();
                    JSONObject object = manager.getOrDefault(String.valueOf(id));
                    if (object.has("money")) {
                        object.put("money", object.getInt("money") + value);
                    } else {
                        object.put("money", value);
                    }

                    if (object.has("total")) {
                        object.put("total", object.getInt("total") + value);
                    } else {
                        object.put("total", value);
                    }
                    manager.save();
                    updateMoney();
                    updateTotal();
                    break;
                }
                case "removemoney": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("NO_PERMISSION"), 0xFF0000)).queue();
                        return;
                    }

                    long id = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(id, event.getUser().getAsTag());
                    userData.get(id).remove(value);
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            lang.get("CURRENT_MONEY").replace("%money%", userData.get(id).get() + " $"), 0x00FFFF)).queue();

                    JSONObject object = manager.getOrDefault(String.valueOf(id));

                    if (object.has("money")) {
                        object.put("money", object.getInt("money") - value);
                    } else {
                        object.put("money", -value);
                    }
                    manager.save();
                    updateMoney();
                    break;
                }
                case "setmoney": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("NO_PERMISSION"), 0xFF0000)).queue();
                        return;
                    }

                    long id = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(id, event.getUser().getAsTag());
                    userData.get(id).set(value);
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            lang.get("CURRENT_MONEY").replace("%money%", userData.get(id).get() + " $"), 0x00FFFF)).queue();

                    manager.getOrDefault(String.valueOf(id)).put("money", value);
                    manager.save();

                    updateMoney();
                    break;
                }
                case "addmoneytotal": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("NO_PERMISSION"), 0xFF0000)).queue();
                        return;
                    }

                    long id = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(id, event.getUser().getAsTag());
                    userData.get(id).addTotal(value);
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            lang.get("CURRENT_TOTAL_MONEY").replace("%total_money%", userData.get(id).getTotal() + " $"), 0x00FFFF)).queue();

                    JSONObject object = manager.getOrDefault(String.valueOf(id));

                    if (object.has("total")) {
                        object.put("total", object.getInt("total") + value);
                    } else {
                        object.put("total", value);
                    }
                    manager.save();

                    updateTotal();
                    break;
                }
                case "removemoneytotal": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("NO_PERMISSION"), 0xFF0000)).queue();
                        return;
                    }

                    long id = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(id, event.getUser().getAsTag());
                    userData.get(id).removeTotal(value);
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            lang.get("CURRENT_TOTAL_MONEY").replace("%total_money%", userData.get(id).getTotal() + " $"), 0x00FFFF)).queue();

                    JSONObject object = manager.getOrDefault(String.valueOf(id));

                    if (object.has("total")) {
                        object.put("total", object.getInt("total") - value);
                    } else {
                        object.put("total", -value);
                    }
                    manager.save();

                    updateTotal();
                    break;
                }
                case "setmoneytotal": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("NO_PERMISSION"), 0xFF0000)).queue();
                        return;
                    }

                    long id = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(id, event.getUser().getAsTag());
                    userData.get(id).setTotal(value);
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            lang.get("CURRENT_TOTAL_MONEY").replace("%total_money%", userData.get(id).getTotal() + " $"), 0x00FFFF)).queue();

                    manager.getOrDefault(String.valueOf(id)).put("total", value);
                    manager.save();

                    updateTotal();
                    break;
                }
                default: {
                }
            }

        }

    }

    @Override
    public void onUserUpdateName(@NotNull UserUpdateNameEvent event) {
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
            if (nameCache.containsKey(data.getID())) {
                name = nameCache.get(data.getID());
            } else {
                nameCache.put(data.getID(), (name = guild.retrieveMemberById(data.getID()).complete().getUser().getAsTag()));
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