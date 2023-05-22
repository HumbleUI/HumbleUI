(ns vdom
  (:require
    [clojure.core.server :as server]
    [clojure.math :as math]
    [clojure.string :as str]
    [clj-async-profiler.core :as profiler]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.signal :as s]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.typeface :as typeface]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.window :as window]
    [examples.state :as state])
  (:import
    [io.github.humbleui.types IRect]
    [io.github.humbleui.skija Canvas TextLine]
    [io.github.humbleui.skija.shaper ShapingOptions]))

(defn request-frame []
  (some-> @state/*window window/request-frame))

(core/defparent AComponent
  "Everything is a component"
  [^:mut children ^:mut mounted?]
  protocols/IComponent
  (-measure [this ctx cs]
    ; (protocols/-on-mount this)
    (protocols/-measure-impl this ctx cs))
    
  (-draw [this ctx rect canvas]
    (protocols/-draw-impl this ctx rect canvas))
  
  (-event [this ctx event]
    (protocols/-event-impl this ctx event))
  
  (-event-impl [this ctx event]
    (reduce #(core/eager-or %1 (protocols/-event (s/maybe-read %2) ctx event)) nil children))
  
  protocols/ILifecycle
  (-on-mount [this]
    (assert (nil? mounted?) "Double mount")
    (set! mounted? true)
    (protocols/-on-mount-impl comp))
  
  (-on-mount-impl [this])
  
  (-on-unmount [this]
    (assert (= true mounted?) "Double unmount")
    (set! mounted? false)
    (protocols/-on-unmount-impl comp))
  
  (-on-unmount-impl [this]))

(defmulti compatible? (fn [ctor props ctx prev] ctor))

(defmethod compatible? :default [ctor props ctx prev]
  (= (class prev) ctor))

(defmulti element (fn [ctor props ctx] ctor))

(defmethod element :default [ctor props ctx]
  ((ns-resolve 'vdom (symbol (str "map->" (.getSimpleName ^Class ctor)))) props))

(defmulti update-props (fn [prev props] (class prev)))

(defmethod update-props :default [prev props]
  prev)

(core/deftype+ Center []
  :extends AComponent
  
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    cs)

  (-draw-impl [this ctx rect canvas]
    (let [child      (core/single children)
          w          (:width rect)
          h          (:height rect)
          child-size (protocols/-measure child ctx (core/isize w h))
          cw         (:width child-size)
          ch         (:height child-size)
          rect'      (core/irect-xywh
                       (-> (:x rect) (+ (/ w 2)) (- (/ cw 2)))
                       (-> (:y rect) (+ (/ h 2)) (- (/ ch 2)))
                       cw ch)]
      (protocols/-draw child ctx rect' canvas))))

(core/deftype+ Label [props ^TextLine line]
  :extends AComponent
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (core/ipoint
      (math/ceil (.getWidth line))
      (* (:scale ctx) (:leading ctx))))
  
  (-draw-impl [this ctx rect ^Canvas canvas]
    (.drawTextLine canvas
      line
      (:x rect)
      (+ (:y rect) (* (:scale ctx) (:leading ctx)))
      (:fill-text ctx)))
  
  protocols/ILifecycle
  (-on-unmount-impl [_]
    (.close line)))

(defn label-props [props ctx]
  {:text     (:text props)
   :font     (:font-ui ctx)
   :features (:features props ShapingOptions/DEFAULT)})

(defmethod compatible? Label [_ props ctx prev]
  (= (:props prev) (label-props props ctx)))

(defmethod element Label [_ props ctx]
  (let [props (label-props props ctx)
        line  (.shapeLine core/shaper (:text props) (:font props) (:features props))]
    (map->Label 
      {:props props
       :line  line})))

(defn app []
  [Center
   [Label {:text     (str (java.time.LocalTime/now))
           :features (-> ShapingOptions/DEFAULT (.withFeatures "tnum"))}]])

(defn reconcile [ctx widget prev]
  (let [[ctor props children] (if (map? (fnext widget))
                                [(first widget) (fnext widget) (nnext widget)]
                                [(first widget) nil (next widget)])
        element'              (if (compatible? ctor props ctx prev)
                                (update-props prev props)
                                (let [comp (element ctor props ctx)]
                                  comp))
        prev-children         (:children prev)
        children'             (mapv #(reconcile ctx %1 (nth prev-children %2)) children (range))]
    (protocols/-set! element' :children children')
    element'))

(defn ensure-child [reconciler ctx]
  (let [elements (reconcile ctx ((:ctor reconciler)) (:child reconciler))]
    (protocols/-set! reconciler :child elements)))

(core/deftype+ Reconciler [ctor ^:mut child]
  protocols/IComponent
  (-measure [this ctx cs]
    (ensure-child this ctx)
    (core/measure child ctx cs))
  
  (-draw [this ctx rect canvas]
    (ensure-child this ctx)
    (core/draw-child child ctx rect canvas)
    (request-frame))

  (-event [this ctx event]
    (ensure-child this ctx)
    (core/event-child child ctx event))

  (-iterate [this ctx cb]
    (ensure-child this ctx)
    (or
      (cb this)
      (core/iterate-child child ctx cb))))

(defn reconciler [ctor]
  (map->Reconciler
    {:ctor ctor}))

(reset! state/*app
  (ui/default-theme
    (reconciler app)))

(defn -main [& args]
  (ui/start-app!
    (let [screen (last (app/screens))
          window (ui/window
                   {:title    "Humble 🐝 UI"
                    :mac-icon "dev/images/icon.icns"
                    :screen   (:id screen)
                    :width    400 #_400
                    :height   600 #_(/ (:height (:work-area screen)) (:scale screen))
                    :x        :center #_:right
                    :y        :center #_:top}
                   state/*app)]
      ; (window/set-z-order window :floating)
      (reset! protocols/*debug? true)
      (reset! state/*window window)
      (request-frame)))
  (let [{port "--port"
         :or {port "5555"}} (apply array-map args)
        port (parse-long port)]
    (println "Started Server Socket REPL on port" port)
    (server/start-server
      {:name          "repl"
       :port          port
       :accept        'clojure.core.server/repl
       :server-daemon false})))

(comment
  (request-frame))
