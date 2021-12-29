package io.github.humbleui.types;

import clojure.lang.*;
import lombok.*;
import org.jetbrains.annotations.*;

@Data
@EqualsAndHashCode(callSuper=false)
@With
public class Point extends ARecord {
    public static final Point ZERO = new Point(0, 0);

    @ApiStatus.Internal public final float _x; 
    @ApiStatus.Internal public final float _y;

    @Contract("null -> null; !null -> new")
    public static @Nullable float[] flattenArray(@Nullable Point[] pts) {
        if (pts == null) return null;
        float[] arr = new float[pts.length * 2];
        for (int i = 0; i < pts.length; ++i) {
            arr[i * 2]     = pts[i]._x;
            arr[i * 2 + 1] = pts[i]._y;
        }
        return arr;
    }

    @Contract("null -> null; !null -> new")
    public static @Nullable Point[] fromArray(@Nullable float[] pts) {
        if (pts == null) return null;
        assert pts.length % 2 == 0 : "Expected " + pts.length + " % 2 == 0";
        Point[] arr = new Point[pts.length / 2];
        for (int i = 0; i < pts.length / 2; ++i)
            arr[i] = new Point(pts[i * 2], pts[i * 2 + 1]);
        return arr;
    }

    @NotNull
    public Point offset(float dx, float dy) {
        return dx == 0 && dy == 0 ? this : new Point(_x + dx, _y + dy);
    }

    @NotNull
    public Point offset(@NotNull Point vec) {
        assert vec != null : "Point::offset expected other != null";
        return offset(vec._x, vec._y);
    }

    @NotNull
    public Point scale(float scale) {
        return scale(scale, scale);
    }

    @NotNull
    public Point scale(float sx, float sy) {
        return (sx == 1 && sy == 1) || (_x == 0 && _y == 0) ? this : new Point(_x * sx, _y * sy);
    }

    @NotNull
    public Point inverse() {
        return scale(-1, -1);
    }

    public boolean isEmpty() {
        return _x <= 0 || _y <= 0;
    }

    @NotNull @Contract("-> new")
    public IPoint toIPoint() {
        return new IPoint((int) _x, (int) _y);
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
        float floatVal = RT.floatCast(val);
        if (_KEYWORD_X == key || _KEYWORD_WIDTH == key)
            return withX(floatVal);
        else if (_KEYWORD_Y == key || _KEYWORD_HEIGHT == key)
            return withY(floatVal);
        else
            throw new IllegalArgumentException("assoc " + key + " is not supported");
    }
}