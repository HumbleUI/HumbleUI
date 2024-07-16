(in-ns 'io.github.humbleui.ui)

(require
  '[io.github.humbleui.debug :as debug])

(alias 'ui 'io.github.humbleui.ui)

(def ^:private ctor-border
  (paint/stroke 0x80FF00FF 4))

(defn maybe-render [node ctx]
  (when (or
          (:dirty? node)
          (when-some [should-render? (:should-render? node)]
            (apply should-render? (next (:element node)))))
    (protocols/-reconcile-impl node ctx (:element node))
    (util/set!! node :dirty? false)))

(util/defparent ANode
  [^:mut element
   ^:mut mounted?
   ^:mut bounds
   ^:mut key
   ^:mut dirty?]
  
  protocols/IComponent
  (-context [_ ctx]
    ctx)

  (-measure [this ctx cs]
    (let [ctx (protocols/-context this ctx)]
      (ui/maybe-render this ctx)
      (protocols/-measure-impl this ctx cs)))
    
  (-draw [this ctx bounds' canvas]
    (let [ctx (protocols/-context this ctx)]
      (protocols/-set! this :bounds bounds')
      (ui/maybe-render this ctx)
      (protocols/-draw-impl this ctx bounds' canvas)
      (when (and @debug/*outlines? (not (:mounted? this)))
        (canvas/draw-rect canvas (-> ^io.github.humbleui.types.IRect bounds' .toRect (.inflate 4)) ui/ctor-border)
        (protocols/-set! this :mounted? true))))
  
  (-event [this ctx event]
    (let [ctx (protocols/-context this ctx)]
      (protocols/-event-impl this ctx event)))
  
  (-event-impl [this ctx event]
    nil)

  (-child-elements [this ctx new-element]
    (let [[_ _ child-els] (parse-element new-element)]
      child-els))
  
  (-reconcile [this ctx new-element]
    (when (not (identical? (:element this) new-element))
      (protocols/-reconcile-impl this ctx new-element)
      (protocols/-set! this :element new-element))
    this)
  
  (-reconcile-impl [this _ctx element]
    (throw (ex-info "Not implemented" {:element (:element this)})))
  
  (-should-reconcile? [_this _ctx _element]
    true)
  
  (-unmount [this]
    (protocols/-unmount-impl this))
  
  (-unmount-impl [_this]))

(util/defparent ATerminalNode
  "Simple component that has no children"
  []
  :extends ANode
  protocols/IComponent
  (-iterate [this _ctx cb]
    (cb this))
  
  (-reconcile-impl [this ctx _new-element]
    this))

(util/defparent AWrapperNode
  "A component that has exactly one child"
  [^:mut child]
  :extends ANode
  protocols/IComponent
  (-measure-impl [this ctx cs]
    (let [ctx (protocols/-context this ctx)]
      (measure (:child this) ctx cs)))

  (-draw-impl [this ctx bounds canvas]
    (let [ctx (protocols/-context this ctx)]
      (draw (:child this) ctx bounds canvas)))
  
  (-event-impl [this ctx event]
    (let [ctx (protocols/-context this ctx)]
      (ui/event (:child this) ctx event)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (let [ctx (protocols/-context this ctx)]
        (iterate (:child this) ctx cb))))
  
  (-reconcile-impl [this ctx el']
    (let [ctx       (protocols/-context this ctx)
          child-els (protocols/-child-elements this ctx el')
          [child']  (reconcile-many ctx [(:child this)] child-els)]
      (protocols/-set! this :child child')))
  
  (-unmount [this]
    (unmount (:child this))
    (protocols/-unmount-impl this)))

(util/defparent AContainerNode
  "A component that has multiple children"
  [^:mut children]
  :extends ANode
  protocols/IComponent  
  (-event [this ctx event]
    (let [ctx (protocols/-context this ctx)]
      (binding [*node* this
                *ctx*  ctx]
        (util/eager-or
          (reduce #(util/eager-or %1 (protocols/-event %2 ctx event)) nil (:children this))
          (protocols/-event-impl this ctx event)))))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (let [ctx (protocols/-context this ctx)]
        (some #(iterate % ctx cb) (:children this)))))
  
  (-reconcile-impl [this ctx el']
    (let [ctx       (protocols/-context this ctx)
          child-els (protocols/-child-elements this ctx el')
          child-els (util/flatten child-els)
          children' (reconcile-many ctx (:children this) child-els)]
      (protocols/-set! this :children children')))
  
  (-unmount [this]
    (doseq [child (:children this)]
      (unmount child))
    (protocols/-unmount-impl this)))

;; FnNode

(util/deftype+ FnNode [^:mut child
                       ^:mut effect
                       ^:mut should-setup?
                       ^:mut should-render?
                       ^:mut after-mount
                       ^:mut before-render
                       ^:mut user-measure
                       ^:mut user-draw
                       ^:mut render
                       ^:mut after-render
                       ^:mut before-draw
                       ^:mut after-draw
                       ^:mut after-unmount]
  :extends ANode
  protocols/IComponent
  (-measure-impl [this ctx cs]
    (binding [*node* this
              *ctx*  ctx]
      (when render
        (set! bounds (util/irect-xywh 0 0 (:width cs) (:height cs)))
        (maybe-render this ctx))
      (if user-measure
        (user-measure child cs)
        (measure child ctx cs))))
  
  (-draw [this ctx bounds' canvas]
    (set! bounds bounds')
    (binding [*node* this
              *ctx*  ctx]
      (when render
        (maybe-render this ctx))
      (util/invoke before-draw)
      (if user-draw
        (user-draw child bounds canvas)
        (protocols/-draw child ctx bounds canvas))
      (util/invoke after-draw)
      (when-not mounted?
        (util/invoke after-mount))
      (when (and @debug/*outlines? (not mounted?))
        (canvas/draw-rect canvas (-> ^IRect bounds .toRect (.inflate 4)) ctor-border)
        (set! mounted? true))))

  (-event-impl [this ctx event]
    (binding [*node* this
              *ctx*  ctx]
      (ui/event child ctx event)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (when render
        (iterate child ctx cb))))
  
  (-reconcile [this ctx new-element]
    (when (if should-render?
            (apply should-render? (next new-element))
            (not (identical? element new-element)))
      (protocols/-reconcile-impl this ctx new-element))
    (set! element new-element)
    this)
  
  (-reconcile-impl [this ctx new-element]
    (when (or
            (not (identical? (first element) (first new-element)))
            (apply util/invoke should-setup? (next new-element)))
      (make-impl this new-element))
    (when render
      (util/invoke before-render)
      (try
        (binding [signal/*context* (volatile! (transient #{}))
                  *ctx*            ctx]
          (let [child-el (apply render (next new-element))
                [child'] (reconcile-many ctx [child] [child-el])
                _        (set! child child')
                _        (set! element new-element)
                signals  (persistent! @@#'signal/*context*)
                window   (:window ctx)]
            (some-> effect signal/dispose!)
            (set! effect
              (when-not (empty? signals)
                (signal/effect signals
                  (force-render this window))))))
        (finally
          (util/invoke after-render)))))
  
  (-unmount [this]
    (unmount child)
    (some-> effect signal/dispose!)
    (when after-unmount
      (after-unmount)))
  
  java.lang.Object
  (toString [_]
    (pr-str element)))

(defmethod print-method FnNode [o ^java.io.Writer w]
  (.write w (str o)))
