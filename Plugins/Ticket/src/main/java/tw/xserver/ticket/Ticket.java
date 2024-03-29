package tw.xserver.ticket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.lang.LangManager;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;
import tw.xserver.loader.util.json.JsonObjFileManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.Permission.VIEW_CHANNEL;
import static net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN;
import static tw.xserver.loader.base.Loader.ROOT_PATH;

public class Ticket extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ticket.class);
    private static final String PATH_FOLDER_NAME = "plugins/Ticket";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private final Map<Long, Step> steps = new HashMap<>();
    private Language lang;
    private JsonObjFileManager manager;

    public Ticket() {
        super(true);

        reloadAll();
        LOGGER.info("loaded Ticket");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded Ticket");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, Ticket.class);
        manager = new JsonObjFileManager('/' + PATH_FOLDER_NAME + "/data.json");
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
                Commands.slash("create-ticket", "create custom reason of ticket")
                        .setNameLocalizations(lang.register.create.name)
                        .setDescriptionLocalizations(lang.register.create.description)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR)),
                Commands.slash("add-ticket", "create custom reason of ticket in the text-channel")
                        .setNameLocalizations(lang.register.add.name)
                        .setDescriptionLocalizations(lang.register.add.description)
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addOptions(
                        new OptionData(OptionType.STRING, "message_id", "for adding extra button", true)
                )
        };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "create-ticket": {
                startCreate(event);
                break;
            }

            case "add-ticket": {
                startAdd(event);
                break;
            }
        }

    }

    private void startAdd(SlashCommandInteractionEvent event) {
        DiscordLocale locale = event.getUserLocale();

        long messageID = event.getOption("message_id").getAsLong();

        Message message = event.getMessageChannel().retrieveMessageById(event.getOption("message_id").getAsLong())
                .onErrorFlatMap(i -> event.getHook().editOriginal("Cannot found the message by id: " + messageID))
                .complete();

        if (message.getIdLong() != messageID) return;

        if (!message.getActionRows().isEmpty() && message.getActionRows().get(0).getComponents().size() == 5) {
            event.getHook().editOriginal("This message is full of buttons, please recreate a new message").queue();
            return;
        }

        Step step = new Step(message, event.getHook());

        step.updateEmbed();
        steps.put(event.getUser().getIdLong(), step);
    }


    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("ticket")) return;
        DiscordLocale locale = event.getUserLocale();
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

            return;
        }

        switch (args[2]) {
            case "btn": {
                JsonObject tmp;

                if ((tmp = manager.getAsJsonArray(event.getMessageId()).get(Integer.parseInt(args[3])).getAsJsonObject()) == null) {
                    event.deferReply(true).addContent("錯誤").queue();
                    return;
                }

                String reason = tmp.getAsJsonObject().get("reasonTitle").getAsString();
                TextInput reasonInput = TextInput.create("reason", "原因", TextInputStyle.PARAGRAPH).build();
                event.replyModal(
                        Modal.create("xs:ticket:push:" + event.getMessageId() + ':' + args[3], reason)
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
                                Button.of(ButtonStyle.SUCCESS, "xs:ticket:unlock:" + args[3] + ':' + args[4] + ':' + args[5], "開啟", Emoji.fromUnicode("🔓")),
                                Button.of(ButtonStyle.DANGER, "xs:ticket:delete:" + args[3] + ':' + args[4] + ':' + args[5], "刪除", Emoji.fromUnicode("🗑")))
                ).queue();
                break;
            }

            case "unlock": {
                TextChannel channel = event.getGuildChannel().asTextChannel();
                Member member = event.getGuild().retrieveMemberById(args[4]).complete();
                channel.upsertPermissionOverride(member).grant(VIEW_CHANNEL).queue();

                event.editComponents(
                        ActionRow.of(
                                Button.of(ButtonStyle.DANGER, "xs:ticket:lock:" + args[3] + ':' + args[4] + ':' + args[5], "關閉", Emoji.fromUnicode("🔒")),
                                Button.of(ButtonStyle.DANGER, "xs:ticket:delete:" + args[3] + ':' + args[4] + ':' + args[5], "刪除", Emoji.fromUnicode("🗑")))
                ).queue();
                break;
            }

            case "delete": {
                List<Role> roles = new ArrayList<>();
                Member member = event.getMember();
                Guild guild = event.getGuild();

                for (JsonElement i : manager.getAsJsonArray(args[3]).get(Integer.parseInt(args[5])).getAsJsonObject()
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


        Step step = steps.get(event.getUser().getIdLong());
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

                case "btnContent": {
                    step.setBtnContent(event.getValue("btnText").getAsString());

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

            return;
        }

        switch (args[2]) {
            case "push": {
                JsonObject tmp;
                if ((tmp = manager.getAsJsonArray(args[3]).get(Integer.parseInt(args[4])).getAsJsonObject()) == null) {
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
                        Button.of(ButtonStyle.DANGER, "xs:ticket:lock:" + args[3] + ':' + event.getUser().getId() + ':' + args[4], "關閉", Emoji.fromUnicode("🔒")),
                        Button.of(ButtonStyle.DANGER, "xs:ticket:delete:" + args[3] + ':' + event.getUser().getId() + ':' + args[4], "刪除", Emoji.fromUnicode("🗑"))
                ).queue();
            }
        }
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("ticket")) return;

        Step step = steps.get(event.getUser().getIdLong());

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

        Step step = steps.get(event.getUser().getIdLong());

        if (args[3].equals("btnColor")) {
            step.setBtnStyle(ButtonStyle.fromKey(Integer.parseInt(event.getValues().get(0))));
            step.updateEmbed();
            event.deferEdit().queue();
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (manager.has(event.getMessageId())) {
            manager.remove(event.getMessageId());
            manager.save();
        }
    }

    private void categoryMenu(ButtonInteractionEvent event) {
        Step step = steps.get(event.getUser().getIdLong());
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
        Step step = steps.get(event.getUser().getIdLong());
        step.updateEmbed();
        event.deferEdit().queue();
    }

    private void previewReason(ButtonInteractionEvent event) {
        Step step = steps.get(event.getUser().getIdLong());
        TextInput reasonInput = TextInput.create("reason", "原因", TextInputStyle.PARAGRAPH).build();

        event.replyModal(
                Modal.create("xs:ticket:cr:preview", step.data.reasonTitle)
                        .addComponents(ActionRow.of(reasonInput))
                        .build()
        ).queue();
    }

    private void startCreate(SlashCommandInteractionEvent event) {
        DiscordLocale locale = event.getUserLocale();

        Step step = new Step(event.getHook());

        step.updateEmbed();
        steps.put(event.getUser().getIdLong(), step);
    }

    private void authorForm(ButtonInteractionEvent event) {
        Step step = steps.get(event.getUser().getIdLong());
        TextInput nameInput = TextInput.create("author", "設定作者名稱", TextInputStyle.SHORT)
                .setValue(step.data.author)
                .setPlaceholder("Ticket 服務")
                .setMaxLength(256)
                .setRequired(false)
                .build();

        TextInput imageInput = TextInput.create("image", "設定作者圖示", TextInputStyle.PARAGRAPH)
                .setValue(step.data.authorIconURL)
                .setPlaceholder("https://img .... 5_wh1200.png")
                .setMaxLength(4000)
                .setRequired(false)
                .build();

        event.replyModal(
                Modal.create("xs:ticket:cr:author", "創建客服單")
                        .addComponents(ActionRow.of(nameInput), ActionRow.of(imageInput))
                        .build()
        ).queue();
    }

    private void content(ButtonInteractionEvent event) {
        Step step = steps.get(event.getUser().getIdLong());
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
        Step step = steps.get(event.getUser().getIdLong());
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
        Step step = steps.get(event.getUser().getIdLong());
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
        Step step = steps.get(event.getUser().getIdLong());
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
        Step step = steps.get(event.getUser().getIdLong());
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
        Step step = steps.get(event.getUser().getIdLong());
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
        Step step = steps.get(event.getUser().getIdLong());
        long id;

        if (step.message != null)
            id = step.confirmCreate();
        else
            id = step.confirmCreate(event.getChannel());

        manager.computeIfAbsent(String.valueOf(id), new JsonArray()).getAsJsonArray().add(step.getJson());
        manager.save();
    }
}