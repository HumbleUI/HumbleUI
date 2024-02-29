(ns examples.todomvc
  (:require
    [clojure.math :as math]
    [clojure.string :as str]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.cursor :as cursor]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Canvas FontMgr FontStyle]))

(defonce *state
  (atom
    {:new-todo {:placeholder "What needs to be done?"}
     :mode     :all
     :next-id  3
     :todos    (sorted-map
                 0 {:label "first"
                    :completed? false}
                 1 {:label "second"
                    :completed? true}
                 2 {:label "third"
                    :completed? false})}))

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
        (update :new-todo assoc :text "")
        (update :next-id inc)
        (update :todos assoc (:next-id state)
          {:label text
           :completed? false})))))

(defn save [state]
  (if-some [editing (:editing state)]
    (let [{:keys [id text]} editing
          state' (dissoc state :editing)]
      (if (str/blank? text)
        (update state' :todos dissoc id)
        (assoc-in state' [:todos id :label] text)))
    state))

(defn edit [state id]
  (let [state (save state)
        label (get-in state [:todos id :label])]
    (assoc state
      :editing {:id   id
                :text label
                :from (count label)
                :to   (count label)})))

(def title
  (ui/halign 0.5
    (ui/with-scale scale
      (ui/label
        {:font  (font/make-with-size typeface-100 (* 100 scale))
         :paint (paint/fill 0x26AF2F2F)}
        "todos"))))


(defn body [child]
  (ui/with-scale scale
    (ui/dynamic _ [empty? (empty? (:todos @*state))]
      (ui/stack
        (ui/padding 0 0 0 10
          (ui/shadow {:dy 2 :blur 4 :color 0x33000000}))
        (ui/padding 0 0 0 10
          (ui/shadow {:dy 25 :blur 50 :color 0x20000000}))
        (when-not empty?
          (ui/padding 10 10 10 0
            (ui/shadow {:dy 1 :blur 1 :fill 0xFFF6F6F6 :color 0x33000000})))
        (when-not empty?
          (ui/padding 5 5 5 5
            (ui/shadow {:dy 1 :blur 1 :fill 0xFFF6F6F6 :color 0x33000000})))
        (ui/padding 0 0 0 10
          (ui/shadow {:dy 1 :blur 1 :fill 0xFFFFFFFF :color 0x33000000}
            (ui/rect (paint/fill 0xFFFFFFFF)
              child)))))))

(defn capture-clicks [child]
  (ui/event-listener :mouse-button
    (fn [e _]
      (when (:pressed? e)
        (swap! *state save)))
    child))

(defn completed-all? []
  (every? :completed? (-> *state deref :todos vals)))

(defn complete-all [val]
  (swap! *state update :todos
    (fn [todos]
      (let [keys (keys todos)]
        (reduce (fn [todos k] (update todos k assoc :completed? val)) todos keys)))))

(def toggle-all
  (ui/padding 5 15 0 15
    (ui/dynamic _ [empty? (empty? (:todos @*state))]
      (if empty?
        (ui/gap 40 40)
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
                  (ui/svg "dev/images/todomvc/check-all.svg"))))))))))

(def *new-todo
  (cursor/cursor *state :new-todo))

(def new-todo
  (ui/rect (paint/fill 0xFFFEFEFE)
    (ui/height 66
      (ui/shadow-inset {:dy -2, :blur 1, :color 0x08000000}
        (ui/row
          (ui/valign 0.5
            toggle-all)
          [:stretch 1
           (ui/valign 0.5
             (ui/focusable {:on-focus #(swap! *state save)}
               (ui/on-key-focused {:enter #(swap! *state add-todo)}
                 (ui/with-cursor :ibeam
                   (ui/text-input {} *new-todo)))))]))))) ;; FIXME reset from/to

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
           {:on-click
            (fn [e]
              (when (= 2 (:clicks e))
                (swap! *state edit id)
                true))}
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
  (ui/with-scale scale
    (ui/height 60
      (ui/row
        (ui/gap 45 0)
        [:stretch 1
         (ui/focusable {:focused (core/now)}
           (ui/on-key-focused
             {:enter #(swap! *state save)
              :escape #(swap! *state dissoc :editing)}
             (ui/with-cursor :ibeam
               (ui/padding 0 0.5 1.5 0.5
                 (ui/shadow-inset {:dy -1, :blur 5, :color 0x33000000}
                   (ui/rect (paint/stroke 0xFF999999 (* 1 scale))
                     (ui/valign 0.5
                       (ui/with-context
                         {:hui.text-field/padding-left 10}
                         (ui/text-input {} (cursor/cursor *state :editing))))))))))]))))

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
    (ui/with-scale scale
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
    (ui/with-scale scale
      (ui/with-context
        {:font-ui (font/make-with-size typeface-300 (* 24 scale))
         :hui.text-field/font-placeholder (font/make-with-size typeface-300-italic (* 24 scale))
         :hui.text-field/fill-placeholder (paint/fill 0xFFF1F1F1)}
        (ui/rect paint-bg
          (ui/vscrollbar
            (ui/halign 0.5
              (ui/padding 0 30 0 30
                (ui/width #(core/clamp (:width %) 230 550)
                  (ui/column
                    title
                    (ui/gap 0 25)
                    (capture-clicks
                      (body
                        (ui/dynamic _ [empty? (empty? (:todos @*state))]
                          (if empty?
                            new-todo
                            (ui/column
                              new-todo
                              divider
                              todos
                              divider
                              footer)))))))))))))))
