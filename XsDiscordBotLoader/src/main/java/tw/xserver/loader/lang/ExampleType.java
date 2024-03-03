package tw.xserver.loader.lang;

public abstract class ExampleType {
    public abstract static class BasicCommand {
        public LocaleData name;
        public LocaleData description;
    }

    public static class Command_t extends BasicCommand {
    }

    public static class String_t extends BasicCommand {
    }

    public static class Integer_t extends BasicCommand {
    }

    public static class Boolean_t extends BasicCommand {
    }

    public static class User_t extends BasicCommand {
    }

    public static class Channel_t extends BasicCommand {
    }

    public static class Role_t extends BasicCommand {
    }

    public static class Mentionable_t extends BasicCommand {
    }

    public static class Member_t extends BasicCommand {
    }

    public static class Attachment_t extends BasicCommand {
    }
}
