(in-ns 'io.github.humbleui.ui)

;; vars

(def ^:dynamic *ctx*)

(def ^:dynamic *node*)

(def ^Shaper shaper
  (Shaper/makeShapeDontWrapOrReorder))

;; utils

(defn dimension ^long [size cs ctx]
  (let [scale (:scale ctx)]
    (->
      (if (fn? size)
        (* scale
          (size {:width  (/ (:width cs) scale)
                 :height (/ (:height cs) scale)
                 :scale  scale}))
        (* scale size))
      (math/round)
      (long))))

(defn scale
  ([]
   (:scale *ctx*))
  ([ctx]
   (:scale ctx)))

(defn scaled
  ([x]
   (when x
     (* x (:scale *ctx*))))
  ([x ctx]
   (when x
     (* x (:scale ctx)))))

(defn descaled
  ([x]
   (when x
     (/ x (:scale *ctx*))))
  ([x ctx]
   (when x
     (/ x (:scale ctx)))))

(defn parse-element [vals]
  (if (map? (nth vals 1))
    [(nth vals 0) (nth vals 1) (subvec vals 2)]
    [(nth vals 0) {} (subvec vals 1)]))

(defn parse-opts [element]
  (let [[_ opts & _] (parse-element element)]
    opts))

(defn keys-match? [keys m1 m2]
  (=
    (select-keys m1 keys)
    (select-keys m2 keys)))

(defn opts-match? [keys element new-element]
  (let [[_ opts _] (parse-element element)
        [_ new-opts _] (parse-element new-element)]
    (keys-match? keys opts new-opts)))

(defn invoke-callback [comp key & args]
  (let [[_ opts _] (parse-element (:element comp))]
    (apply util/invoke (key opts) args)))

(defn force-render [node window]
  (util/set!! node :dirty? true)
  (.requestFrame ^Window window))

;; protocols

(defn measure [comp ctx ^IPoint cs]
  (assert (instance? IPoint cs) (str "Expected IPoint as cs, got: " cs))
  (when comp
    (let [res (protocols/-measure comp ctx cs)]
      (assert (instance? IPoint res) (str "Expected IPoint as result, got: " res))
      res)))

(defn draw [comp ctx ^IRect bounds ^Canvas canvas]
  {:pre [(instance? IRect bounds)]}
  (protocols/-draw comp ctx bounds canvas))

(defn draw-child [comp ctx ^IRect bounds ^Canvas canvas]
  (when comp
    (let [count (.getSaveCount canvas)]
      (try
        (draw comp ctx bounds canvas)
        (finally
          (.restoreToCount canvas count))))))

(defn event [comp ctx event]
  (protocols/-event comp ctx event))

(defn event-child [comp ctx event]
  (when comp
    (protocols/-event comp ctx event)))

(defn iterate-child [comp ctx cb]
  (when comp
    (protocols/-iterate comp ctx cb)))

(defn unmount [comp]
  (when comp
    (protocols/-unmount comp)))

(defn unmount-child [comp]
  (when comp
    (protocols/-unmount comp)))

;; defcomp

(def ^:private *key
  (volatile! 0))

(defn- auto-keys [form]
  (walk/postwalk
    (fn [form]
      (if (and (vector? form) (instance? clojure.lang.IObj form))
        (let [m (meta form)]
          (if (contains? m :key)
            form
            (vary-meta form assoc :key (str "io.github.humbleui.ui/" (vswap! *key inc)))))
        form))
    form))

(defmacro defcomp [name & fdecl]
  (let [[m fdecl] (if (string? (first fdecl))
                    [{:doc (first fdecl)} (next fdecl)]
                    [{} fdecl])
        [m fdecl] (if (map? (first fdecl))
                    [(merge m (first fdecl)) (next fdecl)]
                    [m fdecl])
        fdecl     (if (vector? (first fdecl))
                    (list fdecl)
                    fdecl)
        [m fdecl] (if (map? (last fdecl))
                    [(merge m (last fdecl)) (butlast fdecl)]
                    [m fdecl])]
    `(defn ~(vary-meta name merge m)
       ~@(for [[params & body] fdecl]
           (list params
             `(let [~'&node *node*]
                ~@(auto-keys body)))))))
