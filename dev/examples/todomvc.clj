(ns examples.todomvc
  (:require
    [clojure.math :as math]
    [clojure.string :as str]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Canvas]))

(def ^:dynamic *editing*
  false)

;; FIXME free cursors somehow

(defn cursor-in [*signal path]
  (let [*res (ui/signal (get-in @*signal path))]
    (add-watch *signal *res
      (fn [_ _ old new]
        (let [old (get-in old path)
              new (get-in new path)]
          (when (not= old new)
            (when-not *editing*
              (binding [*editing* true]
                (reset! *res new)))))))
    (add-watch *res ::source
      (fn [_ _ old new]
        (when (not= old new)
          (when-not *editing*
            (binding [*editing* true]
              (swap! *signal assoc-in path new))))))
    *res))

(defn cursor [*signal key]
  (cursor-in *signal [key]))

(def *state
  (ui/signal
    {:new-todo {:text ""
                :placeholder "What needs to be done?"}
     :mode     :all
     :next-id  3
     :todos    (sorted-map
                 0 {:label "first"
                    :completed? false}
                 1 {:label "second"
                    :completed? true}
                 2 {:label "third"
                    :completed? false})}))

(def paint-bg
  {:fill 0xFFF5F5F5})

(def paint-completed
  {:fill 0xFFD9D9D9})

(def paint-label
  {:fill 0xFF4D4D4D})

(defn divider []
  [ui/rect {:paint {:fill 0xFFEDEDED}}
   [ui/gap {:height 1}]])

(defn strikethrough []
  [ui/align {:y 0.7}
   [ui/rect {:paint paint-completed}
    [ui/gap {:height 1}]]])

(def paint-footer
  {:fill 0xFF777777})

(def paint-transparent
  {:fill 0x00000000})

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

(defn title []
  [ui/align {:x :center}
   [ui/label
    {:font-weight 100
     :font-size   100
     :paint       {:fill 0x26AF2F2F}}
    "todos"]])

(defn body [child]
  (let [empty? (empty? (:todos @*state))]
    [ui/stack
     [ui/padding {:bottom 10}
      [ui/shadow {:dy 2 :blur 4 :color 0x33000000}]]
     [ui/padding {:bottom 10}
      [ui/shadow {:dy 25 :blur 50 :color 0x20000000}]]
     (when-not empty?
       [ui/padding {:left 10 :top 10 :right 10}
        [ui/shadow {:dy 1 :blur 1 :fill 0xFFF6F6F6 :color 0x33000000}]])
     (when-not empty?
       [ui/padding {:padding 5}
        [ui/shadow {:dy 1 :blur 1 :fill 0xFFF6F6F6 :color 0x33000000}]])
     [ui/padding {:bottom 10}
      [ui/shadow {:dy 1 :blur 1 :fill 0xFFFFFFFF :color 0x33000000}
       [ui/rect {:paint {:fill 0xFFFFFFFF}}
        child]]]]))

(defn capture-clicks [child]
  [ui/event-listener
   {:event :mouse-button
    :on-event
    (fn [e _]
      (when (:pressed? e)
        (swap! *state save)
        true))}
   child])

(defn completed-all? []
  (every? :completed? (-> *state deref :todos vals)))

(defn complete-all [val]
  (swap! *state update :todos
    (fn [todos]
      (let [keys (keys todos)]
        (reduce (fn [todos k] (update todos k assoc :completed? val)) todos keys)))))

(defn toggle-all []
  [ui/padding {:left 5 :top 15 :bottom 15}
   (let [empty? (empty? (:todos @*state))]
     (if empty?
       [ui/gap {:width 40 :height 40}]
       [ui/clickable
        {:on-click
         (fn [_]
           (complete-all (not (completed-all?)))
           true)}
        [ui/size {:width 40, :height 40}
         (if-let [state (completed-all?)]
           [ui/svg {:src "dev/images/todomvc/uncheck-all.svg"}]
           [ui/svg {:src "dev/images/todomvc/check-all.svg"}])]]))])

(def *new-todo
  (cursor *state :new-todo))

(defn new-todo []
  [ui/rect {:paint {:fill 0xFFFEFEFE}}
   [ui/size {:height 66}
    [ui/shadow-inset {:dy -2, :blur 1, :color 0x08000000}
     [ui/row
      [ui/align {:y :center}
       [toggle-all]]
      ^{:stretch 1}
      [ui/align {:y :center}
       [ui/focusable {:on-focus #(swap! *state save)}
        [ui/on-key-focused {:keymap {:enter #(swap! *state add-todo)}}
         [ui/with-cursor {:cursor :ibeam}
          [ui/text-input {:*state *new-todo}]]]]]]]]]) ;; FIXME reset from/to

(defn todo-toggle [*state]
  [ui/padding {:left 5 :top 10 :bottom 10}
   [ui/clickable
    {:on-click
     (fn [_]
       (swap! *state not)
       true)}
    [ui/size {:width 40, :height 40}
     (if-let [state @*state]
       [ui/svg {:src "dev/images/todomvc/checked.svg"}]
       [ui/svg {:src "dev/images/todomvc/unchecked.svg"}])]]])

(defn todo-delete [hovered? id]
  [ui/padding {:padding 10}
   (if hovered?
     [ui/clickable
      {:on-click
       (fn [_]
         (swap! *state update :todos dissoc id)
         true)}
      (fn [state]
        [ui/size {:width 40, :height 40}
         (if (:hovered state)
           [ui/svg {:src "dev/images/todomvc/delete-hovered.svg"}]
           [ui/svg {:src "dev/images/todomvc/delete.svg"}])])]
     [ui/gap {:width 40 :height 40}])])

(defn todo [id]
  [ui/hoverable
   (fn [state]
     [ui/size {:height 60}
      [ui/row
       [todo-toggle
        (cursor-in *state [:todos id :completed?])]
       [ui/gap {:width 10}]
       ^{:stretch 1}
       [ui/clickable
        {:on-click
         (fn [e]
           (when (= 2 (:clicks e))
             (swap! *state edit id)
             true))}
        [ui/align {:x :left, :y :center}
         (let [{:keys [label completed?]} (get-in @*state [:todos id])]
           (if completed?
             [ui/stack
              [ui/label {:paint paint-completed} label]
              [strikethrough]]
             [ui/label {:paint paint-label} label]))]]
       [todo-delete (:hovered state) id]]])])

(defn todo-edit [id]
  [ui/size {:height 60}
   [ui/row
    [ui/gap {:width 45}]
    ^{:stretch 1}
    [ui/focusable {:focused (util/now)}
     [ui/on-key-focused
      {:keymap
       {:enter #(swap! *state save)
        :escape #(swap! *state dissoc :editing)}}
      [ui/with-cursor {:cursor :ibeam}
       [ui/padding {:top 0.5 :right 1.5 :bottom 0.5}
        [ui/shadow-inset {:dy -1, :blur 5, :color 0x33000000}
         [ui/rect {:paint {:stroke 0xFF999999}}
          [ui/align {:y :center}
           [ui/with-context
            {:hui.text-field/padding-left 10}
            [ui/text-input {:*state (cursor *state :editing)}]]]]]]]]]]])

(defn todos []
  (let [ids (let [{:keys [mode todos]} @*state]
              (sort
                (for [[k v] todos
                      :when (or (= :all mode)
                              (and (= :active mode) (not (:completed? v)))
                              (and (= :completed mode) (:completed? v)))]
                  k)))]
    [ui/column
     (interpose [divider]
       (for [id ids]
         (let [editing? (= id (get-in @*state [:editing :id]))]
           (if editing?
             [todo-edit id]
             [todo id]))))]))

(defn mode [mode label]
  [ui/with-cursor {:cursor :pointing-hand}
   [ui/clickable
    {:on-click
     (fn [_]
       (swap! *state save)
       (swap! *state assoc :mode mode))}
    (fn [state]
      (let [{:keys [scale]} ui/*ctx*
            hovered?  (:hovered? state)
            selected? (= mode (:mode @*state))
            color (cond
                    selected? 0x33AF2F2F
                    hovered?  0x19AF2F2F
                    :else     0x00000000)]
        [ui/rect {:radius 3
                  :paint  {:stroke color}}
         [ui/padding {:padding 7}
          [ui/label label]]]))]])

(defn footer-active []
  (let [cnt (->> @*state
              :todos
              vals
              (remove :completed?)
              count)]
    [ui/label (str cnt " " (if (= cnt 1) "item" "items") " left")]))

(defn footer-modes []
  [ui/row
   [mode :all "All"]
   [ui/gap {:width 10}]
   [mode :active "Active"]
   [ui/gap {:width 10}]
   [mode :completed "Completed"]
   [ui/gap {:width 10}]])
  
(defn footer-clear []
  (let [has-completed? (some :completed? (vals (:todos @*state)))]
    (if has-completed?
      [ui/clickable
       {:on-click
        (fn [_]
          (let [state     (swap! *state save)
                completed (for [[k v] (:todos state)
                                :when (:completed? v)]
                            k)]
            (swap! *state update :todos
              #(reduce dissoc % completed))))}
       (fn [state]
         [ui/with-cursor {:cursor :pointing-hand}
          [ui/column
           [ui/label "Clear completed"]
           [ui/gap {:height 1}]
           [ui/rect {:paint (if (:hovered? state) paint-footer paint-transparent)}
            [ui/gap {:height 0.5}]]]])]
      [ui/gap])))

(defn footer []
  [ui/size {:height 40}
   [ui/with-context
    {:font-size   14
     :font-weight 300
     :paint       paint-footer}
    [ui/padding {:horizontal 15}
     [ui/stack
      [ui/align {:x :left, :y :center}
       [footer-active]]
      [ui/align {:x :center, :y :center}
       [footer-modes]]
      [ui/align {:x :right, :y :center}
       [footer-clear]]]]]])

(defn ui []
  [ui/align {:y :center}
   [ui/vscroll {:clip? false}
    [ui/align {:x :center}
     [ui/padding {:padding 20}
      [ui/with-context
       {:font-family                     "Helvetica Neue, Helvetica, Arial"
        :font-size                       24
        :font-weight                     300
        :hui.text-field/font-placeholder (ui/get-font {:font-family "Helvetica Neue, Helvetica, Arial"
                                                       :font-size   24
                                                       :font-weight 300
                                                       :font-slant  :italic})
        :hui.text-field/fill-placeholder {:fill 0xFFF1F1F1}}
       [ui/rect {:paint paint-bg}
        [ui/size {:width #(util/clamp (:width %) 230 550)}
         [ui/column
          [title]
          [ui/gap {:height 25}]
          [capture-clicks
           [body
            (if-let [empty? (empty? (:todos @*state))]
              [new-todo]
              [ui/column
               [new-todo]
               [divider]
               [todos]
               [divider]
               [footer]])]]]]]]]]]])
