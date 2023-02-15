package com.xs.officialguild;

public class UserStepData {
    public String chineseName = null;
    public String englishName = null;
    public String mc_uuid = null;

    /**
     * @return <code>true</code> if chinese name set
     */
    public boolean chi() {
        return chineseName != null;
    }

    /**
     * @return <code>true</code> if english name set
     */
    public boolean eng() {
        return englishName != null && mc_uuid != null;
    }

    /**
     *
     * @return <code>true</code> if all set
     */
    public boolean verify() {
        return chi() && eng();
    }
}
