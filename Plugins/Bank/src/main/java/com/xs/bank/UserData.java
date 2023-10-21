package com.xs.bank;

import java.util.HashMap;
import java.util.Map;

public class UserData {
    private final long userID;
    private final Map<String, Integer> money = new HashMap<>();

    UserData(final long ID) {
        this.userID = ID;
    }

    Map<String, Integer> get() {
        return money;
    }

    long getUserID() {
        return userID;
    }

    int add(String type, int value) {
        return money.merge(type, value, Integer::sum);
    }

    int remove(String type, int value) {
        return money.merge(type, -value, Integer::sum);
    }
}