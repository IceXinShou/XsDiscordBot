package com.xs.memberpoint;

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
    private final List<UserData> pointBoard = new ArrayList<>();
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "MemberPoint";
    private final String PATH_FOLDER_NAME = "./plugins/MemberPoint";
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
        LangGetter langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT);

        // expert files
        langGetter.exportDefaultLang();
        lang = langGetter.readLangFileData();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("point", "get current point from user")
                        .setNameLocalizations(lang.get("register;get_point;cmd"))
                        .setDescriptionLocalizations(lang.get("register;get_point;description"))
                        .addOptions(
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;get_point;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED),

                Commands.slash("addpoint", "add point to user")
                        .setNameLocalizations(lang.get("register;add_point;cmd"))
                        .setDescriptionLocalizations(lang.get("register;add_point;description"))
                        .addOptions(
                                new OptionData(INTEGER, VALUE, "value", true)
                                        .setDescriptionLocalizations(lang.get("register;add_point;options;value")),
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;add_point;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("removepoint", "remove point from user")
                        .setNameLocalizations(lang.get("register;remove_point;cmd"))
                        .setDescriptionLocalizations(lang.get("register;remove_point;description"))
                        .addOptions(
                                new OptionData(INTEGER, VALUE, "value", true)
                                        .setDescriptionLocalizations(lang.get("register;remove_point;options;value")),
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;remove_point;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("setpoint", "set point to user")
                        .setNameLocalizations(lang.get("register;set_point;cmd"))
                        .setDescriptionLocalizations(lang.get("register;set_point;description"))
                        .addOptions(
                                new OptionData(INTEGER, VALUE, "value", true)
                                        .setDescriptionLocalizations(lang.get("register;set_point;options;value")),
                                new OptionData(USER, USER_TAG, "user", true)
                                        .setDescriptionLocalizations(lang.get("register;set_point;options;user")))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),

                Commands.slash("pointtop", "get board from point")
                        .setNameLocalizations(lang.get("register;point_board;cmd"))
                        .setDescriptionLocalizations(lang.get("register;point_board;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
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
        manager = new JsonFileManager("/" + PATH_FOLDER_NAME + "/data/data.json", TAG, true);

        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        for (String i : manager.getObj().keySet()) {
            User user;
            JSONObject object = manager.getOrDefault(i);
            userData.put(Long.parseLong(i), new UserData(Long.parseLong(i), object.getInt("point"), object.getInt("total")));
            try {
                user = jdaBot.retrieveUserById(Long.parseLong(i)).complete();
                nameCache.put(Long.parseLong(i), user.getAsTag());
            } catch (ErrorResponseException e) {
                nameCache.put(Long.parseLong(i), "unknown (" + i + ')');
            }
        }

        updatepoint();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.isFromGuild() && event.getGuild() != null) {
            DiscordLocale local = event.getUserLocale();
            switch (event.getName()) {
                case "point": {
                    long id = getUserID(event);
                    checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            userData.get(id).get() + " $", 0x00FFFF)).queue();
                    break;
                }

                case "pointtop": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;point_board_title").get(local),
                            fieldGetter(pointBoard, true, event.getGuild()), 0x00FFFF)).queue();
                    break;
                }


                case "addpoint": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    long id = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                    userData.get(id).add(value);
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            lang.get("runtime;current_point").get(local).replace("%point%", userData.get(id).get() + " $"), 0x00FFFF)).queue();

                    JSONObject object = manager.getOrDefault(String.valueOf(id));
                    if (object.has("point"))
                        object.put("point", object.getInt("point") + value);
                    else
                        object.put("point", value);

                    if (object.has("total"))
                        object.put("total", object.getInt("total") + value);
                    else
                        object.put("total", value);

                    manager.save();
                    updatepoint();
                    break;
                }

                case "removepoint": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    long id = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                    userData.get(id).remove(value);
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            lang.get("runtime;current_point").get(local).replace("%point%", userData.get(id).get() + " $"), 0x00FFFF)).queue();

                    JSONObject object = manager.getOrDefault(String.valueOf(id));
                    if (object.has("point"))
                        object.put("point", object.getInt("point") - value);
                    else
                        object.put("point", -value);

                    manager.save();
                    updatepoint();
                    break;
                }

                case "setpoint": {
                    if (!ownerIDs.contains(event.getUser().getIdLong())) {
                        event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;no_permission").get(local), 0xFF0000)).queue();
                        return;
                    }

                    long id = getUserID(event);
                    int value = event.getOption(VALUE).getAsInt();
                    checkData(id, event.getGuild().retrieveMemberById(id).complete().getUser().getAsTag());
                    userData.get(id).set(value);
                    event.getHook().editOriginalEmbeds(createEmbed(getNameByID(event.getGuild(), id),
                            lang.get("runtime;current_point").get(local).replace("%point%", userData.get(id).get() + " $"), 0x00FFFF)).queue();

                    manager.getOrDefault(String.valueOf(id)).put("point", value);
                    manager.save();
                    updatepoint();
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

    void updatepoint() {
        pointBoard.clear();
        pointBoard.addAll(userData.values().stream().sorted(Comparator.comparingInt(UserData::get).reversed()).collect(Collectors.toList()));
    }


    List<MessageEmbed.Field> fieldGetter(List<UserData> board, boolean point, Guild guild) {
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
                            (point ? data.get() : data.getTotal()) + " $", false
                    )
            );
        }
        return fields;
    }

    void checkData(long id, String name) {
        if (!userData.containsKey(id)) {
            userData.put(id, new UserData(id));
            JSONObject object = manager.getOrDefault(String.valueOf(id));
            object.put("point", 0);
            object.put("total", 0);
            manager.save();
            nameCache.put(id, name);
            updatepoint();
        }
    }
}