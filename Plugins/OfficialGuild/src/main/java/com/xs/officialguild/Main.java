package com.xs.officialguild;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.xs.loader.MainLoader.jdaBot;
import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.UrlDataGetter.getData;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.Permission.KICK_MEMBERS;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;
import static net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SUCCESS;

public class Main extends PluginEvent {
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "OG";
    private final String PATH_FOLDER_NAME = "./plugins/OfficialGuild";
    private final long OWN_GUILD_ID = 858672865355890708L;
    private Guild ownGuild;
    private final long LOG_CHANNEL_ID = 858672865816346634L;
    private Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content
    private JsonFileManager manager;
    private final Map<Long, UserStepData> stepData = new HashMap<>();
    private final List<Long> JOINED_ROLE_ID = Arrays.asList(858672865385119755L, 858701368457953360L, 858707058530451476L, 858703314606751764L);
    private final List<Long> AUTHED_ROLE_ID = Arrays.asList(858672865385119757L, 858704345448841226L);

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {
        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        new File(PATH_FOLDER_NAME + "/data").mkdirs();
        manager = new JsonFileManager(PATH_FOLDER_NAME + "/data/userNames.json", TAG, true);
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
        return null;
    }

    @Override
    public void onReady(ReadyEvent event) {
        ownGuild = jdaBot.getGuildById(OWN_GUILD_ID);
        if (ownGuild == null) {
            logger.warn("CANNOT FOUND Main Guild!");
            return;
        }

        ownGuild.upsertCommand(
                Commands.slash("create_firstjoin", "if you dont know what it is, please not to touch!")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
        ).queue();
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getGuild().getIdLong() != OWN_GUILD_ID) return;
        Member member = event.getMember();
        for (long i : JOINED_ROLE_ID) {
            Role role = ownGuild.getRoleById(i);
            if (role == null) continue;

            ownGuild.addRoleToMember(member, role).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "invite": {

                break;
            }

            case "create_firstjoin": {
                Button button = new ButtonImpl("xs:og:create", "開始驗證", SUCCESS, false, null);
                EmbedBuilder builder = new EmbedBuilder()
                        .setTitle("正式加入原之序前，需要您的暱稱等資訊，方便其他成員認識您")
                        .setDescription("原之序並不會要求您提供敏感資訊")
                        .setThumbnail("https://i.imgur.com/6ivsnRr.png")
                        .setFooter("公告")
                        .setTimestamp(OffsetDateTime.now())
                        .setColor(0x00FFFF);
                event.getMessageChannel().sendMessageEmbeds(builder.build()).setActionRow(button).queue();
                break;
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("og")) return;

        switch (args[2]) {
            case "create": {
                startAuth(event);
                break;
            }

            case "chi": {
                createChineseInput(event);
                break;
            }

            case "eng": {
                createEnglishInput(event);
                break;
            }

            case "verify": {
                verifyButton(event);
            }
        }
    }


    private void startAuth(ButtonInteractionEvent event) {
        User user = event.getUser();
        UserStepData step;
        if (stepData.containsKey(user.getIdLong())) {
            step = stepData.get(user.getIdLong());
        } else {
            step = new UserStepData();
            stepData.put(user.getIdLong(), step);
        }

        // TODO: some welcome message...
        step.hook = event.deferReply(true).setEmbeds(createEmbed("更新中...", 0x777700)).complete();
        step.updateEmbed();
    }

    private void createChineseInput(ButtonInteractionEvent event) {
        TextInput chiInp = TextInput.create("chi", "二字中文暱稱", TextInputStyle.SHORT)
                .setPlaceholder("請問要怎麼稱呼你呢？")
                .setMinLength(2)
                .setMaxLength(2)
                .build();

        event.replyModal(
                Modal.create("xs:og:set_chi:" + event.getUser().getId(), "設定中文暱稱")
                        .addActionRows(ActionRow.of(chiInp))
                        .build()
        ).queue();
    }

    private void createEnglishInput(ButtonInteractionEvent event) {
        TextInput engInp = TextInput.create("eng", "英文暱稱", TextInputStyle.SHORT)
                .setPlaceholder("請問要怎麼稱呼你呢？")
                .setMinLength(1)
                .setMaxLength(50)
                .build();

        TextInput mcInp = TextInput.create("mcid", "Minecraft ID (選填) (優先使用為暱稱)", TextInputStyle.SHORT)
                .setPlaceholder("請問你叫什麼呢？")
                .setMinLength(1)
                .setMaxLength(48)
                .setRequired(false)
                .build();

        event.replyModal(
                Modal.create("xs:og:set_eng:" + event.getUser().getId(), "設定英文暱稱")
                        .addActionRows(ActionRow.of(engInp), ActionRow.of(mcInp))
                        .build()
        ).queue();
    }

    private void verifyButton(ButtonInteractionEvent event) {
        UserStepData step = stepData.get(event.getUser().getIdLong());
        manager.getObj().put(event.getUser().getId(), step.getObj());
        manager.save();
        event.deferEdit().queue();
        Member member = event.getMember();
        if (ownGuild == null || member == null) return;

        for (long i : AUTHED_ROLE_ID) {
            Role role = ownGuild.getRoleById(i);
            if (role == null) continue;

            ownGuild.addRoleToMember(member, role).queue();
        }

        if (event.getGuild().getSelfMember().canInteract(member))
            member.modifyNickname(step.getNick()).queue();

        TextChannel logChannel = ownGuild.getTextChannelById(LOG_CHANNEL_ID);
        if (logChannel != null) {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle("歡迎加入")
                    .setFooter("成員誕生")
                    .setTimestamp(OffsetDateTime.now())
                    .setColor(0xFFCCDD);
            if (!step.mc_uuid.equals("")) {
                builder
                        .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                        (" (" + member.getUser().getAsTag() + ')') : ""),
                                "https://namemc.com/profile/" + step.mc_uuid,
                                member.getEffectiveAvatarUrl()
                        )
                        .setImage("https://mc-heads.net/body/" + step.mc_uuid)
                        .setThumbnail("https://mc-heads.net/avatar/" + step.mc_uuid);
            } else {
                builder
                        .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                        (" (" + member.getUser().getAsTag() + ')') : ""), null,
                                member.getEffectiveAvatarUrl()
                        );
            }

            logChannel.sendMessageEmbeds(builder.build()).queue();
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String[] args = event.getModalId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("og")) return;
        switch (args[2]) {
            case "set_chi": {
                getChineseNameByForm(event);
                break;
            }

            case "set_eng": {
                getEnglishNameByForm(event);
                break;
            }
        }
    }

    private void getChineseNameByForm(ModalInteractionEvent event) {
        ModalMapping chiInp;
        UserStepData step = stepData.get(event.getUser().getIdLong());
        if ((chiInp = event.getValue("chi")) == null) return;
        if (!Pattern.matches("^[一-龥]+$", chiInp.getAsString())) { // \\u4E00-\\u9fa5
            event.deferReply(true).setEmbeds(createEmbed("輸入錯誤", 0xFF0000)).queue();
        } else {
            step.chineseName = chiInp.getAsString();
            step.updateEmbed();
            event.deferEdit().queue();
        }
    }

    private void getEnglishNameByForm(ModalInteractionEvent event) {
        ModalMapping engInp, mcid_inp;
        UserStepData step = stepData.get(event.getUser().getIdLong());
        if ((engInp = event.getValue("eng")) == null) return;
        if ((mcid_inp = event.getValue("mcid")) == null) return;

        if (!mcid_inp.getAsString().equals("")) {
            String uuid = getUUIDByName(mcid_inp.getAsString());
            if (uuid == null) {
                event.deferReply(true).setEmbeds(
                        createEmbed("查無 \"" + mcid_inp.getAsString() + "\" 的 minecraft 資料，請檢查後再試一次", 0xFF0000)
                ).queue();
                return;
            }

            step.mc_uuid = uuid;
        } else {
            // reset mc uuid
            step.mc_uuid = "";
        }

        step.englishName = engInp.getAsString();
        step.updateEmbed();
        event.deferEdit().queue();
    }

    @Nullable
    private String getUUIDByName(String mcName) {
        final String check_url = "https://api.mojang.com/users/profiles/minecraft/";

        String respond = getData(check_url + mcName);
        if (respond == null) return null;

        JSONObject data = new JSONObject(respond);
        if (data.has("id")) return data.getString("id");

        return null;
    }
}