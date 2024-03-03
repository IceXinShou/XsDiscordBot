package tw.xserver.economy;

public class UserData {
    private final long id;
    private String name;
    private int money;
    private int total;

    UserData(String name, long id) {
        this(name, id, 0, 0);
    }

    UserData(long id, int money, int total) {
        this(null, id, money, total);
    }

    UserData(String name, long id, int money, int total) {
        this.name = name;
        this.id = id;
        this.money = money;
        this.total = total;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    long getId() {
        return id;
    }

    int get() {
        return money;
    }

    int getTotal() {
        return total;
    }

    int add(int money) {
        this.money += money;
        this.total += money;
        return this.money;
    }

    int remove(int money) {
        this.money -= money;
        return this.money;
    }

    int set(int money) {
        this.money = money;
        return this.money;
    }

    int addTotal(int money) {
        this.total += money;
        return this.total;
    }

    int removeTotal(int money) {
        this.total -= money;
        return this.total;
    }

    int setTotal(int money) {
        this.total = money;
        return this.total;
    }
}