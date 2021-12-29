package io.github.humbleui.types;

import clojure.lang.*;
import lombok.*;
import org.jetbrains.annotations.*;

@Data
@EqualsAndHashCode(callSuper=false)
@With
public class IPoint extends ARecord {
    public static final IPoint ZERO = new IPoint(0, 0);

    @ApiStatus.Internal public final int _x;
    @ApiStatus.Internal public final int _y;

    @ApiStatus.Internal
    public static IPoint _makeFromLong(long l) {
        return new IPoint((int) (l >>> 32), (int) (l & 0xFFFFFFFF));
    }

    @NotNull
    public IPoint offset(int dx, int dy) {
        return dx == 0 && dy == 0 ? this : new IPoint(_x + dx, _y + dy);
    }

    @NotNull
    public IPoint offset(@NotNull IPoint vec) {
        assert vec != null : "IPoint::offset expected other != null";
        return offset(vec._x, vec._y);
    }

    @NotNull
    public IPoint scale(int scale) {
        return scale(scale, scale);
    }

    @NotNull
    public IPoint scale(int sx, int sy) {
        return (sx == 1 && sy == 1) || (_x == 0 && _y == 0) ? this : new IPoint(_x * sx, _y * sy);
    }

    @NotNull
    public IPoint inverse() {
        return scale(-1, -1);
    }

    public boolean isEmpty() {
        return _x <= 0 || _y <= 0;
    }

    @NotNull @Contract("-> new")
    public Point toPoint() {
        return new Point(_x, _y);
    }

    // ILookup
    public static final Keyword _KEYWORD_X = Keyword.intern(null, "x");
    public static final Keyword _KEYWORD_Y = Keyword.intern(null, "y");
    public static final Keyword _KEYWORD_WIDTH  = Keyword.intern(null, "width");
    public static final Keyword _KEYWORD_HEIGHT = Keyword.intern(null, "height");

    @Override
    public Object valAt(Object key, Object notFound) {
        if (_KEYWORD_X == key || _KEYWORD_WIDTH == key)
            return _x;
        else if (_KEYWORD_Y == key || _KEYWORD_HEIGHT == key)
            return _y;
        else
            return notFound;
    }

    // Seqable
    @Override
    public ISeq seq() {
        IPersistentCollection ret = PersistentList.EMPTY;
        ret = ret.cons(MapEntry.create(_KEYWORD_Y, _y));
        ret = ret.cons(MapEntry.create(_KEYWORD_X, _x));
        return ret.seq();
    }

    // IPersistentCollection
    @Override
    public int count() {
        return 2;
    }

    // Associative
    @Override
    public boolean containsKey(Object key) {
        return key == _KEYWORD_X || key == _KEYWORD_Y || key == _KEYWORD_WIDTH || key == _KEYWORD_HEIGHT;
    }

    @Override
    public IMapEntry entryAt(Object key) {
        if (_KEYWORD_X == key)
            return MapEntry.create(_KEYWORD_X, _x);
        else if (_KEYWORD_Y == key)
            return MapEntry.create(_KEYWORD_Y, _y);
        else if (_KEYWORD_WIDTH == key)
            return MapEntry.create(_KEYWORD_WIDTH, _x);
        else if (_KEYWORD_HEIGHT == key)
            return MapEntry.create(_KEYWORD_HEIGHT, _y);
        else
            return null;
    }

    @Override
    public Associative assoc(Object key, Object val) {
        int intVal = RT.intCast(val);
        if (_KEYWORD_X == key || _KEYWORD_WIDTH == key)
            return withX(intVal);
        else if (_KEYWORD_Y == key || _KEYWORD_HEIGHT == key)
            return withY(intVal);
        else
            throw new IllegalArgumentException("assoc " + key + " is not supported");
    }
}