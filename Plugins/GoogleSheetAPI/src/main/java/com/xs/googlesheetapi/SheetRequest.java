package com.xs.googlesheetapi;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.xs.loader.logger.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class SheetRequest {

    private final Sheets service;
    private final Logger logger;
    private String sheetID = "";
    private String sheetRange = "";
    private List<List<Object>> data;
    private final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
    private final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private final MainConfig config;

    public SheetRequest(Logger logger, MainConfig config) throws IOException, GeneralSecurityException {
        this.logger = logger;
        this.config = config;

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName("Google Sheets API")
                .build();

        refresh();
    }

    public List<List<Object>> getData() {
        return data;
    }

    public void set(String sheetID, String sheetRange) {
        this.sheetID = sheetID;
        this.sheetRange = sheetRange;
    }

    public boolean refresh() throws IOException {
        if (sheetID.equals("") || sheetRange.equals("")) return false;

        List<List<Object>> tmp = service.spreadsheets().values()
                .get(sheetID, sheetRange)
                .execute().getValues();


        if (data == null || data.isEmpty()) logger.warn("No data found");

        if (data.equals(tmp)) return false;

        data = tmp;
        return true;
    }


    public boolean refresh(String sheetRange) throws IOException {
        if (sheetID.equals("")) return false;

        this.sheetRange = sheetRange;

        List<List<Object>> tmp = service.spreadsheets().values()
                .get(sheetID, sheetRange)
                .execute().getValues();

        if (data.equals(tmp)) return false;

        data = tmp;
        return true;
    }

    public boolean refresh(String sheetID, String sheetRange) throws IOException {
        this.sheetID = sheetID;
        this.sheetRange = sheetRange;

        List<List<Object>> tmp = service.spreadsheets().values()
                .get(sheetID, sheetRange)
                .execute().getValues();

        if (data.equals(tmp)) return false;

        data = tmp;
        return true;
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        String PATH_FOLDER_NAME = "./plugins/GoogleSheetAPI/";
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, config.client_id, config.client_secret, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(PATH_FOLDER_NAME + "tokens")))
                .setAccessType("offline")
                .build();
        new LocalServerReceiver.Builder().setPort(config.port).build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }
}