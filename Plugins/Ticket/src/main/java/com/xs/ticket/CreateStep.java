package com.xs.ticket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.istack.internal.Nullable;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;

import java.util.List;

import static net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.PRIMARY;
import static net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.SUCCESS;

public class CreateStep {
    public final StepData data;
    public final InteractionHook hook;

    public CreateStep(InteractionHook hook) {
        this.hook = hook;
        data = new StepData();
    }

    public void updateEmbed() {
        hook.editOriginalEmbeds(getPreviewEmbed())
                .setComponents(
                        ActionRow.of(getPreviewComponent()),

                        ActionRow.of(
                                new ButtonImpl("xs:ticket:cr:author", "設定作者", PRIMARY, false, null),
                                new ButtonImpl("xs:ticket:cr:content", "設定文字", PRIMARY, false, null),
                                new ButtonImpl("xs:ticket:cr:reason", "設定原因", PRIMARY, false, null),
                                new ButtonImpl("xs:ticket:cr:admin", "設定允許身分組", PRIMARY, false, null),
                                new ButtonImpl("xs:ticket:cr:color", "設定顏色", PRIMARY, false, null)
                        ),

                        ActionRow.of(
                                new ButtonImpl("xs:ticket:cr:btnContent", "設定按鈕文字", PRIMARY, false, null),
                                new ButtonImpl("xs:ticket:cr:btnColor", "設定按鈕顏色", PRIMARY, false, null),
                                new ButtonImpl("xs:ticket:cr:category", "設定頻道目錄", PRIMARY, false, null),
                                new ButtonImpl("xs:ticket:cr:confirm", "確定建立", SUCCESS, false, null)
                        )
                ).queue();
    }

    private MessageEmbed getPreviewEmbed() {
        return new EmbedBuilder()
                .setAuthor(data.author, null, data.authorIconURL)
                .setTitle(data.title)
                .setDescription(data.description)
                .setColor(data.color).build();
    }

    private ButtonImpl getPreviewComponent() {
        return new ButtonImpl("xs:ticket:cr:btn", data.btnContent, data.btnStyle, false, data.btnEmoji);
    }

    public JsonObject getJson() {
        JsonObject object = new JsonObject();
        JsonArray array = new JsonArray();

        for (Long i : data.adminIDs)
            array.add(i);

        object.addProperty("reasonTitle", data.reasonTitle);
        object.add("adminIDs", array);
        object.addProperty("categoryID", data.categoryID);

        return object;
    }

    public void setAuthor(@Nullable String name, @Nullable String iconURL) {
        if (name == null) {
            data.author = null;
            data.authorIconURL = null;
            return;
        }

        data.author = name;
        data.authorIconURL = iconURL;
    }

    public void setTitle(String title) {
        if (!title.isEmpty()) {
            data.title = title;
        }
    }

    public void setDesc(String desc) {
        if (!desc.isEmpty()) {
            data.description = desc;
        }
    }

    public void setReason(String reason) {
        if (!reason.isEmpty()) {
            data.reasonTitle = reason;
        }
    }

    public void setAdmin(List<Long> adminID) {
        data.adminIDs = adminID;
    }

    public void setCategoryID(long categoryID) {
        data.categoryID = categoryID;
    }

    public void setColor(int color) {
        data.color = color;
    }

    public void setBtnContent(String content) {
        data.btnContent = content;
    }

    public void setBtnEmoji(@Nullable Emoji emoji) {
        data.btnEmoji = emoji;
    }

    public void setBtnStyle(ButtonStyle style) {
        data.btnStyle = style;
    }

    public long confirmCreate(MessageChannelUnion channel) {
        Message message = channel.sendMessageEmbeds(getPreviewEmbed()).complete();
        message.editMessageComponents(ActionRow.of(
                new ButtonImpl("xs:ticket:btn", data.btnContent, data.btnStyle, false, data.btnEmoji)
        )).queue();

        hook.deleteOriginal().queue();
        return message.getIdLong();
    }
}
