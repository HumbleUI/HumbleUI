package io.github.humbleui.types;

import clojure.lang.*;
import java.util.*;
import lombok.*;
import org.jetbrains.annotations.*;

@EqualsAndHashCode(callSuper = true)
public class RRect extends Rect {
    public final float[] _radii;

    @ApiStatus.Internal
    public RRect(float l, float t, float r, float b, float[] radii) {
        super(l, t, r, b);
        this._radii = radii;
    }

    public static RRect makeLTRB(float l, float t, float r, float b, float radius) {
        return new RRect(l, t, r, b, new float[] { radius } );
    }

    public static RRect makeLTRB(float l, float t, float r, float b, float xRad, float yRad) {
        return new RRect(l, t, r, b, new float[] { xRad, yRad } );
    }

    public static RRect makeLTRB(float l, float t, float r, float b, float tlRad, float trRad, float brRad, float blRad) {
        return new RRect(l, t, r, b, new float[] { tlRad, trRad, brRad, blRad });
    }

    public static RRect makeNinePatchLTRB(float l, float t, float r, float b, float lRad, float tRad, float rRad, float bRad) {
        return new RRect(l, t, r, b, new float[] { lRad, tRad, rRad, tRad, rRad, bRad, lRad, bRad });
    }

    public static RRect makeComplexLTRB(float l, float t, float r, float b, float[] radii) {
        return new RRect(l, t, r, b, radii);
    }

    public static RRect makeOvalLTRB(float l, float t, float r, float b) {
        return new RRect(l, t, r, b, new float[] { Math.abs(r - l) / 2f, Math.abs(b - t) / 2f } );
    }

    public static RRect makePillLTRB(float l, float t, float r, float b) {
        return new RRect(l, t, r, b, new float[] { Math.min(Math.abs(r - l), Math.abs(t - b)) / 2f } );
    }

    public static RRect makeXYWH(float l, float t, float w, float h, float radius) {
        return new RRect(l, t, l + w, t + h, new float[] { radius } );
    }

    public static RRect makeXYWH(float l, float t, float w, float h, float xRad, float yRad) {
        return new RRect(l, t, l + w, t + h, new float[] { xRad, yRad } );
    }

    public static RRect makeXYWH(float l, float t, float w, float h, float tlRad, float trRad, float brRad, float blRad) {
        return new RRect(l, t, l + w, t + h, new float[] { tlRad, trRad, brRad, blRad });
    }

    public static RRect makeNinePatchXYWH(float l, float t, float w, float h, float lRad, float tRad, float rRad, float bRad) {
        return new RRect(l, t, l + w, t + h, new float[] { lRad, tRad, rRad, tRad, rRad, bRad, lRad, bRad });
    }

    public static RRect makeComplexXYWH(float l, float t, float w, float h, float[] radii) {
        return new RRect(l, t, l + w, t + h, radii);
    }

    public static RRect makeOvalXYWH(float l, float t, float w, float h) {
        return new RRect(l, t, l + w, t + h, new float[] { w / 2f, h / 2f } );
    }

    public static RRect makePillXYWH(float l, float t, float w, float h) {
        return new RRect(l, t, l + w, t + h, new float[] { Math.min(w, h) / 2f } );
    }

    @Override @NotNull
    public RRect scale(float scale) {
        return scale(scale, scale);
    }

    @Override @NotNull
    public RRect scale(float sx, float sy) {
        if (sx == 1 && sy == 1)
            return this;
        if (sx == sy) {
            switch (_radii.length) {
            case 1:
                return new RRect(_left * sx, _top * sx, _right * sx, _bottom * sx, new float[] { _radii[0] * sx });
            case 2:
                return new RRect(_left * sx, _top * sx, _right * sx, _bottom * sx, new float[] { _radii[0] * sx, _radii[1] * sx });
            case 4:
                return new RRect(_left * sx, _top * sx, _right * sx, _bottom * sx,
                                 new float[] { _radii[0] * sx, _radii[1] * sx, _radii[2] * sx, _radii[3] * sx });
            case 8:
                return new RRect(_left * sx, _top * sx, _right * sx, _bottom * sx,
                                 new float[] { _radii[0] * sx, _radii[1] * sx,
                                               _radii[2] * sx, _radii[3] * sx,
                                               _radii[4] * sx, _radii[5] * sx,
                                               _radii[6] * sx, _radii[7] * sx });
            }
        } else {
            switch (_radii.length) {
            case 1:
                return new RRect(_left * sx, _top * sy, _right * sx, _bottom * sy, new float[] { _radii[0] * sx, _radii[0] * sy });
            case 2:
                return new RRect(_left * sx, _top * sy, _right * sx, _bottom * sy, new float[] { _radii[0] * sx, _radii[1] * sy });
            case 4:
                return new RRect(_left * sx, _top * sy, _right * sx, _bottom * sy,
                                 new float[] { _radii[0] * sx, _radii[0] * sy,
                                               _radii[1] * sx, _radii[1] * sy,
                                               _radii[2] * sx, _radii[2] * sy,
                                               _radii[3] * sx, _radii[3] * sy });
            case 8:
                return new RRect(_left * sx, _top * sy, _right * sx, _bottom * sy,
                                 new float[] { _radii[0] * sx, _radii[1] * sy,
                                               _radii[2] * sx, _radii[3] * sy,
                                               _radii[4] * sx, _radii[5] * sy,
                                               _radii[6] * sx, _radii[7] * sy });
            }
        }
        throw new RuntimeException("Unreachable, _radii=" + Arrays.toString(_radii));
    }

    @Override @NotNull
    public RRect offset(float dx, float dy) {
        return dx == 0 && dy == 0 ? this : new RRect(_left + dx, _top + dy, _right + dx, _bottom + dy, _radii);
    }

    @Override @NotNull
    public RRect offset(@NotNull Point vec) {
        assert vec != null : "Rect::offset expected vec != null";
        return offset(vec._x, vec._y);
    }

    @Override @NotNull
    public Rect inflate(float spread) {
        boolean becomesRect = true;
        for (int i = 0; i < _radii.length; ++i) {
            if (_radii[i] + spread >= 0) {
                becomesRect = false;
                break;
            }
        }

        if (becomesRect)
            return Rect.makeLTRB(_left - spread,
                                 _top - spread,
                                 Math.max(_left - spread, _right + spread),
                                 Math.max(_top - spread, _bottom + spread));
        else {
            float[] radii = Arrays.copyOf(_radii, _radii.length);
            for (int i = 0; i < radii.length; ++i)
                radii[i] = Math.max(0f, radii[i] + spread);
            return new RRect(_left - spread,
                             _top - spread,
                             Math.max(_left - spread, _right + spread),
                             Math.max(_top - spread, _bottom + spread),
                             radii);
        }
    }

    @Override
    public String toString() {
        return "RRect(_left=" + _left + ", _top=" + _top + ", _right=" + _right + ", _bottom=" + _bottom + ", _radii=" + Arrays.toString(_radii) + ")";
    }


    // ILookup
    public static final Keyword _KEYWORD_RADII = Keyword.intern(null, "radii");

    @Override
    public Object valAt(Object key, Object notFound) {
        if (_KEYWORD_RADII == key)
            return RT.seq(_radii);
        else
            return super.valAt(key, notFound);
    }

    // Seqable
    @Override
    public ISeq seq() {
        ISeq ret = super.seq();
        ret.cons(MapEntry.create(_KEYWORD_RADII, RT.seq(_radii)));
        return ret;
    }

    // IPersistentCollection
    @Override
    public int count() {
        return 5;
    }

    // Associative
    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(key) || key == _KEYWORD_RADII;
    }

    @Override
    public IMapEntry entryAt(Object key) {
        if (_KEYWORD_RADII == key)
            return MapEntry.create(_KEYWORD_RADII, RT.seq(_radii));
        else
            return super.entryAt(key);
    }

    @Override
    public Associative assoc(Object key, Object val) {
        if (_KEYWORD_RADII == key) {
            float[] radii;
            if (float[].class == val.getClass()) {
                radii = Arrays.copyOf((float[]) val, ((float[]) val).length);
            } else {
                ISeq seq = RT.seq(val);
                radii = new float[seq.count()];
                int i = 0;
                while (seq != null) {
                    radii[i] = RT.floatCast(seq.first());
                    seq = seq.next();
                }
            }
            return new RRect(_left, _top, _right, _bottom, radii);
        } else
            return super.assoc(key, val);
    }
}