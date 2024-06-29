(ns examples.text-field-debug
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(defn render-form [form]
  (cond
    (map? form)
    [ui/column {:gap 10}
     (for [[k v] form]
       [ui/row {:gap 4}
        [ui/label k]
        [render-form v]])]
    
    (sequential? form)
    [ui/column {:gap 10}
     (map #(vector render-form %) form)]
    
    :else
    [ui/label form]))

(def *state
  (signal/signal {:text ""}))

(defn ui []
  [ui/focus-controller
   [ui/padding {:padding 10}
    [ui/column {:gap 10}
     [ui/with-context
      {:hui.text-field/fill-cursor    (paint/fill 0xFF03BFFF)
       :hui.text-field/fill-selection-active (paint/fill 0x4003BFFF)
       :hui.text-field/cursor-width   2}
      [ui/text-field {:focused (core/now)
                      :*state *state
                      :placeholder "Type here"}]]
     [ui/label (str "\"" (:text @*state) "\"")]
     ^{:stretch 1}
     [ui/vscrollbar
      [render-form @*state]]]]])
