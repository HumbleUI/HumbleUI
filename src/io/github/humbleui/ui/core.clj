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

(defn measure
  ([comp]
   (let [ctx *ctx*
         size (measure comp ctx (util/ipoint 0 0))]
     (util/ipoint
       (descaled (:width size) ctx)
       (descaled (:height size) ctx))))
  ([comp ctx ^IPoint cs]
   (assert (instance? IPoint cs) (str "Expected IPoint as cs, got: " cs))
   (when comp
     (let [res (protocols/-measure comp ctx cs)]
       (assert (instance? IPoint res) (str "Expected IPoint as result, got: " res))
       res))))

(defn draw [comp ctx bounds container-size viewport canvas]
  (assert (instance? IRect bounds) (str "bounds: expected IRect, got: " bounds))
  (assert (or (nil? viewport) (instance? IRect viewport)) (str "viewport: expected IRect, got: " viewport))
  (when comp
    (protocols/-draw comp ctx bounds container-size viewport canvas)))

(defn event [comp ctx event]
  (when comp
    (protocols/-event comp ctx event)))

(defn iterate [comp ctx cb]
  (when comp
    (protocols/-iterate comp ctx cb)))

(defn unmount [comp]
  (when comp
    (protocols/-unmount comp)))

(defn invalidate-size [comp]
  (loop [comp comp]
    (when (:this-size comp)
      (util/set!! comp :this-size nil)
      (recur (:parent comp)))))

;; signals

(defmacro signal
  "Observable derived computation. Create by providing a value:
   
     (signal 123)
   
   Build deriver computations by dereferencing inside `signal`:
   
     (def *width
       (signal 1280))
     
     (def *height
       (signal (-> @*width (/ 16) (* 9))))
   
     @*height ;; -> 720
   
     (reset! *width 1920)
   
     @*height ;; -> 1080"
  [& body]
  `(signal/signal ~@body))

(defmacro effect
  "Do something when any of the inputs change.
   
     (def *width
       (signal 1280))
   
     (effect [*width]
       (println @*width))
   
     (reset! *width 1920)
     ;; -> 1920"
  [inputs & body]
  `(signal/effect ~inputs ~@body))

(def ^{:arglists '([signal value'])} reset-changed!
  signal/reset-changed!)

(def ^{:arglists '([ref-or-val])} maybe-deref
  "If argument is an IDeref, dereference. Otherwise, return as is"
  util/maybe-deref)
