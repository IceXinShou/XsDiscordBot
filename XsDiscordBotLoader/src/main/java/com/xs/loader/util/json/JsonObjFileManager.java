package com.xs.loader.util.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonObjFileManager extends JsonFileManager<JsonObject> {
    public JsonObjFileManager(String FILE_PATH, String TAG) {
        super(FILE_PATH, TAG);
    }

    @Override
    protected Class<JsonObject> getDataType() {
        return JsonObject.class;
    }

    @Override
    protected JsonObject createData() {
        return new JsonObject();
    }

    public String getAsString(String key) {
        return data.get(key).getAsString();
    }

    public byte getAsByte(String key) {
        return data.get(key).getAsByte();
    }

    public short getAsShort(String key) {
        return data.get(key).getAsShort();
    }

    public int getAsInt(String key) {
        return data.get(key).getAsInt();
    }

    public long getAsLong(String key) {
        return data.get(key).getAsLong();
    }

    public Double getAsDouble(String key) {
        return data.get(key).getAsDouble();
    }

    public boolean getAsBoolean(String key) {
        return data.get(key).getAsBoolean();
    }

    public JsonObject getAsJsonObject(String key) {
        return data.get(key).getAsJsonObject();
    }

    public JsonArray getAsJsonArray(String key) {
        return data.get(key).getAsJsonArray();
    }

    public BigInteger getAsBigInteger(String key) {
        return data.get(key).getAsBigInteger();
    }

    public BigDecimal getAsBigDecimal(String key) {
        return data.get(key).getAsBigDecimal();
    }

    public JsonPrimitive getAsJsonPrimitive(String key) {
        return data.get(key).getAsJsonPrimitive();
    }

    public Number getAsNumber(String key) {
        return data.get(key).getAsNumber();
    }

    public String getAsString() {
        return data.getAsString();
    }

    public byte getAsByte() {
        return data.getAsByte();
    }

    public short getAsShort() {
        return data.getAsShort();
    }

    public int getAsInt() {
        return data.getAsInt();
    }

    public long getAsLong() {
        return data.getAsLong();
    }

    public Double getAsDouble() {
        return data.getAsDouble();
    }

    public boolean getAsBoolean() {
        return data.getAsBoolean();
    }

    public JsonArray getAsJsonArray() {
        return data.getAsJsonArray();
    }

    public BigInteger getAsBigInteger() {
        return data.getAsBigInteger();
    }

    public BigDecimal getAsBigDecimal() {
        return data.getAsBigDecimal();
    }

    public JsonPrimitive getAsJsonPrimitive() {
        return data.getAsJsonPrimitive();
    }

    public Number getAsNumber() {
        return data.getAsNumber();
    }

    public JsonObjFileManager add(String key, Number value) {
        data.addProperty(key, value);
        return this;
    }

    public JsonObjFileManager add(String key, String value) {
        data.addProperty(key, value);
        return this;
    }

    public JsonObjFileManager add(String key, Boolean value) {
        data.addProperty(key, value);
        return this;
    }

    public JsonObjFileManager add(String key, Character value) {
        data.addProperty(key, value);
        return this;
    }

    public JsonObjFileManager add(String key, JsonElement value) {
        data.add(key, value);
        return this;
    }

    public JsonElement getOrDefault(String key, JsonElement default_element) {
        if (data.has(key)) {
            return data.get(key);
        } else {
            return default_element;
        }
    }

    public JsonElement computeIfAbsent(String key, @NotNull JsonElement default_element) {
        if (data.has(key)) {
            return data.get(key);
        } else {
            data.add(key, default_element);
            return default_element;
        }
    }

    @Nullable
    public JsonElement computeIfPresent(String key, @Nullable JsonElement default_element) {
        if (data.has(key)) {
            if (default_element == null)
                data.remove(key);
            else
                data.add(key, default_element);
            return default_element;
        } else {
            return null;
        }
    }

    public JsonObjFileManager remove(String key) {
        if (data.has(key)) {
            data.remove(key);
        }
        return this;
    }

    public boolean has(String key) {
        return data.has(key);
    }
}
