(in-ns 'io.github.humbleui.ui)

;; vars

(def ^:dynamic *ctx*)

(def ^:dynamic *node*)

(def ^Shaper shaper
  (Shaper/makeShapeDontWrapOrReorder))

;; utils

(defn dimension? [x]
  (or
    (number? x)
    (fn? x)))

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
  (^long [x]
    (scaled x *ctx*))
  (^long [x ctx]
    (-> x
      (* (:scale ctx))
      math/ceil
      long)))

(defn descaled
  ([x]
   (when x
     (/ x (:scale *ctx*))))
  ([x ctx]
   (when x
     (/ x (:scale ctx)))))

(defn parse-element [el]
  (when el
    (if (map? (nth el 1 nil))
      [(nth el 0) (nth el 1) (subvec el 2)]
      [(nth el 0) {} (subvec el 1)])))

(defn parse-opts [el]
  (when el
    (let [opts (nth el 1 nil)]
      (when (map? opts)
        opts))))

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

(defn draw [comp ctx bounds viewport canvas]
  (assert (instance? IRect bounds) (str "bounds: expected IRect, got: " bounds))
  (assert (or (nil? viewport) (instance? IRect viewport)) (str "viewport: expected IRect, got: " viewport))
  (when comp
    (protocols/-draw comp ctx bounds viewport canvas)))

(defn event [comp ctx event]
  (when comp
    (protocols/-event comp ctx event)))

(defn iterate [comp ctx cb]
  (when comp
    (protocols/-iterate comp ctx cb)))

(defn unmount [comp]
  (when comp
    (protocols/-unmount comp)))
