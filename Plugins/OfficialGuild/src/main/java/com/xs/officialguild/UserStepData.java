package com.xs.officialguild;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

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
        return !chineseName.equals("");
    }

    /**
     * @return <code>true</code> if english name set
     */
    public boolean eng() {
        return !englishName.equals("") || !mc_uuid.equals("");
    }

    /**
     * @return <code>true</code> if all set
     */
    public boolean verify() {
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
                .addField("英文暱稱", !englishName.equals("") ? englishName : "尚未設定", true);

        if (!mc_uuid.equals("")) {
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

    JSONObject getObj() {
        JSONObject obj = new JSONObject();
        obj.put("chi", chineseName).put("eng", englishName);
        if (!mc_uuid.equals("")) obj.put("mc", mc_uuid);
        return obj;
    }

    String getNick() {
        if (!mc_uuid.equals(""))
            return chineseName + " - " + uuidToName();

        return chineseName + " - " + englishName + "*";
    }

    @Nullable
    String uuidToName() {
        JSONObject respond = new JSONObject(getData("https://sessionserver.mojang.com/session/minecraft/profile/" + mc_uuid));
        if (respond.has("name"))
            return respond.getString("name");

        return null;
    }
}
