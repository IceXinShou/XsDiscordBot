package com.xs.chatlogger;

import java.util.Set;

public class ChannelSetting {
    public static class ListData {
        public long id;
        public boolean receive;
        public boolean update;
        public boolean delete;

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ListData)
                return id == ((ListData) obj).id;

            return false;
        }

        @Override
        public int hashCode() {
            return String.valueOf(id).hashCode();
        }
    }

    public boolean whitelist;
    public Set<ListData> white;
    public Set<ListData> black;
}
