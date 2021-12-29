package io.github.humbleui.types;

import clojure.lang.*;
import java.util.*;
import java.util.concurrent.*;

public abstract class ARecord extends AFn implements Associative {
    // ILookup
    @Override
    public Object valAt(Object key) {
        return valAt(key, null);
    }

    // IFn
    @Override
    public Object invoke(Object arg1) {
        return valAt(arg1, null);
    }

    @Override
    public Object invoke(Object arg1, Object arg2) {
        return valAt(arg1, arg2);
    }

    @Override
    public IPersistentCollection cons(Object o) {
        if (o instanceof Map.Entry) {
            Map.Entry e = (Map.Entry) o;
            return assoc(e.getKey(), e.getValue());
        }

        if (o instanceof IPersistentVector) {
            IPersistentVector v = (IPersistentVector) o;
            if(v.count() != 2)
                throw new IllegalArgumentException("Vector arg to IPoint::cons must be a pair");
            return assoc(v.nth(0), v.nth(1));
        }

        Associative ret = this;
        for (ISeq es = RT.seq(o); es != null; es = es.next()) {
            Map.Entry e = (Map.Entry) es.first();
            ret = (Associative) ret.assoc(e.getKey(), e.getValue());
        }
        return ret;
    }

    @Override
    public IPersistentCollection empty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equiv(Object o) {
        return equals(o);
    }
}