package com.xs.bank;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.*;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.xs.loader.MainLoader.ROOT_PATH;
import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.SlashCommandOption.USER_TAG;
import static com.xs.loader.util.SlashCommandOption.VALUE;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

public class Main extends PluginEvent {
    public static MainConfig configFile;
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW", "zh-CN"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "Bank";
    private final String PATH_FOLDER_NAME = "./plugins/Bank";
    private JsonFileManager manager;
    private Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content
    private final Map<String, Integer> moneyTypes = new HashMap<>(); // name, tax
    private long workingGuildID;
    private boolean checkGuildAlive = false;

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
        LangGetter langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, this.getClass());

        // expert files
        langGetter.exportDefaultLang();
        lang = langGetter.readLangFileData();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("add_money", "add money to an user")
                        .setNameLocalizations(lang.get("register;add_money;cmd"))
                        .setDescriptionLocalizations(lang.get("register;add_money;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        moneyTypes.keySet().stream().map(
                                name -> new SubcommandData(name, name).addOptions(
                                        new OptionData(USER, USER_TAG, "user", true)
                                                .setDescriptionLocalizations(lang.get("register;add_money;options;user")),
                                        new OptionData(INTEGER, VALUE, "value", true)
                                                .setDescriptionLocalizations(lang.get("register;add_money;options;user"))
                                )
                        ).collect(Collectors.toList())
                ),

                Commands.slash("remove_money", "remove money from an user")
                        .setNameLocalizations(lang.get("register;remove_money;cmd"))
                        .setDescriptionLocalizations(lang.get("register;remove_money;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        moneyTypes.keySet().stream().map(
                                name -> new SubcommandData(name, name).addOptions(
                                        new OptionData(USER, USER_TAG, "user", true)
                                                .setDescriptionLocalizations(lang.get("register;remove_money;options;user")),
                                        new OptionData(INTEGER, VALUE, "value", true)
                                                .setDescriptionLocalizations(lang.get("register;remove_money;options;user"))
                                )
                        ).collect(Collectors.toList())
                ),

                Commands.slash("transfer_money", "transfer money to another user")
                        .setNameLocalizations(lang.get("register;transfer_money;cmd"))
                        .setDescriptionLocalizations(lang.get("register;transfer_money;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                        .addSubcommands(
                        moneyTypes.keySet().stream().map(
                                name -> new SubcommandData(name, name).addOptions(
                                        new OptionData(USER, USER_TAG, "user", true)
                                                .setDescriptionLocalizations(lang.get("register;transfer_money;options;user")),
                                        new OptionData(INTEGER, VALUE, "value", true)
                                                .setDescriptionLocalizations(lang.get("register;transfer_money;options;user"))
                                )
                        ).collect(Collectors.toList())
                ),

                Commands.slash("check_balance", "get user money")
                        .setNameLocalizations(lang.get("register;check_balance;cmd"))
                        .setDescriptionLocalizations(lang.get("register;check_balance;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                        .addOptions(new OptionData(USER, USER_TAG, "user", false)
                        .setDescriptionLocalizations(lang.get("register;check_balance;options;user"))
                ),
        };
    }

    @Override
    public void loadConfigFile() {

        InputStream inputStream = getter.readYmlInputStream("config.yml", PATH_FOLDER_NAME);

        configFile = new Yaml(new Constructor(MainConfig.class)).load(inputStream);

        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } // close file

        workingGuildID = configFile.guildID;
        moneyTypes.clear();
        for (String i : configFile.moneyType) {
            moneyTypes.put(i.split(";")[0], Integer.parseInt(i.split(";")[1]));
        }

        new File(ROOT_PATH + "/" + PATH_FOLDER_NAME + "/data").mkdirs();
        manager = new JsonFileManager("/" + PATH_FOLDER_NAME + "/data/data.json", TAG, true);

        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        if (workingGuildID == event.getGuild().getIdLong())
            checkGuildAlive = true;
    }

    @Override
    public void onReady(ReadyEvent event) {
        if (!checkGuildAlive) {
            logger.warn("cannot found guild by id: " + workingGuildID);
            logger.warn("please configure the file");
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.isFromGuild() && event.getGuild() != null && event.getGuild().getIdLong() == workingGuildID) {
            DiscordLocale local = event.getUserLocale();
            switch (event.getName()) {
                case "add_money": {
                    if (!event.getMember().hasPermission(ADMINISTRATOR)) return;
                    if (event.getSubcommandName() == null) return;

                    String type = event.getSubcommandName();
                    User user = event.getOption(USER_TAG).getAsUser();
                    JSONObject obj = checkData(user.getId(), type);
                    int cur = obj.getInt(type);
                    int value = event.getOption(VALUE).getAsInt();

                    obj.put(type, cur + value);
                    manager.save();

                    event.getHook().deleteOriginal().complete();
                    event.getChannel().sendMessageEmbeds(createEmbed(lang.get("runtime;add_success").get(local)
                                    .replace("%user%", user.getAsTag())
                                    .replace("%type%", type)
                                    .replace("%before_value%", String.valueOf(cur))
                                    .replace("%after_value%", String.valueOf(cur + value)),
                            0x00FFFF)).queue();
                    break;
                }

                case "remove_money": {
                    if (!event.getMember().hasPermission(ADMINISTRATOR)) return;
                    if (event.getSubcommandName() == null) return;

                    String type = event.getSubcommandName();
                    User user = event.getOption(USER_TAG).getAsUser();
                    JSONObject obj = checkData(user.getId(), type);
                    int cur = obj.getInt(type);
                    int value = event.getOption(VALUE).getAsInt();

                    if (obj.getInt(type) < value) {
                        event.getHook().deleteOriginal().complete();
                        event.getChannel().sendMessageEmbeds(createEmbed(lang.get("runtime;errors;no_such_money").get(local), 0xFF0000)).queue();
                        return;
                    }

                    obj.put(type, cur - value);
                    manager.save();

                    event.getHook().deleteOriginal().complete();
                    event.getChannel().sendMessageEmbeds(createEmbed(lang.get("runtime;remove_success").get(local)
                                    .replace("%user%", user.getAsTag())
                                    .replace("%type%", type)
                                    .replace("%before_value%", String.valueOf(cur))
                                    .replace("%after_value%", String.valueOf(cur - value)),
                            0x00FFFF)).queue();
                    break;
                }

                case "transfer_money": {
                    if (event.getSubcommandName() == null) return;

                    String type = event.getSubcommandName();
                    User fromUser = event.getUser();
                    User toUser = event.getOption(USER_TAG).getAsUser();

                    if (fromUser.getIdLong() == toUser.getIdLong()) {
                        event.getHook().deleteOriginal().complete();
                        event.getChannel().sendMessageEmbeds(createEmbed(lang.get("runtime;errors;transfer_self").get(local), 0xFF0000)).queue();
                        return;
                    }

                    JSONObject fromObj = checkData(fromUser.getId(), type);
                    JSONObject toObj = checkData(toUser.getId(), type);
                    int value = event.getOption(VALUE).getAsInt();

                    if (fromObj.getInt(type) < value + moneyTypes.get(type)) {
                        event.getHook().deleteOriginal().complete();
                        event.getChannel().sendMessageEmbeds(createEmbed(lang.get("runtime;errors;no_such_money").get(local), 0xFF0000)).queue();
                        return;
                    }

                    event.getHook().deleteOriginal().complete();
                    event.getChannel().sendMessageEmbeds(createEmbed(lang.get("runtime;transferring").get(local), 0xff7b33))
                            .delay(new Random().nextInt(configFile.transferMaxDelay), TimeUnit.SECONDS)
                            .queue(i -> i.editMessageEmbeds(createEmbed(lang.get("runtime;transfer_done").get(local), 0x2cff20))
                                    .queue(j -> {
                                        fromObj.put(type, fromObj.getInt(type) - value - moneyTypes.get(type));
                                        toObj.put(type, toObj.getInt(type) + value);
                                        manager.save();
                                        event.getChannel().sendMessageEmbeds(createEmbed(lang.get("runtime;transfer_success").get(local)
                                                        .replace("%value%", String.valueOf(value))
                                                        .replace("%type%", type)
                                                        .replace("%user%", toUser.getAsTag()),
                                                0x00FFFF)).queue();
                                    }));
                    break;
                }

                case "check_balance": {
                    User user = getUserID(event);
                    StringBuilder description = new StringBuilder();
                    for (String i : moneyTypes.keySet()) {
                        JSONObject obj = checkData(user.getId(), i);
                        description.append(lang.get("runtime;check_balance_description").get(local)
                                .replace("%value%", String.valueOf(obj.getInt(i)))
                                .replace("%type%", i)
                        );
                    }

                    event.getHook().deleteOriginal().complete();
                    event.getChannel().sendMessageEmbeds(createEmbed(
                            lang.get("runtime;check_balance_title").get(local).replace("%user%", user.getAsTag()),
                            description.toString(),
                            0x00FFFF)
                    ).queue();
                    break;
                }

            }
        } else {
            event.getHook().deleteOriginal().complete();
            System.out.println(event.getGuild().getIdLong());
            System.out.println(workingGuildID);
            event.getChannel().sendMessageEmbeds(createEmbed("You cannot use this command", 0xFF0000)).queue();
        }
    }

    User getUserID(SlashCommandInteractionEvent event) {
        if (event.getOption(USER_TAG) != null)
            if (event.getMember().hasPermission(ADMINISTRATOR))
                return event.getOption(USER_TAG).getAsUser();

        return event.getUser();
    }

    JSONObject checkData(String userID, String type) {
        if (!manager.getObj().has(userID)) { // if user data is not exist
            manager.getObj().put(userID, new JSONObject().put(type, 0));
        } else if (!manager.getObj().getJSONObject(userID).has(type)) { // if value data is not exist
            manager.getObj().getJSONObject(userID).put(type, 0);
        }

        manager.save();
        return manager.getObj().getJSONObject(userID);
    }
}