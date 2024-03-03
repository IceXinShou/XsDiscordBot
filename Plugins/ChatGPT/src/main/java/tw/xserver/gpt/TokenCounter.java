package tw.xserver.gpt;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;

public class TokenCounter {

    //    private static final Encoding ENC =Encodings.newDefaultEncodingRegistry().getEncodingForModel(ModelType.GPT_3_5_TURBO);
    private static final Encoding ENC = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.P50K_BASE);

    public static int getToken(String content) {
        return ENC.encode(content).size() + 7;
    }
}
