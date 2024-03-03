package tw.xserver.api.placeholder;

public class Placeholder {
    public void register(String key, ValueGetter value) {
        Papi.placeholders.put(key, value);
    }

    public String replace(String input) {
        StringBuilder result = new StringBuilder();
        int start, end = -1, lastEnd = 0;
        ValueGetter value;
        while ((start = input.indexOf('%', end + 1)) != -1 &&
                (end = input.indexOf('%', start + 1)) != -1) {
            if (start + 1 == end) {
                if (lastEnd != start)
                    result.append(input, lastEnd, start);
                result.append('%');
                lastEnd = end + 1;
                continue;
            }

            if ((value = Papi.placeholders.get(input.substring(start + 1, end))) == null) continue;
            if (lastEnd != start) result.append(input, lastEnd, start);
            result.append(value.value());
            lastEnd = end + 1;
        }

        if (lastEnd == 0)
            return input;

        result.append(input, lastEnd, input.length());
        return result.toString();
    }

//    public static void main(String[] args) {
//        Placeholder placeholder = new Placeholder();
//
//        placeholder.register("gg", () -> "30cm");
//        System.out.println(placeholder.replace("you gg is %gg%"));
//    }
}