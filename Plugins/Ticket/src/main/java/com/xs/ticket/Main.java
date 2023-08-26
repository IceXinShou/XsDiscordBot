package com.xs.ticket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.util.*;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.Permission.VIEW_CHANNEL;

public class Main extends Event {
    private static final String TAG = "Ticket";
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private final String PATH_FOLDER_NAME = "plugins/Ticket";
    private LangManager langManager;
    private FileGetter getter;
    private Logger logger;
    private Map<String, Map<DiscordLocale, String>> langMap; // Label, Local, Content
    private final Map<Long, CreateStep> steps = new HashMap<>();
    private JsonFileManager manager;

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {
        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        manager = new JsonFileManager('/' + PATH_FOLDER_NAME + "/data.json", TAG, true);
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
                Commands.slash("create_ticket", "create custom reason of ticket")
                        .setNameLocalizations(langMap.get("register;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
        };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("create_ticket")) return;

        startCreate(event);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("ticket")) return;
        DiscordLocale local = event.getUserLocale();
        if (args[2].equals("cr")) {
            switch (args[3]) {
                case "back": {
                    mainMenu(event);
                    break;
                }
                case "btn": {
                    previewReason(event);
                    break;
                }
                case "author": {
                    authorForm(event);
                    break;
                }
                case "content": {
                    content(event);
                    break;
                }
                case "reason": {
                    reasonForm(event);
                    break;
                }
                case "admin": {
                    adminMenu(event);
                    break;
                }
                case "color": {
                    colorForm(event);
                    break;
                }
                case "btnContent": {
                    btnContentForm(event);
                    break;
                }
                case "btnColor": {
                    btnColorMenu(event);
                    break;
                }
                case "category": {
                    categoryMenu(event);
                    break;
                }
                case "confirm": {
                    confirmCreate(event);
                    break;
                }
            }
        }

        switch (args[2]) {
            case "btn": {
                JsonObject tmp;
                if ((tmp = manager.getObj(event.getMessageId())) == null) {
                    event.deferReply(true).addContent("錯誤").queue();
                    return;
                }

                String reason = tmp.getAsJsonObject().get("reasonTitle").getAsString();
                TextInput reasonInput = TextInput.create("reason", "原因", TextInputStyle.PARAGRAPH).build();
                event.replyModal(
                        Modal.create("xs:ticket:push:" + event.getMessageId(), reason)
                                .addComponents(ActionRow.of(reasonInput))
                                .build()
                ).queue();
                break;
            }

            case "lock": {
                TextChannel channel = event.getGuildChannel().asTextChannel();
                Member member = event.getGuild().retrieveMemberById(args[4]).complete();
                channel.upsertPermissionOverride(member).deny(VIEW_CHANNEL).queue();

                event.editComponents(
                        ActionRow.of(
                                Button.of(ButtonStyle.SUCCESS, "xs:ticket:unlock:" + args[3] + ':' + args[4], "開啟", Emoji.fromUnicode("🔓")),
                                Button.of(ButtonStyle.DANGER, "xs:ticket:delete:" + args[3] + ':' + args[4], "刪除", Emoji.fromUnicode("🗑")))
                ).queue();
                break;
            }

            case "unlock": {
                TextChannel channel = event.getGuildChannel().asTextChannel();
                Member member = event.getGuild().retrieveMemberById(args[4]).complete();
                channel.upsertPermissionOverride(member).grant(VIEW_CHANNEL).queue();

                event.editComponents(
                        ActionRow.of(
                                Button.of(ButtonStyle.DANGER, "xs:ticket:lock:" + args[3] + ':' + args[4], "關閉", Emoji.fromUnicode("🔒")),
                                Button.of(ButtonStyle.DANGER, "xs:ticket:delete:" + args[3] + ':' + args[4], "刪除", Emoji.fromUnicode("🗑")))
                ).queue();
                break;
            }

            case "delete": {
                List<Role> roles = new ArrayList<>();
                Member member = event.getMember();
                Guild guild = event.getGuild();

                for (JsonElement i : manager.getObj(args[3]).getAsJsonObject()
                        .getAsJsonArray("adminIDs").asList()) {
                    Role tmp = guild.getRoleById(i.getAsString());

                    if (tmp != null)
                        roles.add(tmp);
                }

                boolean hasCommon = member.getRoles().stream().anyMatch(roles::contains);
                if (!hasCommon && !member.hasPermission(ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }

                event.deferEdit().queue();
                event.getGuildChannel().asTextChannel().delete().queue();
                break;
            }
        }
    }


    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String[] args = event.getModalId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("ticket")) return;


        CreateStep step = steps.get(event.getUser().getIdLong());
        if (args[2].equals("cr")) {
            switch (args[3]) {
                case "author": {
                    if (event.getValue("author").getAsString().isEmpty()) {
                        step.setAuthor(null, null);
                        break;
                    }

                    String url = null;
                    if (!event.getValue("image").getAsString().isEmpty())
                        url = event.getValue("image").getAsString();

                    step.setAuthor(event.getValue("author").getAsString(), url);
                    break;
                }

                case "content": {
                    step.setTitle(event.getValue("title").getAsString());
                    step.setDesc(event.getValue("desc").getAsString());
                    break;
                }

                case "reason": {
                    step.setReason(event.getValue("reason").getAsString());
                    break;
                }

                case "color": {
                    step.setColor(Integer.parseInt(event.getValue("color").getAsString().substring(1), 16));
                    break;
                }

                case "btnText": {
                    step.setBtnContent(event.getValue("btnText").getAsString());
                    break;
                }

                case "btnEmoji": {
                    if (event.getValue("btnEmoji") == null) {
                        step.setBtnEmoji(null);
                        break;
                    }

                    step.setBtnEmoji(
                            Emoji.fromUnicode(event.getValue("btnEmoji").getAsString())
                    );
                    break;
                }
            }
            step.updateEmbed();
            event.deferEdit().queue();
        }

        switch (args[2]) {
            case "push": {
                JsonObject tmp;
                if ((tmp = manager.getObj(args[3])) == null) {
                    event.deferReply(true).addContent("錯誤").queue();
                    return;
                }
                String reason = event.getValue("reason").getAsString();

                event.deferReply(true).queue();
                long[] roleIDs = tmp.getAsJsonArray("adminIDs").asList().stream()
                        .map(JsonElement::getAsLong).mapToLong(Long::longValue).toArray();

                Category category;
                long categoryID = tmp.get("categoryID").getAsLong();
                if (categoryID != 0)
                    category = event.getGuild().getCategoryById(categoryID);
                else {
                    category = event.getGuildChannel().asTextChannel().getParentCategory();
                }

                if (category == null) {
                    event.deferReply(true).addContent("錯誤 (無法取得目錄)").queue();
                    return;
                }

                ChannelAction<TextChannel> qu = category.createTextChannel(event.getUser().getName())
                        .addPermissionOverride(event.getGuild().getPublicRole(), Permission.getRaw(), VIEW_CHANNEL.getRawValue())
                        .addMemberPermissionOverride(event.getMember().getIdLong(), VIEW_CHANNEL.getRawValue(), Permission.getRaw());

                StringBuilder builder = new StringBuilder();
                for (long i : roleIDs) {
                    qu.addRolePermissionOverride(i, VIEW_CHANNEL.getRawValue(), Permission.getRaw());
                    builder.append("<@&").append(i).append("> ");
                }
                builder.append("\n\n").append(reason);

                TextChannel channel = qu.complete();
                event.getHook().sendMessage("請到此頻道 <#" + channel.getId() + "> 並等待人員回覆繼續!").queue();
                channel.sendMessage(builder.toString()).addActionRow(
                        Button.of(ButtonStyle.DANGER, "xs:ticket:lock:" + args[3] + ':' + event.getUser().getId(), "關閉", Emoji.fromUnicode("🔒")),
                        Button.of(ButtonStyle.DANGER, "xs:ticket:delete:" + args[3] + ':' + event.getUser().getId(), "刪除", Emoji.fromUnicode("🗑"))
                ).queue();
            }
        }
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("ticket")) return;

        CreateStep step = steps.get(event.getUser().getIdLong());

        switch (args[3]) {
            case "admin": {
                step.setAdmin(Arrays.stream(
                                event.getValues().stream().mapToLong(ISnowflake::getIdLong).toArray()
                        ).boxed().collect(Collectors.toList())
                );
                step.updateEmbed();
                event.deferEdit().queue();
                break;
            }

            case "category": {
                step.setCategoryID(event.getValues().get(0).getIdLong());
                step.updateEmbed();
                event.deferEdit().queue();
                break;
            }
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("ticket")) return;

        CreateStep step = steps.get(event.getUser().getIdLong());

        switch (args[3]) {
            case "btnColor": {
                step.setBtnStyle(ButtonStyle.fromKey(Integer.parseInt(event.getValues().get(0))));
                step.updateEmbed();
                event.deferEdit().queue();
                break;
            }
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (manager.getObj().has(event.getMessageId())) {
            manager.getObj().remove(event.getMessageId());
            manager.save();
        }
    }

    private void categoryMenu(ButtonInteractionEvent event) {
        CreateStep step = steps.get(event.getUser().getIdLong());
        EntitySelectMenu menu =
                EntitySelectMenu.create("xs:ticket:cr:category", EntitySelectMenu.SelectTarget.CHANNEL)
                        .setChannelTypes(ChannelType.CATEGORY)
                        .setRequiredRange(1, 1)
                        .setPlaceholder("未設定則為預設")
                        .build();

        step.hook.editOriginalComponents(
                ActionRow.of(menu),
                ActionRow.of(Button.of(ButtonStyle.PRIMARY, "xs:ticket:cr:back", "返回"))
        ).queue();
        event.deferEdit().queue();
    }

    private void mainMenu(ButtonInteractionEvent event) {
        CreateStep step = steps.get(event.getUser().getIdLong());
        step.updateEmbed();
        event.deferEdit().queue();
    }

    private void previewReason(ButtonInteractionEvent event) {
        CreateStep step = steps.get(event.getUser().getIdLong());
        TextInput reasonInput = TextInput.create("reason", "原因", TextInputStyle.PARAGRAPH).build();

        event.replyModal(
                Modal.create("xs:ticket:cr:preview", step.data.reasonTitle)
                        .addComponents(ActionRow.of(reasonInput))
                        .build()
        ).queue();
    }

    private void startCreate(SlashCommandInteractionEvent event) {
        DiscordLocale local = event.getUserLocale();

        CreateStep step = new CreateStep(event.getHook());

        step.updateEmbed();
        steps.put(event.getUser().getIdLong(), step);
    }

    private void authorForm(ButtonInteractionEvent event) {
        CreateStep step = steps.get(event.getUser().getIdLong());
        TextInput nameInput = TextInput.create("author", "設定作者名稱", TextInputStyle.SHORT)
                .setValue(step.data.author)
                .setPlaceholder("Ticket 服務")
                .setMaxLength(256)
                .setRequired(false)
                .build();

        TextInput imageInput = TextInput.create("image", "設定作者圖示", TextInputStyle.SHORT)
                .setValue(step.data.authorIconURL)
                .setPlaceholder("https://img .... 5_wh1200.png")
                .setMaxLength(256)
                .setRequired(false)
                .build();

        event.replyModal(
                Modal.create("xs:ticket:cr:author", "創建客服單")
                        .addComponents(ActionRow.of(nameInput), ActionRow.of(imageInput))
                        .build()
        ).queue();
    }

    private void content(ButtonInteractionEvent event) {
        CreateStep step = steps.get(event.getUser().getIdLong());
        TextInput titleInput = TextInput.create("title", "設定標題", TextInputStyle.SHORT)
                .setValue(step.data.title)
                .setPlaceholder("\uD83D\uDEE0 聯絡我們")
                .setMaxLength(256)
                .build();

        TextInput descInput = TextInput.create("desc", "設定內文", TextInputStyle.PARAGRAPH)
                .setValue(step.data.description)
                .setPlaceholder("\uD83D\uDEE0 聯絡我們")
                .setMaxLength(4000)
                .build();


        event.replyModal(
                Modal.create("xs:ticket:cr:content", "創建客服單")
                        .addComponents(
                                ActionRow.of(titleInput),
                                ActionRow.of(descInput)
                        )
                        .build()
        ).queue();
    }

    private void reasonForm(ButtonInteractionEvent event) {
        CreateStep step = steps.get(event.getUser().getIdLong());
        TextInput reasonInput = TextInput.create("reason", "設定原因", TextInputStyle.PARAGRAPH)
                .setValue(step.data.reasonTitle)
                .setPlaceholder("有任何可以幫助的問題嗎~")
                .setMaxLength(45)
                .build();

        event.replyModal(
                Modal.create("xs:ticket:cr:reason", "創建客服單")
                        .addComponents(ActionRow.of(reasonInput))
                        .build()
        ).queue();
    }

    private void adminMenu(ButtonInteractionEvent event) {
        CreateStep step = steps.get(event.getUser().getIdLong());
        EntitySelectMenu menu =
                EntitySelectMenu.create("xs:ticket:cr:admin", EntitySelectMenu.SelectTarget.ROLE)
                        .setMaxValues(25)
                        .build();


        step.hook.editOriginalComponents(
                ActionRow.of(menu),
                ActionRow.of(Button.of(ButtonStyle.PRIMARY, "xs:ticket:cr:back", "返回"))
        ).queue();
        event.deferEdit().queue();
    }

    private void colorForm(ButtonInteractionEvent event) {
        CreateStep step = steps.get(event.getUser().getIdLong());
        TextInput colorInput = TextInput.create("color", "設定顏色", TextInputStyle.SHORT)
                .setValue(String.format("#%06X", 0xFFFFFF & step.data.color))
                .setPlaceholder("0x00FFFF")
                .build();

        event.replyModal(
                Modal.create("xs:ticket:cr:color", "創建客服單")
                        .addComponents(ActionRow.of(colorInput))
                        .build()
        ).queue();
    }

    private void btnContentForm(ButtonInteractionEvent event) {
        CreateStep step = steps.get(event.getUser().getIdLong());
        TextInput btnTextInput = TextInput.create("btnText", "設定按鈕文字", TextInputStyle.SHORT)
                .setValue(step.data.btnContent)
                .setPlaceholder("聯絡我們")
                .setMaxLength(80)
                .build();

        TextInput btnEmojiInput = TextInput.create("btnEmoji", "設定按鈕符號", TextInputStyle.SHORT)
                .setValue(step.data.btnEmoji.getAsReactionCode())
                .setPlaceholder("✉")
                .setRequired(false)
                .build();

        event.replyModal(
                Modal.create("xs:ticket:cr:btnContent", "創建客服單")
                        .addComponents(
                                ActionRow.of(btnTextInput),
                                ActionRow.of(btnEmojiInput)
                        )
                        .build()
        ).queue();
    }

    private void btnColorMenu(ButtonInteractionEvent event) {
        CreateStep step = steps.get(event.getUser().getIdLong());
        StringSelectMenu menu =
                StringSelectMenu.create("xs:ticket:cr:btnColor")
                        .addOption("綠色", "3")
                        .addOption("藍色", "1")
                        .addOption("紅色", "4")
                        .addOption("灰色", "2")
                        .build();

        step.hook.editOriginalComponents(
                ActionRow.of(menu),
                ActionRow.of(Button.of(ButtonStyle.PRIMARY, "xs:ticket:cr:back", "返回"))
        ).queue();
        event.deferEdit().queue();
    }

    private void confirmCreate(ButtonInteractionEvent event) {
        CreateStep step = steps.get(event.getUser().getIdLong());
        long id = step.confirmCreate(event.getChannel());
        manager.getObj().add(String.valueOf(id), step.getJson());
        manager.save();
    }
}