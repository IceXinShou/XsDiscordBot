package com.xs.economy;

import com.xs.loader.PluginEvent;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.*;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.PermissionERROR.permissionCheck;
import static com.xs.loader.util.SlashCommandOption.USER_TAG;
import static com.xs.loader.util.SlashCommandOption.VALUE;
import static com.xs.loader.util.Tag.tagUserID;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

public class Main extends PluginEvent {
    private JSONObject config;
    private Map<String, String> lang;
    private Map<Long, UserData> userData = new HashMap<>();
    private List<UserData> moneyBoard = new ArrayList<>();
    private List<UserData> totalBoard = new ArrayList<>();
    private final String[] LANG_DEFAULT = {"en_US", "zh_TW"};
    private final String[] LANG_PARAMETERS_DEFAULT = {
            "REGISTER_NAME", "REGISTER_OPTION_USER_YOU_CHOOSE", "REGISTER_OPTION_TIME_DAY",
            "REGISTER_OPTION_REASON", "NO_PERMISSION", "PERMISSION_DENIED", "SUCCESS", "UNKNOWN_ERROR"
    };

    FileGetter getter;
    Logger logger;
    final String TAG = "Economy";
    final String PATH_FOLDER_NAME = "Economy";


    @Override
    public void initLoad() {
        getter = new FileGetter(TAG, PATH_FOLDER_NAME, Main.class.getClassLoader());
        logger = new Logger(TAG);
        loadConfigFile();
        loadVariables();
        loadLang();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
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
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_USER_YOU_CHOOSE")),
                        new OptionData(INTEGER, VALUE, lang.get("REGISTER_OPTION_ADD_MONEY_VALUE"), true)),
                new CommandDataImpl("removemoney", lang.get("REGISTER_REMOVE_MONEY_NAME")).addOptions(
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_USER_YOU_CHOOSE")),
                        new OptionData(INTEGER, VALUE, lang.get("REGISTER_OPTION_REMOVE_MONEY_VALUE"), true)),
                new CommandDataImpl("setmoney", lang.get("REGISTER_SET_MONEY_NAME")).addOptions(
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_USER_YOU_CHOOSE")),
                        new OptionData(INTEGER, VALUE, lang.get("REGISTER_OPTION_SET_MONEY_VALUE"), true)),
                new CommandDataImpl("addmoneytotal", lang.get("REGISTER_ADD_MONEY_TOTAL_NAME")).addOptions(
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_USER_YOU_CHOOSE")),
                        new OptionData(INTEGER, VALUE, lang.get("REGISTER_OPTION_ADD_MONEY_TOTAL_VALUE"), true)),
                new CommandDataImpl("removemoneytotal", lang.get("REGISTER_REMOVE_MONEY_TOTAL_NAME")).addOptions(
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_USER_YOU_CHOOSE")),
                        new OptionData(INTEGER, VALUE, lang.get("REGISTER_OPTION_REMOVE_MONEY_TOTAL_VALUE"), true)),
                new CommandDataImpl("setmoneytotal", lang.get("REGISTER_SET_MONEY_TOTAL_NAME")).addOptions(
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_USER_YOU_CHOOSE")),
                        new OptionData(INTEGER, VALUE, lang.get("REGISTER_OPTION_SET_MONEY_TOTAL_VALUE"), true)),
                new CommandDataImpl("moneytop", lang.get("REGISTER_GET_MONEY_TOP_NAME")),
                new CommandDataImpl("moneytoptotal", lang.get("REGISTER_GET_MONEY_TOP_TOTAL_NAME")),
        };
    }

    @Override
    public void loadConfigFile() {
        config = new JSONObject(getter.readYml("config.yml", "plugins/" + PATH_FOLDER_NAME));
        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void loadVariables() {
    }

    @Override
    public void loadLang() {
        getter.exportLang(LANG_DEFAULT, LANG_PARAMETERS_DEFAULT);
        lang = getter.getLangFileData(config.getString("Lang"));
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "money" -> {
                long id = getUserID(event);
                checkData(event, id);
                event.getHook().editOriginalEmbeds(createEmbed(tagUserID(id), String.valueOf(userData.get(id).get()), 0x00FFFF)).queue();
            }
            case "moneytotal" -> {
                long id = getUserID(event);
                checkData(event, id);
                event.getHook().editOriginalEmbeds(createEmbed(tagUserID(id), String.valueOf(userData.get(id).getTotal()), 0x00FFFF)).queue();
            }
            case "moneytop" -> {
                event.getHook().editOriginalEmbeds(createEmbed(lang.get("MONEY_BOARD"), fieldGetter(moneyBoard, true), 0x00FFFF)).queue();
            }
            case "moneytoptotal" ->
                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("TOTAL_BOARD"), fieldGetter(totalBoard, false), 0x00FFFF)).queue();
            case "addmoney" -> {
                if (!permissionCheck(ADMINISTRATOR, event))
                    return;

                long id = getUserID(event);
                checkData(event, id);
                userData.get(id).add(event.getOption(VALUE).getAsInt());
                event.getHook().editOriginalEmbeds(createEmbed(tagUserID(id),
                        lang.get("CURRENT_MONEY").replace("%money%", String.valueOf(userData.get(id).get())), 0x00FFFF)).queue();
                updateMoney();
                updateTotal();
            }
            case "removemoney" -> {
                if (!permissionCheck(ADMINISTRATOR, event))
                    return;

                long id = getUserID(event);
                checkData(event, id);
                userData.get(id).remove(event.getOption(VALUE).getAsInt());
                event.getHook().editOriginalEmbeds(createEmbed(tagUserID(id),
                        lang.get("CURRENT_MONEY").replace("%money%", String.valueOf(userData.get(id).get())), 0x00FFFF)).queue();
                updateMoney();
            }
            case "setmoney" -> {
                if (!permissionCheck(ADMINISTRATOR, event))
                    return;

                long id = getUserID(event);
                checkData(event, id);
                userData.get(id).set(event.getOption(VALUE).getAsInt());
                event.getHook().editOriginalEmbeds(createEmbed(tagUserID(id),
                        lang.get("CURRENT_MONEY").replace("%money%", String.valueOf(userData.get(id).get())), 0x00FFFF)).queue();
                updateMoney();
            }
            case "addmoneytotal" -> {
                if (!permissionCheck(ADMINISTRATOR, event))
                    return;

                long id = getUserID(event);
                checkData(event, id);
                userData.get(id).addTotal(event.getOption(VALUE).getAsInt());
                event.getHook().editOriginalEmbeds(createEmbed(tagUserID(id),
                        lang.get("CURRENT_TOTAL_MONEY").replace("%total_money%", String.valueOf(userData.get(id).getTotal())), 0x00FFFF)).queue();
                updateTotal();
            }
            case "removemoneytotal" -> {
                if (!permissionCheck(ADMINISTRATOR, event))
                    return;

                long id = getUserID(event);
                checkData(event, id);
                userData.get(id).removeTotal(event.getOption(VALUE).getAsInt());
                event.getHook().editOriginalEmbeds(createEmbed(tagUserID(id),
                        lang.get("CURRENT_TOTAL_MONEY").replace("%total_money%", String.valueOf(userData.get(id).getTotal())), 0x00FFFF)).queue();
                updateTotal();
            }
            case "setmoneytotal" -> {
                if (!permissionCheck(ADMINISTRATOR, event))
                    return;

                long id = getUserID(event);
                checkData(event, id);
                userData.get(id).setTotal(event.getOption(VALUE).getAsInt());
                event.getHook().editOriginalEmbeds(createEmbed(tagUserID(id),
                        lang.get("CURRENT_TOTAL_MONEY").replace("%total_money%", String.valueOf(userData.get(id).getTotal())), 0x00FFFF)).queue();
                updateTotal();
            }
            default -> {
                return;
            }
        }
    }

    long getUserID(SlashCommandInteractionEvent event) {
        if (event.getOption(USER_TAG) != null) {
            return event.getOption(USER_TAG).getAsUser().getIdLong();
        } else {
            return event.getUser().getIdLong();
        }
    }

    void updateMoney() {
        moneyBoard.sort(Comparator.comparingInt(UserData::get));
    }

    void updateTotal() {
        totalBoard.sort(Comparator.comparingInt(UserData::getTotal));
    }

    List<MessageEmbed.Field> fieldGetter(List<UserData> board, boolean money) {
        List<MessageEmbed.Field> fields = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            fields.add(new MessageEmbed.Field(
                    (i + 1) + ". " + tagUserID(board.get(i).getID()),
                    String.valueOf(money ? board.get(i).get() : board.get(i).getTotal()), false)
            );
        }
        return fields;
    }

    void checkData(SlashCommandInteractionEvent event, long id) {
        if (!userData.containsKey(id)) {
            userData.put(event.getUser().getIdLong(), new UserData(id));
        }
    }
}