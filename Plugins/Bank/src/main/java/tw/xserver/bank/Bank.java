package tw.xserver.bank;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.*;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN;
import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;
import static net.dv8tion.jda.internal.utils.Checks.notNull;
import static tw.xserver.loader.base.Loader.ROOT_PATH;
import static tw.xserver.loader.util.EmbedCreator.createEmbed;

public class Bank extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bank.class);
    private static final String PATH_FOLDER_NAME = "plugins/Bank";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private Language lang;
    private JsonObjFileManager manager;
    private final Map<String, Integer> moneyTax = new HashMap<>();
    private boolean checkGuildAlive = false;
    public MainConfig configFile;

    public Bank() {
        super(true);

        reloadAll();
        LOGGER.info("loaded Bank");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded Bank");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, Bank.class);

        try (InputStream inputStream = getter.readInputStream("config.yml")) {
            if (inputStream == null) return;
            configFile = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader(), new LoaderOptions()))
                    .loadAs(inputStream, MainConfig.class);
            LOGGER.info("setting file loaded successfully");
        } catch (IOException e) {
            LOGGER.error("please configure /" + PATH_FOLDER_NAME + "/config.yml");
            throw new RuntimeException(e);
        }

        moneyTax.clear();
        for (String i : configFile.moneyType) {
            moneyTax.put(i.split(";")[0], Integer.parseInt(i.split(";")[1]));
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
                Commands.slash("add-money", "add money to a member")
                        .setNameLocalizations(lang.register.add_money.name)
                        .setDescriptionLocalizations(lang.register.add_money.description)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        moneyTax.keySet().stream().map(
                                name -> new SubcommandData(name, name).addOptions(
                                        new OptionData(USER, "member", "member", true)
                                                .setNameLocalizations(lang.register.add_money.options.member.name)
                                                .setDescriptionLocalizations(lang.register.add_money.options.member.description),
                                        new OptionData(INTEGER, "value", "value", true)
                                                .setNameLocalizations(lang.register.add_money.options.value.name)
                                                .setDescriptionLocalizations(lang.register.add_money.options.value.description)
                                )
                        ).collect(Collectors.toList())
                ),

                Commands.slash("remove-money", "remove money from an member")
                        .setNameLocalizations(lang.register.remove_money.name)
                        .setDescriptionLocalizations(lang.register.remove_money.description)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        moneyTax.keySet().stream().map(
                                name -> new SubcommandData(name, name).addOptions(
                                        new OptionData(USER, "member", "member", true)
                                                .setNameLocalizations(lang.register.remove_money.options.member.name)
                                                .setDescriptionLocalizations(lang.register.remove_money.options.member.description),
                                        new OptionData(INTEGER, "value", "value", true)
                                                .setNameLocalizations(lang.register.remove_money.options.value.name)
                                                .setDescriptionLocalizations(lang.register.remove_money.options.value.description)
                                )
                        ).collect(Collectors.toList())
                ),

                Commands.slash("transfer-money", "transfer money to another member")
                        .setNameLocalizations(lang.register.transfer_money.name)
                        .setDescriptionLocalizations(lang.register.transfer_money.description)
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                        .addSubcommands(
                        moneyTax.keySet().stream().map(
                                name -> new SubcommandData(name, name).addOptions(
                                        new OptionData(USER, "member", "member", true)
                                                .setNameLocalizations(lang.register.transfer_money.options.member.name)
                                                .setDescriptionLocalizations(lang.register.transfer_money.options.member.description),
                                        new OptionData(INTEGER, "value", "value", true)
                                                .setNameLocalizations(lang.register.transfer_money.options.value.name)
                                                .setDescriptionLocalizations(lang.register.transfer_money.options.value.description)
                                )
                        ).collect(Collectors.toList())
                ),

                Commands.slash("check-balance", "get member money")
                        .setNameLocalizations(lang.register.check_balance.name)
                        .setDescriptionLocalizations(lang.register.check_balance.description)
                        .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                        .addOptions(new OptionData(USER, "member", "member", false)
                        .setNameLocalizations(lang.register.check_balance.options.member.name)
                        .setDescriptionLocalizations(lang.register.check_balance.options.member.description)
                ),
        };
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        if (configFile.guildID == event.getGuild().getIdLong())
            checkGuildAlive = true;
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        if (!checkGuildAlive) {
            LOGGER.error("cannot found guild by id: " + configFile.guildID);
            LOGGER.error("please configure the file");
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        DiscordLocale locale = event.getUserLocale();
        Member member = event.getMember();

        notNull(member, "Member Executor");

        switch (event.getName()) {
            case "add-money": {
                if (!event.getMember().hasPermission(ADMINISTRATOR)) return;

                String type = event.getSubcommandName();
                notNull(type, "Currency Type");

                User targetUser = event.getOption("member", null, OptionMapping::getAsUser);
                JsonObject obj = initData(targetUser.getId(), type);
                int cur = obj.get(type).getAsInt();
                int value = event.getOption("value", 0, OptionMapping::getAsInt);

                obj.addProperty(type, cur + value);
                manager.save();

                event.getHook().deleteOriginal().complete();
                event.getChannel().sendMessageEmbeds(createEmbed(lang.runtime.successes.add_success.get(locale)
                                .replace("%member%", targetUser.getName())
                                .replace("%type%", type)
                                .replace("%before_value%", String.valueOf(cur))
                                .replace("%after_value%", String.valueOf(cur + value)),
                        0x00FFFF)).queue();
                break;
            }

            case "remove-money": {
                if (!event.getMember().hasPermission(ADMINISTRATOR)) return;
                if (event.getSubcommandName() == null) return;

                String type = event.getSubcommandName();
                User targetUser = event.getOption("member", null, OptionMapping::getAsUser);
                JsonObject obj = initData(targetUser.getId(), type);
                int cur = obj.get(type).getAsInt();
                int value = event.getOption("value", 0, OptionMapping::getAsInt);

                if (obj.get(type).getAsInt() < value) {
                    event.getHook().deleteOriginal().complete();
                    event.getChannel().sendMessageEmbeds(createEmbed(lang.runtime.errors.no_such_money.get(locale), 0xFF0000)).queue();
                    return;
                }

                obj.addProperty(type, cur - value);
                manager.save();

                event.getHook().deleteOriginal().complete();
                event.getChannel().sendMessageEmbeds(createEmbed(lang.runtime.successes.remove_success.get(locale)
                                .replace("%member%", targetUser.getName())
                                .replace("%type%", type)
                                .replace("%before_value%", String.valueOf(cur))
                                .replace("%after_value%", String.valueOf(cur - value)),
                        0x00FFFF)).queue();
                break;
            }

            case "transfer-money": {
                if (event.getSubcommandName() == null) return;

                String type = event.getSubcommandName();
                User fromUser = event.getUser();
                User targetUser = event.getOption("member", null, OptionMapping::getAsUser);
                int value = event.getOption("value", 0, OptionMapping::getAsInt);

                if (fromUser.getIdLong() == targetUser.getIdLong()) {
                    event.getHook().deleteOriginal().complete();
                    event.getChannel().sendMessageEmbeds(createEmbed(lang.runtime.errors.transfer_self.get(locale), 0xFF0000)).queue();
                    return;
                }

                JsonObject fromObj = initData(fromUser.getId(), type);
                JsonObject toObj = initData(targetUser.getId(), type);

                if (fromObj.get(type).getAsInt() < value + moneyTax.get(type)) {
                    event.getHook().deleteOriginal().complete();
                    event.getChannel().sendMessageEmbeds(createEmbed(lang.runtime.errors.no_such_money.get(locale), 0xFF0000)).queue();
                    return;
                }

                event.getHook().deleteOriginal().complete();
                event.getChannel().sendMessageEmbeds(createEmbed(lang.runtime.successes.transferring.get(locale), 0xff7b33))
                        .delay(new Random().nextInt(configFile.transferMaxDelay), TimeUnit.SECONDS)
                        .queue(i -> i.editMessageEmbeds(createEmbed(lang.runtime.successes.transfer_done.get(locale), 0x2cff20))
                                .queue(j -> {
                                    fromObj.addProperty(type, fromObj.get(type).getAsInt() - value - moneyTax.get(type));
                                    toObj.addProperty(type, toObj.get(type).getAsInt() + value);
                                    manager.save();
                                    event.getChannel().sendMessageEmbeds(createEmbed(lang.runtime.successes.transfer_success.get(locale)
                                                    .replace("%value%", String.valueOf(value))
                                                    .replace("%type%", type)
                                                    .replace("%member%", targetUser.getName()),
                                            0x00FFFF)).queue();
                                }));
                break;
            }

            case "check-balance": {
                User user = getUserID(event);
                StringBuilder description = new StringBuilder();
                for (String i : moneyTax.keySet()) {
                    JsonObject obj = initData(user.getId(), i);
                    description.append(lang.runtime.successes.check_balance_description.get(locale)
                            .replace("%value%", String.valueOf(obj.get(i).getAsInt()))
                            .replace("%type%", i)
                    );
                }

                event.getHook().deleteOriginal().complete();
                event.getChannel().sendMessageEmbeds(createEmbed(
                        lang.runtime.successes.check_balance_title.get(locale)
                                .replace("%member%", user.getName()),
                        description.toString(),
                        0x00FFFF)
                ).queue();
                break;
            }

        }
    }

    private User getUserID(SlashCommandInteractionEvent event) {
        User targetUser = event.getOption("member", null, OptionMapping::getAsUser);
        if (targetUser == null) return event.getUser();

        Member member = event.getMember();
        notNull(member, "Member Executor");

        if (member.hasPermission(ADMINISTRATOR))
            return targetUser;

        return event.getUser();
    }

    private JsonObject initData(String userID, String type) {
        if (!manager.has(userID)) { // if user data is not exist
            JsonObject tmp = new JsonObject();
            tmp.addProperty(type, 0);
            manager.add(userID, tmp);
        } else if (!manager.getAsJsonObject(userID).has(type)) { // if value data is not exist
            manager.getAsJsonObject(userID).addProperty(type, 0);
        }

        manager.save();
        return manager.getAsJsonObject(userID);
    }
}