package com.xs;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.util.FileGetter;
import com.xs.util.JsonFileManager;
import kotlin.Pair;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.*;

import static com.xs.util.EmbedCreator.createEmbed;
import static com.xs.util.JsonKeys.*;
import static com.xs.util.PermissionERROR.permissionCheck;
import static com.xs.util.Tag.tagUserID;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class Main2 extends PluginEvent {

    public static Map<String, Object> config = new HashMap<>();
    public static JSONObject lang;
    private final String TAG = "CustomRoom";
    private final Map<Long, Pair<Integer, Long>> step = new HashMap<>();
    FileGetter getter;
    JsonFileManager manager;

    @Override
    public void initLoad() {
        super.initLoad();
        loadConfigFile();
        loadLang();
        System.out.printf("%s Loaded\n", TAG);
    }

    @Override
    public void unload() {
        super.unload();
        System.out.printf("%s UnLoaded\n", TAG);
    }

    @Override
    public SubcommandData[] subGuildCommands() {
        return new SubcommandData[]{
                new SubcommandData("newroom", "創建自動化房間").addOptions(
                        new OptionData(CHANNEL, "detectchannel", "偵測頻道", true),
                        new OptionData(STRING, "voicename", "語音名稱(可包含空白鍵, %guild_name%, %user%, %user_name%, %user_tag%, 或 %nickname%)", true),
                        new OptionData(STRING, "textname", "文字名稱(不可包含空白鍵, %guild_name%, %user%, %user_name%, %user_tag%, 或 %nickname%)"),
                        new OptionData(CHANNEL, "voicecategory", "語音頻道目錄"),
                        new OptionData(CHANNEL, "textcategory", "文字頻道目錄"),
                        new OptionData(INTEGER, "voicebitrate", "語音位元率 (kbps)"),
                        new OptionData(INTEGER, "memberlimit", "語音人數限制 (1~99)")
                ),
                new SubcommandData("removeroom", "移除自動化房間")
                        .addOption(CHANNEL, "detectchannel", "偵測頻道", true),
        };
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!permissionCheck(ADMINISTRATOR, event))
            return;
        if (event.getName().equals("newroom")) {
            newRoom(event);
        } else if (event.getName().equals("removeroom")) {
            removeRoom(event);
        }
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (step.containsKey(event.getMember().getIdLong())) {
            if (step.get(event.getMember().getIdLong()).getFirst() == 0) {
                step.put(event.getMember().getIdLong(), new Pair<>(1, 0L));
                newRoom(event);
            }
        }
        if (manager.guildData.containsKey(event.getGuild().getIdLong())) {
            JSONObject object;
            if ((object = manager.getDataObject(event.getGuild().getIdLong())).containsKey(event.getChannelJoined().getId())) {
                newChannel(event, object);
            }
        }
    }

    public static String placeholderReplacer(String input, Member member) {
        String fullName = member.getUser().getAsTag();
        if (!input.contains("%")) {
            return input;
        }

        return input
                .replace("%guild_name%", member.getGuild().getName())
                .replace("%user%", tagUserID(member.getId()))
                .replace("%user_name%", member.getUser().getName())
                .replace("%user_tag%", fullName.substring(fullName.lastIndexOf("#")))
                .replace("%nickname%", member.getNickname() == null ? member.getUser().getName() : member.getNickname());
    }

    private void newChannel(GuildVoiceJoinEvent event, JSONObject inputData) {
        Collection<Permission> allow = new ArrayList<>();
        allow.add(Permission.VIEW_CHANNEL);
        allow.add(Permission.MANAGE_CHANNEL);

        JSONObject data = inputData.getJson(event.getChannelJoined().getId());
        final net.dv8tion.jda.api.entities.Category textCategory;
        net.dv8tion.jda.api.entities.Category voiceCategory = event.getGuild().getCategoryById(data.getString(ROOM_VOICE_CATEGORY_ID));
        boolean hasTextChannel;
        if (data.containsKey(ROOM_TEXT_CATEGORY_ID)) {
            hasTextChannel = true;
            textCategory = event.getGuild().getCategoryById(data.getString(ROOM_TEXT_CATEGORY_ID));
        } else {
            hasTextChannel = false;
            textCategory = null;
        }

        if (voiceCategory == null || (hasTextChannel && textCategory == null)) {
            inputData.remove(event.getChannelJoined().getId());
            return;
        }

        event.getGuild().createVoiceChannel(
                placeholderReplacer(data.getString(ROOM_VOICE_NAME), event.getMember()),
                voiceCategory
        ).setBitrate(
                data.getInt(ROOM_VOICE_BITRATE) * 1000
        ).queue(nvc -> {
            if (data.containsKey(ROOM_VOICE_MEMBER_LIMIT))
                nvc.getManager().setUserLimit(data.getInt(ROOM_VOICE_MEMBER_LIMIT)).queue();
            nvc.upsertPermissionOverride(event.getMember()).setAllowed(allow).queue();
            event.getGuild().moveVoiceMember(event.getMember(), nvc).queue();
            Map<String, List<String>> map = voiceState.get(event.getGuild().getId());
            List<String> channels = new ArrayList<>(List.of(nvc.getId()));
            if (map == null) {
                voiceState.put(event.getGuild().getId(), new HashMap<>() {{
                    put(event.getMember().getId(), channels);
                }});
            } else
                map.put(event.getMember().getId(), channels);

            if (hasTextChannel)
                // 創建專屬文字頻道
                event.getGuild().createTextChannel(
                        placeholderReplacer(data.getString(ROOM_TEXT_NAME), event.getMember()),
                        textCategory
                ).queue(ntc -> {
                    ntc.upsertPermissionOverride(event.getMember()).setAllowed(allow).queue();
                    channels.add(ntc.getId());
                });
        });
    }

    public void newRoom(GenericGuildVoiceEvent event) {
        if (!step.containsKey(event.getMember().getIdLong())) {
            step.put(event.getMember().getIdLong(), event.getMember().getVoiceState().inAudioChannel() ? 1 : 0);

            event.getGuild().createTextChannel("Configuration").queue(i -> {
                i.sendMessageEmbeds(createEmbed("請在此繼續進行包廂設定", 0x00FFFF)).queue();
                switch (step.get(event.getMember().getIdLong())) {
                    case 0: {
                        if (!event.getMember().getVoiceState().inAudioChannel()) {
                            i.sendMessageEmbeds(createEmbed("請加入預想被偵測的語音頻道內", 0x00FFFF)).queue();
                            step.put(event.getMember().getIdLong(), 1);
                            break;
                        }
                    }
                    case 1: {

                        break;
                    }

                }
            });
        }

        GuildChannel detectChannel = event.getOption("detectchannel").getAsChannel();
        String detectID = event.getOption("detectchannel").getAsChannel().getId();
        String voiceName = event.getOption("voicename").getAsString();
        String textName = null;
        short voiceBitrate;
        String voiceCategoryID;
        String textCategoryID = null;
        Byte memberLimit = null;
        boolean hasTextChannel = false;
        Guild guild = event.getGuild();

        if (event.getOption("textname") != null) {
            textName = event.getOption("textname").getAsString();
            hasTextChannel = true;
            if (event.getOption("textcategory") != null)
                textCategoryID = event.getOption("textcategory").getAsString();
            else
                textCategoryID = guild.getVoiceChannelById(detectChannel.getId()).getParentCategoryId();
        }

        if (event.getOption("voicebitrate") != null)
            voiceBitrate = Short.parseShort(event.getOption("voicebitrate").getAsString()); // 8~384
        else
            voiceBitrate = 64;
        if (event.getOption("voicecategory") != null)
            voiceCategoryID = event.getOption("voicecategory").getAsString();
        else
            voiceCategoryID = guild.getVoiceChannelById(detectChannel.getId()).getParentCategoryId();

        if (event.getOption("memberlimit") != null && event.getOption("memberlimit").getAsLong() > 0)
            memberLimit = Byte.parseByte(event.getOption("memberlimit").getAsString());

        List<MessageEmbed.Field> fields = new ArrayList<>();

        if (hasTextChannel && textName.contains(" "))
            fields.add(new MessageEmbed.Field("文字頻道名稱無法包含空格", "", false));

        if (voiceName.length() > 100)
            fields.add(new MessageEmbed.Field("語音頻道名稱長度不能大於 100", "", false));

        if (hasTextChannel && textName.length() > 100)
            fields.add(new MessageEmbed.Field("文字頻道名稱長度不能大於 100", "", false));

        if (voiceBitrate * 1000 > guild.getBoostTier().getMaxBitrate())
            fields.add(new MessageEmbed.Field("您的伺服器目前無法達到如此高的音訊位元率", "", false));

        if (memberLimit != null && memberLimit > 99)
            fields.add(new MessageEmbed.Field("人數限制最大只能達到 99 人", "", false));


        if (fields.size() > 0) {
            event.getHook().editOriginalEmbeds(createEmbed("錯誤回報", fields, 0xFF0000)).queue();
            return;
        }

        fields.add(new MessageEmbed.Field("偵測語音頻道", detectChannel.getName() + "\n`(" + detectID + ")`", false));
        fields.add(new MessageEmbed.Field("語音頻道目錄",
                guild.getCategoryById(voiceCategoryID).getName() + "\n`(" + voiceCategoryID + ")`", false));

        if (hasTextChannel)
            fields.add(new MessageEmbed.Field("文字頻道目錄",
                    guild.getCategoryById(textCategoryID).getName() + "\n`(" + textCategoryID + ")`", false));

        fields.add(new MessageEmbed.Field("語音頻道名稱", "`" + voiceName + "`", false));
        if (hasTextChannel)
            fields.add(new MessageEmbed.Field("文字頻道名稱", "`" + textName + "`", false));
        fields.add(new MessageEmbed.Field("語音人數限制", memberLimit == null ? "`無`" : "`" + memberLimit + "`", false));
        fields.add(new MessageEmbed.Field("語音位元率", "`" + voiceBitrate + " kbps`", false));

        JSONObject channelData = new JSONObject();
        channelData.put(ROOM_VOICE_CATEGORY_ID, voiceCategoryID);
        if (hasTextChannel) {
            channelData.put(ROOM_TEXT_CATEGORY_ID, textCategoryID);
            channelData.put(ROOM_TEXT_NAME, textName);
        }
        channelData.put(ROOM_VOICE_NAME, voiceName);
        channelData.put(ROOM_VOICE_BITRATE, voiceBitrate);
        if (memberLimit != null)
            channelData.put(ROOM_VOICE_MEMBER_LIMIT, memberLimit);

        JSONObject roomSetting = settingHelper.getSettingData(guild, ROOM_SETTING);
        roomSetting.put(detectID, channelData);

        settingHelper.getGuildSettingManager(guild.getId()).saveFile();

        event.getHook().editOriginalEmbeds(createEmbed("設定成功", fields, 0x11FF99)).queue();
    }

    public void removeRoom(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        String detectID = event.getOption("detectchannel").getAsGuildChannel().getId();

        JSONObject data = settingHelper.getSettingData(guild, ROOM_SETTING);
        if (voiceState.get(guild.getId()).size() > 0) {
            Map<String, List<String>> memberData = voiceState.get(guild.getId());
            for (String key : memberData.keySet()) {
                for (String channelID : memberData.get(key))
                    try {
                        guild.getVoiceChannelById(channelID).delete().queue();
                    } catch (Exception ignored) {
                    }

                memberData.remove(key);
            }
        }

        data.remove(detectID);
        event.getHook().editOriginalEmbeds(createEmbed("移除成功", 0x00FFFF)).queue();

        settingHelper.getGuildSettingManager(guild.getId()).saveFile();
    }

    @Override
    public void loadConfigFile() {
        config = getter.readYml("custom_room_poll_config.yml", "config.yml", "plugins\\Poll", TAG, this.getClass().getClassLoader());
        langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, config.getString("Lang"));
        getter = new FileGetter();
        manager = new JsonFileManager(Main.ROOT_PATH + "plugins\\CustomRoom", TAG, "CustomRoom");
        System.out.println(TAG + " Setting File Loaded Successfully");
    }

    @Override
    public void loadLang() {
        String langCode;
        if ((langCode = (String) config.get("Lang")) == null) langCode = "zh-TW";
        try {
            getter.readYml("lang/zh-TW.yml", langCode + ".yml", "plugins\\Poll\\Lang", TAG, this.getClass().getClassLoader()).forEach((i, j) -> {
                lang.put(i, (String) j);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}