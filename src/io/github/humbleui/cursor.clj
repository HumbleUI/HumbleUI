(ns io.github.humbleui.cursor)

(deftype Cursor [ref path ^:volatile-mutable meta watches]
  clojure.lang.IDeref

  (deref [this]
    (get-in (deref ref) path))

  clojure.lang.IRef

  (setValidator [this vf]
    (throw (UnsupportedOperationException. "io.github.humbleui.cursor.Cursor/setValidator")))

  (getValidator [this]
    (throw (UnsupportedOperationException. "io.github.humbleui.cursor.Cursor/getValidator")))

  (getWatches [this]
    @watches)

  (addWatch [this key callback]
    (vswap! watches assoc key callback)
    (add-watch ref (list this key)
               (fn [_ _ oldv newv]
                 (let [old (get-in oldv path)
                       new (get-in newv path)]
                   (when (not= old new)
                     (callback key this old new)))))
    this)

  (removeWatch [this key]
    (vswap! watches dissoc key)
    (remove-watch ref (list this key))
    this)

  clojure.lang.IAtom

  (swap [this f]
    (-> (swap! ref update-in path f)
        (get-in path)))

  (swap [this f a]
    (-> (swap! ref update-in path f a)
        (get-in path)))

  (swap [this f a b]
    (-> (swap! ref update-in path f a b)
        (get-in path)))

  (swap [this f a b rest]
    (-> (apply swap! ref update-in path f a b rest)
        (get-in path)))

  (compareAndSet [this oldv newv]
    (loop []
      (let [refv @ref]
        (if (not= oldv (get-in refv path))
          false
          (or (compare-and-set! ref refv (assoc-in refv path newv))
              (recur))))))

  (reset [this newv]
    (swap! ref assoc-in path newv)
    newv)

  clojure.lang.IMeta

  (meta [this]
    meta)

  clojure.lang.IReference

  (alterMeta [this f args]
    (.resetMeta this (apply f meta args)))

  (resetMeta [this m]
    (set! meta m)
    m))

(defn cursor-in
  "Given atom with deep nested value and path inside it, creates an atom-like structure
   that can be used separately from main atom, but will sync changes both ways:
  
   ```
   (def db
     (atom {:users {\"Ivan\" {:age 30}}}))
   
   (def ivan
     (cursor db [:users \"Ivan\"]))

   (deref ivan) ;; => {:age 30}
   
   (swap! ivan update :age inc) ;; => {:age 31}

   (deref db) ;; => {:users {\"Ivan\" {:age 31}}}
   
   (swap! db update-in [:users \"Ivan\" :age] inc)
   ;; => {:users {\"Ivan\" {:age 32}}}
   
   (deref ivan) ;; => {:age 32}
   ```
  
   Returned value supports `deref`, `swap!`, `reset!`, watches and metadata.
  
   The only supported option is `:meta`"
  ^Cursor [ref path & {:as options}]
  (if (instance? Cursor ref)
    (Cursor. (.-ref ^Cursor ref) (into (.-path ^Cursor ref) path) (:meta options) (volatile! {}))
    (Cursor. ref path (:meta options) (volatile! {}))))

(defn cursor
  "Same as [[cursor-in]] but accepts single key instead of path vector."
  ^Cursor [ref key & options]
  (apply cursor-in ref [key] options))
