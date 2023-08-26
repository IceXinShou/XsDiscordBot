package com.xs.bank;

import com.google.gson.JsonObject;
import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.xs.loader.base.Loader.ROOT_PATH;
import static com.xs.loader.util.EmbedCreator.createEmbed;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

public class Main extends Event {
    private static final String TAG = "Bank";
    public static MainConfig configFile;
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW", "zh-CN"};
    private final String PATH_FOLDER_NAME = "plugins/Bank";
    private final Map<String, Integer> moneyTypes = new HashMap<>(); // name, tax
    private LangManager langManager;
    private FileGetter getter;
    private Logger logger;
    private JsonFileManager manager;
    private Map<String, Map<DiscordLocale, String>> langMap; // Label, Local, Content
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
        langManager = new LangManager(logger, getter, PATH_FOLDER_NAME, LANG_DEFAULT, DiscordLocale.CHINESE_TAIWAN);

        langMap = langManager.getMap();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("add_money", "add money to an user")
                        .setNameLocalizations(langMap.get("register;add_money;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;add_money;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        moneyTypes.keySet().stream().map(
                                name -> new SubcommandData(name, name).addOptions(
                                        new OptionData(USER, "user", "user", true)
                                                .setDescriptionLocalizations(langMap.get("register;add_money;options;user")),
                                        new OptionData(INTEGER, "value", "value", true)
                                                .setDescriptionLocalizations(langMap.get("register;add_money;options;user"))
                                )
                        ).collect(Collectors.toList())
                ),

                Commands.slash("remove_money", "remove money from an user")
                        .setNameLocalizations(langMap.get("register;remove_money;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;remove_money;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        moneyTypes.keySet().stream().map(
                                name -> new SubcommandData(name, name).addOptions(
                                        new OptionData(USER, "user", "user", true)
                                                .setDescriptionLocalizations(langMap.get("register;remove_money;options;user")),
                                        new OptionData(INTEGER, "value", "value", true)
                                                .setDescriptionLocalizations(langMap.get("register;remove_money;options;user"))
                                )
                        ).collect(Collectors.toList())
                ),

                Commands.slash("transfer_money", "transfer money to another user")
                        .setNameLocalizations(langMap.get("register;transfer_money;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;transfer_money;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                        .addSubcommands(
                        moneyTypes.keySet().stream().map(
                                name -> new SubcommandData(name, name).addOptions(
                                        new OptionData(USER, "user", "user", true)
                                                .setDescriptionLocalizations(langMap.get("register;transfer_money;options;user")),
                                        new OptionData(INTEGER, "value", "value", true)
                                                .setDescriptionLocalizations(langMap.get("register;transfer_money;options;user"))
                                )
                        ).collect(Collectors.toList())
                ),

                Commands.slash("check_balance", "get user money")
                        .setNameLocalizations(langMap.get("register;check_balance;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;check_balance;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                        .addOptions(new OptionData(USER, "user", "user", false)
                        .setDescriptionLocalizations(langMap.get("register;check_balance;options;user"))
                ),
        };
    }

    @Override
    public void loadConfigFile() {
        try (InputStream inputStream = getter.readInputStreamOrDefaultFromSource("config.yml")) {
            if (inputStream == null) return;
            configFile = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader()))
                    .loadAs(inputStream, MainConfig.class);
            logger.log("Setting File Loaded Successfully");
        } catch (IOException e) {
            logger.warn("Please configure /" + PATH_FOLDER_NAME + "/config.yml");
            throw new RuntimeException(e);
        }

        workingGuildID = configFile.guildID;
        moneyTypes.clear();
        for (String i : configFile.moneyType) {
            moneyTypes.put(i.split(";")[0], Integer.parseInt(i.split(";")[1]));
        }

        new File(ROOT_PATH + '/' + PATH_FOLDER_NAME + "/data").mkdirs();
        manager = new JsonFileManager('/' + PATH_FOLDER_NAME + "/data/data.json", TAG, true);

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
        DiscordLocale local = event.getUserLocale();
        switch (event.getName()) {
            case "add_money": {
                if (!event.getMember().hasPermission(ADMINISTRATOR)) return;
                if (event.getSubcommandName() == null) return;

                String type = event.getSubcommandName();
                User user = event.getOption("user").getAsUser();
                JsonObject obj = checkData(user.getId(), type);
                int cur = obj.get(type).getAsInt();
                int value = event.getOption("value").getAsInt();

                obj.addProperty(type, cur + value);
                manager.save();

                event.getHook().deleteOriginal().complete();
                event.getChannel().sendMessageEmbeds(createEmbed(langManager.get("runtime;add_success", local)
                                .replace("%user%", user.getName())
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
                User user = event.getOption("user").getAsUser();
                JsonObject obj = checkData(user.getId(), type);
                int cur = obj.get(type).getAsInt();
                int value = event.getOption("value").getAsInt();

                if (obj.get(type).getAsInt() < value) {
                    event.getHook().deleteOriginal().complete();
                    event.getChannel().sendMessageEmbeds(createEmbed(langManager.get("runtime;errors;no_such_money", local), 0xFF0000)).queue();
                    return;
                }

                obj.addProperty(type, cur - value);
                manager.save();

                event.getHook().deleteOriginal().complete();
                event.getChannel().sendMessageEmbeds(createEmbed(langManager.get("runtime;remove_success", local)
                                .replace("%user%", user.getName())
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
                User toUser = event.getOption("user").getAsUser();

                if (fromUser.getIdLong() == toUser.getIdLong()) {
                    event.getHook().deleteOriginal().complete();
                    event.getChannel().sendMessageEmbeds(createEmbed(langManager.get("runtime;errors;transfer_self", local), 0xFF0000)).queue();
                    return;
                }

                JsonObject fromObj = checkData(fromUser.getId(), type);
                JsonObject toObj = checkData(toUser.getId(), type);
                int value = event.getOption("value").getAsInt();

                if (fromObj.get(type).getAsInt() < value + moneyTypes.get(type)) {
                    event.getHook().deleteOriginal().complete();
                    event.getChannel().sendMessageEmbeds(createEmbed(langManager.get("runtime;errors;no_such_money", local), 0xFF0000)).queue();
                    return;
                }

                event.getHook().deleteOriginal().complete();
                event.getChannel().sendMessageEmbeds(createEmbed(langManager.get("runtime;transferring", local), 0xff7b33))
                        .delay(new Random().nextInt(configFile.transferMaxDelay), TimeUnit.SECONDS)
                        .queue(i -> i.editMessageEmbeds(createEmbed(langManager.get("runtime;transfer_done", local), 0x2cff20))
                                .queue(j -> {
                                    fromObj.addProperty(type, fromObj.get(type).getAsInt() - value - moneyTypes.get(type));
                                    toObj.addProperty(type, toObj.get(type).getAsInt() + value);
                                    manager.save();
                                    event.getChannel().sendMessageEmbeds(createEmbed(langManager.get("runtime;transfer_success", local)
                                                    .replace("%value%", String.valueOf(value))
                                                    .replace("%type%", type)
                                                    .replace("%user%", toUser.getName()),
                                            0x00FFFF)).queue();
                                }));
                break;
            }

            case "check_balance": {
                User user = getUserID(event);
                StringBuilder description = new StringBuilder();
                for (String i : moneyTypes.keySet()) {
                    JsonObject obj = checkData(user.getId(), i);
                    description.append(langManager.get("runtime;check_balance_description", local)
                            .replace("%value%", String.valueOf(obj.get(i).getAsInt()))
                            .replace("%type%", i)
                    );
                }

                event.getHook().deleteOriginal().complete();
                event.getChannel().sendMessageEmbeds(createEmbed(
                        langManager.get("runtime;check_balance_title", local).replace("%user%", user.getName()),
                        description.toString(),
                        0x00FFFF)
                ).queue();
                break;
            }

        }
    }

    User getUserID(SlashCommandInteractionEvent event) {
        if (event.getOption("user") != null)
            if (event.getMember().hasPermission(ADMINISTRATOR))
                return event.getOption("user").getAsUser();

        return event.getUser();
    }

    JsonObject checkData(String userID, String type) {
        if (!manager.getObj().has(userID)) { // if user data is not exist
            JsonObject tmp = new JsonObject();
            tmp.addProperty(type, 0);
            manager.getObj().add(userID, tmp);
        } else if (!manager.getObj(userID).getAsJsonObject().has(type)) { // if value data is not exist
            manager.getObj(userID).getAsJsonObject().addProperty(type, 0);
        }

        manager.save();
        return manager.getObj(userID).getAsJsonObject();
    }
}