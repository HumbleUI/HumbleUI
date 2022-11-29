(ns examples.todomvc
  (:require
    [clojure.math :as math]
    [clojure.string :as str]
    [examples.state :as state]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.cursor :as cursor]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Canvas FilterBlurMode FontMgr FontStyle ImageFilter MaskFilter Path PathDirection]))

(def *state
  state/*todomvc-state)

(def ^FontMgr font-mgr
  (FontMgr/getDefault))

(def font-families
  (into-array String
    ["Helvetica Neue" "Helvetica" "Arial"]))

(def typeface-100
  (.matchFamiliesStyle font-mgr
    font-families
    (-> FontStyle/NORMAL (.withWeight 100))))

(def typeface-300
  (.matchFamiliesStyle font-mgr
    font-families
    (-> FontStyle/NORMAL (.withWeight 300))))

(def typeface-300-italic
  (.matchFamiliesStyle font-mgr
    font-families
    (-> FontStyle/ITALIC (.withWeight 300))))

(def paint-bg
  (paint/fill 0xFFF5F5F5))

(def paint-completed
  (paint/fill 0xFFD9D9D9))

(def paint-label
  (paint/fill 0xFF4D4D4D))

(def divider
  (ui/rect (paint/fill 0xFFEDEDED)
    (ui/gap 0 1)))

(def strikethrough
  (ui/valign 0.7
    (ui/rect paint-completed
      (ui/gap 0 1))))

(def paint-footer
  (paint/fill 0xFF777777))

(def paint-transparent
  (paint/fill 0x00000000))

(defn add-todo [state]
  (let [text (:text (:new-todo state))]
    (if (str/blank? text)
      state
      (-> state
        (update :new-todo assoc :text "" :from 0 :to 0)
        (update :next-id inc)
        (update :todos assoc (:next-id state)
          {:label text
           :completed? false})))))

(defn save [state]
  (let [state (add-todo state)]
    (if-some [editing (:editing state)]
      (let [{:keys [id text]} editing
            state' (dissoc state :editing)]
        (if (str/blank? text)
          (update state' :todos dissoc id)
          (assoc-in state' [:todos id :label] text)))
      state)))

(defn edit [state id]
  (let [state (save state)
        label (get-in state [:todos id :label])]
    (assoc state
      :editing {:id   id
                :text label
                :from (count label)
                :to   (count label)})))

(defn inset-shadow [dx dy r color child]
  (ui/stack
    (ui/canvas
      {:on-paint
       (fn [ctx ^Canvas canvas size]
         (let [{:keys [width height]} size
               {:keys [scale]} ctx
               r'     (* r scale)
               inner  (core/rect-ltrb 0 0 width height)
               extra  (+ r'
                        (max
                          (abs (* dx scale))
                          (abs (* dy scale))))
               outer  (core/rect-ltrb (- extra) (- extra) (+ width extra) (+ height extra))]
           (with-open [paint  (paint/fill color)
                       filter (MaskFilter/makeBlur FilterBlurMode/NORMAL (core/radius->sigma r'))
                       path   (Path.)]
             (.addRect path outer)
             (.addRect path inner PathDirection/COUNTER_CLOCKWISE)
             (paint/set-mask-filter paint filter)
             (canvas/translate canvas (* dx scale) (* dy scale))
             (.drawPath canvas path paint))))})
    child))

(def title
  (ui/halign 0.5
    (ui/dynamic ctx [{:keys [scale]} ctx]
      (ui/label
        {:font  (font/make-with-size typeface-100 (* 100 scale))
         :paint (paint/fill 0x26AF2F2F)}
        "todos"))))

(defn body [child]
  (ui/dynamic ctx [{:keys [scale]} ctx]
    (let [r1       (core/radius->sigma (* 4 scale))
          shadow1  (ImageFilter/makeDropShadow 0 (* 2 scale) r1 r1 0x33000000)
          r2       (core/radius->sigma (* 50 scale))
          shadow2  (ImageFilter/makeDropShadow 0 (* 25 scale) r2 r2 0x20000000)
          shadow   (ImageFilter/makeCompose shadow1 shadow2)
          paint-fg (-> (paint/fill 0xFFFFFFFF)
                     (paint/set-image-filter shadow))]
      (ui/rect paint-fg
        child))))

(defn event-handler [child]
  (ui/event-listener
    {:capture? true
     :key
     (fn [e]
       (when (and (:pressed? e) (= :enter (:key e)))
         (swap! *state save)
         true))}
    (ui/event-listener
      {:mouse-button
       (fn [e]
         (when (:pressed? e)
           (swap! *state save)))}
      child)))

(defn completed-all? []
  (every? :completed? (-> *state deref :todos vals)))

(defn complete-all [val]
  (swap! *state update :todos
    (fn [todos]
      (let [keys (keys todos)]
        (reduce (fn [todos k] (update todos k assoc :completed? val)) todos keys)))))

(def toggle-all
  (ui/padding 5 15 0 15
    (ui/clickable
      {:on-click
       (fn [_]
         (complete-all (not (completed-all?)))
         true)}
      (ui/width 40
        (ui/height 40
          (ui/dynamic _ [state (completed-all?)]
            (if state
              (ui/svg "dev/images/todomvc/uncheck-all.svg")
              (ui/svg "dev/images/todomvc/check-all.svg"))))))))

(def *new-todo
  (cursor/cursor *state :new-todo))

(def new-todo
  (ui/rect (paint/fill 0xFFFEFEFE)
    (ui/height 66
      (inset-shadow 0 -2 1 0x08000000
        (ui/row
          (ui/valign 0.5
            toggle-all)
          [:stretch 1
           (ui/valign 0.5
             (ui/focusable {:on-focus #(swap! *state save)
                            :on-blur  #(swap! *state add-todo)}
               (ui/with-cursor :ibeam
                 (ui/text-input {} *new-todo))))]))))) ;; FIXME reset from/to

(defn todo-toggle [*state]
  (ui/padding 5 10 0 10
    (ui/clickable
      {:on-click
       (fn [_]
         (swap! *state not)
         true)}
      (ui/width 40
        (ui/height 40
          (ui/dynamic _ [state @*state]
            (if state
              (ui/svg "dev/images/todomvc/checked.svg")
              (ui/svg "dev/images/todomvc/unchecked.svg"))))))))

(defn todo-delete [id]
  (ui/padding 10
    (ui/dynamic ctx [{:hui/keys [hovered?]} ctx]
      (if hovered?
        (ui/with-context {:hui/hovered? false}
          (ui/clickable
            {:on-click
             (fn [_]
               (swap! *state update :todos dissoc id)
               true)}
            (ui/width 40
              (ui/height 40
                (ui/dynamic ctx [{:hui/keys [hovered?]} ctx]
                  (if hovered?
                    (ui/svg "dev/images/todomvc/delete-hovered.svg")
                    (ui/svg "dev/images/todomvc/delete.svg")))))))
        (ui/gap 40 40)))))

(defn todo [id]
  (ui/hoverable
    (ui/height 60
      (ui/row
        (todo-toggle
          (cursor/cursor-in *state [:todos id :completed?]))
        (ui/gap 10 0)
        [:stretch 1
         (ui/clickable
           {:on-click ;; TODO double click
            (fn [_]
              (swap! *state edit id)
              true)}
           (ui/valign 0.5
             (ui/halign 0
               (ui/dynamic _ [{:keys [label completed?]} (get-in @*state [:todos id])]
                 (if completed?
                   (ui/stack
                     (ui/label {:paint paint-completed} label)
                     strikethrough)
                   (ui/label {:paint paint-label} label))))))]
        (todo-delete id)))))

(defn todo-edit [id]
  (ui/dynamic ctx [{:keys [scale]} ctx]
    (ui/height 60
      (ui/row
        (ui/gap 45 0)
        [:stretch 1
         (ui/focusable {:focused? true}
           (ui/with-cursor :ibeam
             (ui/padding 0 0.5 1.5 0.5
               (inset-shadow 0 -1 5 0x33000000
                 (ui/rect (paint/stroke 0xFF999999 (* 1 scale))
                   (ui/valign 0.5
                     (ui/with-context
                       {:hui.text-field/padding-left 10}
                       (ui/text-input {} (cursor/cursor *state :editing)))))))))]))))

(def todos
  (ui/dynamic _ [ids (let [{:keys [mode todos]} @*state]
                       (sort
                         (for [[k v] todos
                               :when (or (= :all mode)
                                       (and (= :active mode) (not (:completed? v)))
                                       (and (= :completed mode) (:completed? v)))]
                           k)))]
    (ui/column
      (interpose divider
        (for [id ids]
          (ui/dynamic _ [editing? (= id (get-in @*state [:editing :id]))]
            (if editing?
              (todo-edit id)
              (todo id))))))))

(defn mode [mode label]
  (ui/with-cursor :pointing-hand
    (ui/clickable
      {:on-click
       (fn [_]
         (swap! *state save)
         (swap! *state assoc :mode mode))}
      (ui/dynamic ctx [{:keys [scale]
                        :hui/keys [hovered?]} ctx
                       selected? (= mode (:mode @*state))]
        (let [color (cond
                      selected? 0x33AF2F2F
                      hovered?  0x19AF2F2F
                      :else     0x00000000)]
          (ui/rounded-rect {:radius 3} (paint/stroke color (* 1 scale))
            (ui/padding 7
              (ui/label label))))))))

(def footer-active
  (ui/dynamic _ [cnt (->> @*state
                       :todos
                       vals
                       (remove :completed?)
                       count)]
    (ui/label (str cnt " " (if (= cnt 1) "item" "items") " left"))))

(def footer-modes
  (ui/row
    (mode :all "All")
    (ui/gap 10 0)
    (mode :active "Active")
    (ui/gap 10 0)
    (mode :completed "Completed")
    (ui/gap 10 0)))
  
(def footer-clear
  (ui/dynamic _ [has-completed? (some :completed? (vals (:todos @*state)))]
    (if has-completed?
      (ui/clickable
        {:on-click
         (fn [_]
           (let [state     (swap! *state save)
                 completed (for [[k v] (:todos state)
                                 :when (:completed? v)]
                             k)]
             (swap! *state update :todos
               #(reduce dissoc % completed))))}
        (ui/with-cursor :pointing-hand
          (ui/dynamic ctx [{:hui/keys [hovered?]} ctx]
            (ui/column
              (ui/label "Clear completed")
              (ui/gap 0 1)
              (ui/rect (if hovered? paint-footer paint-transparent) 
                (ui/gap 0 0.5))))))
      (ui/gap 0 0))))

(def footer
  (ui/height 40
    (ui/dynamic ctx [{:keys [scale]} ctx]
      (let [font-footer (font/make-with-size typeface-300 (* 14 scale))]
        (ui/with-context
          {:font-ui   font-footer
           :fill-text paint-footer}
          (ui/padding 15 0
            (ui/stack
              (ui/halign 0
                (ui/valign 0.5
                  footer-active))
              (ui/halign 0.5
                (ui/valign 0.5
                  footer-modes))
              (ui/halign 1
                (ui/valign 0.5
                  footer-clear)))))))))

(def ui
  (ui/focus-controller
    (ui/dynamic ctx [{:keys [scale]} ctx]
      (ui/with-context
        {:font-ui (font/make-with-size typeface-300 (* 24 scale))
         :hui.text-field/font-placeholder (font/make-with-size typeface-300-italic (* 24 scale))
         :hui.text-field/fill-placeholder (paint/fill 0xFFF1F1F1)}
        (ui/rect paint-bg
          (ui/vscrollbar ;; FIXME scroll doesn’t update when adding todos
            (ui/vscroll
              (ui/halign 0.5
                (ui/padding 0 30 0 30
                  (ui/width #(core/clamp (:width %) 230 550)
                    (ui/column
                      title
                      (ui/gap 0 25)
                      (event-handler
                        (body
                          (ui/column
                            new-todo
                            divider
                            todos
                            divider
                            footer))))))))))))))
