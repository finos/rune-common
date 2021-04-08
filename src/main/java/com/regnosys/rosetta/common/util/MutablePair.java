package com.regnosys.rosetta.common.util;

public class MutablePair<L, R> {
    private static final long serialVersionUID = 4954918890077093841L;
    public L left;
    public R right;

    public static <L, R> MutablePair<L, R> of(L left, R right) {
        return new MutablePair(left, right);
    }

    public MutablePair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return this.left;
    }

    public void setLeft(L left) {
        this.left = left;
    }

    public R getRight() {
        return this.right;
    }

    public void setRight(R right) {
        this.right = right;
    }

    public R setValue(R value) {
        R result = this.getRight();
        this.setRight(value);
        return result;
    }

    public void set(L left, R right) {
        this.setLeft(left);
        this.setRight(right);
    }
}