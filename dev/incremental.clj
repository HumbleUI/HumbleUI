(ns incremental
  (:require
    [clojure.core.server :as server]
    [clojure.math :as math]
    [clojure.string :as str]
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

(def *scale
  (s/signal
    (or
      (when-some [window @state/*window]
        (app/doui
          (window/scale window)))
      1)))

(def *face-ui
  (s/signal
    (typeface/make-from-resource "io/github/humbleui/fonts/Inter-Regular.ttf")))

(def *font-ui
  (s/computed
    (font/make-with-cap-height @*face-ui (* 10 @*scale))))

(def *fill-text
  (s/signal
    (paint/fill 0xFF000000)))

(def *padding
  (s/signal 10))

(def *fill-button
  (s/signal 0xFFA5C9EF))

(defn draw-repaint [comp ctx rect canvas]
  (protocols/-set! comp :renders ((fnil inc 0) (:renders comp)))
  (when (= 1 (:renders comp))
    (canvas/draw-rect canvas (-> rect .toRect (.inflate (* 2 @*scale))) (paint/stroke 0x80FF00FF @*scale))))
  
(core/defparent ATerminal2
  "Simple component that has no children"
  [^:mut renders]
  protocols/IComponent
  (-measure [_ _ cs]
    (core/ipoint 0 0))
  (-draw [this ctx rect canvas]
    (draw-repaint this ctx rect canvas))
  (-event [_ _ _])
  (-iterate [this _ cb]
    (cb this)))

(core/defparent AWrapper2
  "A component that has exactly one child"
  [*child ^:mut child-rect ^:mut renders]
  protocols/IComponent
  (-measure [this ctx cs]
    (core/measure (s/maybe-read *child) ctx cs))
  
  (-draw [this ctx rect canvas]
    (set! child-rect rect)
    (core/draw-child (s/maybe-read *child) ctx rect canvas)
    (draw-repaint this ctx rect canvas))

  (-event [this ctx event]
    (core/event-child (s/maybe-read *child) ctx event)))

(core/deftype+ Clickable [*on-click]
  :extends AWrapper2
  protocols/IComponent
  (-event [this ctx event]
    (when (and
            (= :mouse-button (:event event))
            (:pressed? event)
            child-rect
            (core/rect-contains? child-rect (core/ipoint (:x event) (:y event))))
      ((s/maybe-read *on-click))
      nil)))

(defn clickable [*on-click *child]
  (map->Clickable
    {:*on-click *on-click
     :*child *child}))

(core/deftype+ Label [*w *h *paint *line *metrics]
  :extends ATerminal2
  
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/ipoint @*w @*h))
  
  (-draw [this ctx rect ^Canvas canvas]
    (.drawTextLine canvas
      @*line
      (:x rect)
      (+ (:y rect) @*h)
      (s/maybe-read *paint))
    (draw-repaint this ctx rect canvas)))

(defn label [*text]
  (let [*line    (s/computed
                   (let [text (str (s/maybe-read *text))
                         font @*font-ui]
                     (.shapeLine core/shaper text font ShapingOptions/DEFAULT)))
        *metrics (s/computed
                   (font/metrics @*font-ui))]
    (map->Label
      {:*w       (s/computed
                   (math/ceil (.getWidth ^TextLine @*line)))
       :*h       (s/computed
                   (math/ceil (:cap-height @*metrics)))
       :*paint   *fill-text
       :*line    *line
       :*metrics *metrics
       :*repaint (s/effect [*text *font-ui *fill-text]
                   (request-frame))})))

(core/deftype+ Padding [*amount]
  :extends AWrapper2
  
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [amount     (* 2 (s/maybe-read *amount))
          child-cs   (core/ipoint (- (:width cs) amount) (- (:height cs) amount))
          child-size (core/measure (s/maybe-read *child) ctx child-cs)]
      (core/ipoint
        (+ (:width child-size) amount)
        (+ (:height child-size) amount))))

  (-draw [this ctx rect canvas]
    (let [amount (s/maybe-read *amount)]
      (protocols/-draw (s/maybe-read *child) ctx
        (core/irect-ltrb
          (+ (:x rect) amount)
          (+ (:y rect) amount)
          (- (:right rect) amount)
          (- (:bottom rect) amount))
        canvas)
      (draw-repaint this ctx rect canvas))))

(defn padding [*amount *child]
  (map->Padding
    {:*amount  (s/computed (* @*scale (s/maybe-read *amount)))
     :*child   *child
     :*repaint (s/effect [*amount]
                 (request-frame))}))

(core/deftype+ Center []
  :extends AWrapper2
  
  protocols/IComponent
  (-measure [_ ctx cs]
    cs)

  (-draw [this ctx rect canvas]
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
      (protocols/-draw child ctx rect' canvas))
    (draw-repaint this ctx rect canvas)))

(defn center [*child]
  (map->Center
    {:*child *child}))

(core/deftype+ Fill [*paint]
  :extends AWrapper2
  
  protocols/IComponent
  (-draw [this ctx rect canvas]
    (canvas/draw-rect canvas rect (s/maybe-read *paint))
    (core/draw-child (s/maybe-read *child) ctx rect canvas)
    (draw-repaint this ctx rect canvas)))

(defn fill [*color *child]
  (map->Fill
    {:*paint (s/computed (paint/fill (s/maybe-read *color)))
     :*child *child 
     :*repaint (s/effect [*color]
                 (request-frame))}))

(core/deftype+ Gap [*width *height]
  :extends ATerminal2
  protocols/IComponent
  (-measure [_ ctx _cs]
    (core/ipoint @*width @*height)))

(defn gap [*width *height]
  (map->Gap
    {:*width   (s/computed (core/iceil (* @*scale (s/maybe-read *width))))
     :*height  (s/computed (core/iceil (* @*scale (s/maybe-read *height))))
     :*repaint (s/effect [*width *height]
                 (request-frame))}))

(core/deftype+ Column [*children ^:mut renders]
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [gap (* @*scale @*padding)]
      (loop [children (s/maybe-read *children)
             w        0
             h        0]
        (if-some [child (first children)]
          (let [size (protocols/-measure child ctx cs)]
            (recur (next children) (max w (:width size)) (+ h (:height size) gap)))
          (core/isize w h)))))
  
  (-draw [this ctx rect canvas]
    (let [gap (* @*scale @*padding)]
      (loop [children (s/maybe-read *children)
             top      (:y rect)]
        (when-some [child (first children)]
          (let [size (protocols/-measure child ctx (core/isize (:width rect) (:height rect)))]
            (protocols/-draw child ctx (core/irect-xywh (:x rect) top (:width size) (:height size)) canvas)
            (recur (next children) (+ top (:height size) gap))))))
    (draw-repaint this ctx rect canvas))
  
  (-event [_ ctx event]
    (reduce #(core/eager-or %1 (protocols/-event %2 ctx event)) nil (s/maybe-read *children))))

(defn column [*children]
  (map->Column
    {:*children *children}))

(core/deftype+ Row [*children ^:mut renders]
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [gap (* @*scale @*padding)]
      (loop [children (s/maybe-read *children)
             w        0
             h        0]
        (if-some [*child (first children)]
          (let [child (s/maybe-read *child)
                size  (protocols/-measure child ctx cs)]
            (recur (next children) (+ w (:width size) gap) (max h (:height size))))
          (core/isize w h)))))
  
  (-draw [this ctx rect canvas]
    (let [gap (* @*scale @*padding)]
      (loop [children (s/maybe-read *children)
             left     (:x rect)]
        (when-some [*child (first children)]
          (let [child (s/maybe-read *child)
                size  (protocols/-measure child ctx (core/isize (:width rect) (:height rect)))]
            (protocols/-draw child ctx (core/irect-xywh left (:y rect) (:width size) (:height size)) canvas)
            (recur (next children) (+ left (:width size) gap))))))
    (draw-repaint this ctx rect canvas))
  
  (-event [_ ctx event]
    (reduce #(core/eager-or %1 (protocols/-event (s/maybe-read %2) ctx event)) nil (s/maybe-read *children))))

(defn row [*children]
  (map->Row
    {:*children *children}))

(defn random-todo []
  {:id      (+ 100 (rand-int 900))
   :checked (rand-nth [true false])})

(def *todos
  (s/signal
    (vec
      (repeatedly 5 #(s/signal (random-todo))))))

(def *filter
  (s/signal
    :all))

(def *filtered-todos
  (s/computed
    (case @*filter
      :all       @*todos
      :active    (vec (remove #(:checked @%) @*todos))
      :completed (filterv #(:checked @%) @*todos))))

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
         (label (s/computed (if (:checked @*todo) "✅" "❌"))))
       (label (s/computed (:id @*todo)))
       (clickable
         #(s/swap! *todos without *todo)
         (label "🗑️"))])))

(def filter-btns
  (row
    (for [f [:all :active :completed]
          :let [text (str/capitalize (name f))
                *s   (s/computed (= f @*filter))]]
      (s/computed
        (if @*s
          (padding *padding (label text))
          (button #(s/reset! *filter f) text))))))

(def app
  (center
    (let [header    (padding *padding
                      (row
                        [(label "Todos:")
                         (label (s/computed (count @*todos)))
                         (label "visible:")
                         (label (s/computed (count @*filtered-todos)))]))
          first-btn (button
                      (fn [] (s/swap! *todos #(vec (cons (s/signal (random-todo)) %))))
                      "Add First")
          last-btn  (button
                      #(s/swap! *todos conj (s/signal (random-todo)))
                      "Add last")
          *body     #_(s/computed
                        (mapv render-todo @*filtered-todos))
          (s/mapv render-todo @*filtered-todos)]
      (column
        (s/computed
          (concat
            [header
             filter-btns]
            (s/maybe-read *body)
            [(row [first-btn last-btn])]))))))

(reset! state/*app app)

(defn -main [& args]  
  (ui/start-app!
    (let [screen (last (app/screens))
          _      (s/reset! *scale (:scale screen))
          window (ui/window
                   {:title    "Humble 🐝 UI"
                    :mac-icon "dev/images/icon.icns"
                    :screen   (:id screen)
                    :width    800
                    :height   600
                    :x        :center
                    :y        :center}
                   state/*app)]
      ; (window/set-z-order window :floating)
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
  (s/reset! *padding 0)
  (s/reset! *padding 5)
  (s/reset! *padding 7)
  (s/reset! *padding 10)
  (s/reset! *padding 20)
  (s/reset! *fill-button 0xFFE0E0E0)
  (s/reset! *fill-button 0xFFA5C9EF)
  (s/reset! *fill-button 0xFFD4D0C8)
  (s/reset! *scale 2)
  (s/reset! *scale 4))
