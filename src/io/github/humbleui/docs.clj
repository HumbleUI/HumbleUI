(ns io.github.humbleui.docs
  (:require
    [io.github.humbleui.app :as app]
    [io.github.humbleui.docs.align :as align]
    [io.github.humbleui.docs.button :as button]
    [io.github.humbleui.docs.clip :as clip]
    [io.github.humbleui.docs.color :as color]
    [io.github.humbleui.docs.column :as column]
    [io.github.humbleui.docs.devtools :as devtools]
    [io.github.humbleui.docs.gap :as gap]
    [io.github.humbleui.docs.grid :as grid]
    [io.github.humbleui.docs.label :as label]
    [io.github.humbleui.docs.overlay :as overlay]
    [io.github.humbleui.docs.padding :as padding]
    [io.github.humbleui.docs.paint :as paint]
    [io.github.humbleui.docs.rect :as rect]
    [io.github.humbleui.docs.row :as row]
    [io.github.humbleui.docs.size :as size]
    [io.github.humbleui.docs.split :as split]
    [io.github.humbleui.docs.toggle-button :as toggle-button]
    [io.github.humbleui.docs.vscroll :as vscroll]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.window :as window]))

(defonce *window
  (atom nil))

(defonce *example
  (ui/signal "Align"))

(defonce *app
  (atom nil))

(def examples
  [["Align" align/ui]
   ["Button" button/ui]
   ["Clip" clip/ui]
   ["Color" color/ui]
   ["Column" column/ui]
   ["Gap" gap/ui]
   ["Grid" grid/ui]
   ["Label" label/ui]
   ["Overlay" overlay/ui]
   ["Padding" padding/ui]
   ["Paint" paint/ui]
   ["Rect" rect/ui]
   ["Row" row/ui]
   ["Size" size/ui]
   ["Split" split/ui]
   ["Toggle button" toggle-button/ui]
   ["VScroll" vscroll/ui]])

(ui/defcomp example-label [name]
  [ui/clickable
   {:on-click
    (fn [_]
      (reset! *example name))}
   (fn [state]
     [ui/rect {:paint {:fill 
                       (cond
                         (= name @*example) "B2D7FE"
                         (:pressed state)   "A2C7EE"
                         (:hovered state)   "E1EFFA")}}
      [ui/padding {:horizontal 20 :vertical 10}
       [ui/label name]]])])

(ui/defcomp app []
  (let [examples-map (into {"DevTools" devtools/ui} examples)]
    (fn []
      [ui/key-listener
       {:on-key-down
        (fn [e]
          (when (and
                  (= :w (:key e))
                  (contains? (:modifiers e)
                    (if (= :macos app/platform) :mac-command :ctrl)))
            (window/close @*window)))}
       [ui/hsplit {:width 150
                   :gap [ui/rect {:paint {:fill "CCC"}}
                         [ui/gap {:width 1}]]}
        [ui/align {:y :top}
         [ui/vscroll
          [ui/padding {:bottom 10}
           [ui/column
            (for [[name _] examples]
              [example-label name])
            [ui/gap {:height 10}]
            [example-label "DevTools"]]]]]
        [ui/clip
         [(examples-map @*example)]]]])))

(reset! *app
  app)

(defn open! []
  (ui/start-app!
    (when ((some-fn nil? window/closed?) @*window)
      (reset! *window
        (doto (ui/window
                {:title          "Humble 🐝 UI Docs"
                 :exit-on-close? false}
                *app)
          (.focus)
          (.bringToFront))))))

(comment
  (open!))
