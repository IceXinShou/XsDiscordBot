package com.xs.ticket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.istack.internal.Nullable;
import net.dv8tion.jda.api.EmbedBuilder;
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
        data = new StepData(hook.getInteraction().getIdLong());
    }

    public void updateEmbed() {
        hook.editOriginalEmbeds(getPreviewEmbed())
                .setComponents(
                        ActionRow.of(getPreviewComponent()),

                        ActionRow.of(
                                new ButtonImpl("xs:ticket:cr:author", "設定作者", PRIMARY, false, null),
                                new ButtonImpl("xs:ticket:cr:title", "設定標體", PRIMARY, false, null),
                                new ButtonImpl("xs:ticket:cr:desc", "設定內文", PRIMARY, false, null),
                                new ButtonImpl("xs:ticket:cr:reason", "設定原因", PRIMARY, false, null),
                                new ButtonImpl("xs:ticket:cr:admin", "設定允許身分組", PRIMARY, false, null)
                        ),

                        ActionRow.of(
                                new ButtonImpl("xs:ticket:cr:color", "設定顏色", PRIMARY, false, null),
                                new ButtonImpl("xs:ticket:cr:btnText", "設定按鈕文字", PRIMARY, false, null),
                                new ButtonImpl("xs:ticket:cr:btnEmoji", "設定按鈕符號", PRIMARY, false, null),
                                new ButtonImpl("xs:ticket:cr:btnColor", "設定按鈕顏色", PRIMARY, false, null),
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

    public void confirmCreate(MessageChannelUnion channel) {
        channel.sendMessageEmbeds(getPreviewEmbed())
                .setActionRow(
                        new ButtonImpl("xs:ticket:btn:" + data.uniqueId, data.btnContent, data.btnStyle, false, data.btnEmoji)
                )
                .complete();

        hook.deleteOriginal().queue();
    }
}
