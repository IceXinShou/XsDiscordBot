package com.xs;

import com.xs.loader.PluginEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.xs.util.EmbedCreator.createEmbed;
import static com.xs.util.JsonKeys.*;
import static com.xs.util.PermissionERROR.permissionCheck;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class Main extends PluginEvent {
    private final String TAG = "[CustomRoom]";
    FileSetting setting;

    @Override
    public void load() {
        System.out.printf("%s Loading...\n", TAG);
        setting = new FileSetting();
        System.out.printf("%s Loaded\n", TAG);
    }

    @Override
    public void unload() {
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


    public void newRoom(SlashCommandInteractionEvent event) {
        GuildChannel detectChannel = event.getOption("detectchannel").getAsChannel();
        String detectID = event.getOption("detectchannel").getAsChannel().getId();
        String voiceName = event.getOption("voicename").getAsString();
        String textName = null;
        Short voiceBitrate;
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
        event.getMember().
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

}