package com.xs.chatlogger;

import com.xs.loader.util.JsonFileManager;

public class JsonManager {
    private final String TAG = "ChatLogger";
    private final String PATH_FOLDER_NAME = "./plugins/ChatLogger";
    private final JsonFileManager fileManager = new JsonFileManager(PATH_FOLDER_NAME + "/Data/data.json", TAG, true);


    public boolean isExist(long id) {
        return fileManager.getObj().has(String.valueOf(id));
    }


}
