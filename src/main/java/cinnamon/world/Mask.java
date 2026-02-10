package cinnamon.world;

public class Mask {
    private int mask, excludeMask;

    public Mask() {
        this(0b1);
    }

    public Mask(int mask) {
        this(mask, 0b0);
    }

    public Mask(int mask, int excludeMask) {
        this.mask = mask;
        this.excludeMask = excludeMask;
    }

    public boolean test(Mask other) {
        return this.test(other.mask, other.excludeMask);
    }

    public boolean test(int mask, int excludeMask) {
        return (this.mask & mask) != 0 && (this.excludeMask & mask) == 0 && (this.mask & excludeMask) == 0;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    public void setMask(int index, boolean bool) {
        if (bool) mask |= (1 << index);
        else      mask &= ~(1 << index);
    }

    public int getMask() {
        return mask;
    }

    public void setExcludeMask(int excludeMask) {
        this.excludeMask = excludeMask;
    }

    public void setExcludeMask(int index, boolean bool) {
        if (bool) excludeMask |= (1 << index);
        else      excludeMask &= ~(1 << index);
    }

    public int getExcludeMask() {
        return excludeMask;
    }
}
