package tw.xserver.dynamic;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.lang.LangManager;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;
import tw.xserver.loader.util.json.JsonAryFileManager;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN;
import static net.dv8tion.jda.api.interactions.commands.OptionType.CHANNEL;
import static net.dv8tion.jda.internal.utils.Checks.notNull;
import static tw.xserver.loader.base.Loader.ROOT_PATH;
import static tw.xserver.loader.util.EmbedCreator.createEmbed;
import static tw.xserver.loader.util.GlobalUtil.checkCommand;

public class DynamicChannel extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicChannel.class);
    private static final String PATH_FOLDER_NAME = "plugins/DynamicVC";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private Language lang;
    private final HashSet<Long> trackedChannel = new HashSet<>();
    private final HashSet<TrackedChannel> originChannel = new HashSet<>();

    public DynamicChannel() {
        super(true);

        reloadAll();
        LOGGER.info("loaded DynamicChannel");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded DynamicChannel");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, DynamicChannel.class);

        File folder = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME + "/Data");
        if (folder.mkdirs())
            LOGGER.info("default data folder created");

        File[] files = folder.listFiles();
        if (files == null) return;

        for (File i : files) {
            JsonAryFileManager fileManager = new JsonAryFileManager(PATH_FOLDER_NAME + "/Data/" + i.getName());

            for (Object j : fileManager.get()) {
                JsonObject obj = (JsonObject) j;
                originChannel.add(new TrackedChannel(
                                Long.parseLong(i.getName().substring(0, i.getName().length() - 5)),
                                obj.get("category").getAsLong(),
                                obj.get("name").getAsString(),
                                obj.get("bitrate").getAsInt(),
                                obj.get("limit").getAsInt()
                        )
                );
            }
        }
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
    public void reloadAll() {
        originChannel.clear();
        reloadConfigFile();
        reloadLang();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("dynamic-vc", "commands about dynamic voice chat ")
                        .setNameLocalizations(lang.register.name)
                        .setDescriptionLocalizations(lang.register.description)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        new SubcommandData("create-by-channel", "create a dynamic voice chat by channel name")
                                .setNameLocalizations(lang.register.subcommand.create.name)
                                .setDescriptionLocalizations(lang.register.subcommand.create.description)
                                .addOptions(
                                        new OptionData(CHANNEL, "detect", "the channel be detected", true)
                                                .setNameLocalizations(lang.register.subcommand.create.options.detect.name)
                                                .setDescriptionLocalizations(lang.register.subcommand.create.options.detect.description)
                                ),

                        new SubcommandData("remove-by-channel", "remove a dynamic voice chat by channel name")
                                .setNameLocalizations(lang.register.subcommand.remove.name)
                                .setDescriptionLocalizations(lang.register.subcommand.remove.description)
                                .addOptions(
                                        new OptionData(CHANNEL, "detect", "the channel be detected", true)
                                                .setNameLocalizations(lang.register.subcommand.remove.options.detect.name)
                                                .setDescriptionLocalizations(lang.register.subcommand.remove.options.detect.description)
                                )
                )
        };
    }

    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        if (checkCommand(event, "dynamic-vc")) return;
        if (event.getSubcommandName() == null) return;

        Guild guild = event.getGuild();
        notNull(guild, "Guild");

        DiscordLocale locale = event.getUserLocale();
        long guildID = event.getGuild().getIdLong();
        switch (event.getSubcommandName()) {
            case "create-by-channel": {
                try {
                    long category;
                    String name;
                    int bitrate;
                    int limit;

                    VoiceChannel channel = event.getOption("detect", null, OptionMapping::getAsChannel).asVoiceChannel();
                    category = channel.getParentCategoryIdLong();
                    name = channel.getName();
                    bitrate = channel.getBitrate();
                    limit = channel.getUserLimit();

                    JsonObject object = new JsonObject();
                    object.addProperty("category", category);
                    object.addProperty("name", name);
                    object.addProperty("bitrate", bitrate);
                    object.addProperty("limit", limit);

                    JsonAryFileManager fileManager = new JsonAryFileManager(PATH_FOLDER_NAME + "/Data/" + guildID + ".json");
                    fileManager.add(object);
                    fileManager.save();

                    trackedChannel.add(channel.getIdLong());

                    event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.successes.done.get(locale), 0x00FFFF)).queue();
                } catch (Exception e) {
                    event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.unknown.get(locale), 0xFF0000)).queue();
                    LOGGER.error(Arrays.toString(e.getStackTrace()));
                    LOGGER.error(e.getMessage());
                }

                break;
            }

            case "remove-by-channel": {
                JsonAryFileManager fileManager = new JsonAryFileManager(PATH_FOLDER_NAME + "/Data/" + guildID + ".json");
                VoiceChannel channel = event.getOption("detect", null, OptionMapping::getAsChannel).asVoiceChannel();

                boolean removed = isRemoved(channel, fileManager);

                if (removed) {
                    event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.successes.remove_success.get(locale), 0x00FFFF)).queue();
                } else {
                    event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.successes.no_remove_success.get(locale), 0x00FFFF)).queue();
                }

                break;
            }
        }
    }

    @Override
    public void onGuildReady(@Nonnull GuildReadyEvent event) {
        for (TrackedChannel i : originChannel.stream()
                .filter(i -> event.getGuild().getIdLong() == i.guildID).collect(Collectors.toList())) {

            Category category = event.getGuild().getCategoryById(i.categoryID);
            if (category == null) continue;

            trackedChannel.addAll(category.getVoiceChannels().stream()
                    .filter(j -> j.getName().equals(i.name)).map(ISnowflake::getIdLong).collect(Collectors.toList()));
        }
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        AudioChannelUnion joinChannel = event.getChannelJoined();
        if (joinChannel != null && trackedChannel.contains(joinChannel.getIdLong())) {
            if (joinChannel.getMembers().size() == 1) { // copy a new channel
                joinChannel.createCopy().queue(i -> trackedChannel.add(i.getIdLong()));
            }
        }


        AudioChannelUnion leftChannel = event.getChannelLeft();
        if (leftChannel != null && trackedChannel.contains(leftChannel.getIdLong())) {
            if (leftChannel.getMembers().isEmpty()) { // remove channel
                trackedChannel.remove(leftChannel.getIdLong());
                leftChannel.delete().queue();
            }
        }
    }

    private static boolean isRemoved(VoiceChannel channel, JsonAryFileManager fileManager) {
        JsonObject targetObject = new JsonObject();
        targetObject.addProperty("category", channel.getParentCategoryIdLong());
        targetObject.addProperty("name", channel.getName());
        targetObject.addProperty("limit", channel.getUserLimit());
        targetObject.addProperty("bitrate", channel.getBitrate());

        boolean removed = false;
        JsonArray array = fileManager.get();
        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            if (obj.equals(targetObject)) {
                array.remove(i);
                removed = true;
            }
        }

        if (removed)
            fileManager.save();

        return removed;
    }
}