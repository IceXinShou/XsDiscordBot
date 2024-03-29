package tw.xserver.api.google.sheet;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import javafx.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import static tw.xserver.api.google.sheet.SheetAPI.configFile;

public class SheetRequest {
    public final Sheets service;
    private static final Logger LOGGER = LoggerFactory.getLogger(SheetRequest.class);
    private final List<String> SCOPES = Collections.singletonList(SheetsScopes.DRIVE);
    private final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private final MainConfig config;
    private String sheetID = "";
    private String sheetRange = "";
    private List<List<Object>> data;

    public SheetRequest() throws IOException, GeneralSecurityException {
        config = configFile;

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName("Google Sheets API")
                .build();
    }

    public SheetRequest(MainConfig config) throws IOException, GeneralSecurityException {
        this.config = config;

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName("Google Sheets API")
                .build();
    }

    public List<List<Object>> getData() {
        return data;
    }

    public void set(String sheetID, String sheetRange) {
        this.sheetID = sheetID;
        this.sheetRange = sheetRange;
    }

    @Nullable
    public Pair<Integer, Integer> where(String range, String key) throws IOException {
        refresh(range);

        if (data == null) {
            return null;
        }

        for (int i = 0; i < data.size(); i++) {
            List<Object> tmp = data.get(i);
            for (int j = 0; j < tmp.size(); j++) {
                if (tmp.get(j).equals(key))
                    return new Pair<>(j, i);
            }
        }

        return null;
    }

    public void setSheetID(String sheetID) {
        this.sheetID = sheetID;
    }

    public void setSheetRange(String sheetRange) {
        this.sheetRange = sheetRange;
    }

    public boolean refresh() throws IOException {
        // return true, if data updated

        if (sheetID.isEmpty()) {
            LOGGER.error("sheetID is empty");
            return false;
        }
        if (sheetRange.isEmpty()) {
            LOGGER.error("sheetRange is empty");
            return false;
        }

        List<List<Object>> tmp = service.spreadsheets().values()
                .get(sheetID, sheetRange)
                .execute().getValues();


        if (tmp == null || tmp.isEmpty()) {
            LOGGER.warn("No data found");
            return false;
        }

        if (data != null && data.equals(tmp)) return false;

        data = tmp;
        return true;
    }

    public boolean refresh(String sheetRange) throws IOException {
        this.sheetRange = sheetRange;

        return refresh();
    }

    public boolean refresh(String sheetID, String sheetRange) throws IOException {
        this.sheetID = sheetID;
        this.sheetRange = sheetRange;

        return refresh();
    }

    public UpdateValuesResponse write(List<List<Object>> values, String range, ValueInputOption option) throws IOException {
        return service.spreadsheets().values()
                .update(sheetID, range, new ValueRange().setValues(values))
                .setValueInputOption(option.name())
                .execute();
    }

    public AppendValuesResponse append_down(List<List<Object>> values, String range, ValueInputOption option) throws IOException {
        return service.spreadsheets().values()
                .append(sheetID, range, new ValueRange().setValues(values))
                .setValueInputOption(option.name())
                .setInsertDataOption("INSERT_ROWS")
                .setIncludeValuesInResponse(true)
                .execute();
    }


    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, config.client_id, config.client_secret, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("plugins/GoogleSheetAPI/tokens")))
                .setAccessType("offline")
                .build();

        return null;
        // TODO: check mark
//        localeServerReceiver receiver = new localeServerReceiver.Builder().setPort(config.port).build();
//        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public char toUpperAlpha(final int digit) {
        return (char) (digit + 65);
    }

    public enum ValueInputOption {
        RAW,
        USER_ENTERED
    }
}
