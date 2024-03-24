package tw.xserver.loader.util.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonAryFileManager extends JsonFileManager<JsonArray> {
    public JsonAryFileManager(String FILE_PATH) {
        super(FILE_PATH);
    }

    @Override
    protected Class<JsonArray> getDataType() {
        return JsonArray.class;
    }

    @Override
    protected JsonArray createData() {
        return new JsonArray();
    }

    public JsonElement get(int index) {
        return data.get(index);
    }

    public String getAsString(int index) {
        return data.get(index).getAsString();
    }

    public byte getByte(int index) {
        return data.get(index).getAsByte();
    }

    public short getShort(int index) {
        return data.get(index).getAsShort();
    }

    public int getInt(int index) {
        return data.get(index).getAsInt();
    }

    public long getLong(int index) {
        return data.get(index).getAsLong();
    }

    public Double getDouble(int index) {
        return data.get(index).getAsDouble();
    }

    public boolean getBoolean(int index) {
        return data.get(index).getAsBoolean();
    }

    public JsonObject getJsonObject(int index) {
        return data.get(index).getAsJsonObject();
    }

    public JsonArray getJsonArray(int index) {
        return data.get(index).getAsJsonArray();
    }

    public BigInteger getBigInteger(int index) {
        return data.get(index).getAsBigInteger();
    }

    public BigDecimal getBigDecimal(int index) {
        return data.get(index).getAsBigDecimal();
    }

    public JsonPrimitive getJsonPrimitive(int index) {
        return data.get(index).getAsJsonPrimitive();
    }

    public Number getNumber(int index) {
        return data.get(index).getAsNumber();
    }

    public JsonAryFileManager add(Number number) {
        data.add(number);
        return this;
    }

    public JsonAryFileManager add(String string) {
        data.add(string);
        return this;
    }

    public JsonAryFileManager add(Boolean bool) {
        data.add(bool);
        return this;
    }

    public JsonAryFileManager add(Character character) {
        data.add(character);
        return this;
    }

    public JsonAryFileManager add(JsonElement element) {
        data.add(element);
        return this;
    }

    public JsonAryFileManager addAll(JsonArray jsonArray) {
        data.addAll(jsonArray);
        return this;
    }

    public boolean remove(int index) {
        try {
            data.remove(index);
        } catch (IndexOutOfBoundsException e) {
            return true;
        }
        return false;
    }
}
