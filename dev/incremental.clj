;; Run with ./script/incremental.sh

(ns incremental
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

(def *frame
  (atom 0))

(s/defsignal *scale
  (or
    (when-some [window @state/*window]
      (app/doui
        (window/scale window)))
    1))

(s/defsignal *face-ui
  (typeface/make-from-resource "io/github/humbleui/fonts/Inter-Regular.ttf"))

(s/defsignal *font-ui
  (font/make-with-cap-height @*face-ui (* 10 @*scale)))

(s/defsignal *font-ui-cap-height
  (-> @*font-ui font/metrics :cap-height math/ceil))

(s/defsignal *color-text
  0xFF000000)

(s/defsignal *fill-text
  (paint/fill @*color-text))

(s/defsignal *accent
  0xFFA5C9EF)

(s/defsignal *fill-button
  (paint/fill @*accent))

(s/defsignal *padding
  10)

(def ^:dynamic *mutations*)

(defn mutate-later [f]
  (vswap! *mutations* conj! f))

(def ^:dynamic *rendered*)

(defn before-draw [comp ctx rect canvas]
  (protocols/-on-mount comp))

(defn after-draw [comp ctx rect canvas]
  (when (nil? (:frame comp))
    (canvas/draw-rect canvas (-> ^IRect rect .toRect (.inflate (* 2 @*scale))) (paint/stroke 0x80FF00FF @*scale)))
  (protocols/-set! comp :frame @*frame)
  (vswap! *rendered* conj! comp))

(defn on-mount [comp]
  (when-not (:mounted? comp)
    (protocols/-on-mount-impl comp)
    (protocols/-set! comp :mounted? true)))

(defn on-unmount [comp]
  (assert (:mounted? comp) (str "Must be mounted: " comp))
  (protocols/-on-unmount-impl comp)
  (protocols/-set! comp :mounted? false))
  
(core/defparent ATerminal2
  "Simple component that has no children"
  [^:mut mounted? ^:mut frame]
  protocols/IComponent
  (-measure [this ctx cs]
    (protocols/-on-mount this)
    (protocols/-measure-impl this ctx cs))
  
  (-draw [this ctx rect canvas]
    (before-draw this ctx rect canvas)
    (protocols/-draw-impl this ctx rect canvas)
    (after-draw this ctx rect canvas))
  
  (-draw-impl [this ctx rect canvas])
  
  (-event [this ctx event]
    (protocols/-event-impl this ctx event))
  
  (-event-impl [_ _ _])
  
  (-iterate [this _ cb]
    (cb this))
  
  protocols/ILifecycle
  (-on-mount [this]
    (on-mount this))
  
  (-on-mount-impl [this])
  
  (-on-unmount [this]
    (on-unmount this))
  
  (-on-unmount-impl [this]))

(core/deftype+ Label [*paint *text ^:mut *line]
  :extends ATerminal2  
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [^TextLine line @*line]
      (core/ipoint (math/ceil (.getWidth line)) @*font-ui-cap-height)))
  
  (-draw-impl [this ctx rect ^Canvas canvas]
    (.drawTextLine canvas
      @*line
      (:x rect)
      (+ (:y rect) @*font-ui-cap-height)
      (s/maybe-read *paint)))
  
  protocols/ILifecycle
  (-on-mount-impl [_]
    (set! *line
      (s/signal-named (str "label/line[" (s/maybe-read *text) "]")
        (let [text (str (s/maybe-read *text))
              font @*font-ui]
          (.shapeLine core/shaper text font ShapingOptions/DEFAULT)))))
  
  (-on-unmount-impl [_]
    (set! *line (s/dispose! *line))))

(defn label [*text]
  (map->Label
    {:*paint *fill-text
     :*text  *text}))

(core/deftype+ Gap [*width *height]
  :extends ATerminal2
  protocols/IComponent
  (-measure-impl [_ ctx _cs]
    (core/ipoint
      (* @*scale @*width)
      (* @*scale @*height))))

(defn gap [*width *height]
  (map->Gap
    {:*width  *width
     :*height *height}))

(core/defparent AWrapper2
  "A component that has exactly one child"
  [*child ^:mut mounted? ^:mut frame]
  protocols/IComponent
  (-measure [this ctx cs]
    (protocols/-on-mount this)
    (protocols/-measure-impl this ctx cs))
  
  (-measure-impl [this ctx cs]
    (core/measure (s/maybe-read *child) ctx cs))
  
  (-draw [this ctx rect canvas]
    (before-draw this ctx rect canvas)
    (protocols/-draw-impl this ctx rect canvas)
    (after-draw this ctx rect canvas))
  
  (-event [this ctx event]
    (protocols/-event-impl this ctx event))
    
  (-event-impl [this ctx event]
    (core/event-child (s/maybe-read *child) ctx event))
  
  protocols/ILifecycle
  (-on-mount [this]
    (on-mount this))
  
  (-on-mount-impl [this])
  
  (-on-unmount [this]
    (on-unmount this))
  
  (-on-unmount-impl [this]))

(core/deftype+ Clickable [*on-click ^:mut child-rect]
  :extends AWrapper2
  protocols/IComponent
  (-draw-impl [this ctx rect canvas]
    (set! child-rect rect)
    (core/draw-child (s/maybe-read *child) ctx rect canvas))
  
  (-event-impl [this ctx event]
    (when (and
            (= :mouse-button (:event event))
            (not (:pressed? event))
            (core/rect-contains? child-rect (core/ipoint (:x event) (:y event))))
      (mutate-later (s/maybe-read *on-click))
      nil)))

(defn clickable [*on-click *child]
  (map->Clickable
    {:*on-click *on-click
     :*child *child}))

(core/deftype+ Padding [*amount]
  :extends AWrapper2
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [amount     (* 2 @*scale (s/maybe-read *amount))
          child-cs   (core/ipoint (- (:width cs) amount) (- (:height cs) amount))
          child-size (core/measure (s/maybe-read *child) ctx child-cs)]
      (core/ipoint
        (+ (:width child-size) amount)
        (+ (:height child-size) amount))))

  (-draw-impl [this ctx rect canvas]
    (let [amount (* @*scale (s/maybe-read *amount))]
      (protocols/-draw (s/maybe-read *child) ctx
        (core/irect-ltrb
          (+ (:x rect) amount)
          (+ (:y rect) amount)
          (- (:right rect) amount)
          (- (:bottom rect) amount))
        canvas))))

(defn padding [*amount *child]
  (map->Padding
    {:*amount *amount
     :*child  *child}))

(core/deftype+ Center []
  :extends AWrapper2
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    cs)

  (-draw-impl [this ctx rect canvas]
    (let [child      (s/maybe-read *child)
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

(defn center [*child]
  (map->Center
    {:*child *child}))

(core/deftype+ Fill [*paint]
  :extends AWrapper2
  protocols/IComponent
  (-draw-impl [this ctx rect canvas]
    (canvas/draw-rect canvas rect (s/maybe-read *paint))
    (core/draw-child (s/maybe-read *child) ctx rect canvas)))

(defn fill [*paint *child]
  (map->Fill
    {:*paint *paint
     :*child *child}))

(core/defparent AContainer2
  "A component that has 1+ child"
  [*children ^:mut mounted? ^:mut frame]
  protocols/IComponent
  (-measure [this ctx cs]
    (protocols/-on-mount this)
    (protocols/-measure-impl this ctx cs))
    
  (-draw [this ctx rect canvas]
    (before-draw this ctx rect canvas)
    (protocols/-draw-impl this ctx rect canvas)
    (after-draw this ctx rect canvas))
  
  (-event [this ctx event]
    (protocols/-event-impl this ctx event))
  
  (-event-impl [this ctx event]
    (reduce #(core/eager-or %1 (protocols/-event (s/maybe-read %2) ctx event)) nil (s/maybe-read *children)))  
  
  protocols/ILifecycle
  (-on-mount [this]
    (on-mount this))
  
  (-on-mount-impl [this])
  
  (-on-unmount [this]
    (on-unmount this))
  
  (-on-unmount-impl [this]))

(core/deftype+ Column []
  :extends AContainer2
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [gap (* @*scale @*padding)]
      (loop [children (s/maybe-read *children)
             w        0
             h        0]
        (if-some [child (first children)]
          (let [size (protocols/-measure child ctx cs)]
            (recur
              (next children)
              (long (max w (:width size)))
              (long (+ h (:height size) gap))))
          (core/isize w h)))))
  
  (-draw-impl [this ctx rect canvas]
    (let [gap (* @*scale @*padding)]
      (loop [children (s/maybe-read *children)
             top      (:y rect)]
        (when-some [child (first children)]
          (let [size (protocols/-measure child ctx (core/isize (:width rect) (:height rect)))]
            (protocols/-draw child ctx (core/irect-xywh (:x rect) top (:width size) (:height size)) canvas)
            (recur (next children) (+ top (:height size) gap))))))))

(defn column [*children]
  (map->Column
    {:*children *children}))

(core/deftype+ Row []
  :extends AContainer2
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [gap (* @*scale @*padding)]
      (loop [children (s/maybe-read *children)
             w        0
             h        0]
        (if-some [*child (first children)]
          (let [child (s/maybe-read *child)
                size  (protocols/-measure child ctx cs)]
            (recur
              (next children)
              (long (+ w (:width size) gap))
              (long (max h (:height size)))))
          (core/isize w h)))))
  
  (-draw-impl [this ctx rect canvas]
    (let [gap (* @*scale @*padding)]
      (loop [children (s/maybe-read *children)
             left     (:x rect)]
        (when-some [*child (first children)]
          (let [child (s/maybe-read *child)
                size  (protocols/-measure child ctx (core/isize (:width rect) (:height rect)))]
            (protocols/-draw child ctx (core/irect-xywh left (:y rect) (:width size) (:height size)) canvas)
            (recur (next children) (+ left (:width size) gap))))))))

(defn row [*children]
  (map->Row
    {:*children *children}))

(core/deftype+ Shell [*child ^:mut effect ^:mut rendered]
  protocols/IComponent
  (-measure [this ctx cs]
    cs)
  
  (-draw [this ctx rect canvas]
    (let [frame     @*frame
          _         (some-> effect s/dispose!)
          *context  (volatile! (transient #{}))
          *rendered (volatile! (transient []))
          _         (binding [s/*context* *context
                              *rendered*  *rendered]
                      (core/draw-child (s/maybe-read *child) ctx rect canvas))
          signals   (persistent! @*context)] ;; what was read during draw
      
      (doseq [comp rendered
              :when (< (:frame comp) frame)]
        (protocols/-on-unmount comp))
      (set! rendered (persistent! @*rendered))
      
      ;; log all watched signals
      ; (println "Watching for next re-render:")
      ; (doseq [s signals]
      ;   (core/log " " (:name s) (:value s)))
      ; (println "")

      ;; actually watch signals
      (set! effect
        (s/effect-named "request-frame" signals
          (request-frame)))
      
      ;; bump frame number
      (reset! *frame (inc frame))
      
      #_(request-frame)
      ))
  
  (-event [this ctx event]
    ;; track mutations
    (let [*mutations (volatile! (transient []))
          res (binding [*mutations* *mutations]
                (core/event-child (s/maybe-read *child) ctx event))
          mutations (persistent! @*mutations)]
      (doseq [m mutations]
        (m))
      res)))

(defn shell [*child]
  (map->Shell
    {:*child *child}))

(s/defsignal *filter
  :all)

(defn random-todo [id filter]
  {:id      id
   :checked (case filter
              :all       (rand-nth [true false])
              :active    false
              :completed true)})

(defn random-*todo [id filter]
  (let [todo (random-todo id filter)]
    (s/signal-named (str "todo[" (:id todo) "]")
      todo)))

(s/defsignal *todos
  (mapv #(random-*todo % :all) (range 0 1)))

(s/defsignal *filtered-todos
  (case @*filter
    :all       @*todos
    :active    (vec (remove #(:checked @%) @*todos))
    :completed (filterv #(:checked @%) @*todos)))

(defn button [*on-click *label]
  (clickable *on-click
    (fill *fill-button
      (padding *padding
        (label *label)))))

(defn without [xs x]
  (vec (remove #(= % x) xs)))

(defn render-todo [*todo]
  (padding *padding
    (row
      [(clickable
         #(s/swap! *todo update :checked not)
         (label
           (s/signal-named (str "todo[" (:id @*todo) "]/checkbox")
             (if (:checked @*todo) "✅" "☑️"))))
       (label
         (s/signal-named (str "todo[" (:id @*todo) "]/label")
           (str @*todo " frame " @*frame)))
       (clickable
         #(s/swap! *todos without *todo)
         (label "🗑️"))])))

(def filter-btns
  (row
    (for [f [:all :active :completed]
          :let [text (str/capitalize (name f))
                *s   (s/signal-named (str "filter-btn[" f "]/match")
                       (= f @*filter))]]
      (s/signal-named (str "filter-btn[" f "]")
        (if @*s
          (padding *padding (label text))
          (button #(s/reset! *filter f) text))))))

(defn add-first []
  (let [filter @*filter
        id     (-> @*todos first deref :id dec)]
    (s/swap! *todos #(vec (cons (random-*todo id filter) %)))))

(defn add-last []
  (let [filter @*filter
        id     (-> @*todos last deref :id inc)]
    (s/swap! *todos conj (random-*todo id filter))))

(def app
  (shell
    (center
      (let [header    (padding *padding
                        (row
                          [(label "Todos:")
                           (label (s/signal-named "header/count-all" (count @*todos)))
                           (label "visible:")
                           (label (s/signal-named "header/count-visible" (count @*filtered-todos)))]))
            *body     (s/mapv render-todo *filtered-todos)
            first-btn (button add-first "Add First")
            last-btn  (button add-last "Add last")
            gc        (button (fn [] (System/gc)) "GC")
            footer    (row [first-btn last-btn gc])]
        (column
          (s/signal-named "column"
            (vec
              (concat
                [header
                 filter-btns]
                @*body
                [footer]))))))))

(reset! state/*app app)

(defn -main [& args]
  (ui/start-app!
    (let [screen (first (app/screens))
          _      (s/reset! *scale (:scale screen))
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
      (reset! state/*window window)))
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
  (profiler/profile
    (dotimes [i 1000]
      (add-last)))
  
  (s/reset! *padding 0)
  (s/reset! *padding 5)
  (s/reset! *padding 7)
  (s/reset! *padding 10)
  (s/reset! *padding 20)
  (s/reset! *accent 0xFFE0E0E0)
  (s/reset! *accent 0xFFD4D0C8)
  (s/reset! *accent 0xFFA5C9EF)
  (s/reset! *scale 2)
  (s/reset! *scale 3)
  (s/reset! *scale 4)
  (request-frame))
