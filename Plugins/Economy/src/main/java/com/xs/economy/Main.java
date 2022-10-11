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
    private JSONObject lang_register_get_money;
    private JSONObject lang_register_get_money_history;
    private JSONObject lang_register_add_money;
    private JSONObject lang_register_add_money_options;
    private JSONObject lang_register_remove_money;
    private JSONObject lang_register_remove_money_options;
    private JSONObject lang_register_set_money;
    private JSONObject lang_register_set_money_options;
    private JSONObject lang_register_add_money_history;
    private JSONObject lang_register_add_money_history_options;
    private JSONObject lang_register_remove_money_history;
    private JSONObject lang_register_remove_money_history_options;
    private JSONObject lang_register_set_money_history;
    private JSONObject lang_register_set_money_history_options;
    private JSONObject lang_register_money_board;
    private JSONObject lang_register_money_history_board;
    private JSONObject lang_runtime;
    private JSONObject lang_runtime_errors;

    private LangGetter langGetter;
    private final Map<Long, String> nameCache = new HashMap<>();
    private final Map<Long, UserData> userData = new HashMap<>();
    private final List<UserData> moneyBoard = new ArrayList<>();
    private final List<UserData> totalBoard = new ArrayList<>();
    private final String[] LANG_DEFAULT = {"en_US", "zh_TW"};

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
                new CommandDataImpl(lang_register_get_money.getString("cmd"), lang_register_get_money.getString("description")).addOptions(
                        new OptionData(USER, USER_TAG, lang_register_get_money.getJSONObject("options").getString("user"))),
                new CommandDataImpl(lang_register_get_money_history.getString("cmd"), lang_register_get_money_history.getString("description")).addOptions(
                        new OptionData(USER, USER_TAG, lang_register_get_money_history.getJSONObject("options").getString("user"))),
                new CommandDataImpl(lang_register_add_money.getString("cmd"), lang_register_add_money.getString("description")).addOptions(
                        new OptionData(INTEGER, VALUE, lang_register_add_money_options.getString("value"), true),
                        new OptionData(USER, USER_TAG, lang_register_add_money_options.getString("user"))),
                new CommandDataImpl(lang_register_remove_money.getString("cmd"), lang_register_remove_money.getString("description")).addOptions(
                        new OptionData(INTEGER, VALUE, lang_register_remove_money_options.getString("value"), true),
                        new OptionData(USER, USER_TAG, lang_register_remove_money_options.getString("user"))),
                new CommandDataImpl(lang_register_set_money.getString("cmd"), lang_register_set_money.getString("description")).addOptions(
                        new OptionData(INTEGER, VALUE, lang_register_set_money_options.getString("value"), true),
                        new OptionData(USER, USER_TAG, lang_register_set_money_options.getString("user"))),
                new CommandDataImpl(lang_register_add_money_history.getString("cmd"), lang_register_add_money_history.getString("description")).addOptions(
                        new OptionData(INTEGER, VALUE, lang_register_add_money_history_options.getString("value"), true),
                        new OptionData(USER, USER_TAG, lang_register_add_money_history_options.getString("user"))),
                new CommandDataImpl(lang_register_remove_money_history.getString("cmd"), lang_register_remove_money_history.getString("description")).addOptions(
                        new OptionData(INTEGER, VALUE, lang_register_remove_money_history_options.getString("value"), true),
                        new OptionData(USER, USER_TAG, lang_register_remove_money_history_options.getString("user"))),
                new CommandDataImpl(lang_register_set_money_history.getString("cmd"), lang_register_set_money_history.getString("description")).addOptions(
                        new OptionData(INTEGER, VALUE, lang_register_set_money_history_options.getString("value"), true),
                        new OptionData(USER, USER_TAG, lang_register_set_money_history_options.getString("user"))),
                new CommandDataImpl(lang_register_money_board.getString("cmd"), lang_register_money_board.getString("description")),
                new CommandDataImpl(lang_register_money_history_board.getString("cmd"), lang_register_money_history_board.getString("description"))
        };
    }

    @Override
    public void loadConfigFile() {
        config = new JSONObject(getter.readYml("config.yml", PATH_FOLDER_NAME));
        langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, config.getString("Lang"));
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

        new File(ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data").mkdirs();
        manager = new JsonFileManager("/" + PATH_FOLDER_NAME + "/data/data.json", TAG);
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        for (String i : manager.get().keySet()) {
            User user;
            JSONObject object = manager.getOrDefault(i);
            userData.put(Long.parseLong(i), new UserData(Long.parseLong(i), object.getInt("money"), object.getInt("total")));
            if ((user = jdaBot.retrieveUserById(Long.parseLong(i)).complete()) != null) {
                nameCache.put(Long.parseLong(i), user.getAsTag());
            } else {
                nameCache.put(Long.parseLong(i), "unknown");
            }
        }

        updateMoney();
        updateTotal();
    }

    @Override
    public void loadLang() {
        langGetter.exportDefaultLang();
        JSONObject lang = langGetter.getLangFileData();
        JSONObject lang_register = lang.getJSONObject("register");
        lang_register_get_money = lang_register.getJSONObject("get_money");
        lang_register_get_money_history = lang_register.getJSONObject("get_money_history");
        lang_register_add_money = lang_register.getJSONObject("add_money");
        lang_register_add_money_options = lang_register_add_money.getJSONObject("options");
        lang_register_remove_money = lang_register.getJSONObject("remove_money");
        lang_register_remove_money_options = lang_register_remove_money.getJSONObject("options");
        lang_register_set_money = lang_register.getJSONObject("set_money");
        lang_register_set_money_options = lang_register_set_money.getJSONObject("options");
        lang_register_add_money_history = lang_register.getJSONObject("add_money_history");
        lang_register_add_money_history_options = lang_register_add_money_history.getJSONObject("options");
        lang_register_remove_money_history = lang_register.getJSONObject("remove_money_history");
        lang_register_remove_money_history_options = lang_register_remove_money_history.getJSONObject("options");
        lang_register_set_money_history = lang_register.getJSONObject("set_money_history");
        lang_register_set_money_history_options = lang_register_set_money_history.getJSONObject("options");
        lang_register_money_board = lang_register.getJSONObject("money_board");
        lang_register_money_history_board = lang_register.getJSONObject("money_history_board");
        lang_runtime = lang.getJSONObject("runtime");
        lang_runtime_errors = lang_runtime.getJSONObject("errors");
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.isFromGuild() && event.getGuild() != null) {
            lang_register_get_money.getString("cmd");
            String name = event.getName();
            if (name.equals(lang_register_get_money.getString("cmd"))) {
                long id = getUserID(event);
                checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id), userData.get(id).get() + " $", 0x00FFFF)).queue();
            } else if (name.equals(lang_register_get_money_history.getString("cmd"))) {
                long id = getUserID(event);
                checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id), userData.get(id).getTotal() + " $", 0x00FFFF)).queue();
            } else if (name.equals(lang_register_money_board.getString("cmd"))) {
                if (!ownerIDs.contains(event.getUser().getIdLong())) {
                    event.getHook().editOriginalEmbeds(createEmbed(lang_runtime_errors.getString("no_permission"), 0xFF0000)).queue();
                    return;
                }
                event.getHook().editOriginalEmbeds(createEmbed(lang_runtime.getString("money_board_title"), fieldGetter(moneyBoard, true, event.getGuild()), 0x00FFFF)).queue();
            } else if (name.equals(lang_register_money_history_board.getString("cmd"))) {
                if (!ownerIDs.contains(event.getUser().getIdLong())) {
                    event.getHook().editOriginalEmbeds(createEmbed(lang_runtime_errors.getString("no_permission"), 0xFF0000)).queue();
                    return;
                }
                event.getHook().editOriginalEmbeds(createEmbed(lang_runtime.getString("money_history_board_title"), fieldGetter(totalBoard, false, event.getGuild()), 0x00FFFF)).queue();
            } else if (name.equals(lang_register_add_money.getString("cmd"))) {
                if (!ownerIDs.contains(event.getUser().getIdLong())) {
                    event.getHook().editOriginalEmbeds(createEmbed(lang_runtime_errors.getString("no_permission"), 0xFF0000)).queue();
                    return;
                }
                long id = getUserID(event);
                int value = event.getOption(VALUE).getAsInt();
                checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                userData.get(id).add(value);
                event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                        lang_runtime.getString("current_money").replace("%money%", userData.get(id).get() + " $"), 0x00FFFF)).queue();
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
            } else if (name.equals(lang_register_remove_money.getString("cmd"))) {
                if (!ownerIDs.contains(event.getUser().getIdLong())) {
                    event.getHook().editOriginalEmbeds(createEmbed(lang_runtime_errors.getString("no_permission"), 0xFF0000)).queue();
                    return;
                }

                long id = getUserID(event);
                int value = event.getOption(VALUE).getAsInt();
                checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                userData.get(id).remove(value);
                event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                        lang_runtime.getString("current_money").replace("%money%", userData.get(id).get() + " $"), 0x00FFFF)).queue();

                JSONObject object = manager.getOrDefault(String.valueOf(id));

                if (object.has("money")) {
                    object.put("money", object.getInt("money") - value);
                } else {
                    object.put("money", -value);
                }
                manager.save();
                updateMoney();
            } else if (name.equals(lang_register_set_money.getString("cmd"))) {
                if (!ownerIDs.contains(event.getUser().getIdLong())) {
                    event.getHook().editOriginalEmbeds(createEmbed(lang_runtime_errors.getString("no_permission"), 0xFF0000)).queue();
                    return;
                }

                long id = getUserID(event);
                int value = event.getOption(VALUE).getAsInt();
                checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                userData.get(id).set(value);
                event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                        lang_runtime.getString("current_money").replace("%money%", userData.get(id).get() + " $"), 0x00FFFF)).queue();

                manager.getOrDefault(String.valueOf(id)).put("money", value);
                manager.save();

                updateMoney();
            } else if (name.equals(lang_register_add_money_history.getString("cmd"))) {
                if (!ownerIDs.contains(event.getUser().getIdLong())) {
                    event.getHook().editOriginalEmbeds(createEmbed(lang_runtime_errors.getString("no_permission"), 0xFF0000)).queue();
                    return;
                }

                long id = getUserID(event);
                int value = event.getOption(VALUE).getAsInt();
                checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                userData.get(id).addTotal(value);
                event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                        lang_runtime.getString("current_money_history").replace("%log_money%", userData.get(id).getTotal() + " $"), 0x00FFFF)).queue();

                JSONObject object = manager.getOrDefault(String.valueOf(id));

                if (object.has("total")) {
                    object.put("total", object.getInt("total") + value);
                } else {
                    object.put("total", value);
                }
                manager.save();

                updateTotal();
            } else if (name.equals(lang_register_remove_money_history.getString("cmd"))) {
                if (!ownerIDs.contains(event.getUser().getIdLong())) {
                    event.getHook().editOriginalEmbeds(createEmbed(lang_runtime_errors.getString("no_permission"), 0xFF0000)).queue();
                    return;
                }

                long id = getUserID(event);
                int value = event.getOption(VALUE).getAsInt();
                checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                userData.get(id).removeTotal(value);
                event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                        lang_runtime.getString("current_money_history").replace("%log_money%", userData.get(id).getTotal() + " $"), 0x00FFFF)).queue();

                JSONObject object = manager.getOrDefault(String.valueOf(id));

                if (object.has("total")) {
                    object.put("total", object.getInt("total") - value);
                } else {
                    object.put("total", -value);
                }
                manager.save();

                updateTotal();
            } else if (name.equals(lang_register_set_money_history.getString("cmd"))) {
                if (!ownerIDs.contains(event.getUser().getIdLong())) {
                    event.getHook().editOriginalEmbeds(createEmbed(lang_runtime_errors.getString("no_permission"), 0xFF0000)).queue();
                    return;
                }

                long id = getUserID(event);
                int value = event.getOption(VALUE).getAsInt();
                checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                userData.get(id).setTotal(value);
                event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                        lang_runtime.getString("current_money_history").replace("%log_money%", userData.get(id).getTotal() + " $"), 0x00FFFF)).queue();

                manager.getOrDefault(String.valueOf(id)).put("total", value);
                manager.save();

                updateTotal();
            }
        }
    }

    @Override
    public void onUserUpdateName(@NotNull UserUpdateNameEvent event) {
        nameCache.put(event.getUser().getIdLong(), event.getUser().getAsTag());
    }

    @Override
    public void onUserUpdateDiscriminator(UserUpdateDiscriminatorEvent event) {
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