package io.github.humbleui.types;

import clojure.lang.*;
import lombok.*;
import org.jetbrains.annotations.*;

@Getter
@EqualsAndHashCode(callSuper=false)
@ToString
@With
public class IRect extends ARecord {
    public final int _left;
    public final int _top;
    public final int _right;
    public final int _bottom;

    @ApiStatus.Internal
    public IRect(int l, int t, int r, int b) {
        _left = l;
        _top = t;
        _right = r;
        _bottom = b;
    }

    public int getWidth() {
        return _right - _left;
    }

    public int getHeight() {
        return _bottom - _top;
    }

    @NotNull @Contract("_ -> new")
    public IRect withWidth(int width) {
        return new IRect(_left, _top, _left + width, _bottom);
    }

    @NotNull @Contract("_ -> new")
    public IRect withHeight(int height) {
        return new IRect(_left, _top, _right, _top + height);
    }

    @NotNull @Contract("_, _, _, _ -> new")
    public static IRect makeLTRB(int l, int t, int r, int b) {
        if (l > r)
            throw new IllegalArgumentException("IRect::makeLTRB expected l <= r, got " + l + " > " + r);
        if (t > b)
            throw new IllegalArgumentException("IRect::makeLTRB expected t <= b, got " + t + " > " + b);
        return new IRect(l, t, r, b);
    }

    @NotNull @Contract("_, _, _, _ -> new")
    public static IRect makeXYWH(int l, int t, int w, int h) {
        if (w < 0)
            throw new IllegalArgumentException("IRect::makeXYWH expected w >= 0, got: " + w);
        if (h < 0)
            throw new IllegalArgumentException("IRect::makeXYWH expected h >= 0, got: " + h);
        return w >= 0 && h >= 0 ? new IRect(l, t, l + w, t + h) : null;
    }

    @NotNull @Contract("_, _ -> new")
    public static IRect makeWH(int w, int h) {
        if (w < 0)
            throw new IllegalArgumentException("IRect::makeWH expected w >= 0, got: " + w);
        if (h < 0)
            throw new IllegalArgumentException("IRect::makeWH expected h >= 0, got: " + h);
        return w >= 0 && h >= 0 ? new IRect(0, 0, w, h) : null;
    }

    @Nullable
    public IRect intersect(@NotNull IRect other) {
        assert other != null : "IRect::intersect expected other != null";
        if (_right <= other._left || other._right <= _left || _bottom <= other._top || other._bottom <= _top)
            return null;
        return new IRect(Math.max(_left, other._left), Math.max(_top, other._top), Math.min(_right, other._right), Math.min(_bottom, other._bottom));
    }

    @NotNull
    public IRect scale(int scale) {
        return scale(scale, scale);
    }

    @NotNull
    public IRect scale(int sx, int sy) {
        return sx == 1 && sy == 1 ? this : new IRect(_left * sx, _top * sy, _right * sx, _bottom * sy);
    }

    @NotNull
    public IRect offset(int dx, int dy) {
        return dx == 0 && dy == 0 ? this : new IRect(_left + dx, _top + dy, _right + dx, _bottom + dy);
    }

    @NotNull
    public IRect offset(@NotNull IPoint vec) {
        assert vec != null : "IRect::offset expected vec != null";
        return offset(vec._x, vec._y);
    }

    @NotNull @Contract("-> new")
    public Rect toRect() {
        return new Rect(_left, _top, _right, _bottom);
    }

    public boolean isEmpty() {
        return _right == _left || _top == _bottom;
    }

    public boolean contains(int x, int y) {
        return _left <= x && x <= _right && _top <= y && y <= _bottom;
    }

    public boolean contains(@NotNull IPoint vec) {
        assert vec != null : "IRect::contains expected vec != null";
        return _left <= vec._x && vec._x <= _right && _top <= vec._y && vec._y <= _bottom;
    }

    // ILookup
    public static final Keyword _KEYWORD_X      = Keyword.intern(null, "x");
    public static final Keyword _KEYWORD_Y      = Keyword.intern(null, "y");
    public static final Keyword _KEYWORD_RIGHT  = Keyword.intern(null, "right");
    public static final Keyword _KEYWORD_BOTTOM = Keyword.intern(null, "bottom");
    public static final Keyword _KEYWORD_WIDTH  = Keyword.intern(null, "width");
    public static final Keyword _KEYWORD_HEIGHT = Keyword.intern(null, "height");

    @Override
    public Object valAt(Object key, Object notFound) {
        if (_KEYWORD_X == key)
            return _left;
        else if (_KEYWORD_Y == key)
            return _top;
        else if (_KEYWORD_RIGHT == key)
            return _right;
        else if (_KEYWORD_BOTTOM == key)
            return _bottom;
        else if (_KEYWORD_WIDTH == key)
            return _right - _left;
        else if (_KEYWORD_HEIGHT == key)
            return _bottom - _top;
        else
            return notFound;
    }

    // Seqable
    @Override
    public ISeq seq() {
        IPersistentCollection ret = PersistentList.EMPTY;
        ret = ret.cons(MapEntry.create(_KEYWORD_BOTTOM, _bottom));
        ret = ret.cons(MapEntry.create(_KEYWORD_RIGHT, _right));
        ret = ret.cons(MapEntry.create(_KEYWORD_Y, _top));
        ret = ret.cons(MapEntry.create(_KEYWORD_X, _left));
        return ret.seq();
    }

    // IPersistentCollection
    @Override
    public int count() {
        return 4;
    }

    // Associative
    @Override
    public boolean containsKey(Object key) {
        return key == _KEYWORD_X || key == _KEYWORD_Y || key == _KEYWORD_RIGHT || key == _KEYWORD_BOTTOM || key == _KEYWORD_WIDTH || key == _KEYWORD_HEIGHT;
    }

    @Override
    public IMapEntry entryAt(Object key) {
        if (_KEYWORD_X == key)
            return MapEntry.create(_KEYWORD_X, _left);
        else if (_KEYWORD_Y == key)
            return MapEntry.create(_KEYWORD_Y, _top);
        else if (_KEYWORD_RIGHT == key)
            return MapEntry.create(_KEYWORD_RIGHT, _right);
        else if (_KEYWORD_BOTTOM == key)
            return MapEntry.create(_KEYWORD_BOTTOM, _bottom);
        else if (_KEYWORD_WIDTH == key)
            return MapEntry.create(_KEYWORD_WIDTH, _right - _left);
        else if (_KEYWORD_BOTTOM == key)
            return MapEntry.create(_KEYWORD_BOTTOM, _bottom - _top);
        else
            return null;
    }

    @Override
    public Associative assoc(Object key, Object val) {
        int intVal = RT.intCast(val);
        if (_KEYWORD_X == key)
            return withLeft(intVal);
        else if (_KEYWORD_Y == key)
            return withTop(intVal);
        else if (_KEYWORD_RIGHT == key)
            return withRight(intVal);
        else if (_KEYWORD_BOTTOM == key)
            return withBottom(intVal);
        else if (_KEYWORD_WIDTH == key)
            return withWidth(intVal);
        else if (_KEYWORD_HEIGHT == key)
            return withHeight(intVal);
        else
            throw new IllegalArgumentException("assoc " + key + " is not supported");
    }
}