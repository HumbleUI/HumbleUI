package io.github.humbleui.types;

import clojure.lang.*;
import lombok.*;
import org.jetbrains.annotations.*;

@Data
@EqualsAndHashCode(callSuper=false)
@With
public class IRange extends ARecord {
    @ApiStatus.Internal
    public final int _start;
    
    @ApiStatus.Internal
    public final int _end;

    @ApiStatus.Internal
    public static IRange _makeFromLong(long l) {
        return new IRange((int) (l >>> 32), (int) (l & 0xFFFFFFFF));
    }

    // ILookup
    public static final Keyword _KEYWORD_START = Keyword.intern(null, "start");
    public static final Keyword _KEYWORD_END = Keyword.intern(null, "end");

    @Override
    public Object valAt(Object key, Object notFound) {
        if (_KEYWORD_START == key)
            return _start;
        else if (_KEYWORD_END == key)
            return _end;
        else
            return notFound;
    }

    // Seqable
    @Override
    public ISeq seq() {
        IPersistentCollection ret = PersistentList.EMPTY;
        ret = ret.cons(MapEntry.create(_KEYWORD_END, _end));
        ret = ret.cons(MapEntry.create(_KEYWORD_START, _start));
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
        return key == _KEYWORD_START || key == _KEYWORD_END;
    }

    @Override
    public IMapEntry entryAt(Object key) {
        if (_KEYWORD_START == key)
            return MapEntry.create(_KEYWORD_START, _start);
        else if (_KEYWORD_END == key)
            return MapEntry.create(_KEYWORD_END, _end);
        else
            return null;
    }

    @Override
    public Associative assoc(Object key, Object val) {
        int intVal = RT.intCast(val);
        if (_KEYWORD_START == key)
            return withStart(intVal);
        else if (_KEYWORD_END == key)
            return withEnd(intVal);
        else
            throw new IllegalArgumentException("assoc " + key + " is not supported");
    }
}