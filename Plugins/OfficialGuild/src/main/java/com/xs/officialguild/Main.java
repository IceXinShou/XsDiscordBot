package com.xs.officialguild;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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
import static net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.*;

public class Main extends PluginEvent {
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "OG";
    private final String PATH_FOLDER_NAME = "./plugins/OfficialGuild";
    private final long OWN_GUILD_ID = 858672865355890708L;
    private final long AUTH_CATEGORY_ID = 858672866283356216L;

    private Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content
    private JsonFileManager manager;
    private Category authCategory;
    private final Map<Long, UserStepData> stepData = new HashMap<>();

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
        return new SlashCommandData[]{
                Commands.slash("invite", "invite a member who hasn't been invited")
                        .setNameLocalizations(lang.get("register;cmd"))
                        .setDescriptionLocalizations(lang.get("register;description"))
                        .addOptions(
                                new OptionData(USER, "user", "user", true)
                                        .setDescriptionLocalizations(lang.get("register;options;user"))
                        )
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(KICK_MEMBERS)),
        };
    }

    @Override
    public void onReady(ReadyEvent event) {
        Guild guild = jdaBot.getGuildById(OWN_GUILD_ID);
        if (guild == null) {
            logger.warn("CANNOT FOUND Main Guild!");
            return;
        }

        authCategory = guild.getCategoryById(AUTH_CATEGORY_ID);
        if (authCategory == null) {
            logger.warn("CANNOT FOUND Auth Category!");
        }

        guild.upsertCommand(
                Commands.slash("create_firstjoin", "if you dont know what it is, please not to touch!")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
        ).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "invite": {

                break;
            }

            case "create_firstjoin": {
                Button button = new ButtonImpl("xs:og:create", "test", SUCCESS, false, null);
                event.getMessageChannel().sendMessage("TEST").setActionRow(button).queue();
                break;
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("og")) return;
        Guild guild = event.getGuild();
        if (guild == null || event.getGuild().getIdLong() != OWN_GUILD_ID) return;

        switch (args[2]) {
            case "create": {
                createNewAuthChannel(event);
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
        }
    }

    private void createNewAuthChannel(ButtonInteractionEvent event) {
        User user = event.getUser();
        UserStepData step;
        if (stepData.containsKey(user.getIdLong())) {
            step = stepData.get(user.getIdLong());
        } else {
            step = new UserStepData();
            stepData.put(user.getIdLong(), step);
        }

        TextChannel newChannel;
        List<TextChannel> tmp = authCategory.getTextChannels().stream().filter(i -> i.getTopic() != null && i.getTopic().equals(user.getId())).collect(Collectors.toList());
        if (tmp.isEmpty()) {
            newChannel = authCategory.createTextChannel("驗證-" + user.getAsTag()).setTopic(user.getId()).complete();
            newChannel.delete().submitAfter(10, TimeUnit.MINUTES);
        } else {
            newChannel = tmp.get(0);
        }


        if (manager.getObj().has(user.getId())) {
            // joined before
            final String check_url = "https://sessionserver.mojang.com/session/minecraft/profile/";
            final String namemc_url = "https://namemc.com/profile/";
            final String head_url = "https://mc-heads.net/avatar/";
            final String body_url = "https://mc-heads.net/body/";

            String uuid = manager.getObj().getJSONObject(user.getId()).getString("mc");
            String mcName;

            JSONObject data = new JSONObject(getData(check_url + uuid));
            if (!data.has("errorMessage")) {
                mcName = data.getString("name");

                EmbedBuilder builder = new EmbedBuilder()
                        .setAuthor(uuid, namemc_url + uuid, head_url + uuid)
                        .setTitle(mcName)
                        .setDescription("請問此 Minecraft 玩家是你嗎？")
                        .setImage(body_url + uuid)
                        .setFooter("STEP 1 / n")
                        .setTimestamp(OffsetDateTime.now())
                        .setColor(0x00FFFF);

                Button confirm = new ButtonImpl("xs:og:1confirm", "是", SUCCESS, false, null);
                Button renew = new ButtonImpl("xs:og:1renew", "不是，我有另創帳號", PRIMARY, false, null);
                Button deny = new ButtonImpl("xs:og:1deny", "不是，我沒有 Minecraft 帳號", DANGER, false, null);

                newChannel.sendMessageEmbeds(builder.build()).setActionRow(confirm, renew, deny).queue();
            } else {
                // TODO: data too old to be found
            }

        } else {
            // hasn't joined before

            // TODO: some welcome message...
            step.channel = newChannel;
            step.messageID = newChannel.sendMessageEmbeds(createEmbed("HI", 0x00FFFF)).setActionRow(step.getSettingButton()).complete().getIdLong();
        }
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
                .setMinLength(0)
                .setMaxLength(48)
                .build();

        event.replyModal(
                Modal.create("xs:og:set_eng:" + event.getUser().getId(), "設定英文暱稱")
                        .addActionRows(ActionRow.of(engInp), ActionRow.of(mcInp))
                        .build()
        ).queue();
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


    private void getEnglishNameByForm(ModalInteractionEvent event) {
        ModalMapping engInp;
        @Nullable ModalMapping mcid_inp;
        UserStepData step = stepData.get(event.getUser().getIdLong());
        if ((engInp = event.getValue("eng")) == null) return;
        mcid_inp = event.getValue("mcid");

        if (!Pattern.matches("^[一-龥]+$", engInp.getAsString())) { // \\u4E00-\\u9fa5
            event.deferReply(true).setEmbeds(createEmbed("輸入錯誤", 0xFF0000)).queue();
        } else {
            step.chineseName = engInp.getAsString();
            step.updateButton();
            event.deferEdit().queue();
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
            step.updateButton();
            event.deferEdit().queue();
        }
    }

    @Nullable
    private String getUUIDByName(ModalInteractionEvent event, String mcName) {
        UserStepData step = stepData.get(event.getUser().getIdLong());
        final String check_url = "https://api.mojang.com/users/profiles/minecraft/";

        String respond = getData(check_url + mcName);
        if (respond.equals("")) {
            event.deferReply(true).setEmbeds(createEmbed("查無 \"" + mcName + "\" 的 minecraft 資料", 0xFF0000)).queue();
        }
        JSONObject data = new JSONObject(getData(check_url + mcName));

//        if (!data.has("errorMessage")) {
//            mcName = data.getString("name");
//
//            EmbedBuilder builder = new EmbedBuilder()
//                    .setAuthor(uuid, namemc_url + uuid, head_url + uuid)
//                    .setTitle(mcName)
//                    .setDescription("請問此 Minecraft 玩家是你嗎？")
//                    .setImage(body_url + uuid)
//                    .setFooter("STEP 1 / n")
//                    .setTimestamp(OffsetDateTime.now())
//                    .setColor(0x00FFFF);
//
//            Button confirm = new ButtonImpl("xs:og:1confirm", "是", SUCCESS, false, null);
//            Button renew = new ButtonImpl("xs:og:1renew", "不是，我有另創帳號", PRIMARY, false, null);
//            Button deny = new ButtonImpl("xs:og:1deny", "不是，我沒有 Minecraft 帳號", DANGER, false, null);
//
//            newChannel.sendMessageEmbeds(builder.build()).setActionRow(confirm, renew, deny).queue();
//        } else {
//            // TODO: data too old to be found
//        }

        return null;
    }
}