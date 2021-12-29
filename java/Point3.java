package io.github.humbleui.types;

import clojure.lang.*;
import lombok.*;
import org.jetbrains.annotations.*;

@Data
@EqualsAndHashCode(callSuper=false)
@With
public class Point3 extends ARecord {
    public static final Point ZERO = new Point(0, 0);

    @ApiStatus.Internal public final float _x;
    @ApiStatus.Internal public final float _y;
    @ApiStatus.Internal public final float _z;

    // ILookup
    public static final Keyword _KEYWORD_X = Keyword.intern(null, "x");
    public static final Keyword _KEYWORD_Y = Keyword.intern(null, "y");
    public static final Keyword _KEYWORD_Z = Keyword.intern(null, "z");
    public static final Keyword _KEYWORD_WIDTH  = Keyword.intern(null, "width");
    public static final Keyword _KEYWORD_HEIGHT = Keyword.intern(null, "height");
    public static final Keyword _KEYWORD_DEPTH  = Keyword.intern(null, "depth");

    @Override
    public Object valAt(Object key, Object notFound) {
        if (_KEYWORD_X == key || _KEYWORD_WIDTH == key)
            return _x;
        else if (_KEYWORD_Y == key || _KEYWORD_HEIGHT == key)
            return _y;
        else if (_KEYWORD_Z == key || _KEYWORD_DEPTH == key)
            return _z;
        else
            return notFound;
    }

    // Seqable
    @Override
    public ISeq seq() {
        IPersistentCollection ret = PersistentList.EMPTY;
        ret = ret.cons(MapEntry.create(_KEYWORD_Z, _z));
        ret = ret.cons(MapEntry.create(_KEYWORD_Y, _y));
        ret = ret.cons(MapEntry.create(_KEYWORD_X, _x));
        return ret.seq();
    }

    // IPersistentCollection
    @Override
    public int count() {
        return 3;
    }

    // Associative
    @Override
    public boolean containsKey(Object key) {
        return key == _KEYWORD_X || key == _KEYWORD_Y || key == _KEYWORD_Z || key == _KEYWORD_WIDTH || key == _KEYWORD_HEIGHT || key == _KEYWORD_DEPTH;
    }

    @Override
    public IMapEntry entryAt(Object key) {
        if (_KEYWORD_X == key)
            return MapEntry.create(_KEYWORD_X, _x);
        else if (_KEYWORD_Y == key)
            return MapEntry.create(_KEYWORD_Y, _y);
        else if (_KEYWORD_Z == key)
            return MapEntry.create(_KEYWORD_Z, _z);
        else if (_KEYWORD_WIDTH == key)
            return MapEntry.create(_KEYWORD_WIDTH, _x);
        else if (_KEYWORD_HEIGHT == key)
            return MapEntry.create(_KEYWORD_HEIGHT, _y);
        else if (_KEYWORD_DEPTH == key)
            return MapEntry.create(_KEYWORD_DEPTH, _z);
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
        else if (_KEYWORD_Z == key || _KEYWORD_DEPTH == key)
            return withZ(floatVal);
        else
            throw new IllegalArgumentException("assoc " + key + " is not supported");
    }
}