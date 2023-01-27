package com.xs.memberpoint;

public class UserData {
    private final long ID;
    private int money;
    private int total;

    UserData(final long ID) {
        this.ID = ID;
        this.money = 0;
        this.total = 0;
    }

    UserData(final long ID, final int MONEY, final int TOTAL) {
        this.ID = ID;
        this.money = MONEY;
        this.total = TOTAL;
    }

    long getID() {
        return ID;
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