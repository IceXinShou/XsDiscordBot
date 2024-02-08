package com.xs.officialguild;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;

import static com.xs.loader.util.UrlDataGetter.getData;
import static net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.*;

public class UserStepData {
    public InteractionHook hook;
    public String chineseName = "";
    public String englishName = "";
    public String mc_uuid = "";

    /**
     * @return <code>true</code> if chinese name set
     */
    public boolean chi() {
        return !chineseName.isEmpty();
    }

    /**
     * @return <code>true</code> if english name set
     */
    public boolean eng() {
        return !englishName.isEmpty() || !mc_uuid.isEmpty();
    }

    /**
     * @return <code>true</code> if all set
     */
    private boolean verify() {
        return !chi() || !eng();
    }


    public void updateEmbed() {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("設定暱稱")
                .setDescription("請完成下方按鈕資訊")
                .setFooter("驗證中")
                .setTimestamp(OffsetDateTime.now())
                .setColor(0x00FFFF)
                .addField("中文暱稱", chi() ? chineseName : "尚未設定", true)
                .addField("英文暱稱", !englishName.isEmpty() ? englishName : "尚未設定", true);

        if (!mc_uuid.isEmpty()) {
            builder.addField("Minecraft UUID", mc_uuid, true);
        }

        hook.editOriginalEmbeds(builder.build())
                .setActionRow(
                        new ButtonImpl("xs:og:chi", "中文暱稱", chi() ? SUCCESS : DANGER, false, null),
                        new ButtonImpl("xs:og:eng", "英文暱稱", eng() ? SUCCESS : DANGER, false, null),
                        new ButtonImpl("xs:og:verify", "同意群規", PRIMARY, verify(), null)
                )
                .queue();
    }

    public JsonObject getObj() {
        JsonObject obj = new JsonObject();
        obj.addProperty("chi", chineseName);
        obj.addProperty("eng", englishName);
        if (!mc_uuid.isEmpty())
            obj.addProperty("mc", mc_uuid);
        return obj;
    }

    public String getNick() {
        if (!mc_uuid.isEmpty())
            return chineseName + " - " + uuidToName();

        return chineseName + " - " + englishName + "*";
    }

    @Nullable
    private String uuidToName() {
        JsonObject respond = JsonParser.parseString(
                getData("https://sessionserver.mojang.com/session/minecraft/profile/" + mc_uuid)
        ).getAsJsonObject();

        if (respond.has("name"))
            return respond.get("name").getAsString();

        return null;
    }
}
