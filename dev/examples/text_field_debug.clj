(ns examples.text-field-debug
  (:require
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(def *state (atom {:text ""}))

(defn render-form [form]
  (cond
    (map? form)
    (ui/column
      (interpose (ui/gap 0 10)
        (map (fn [[k v]] (ui/row (ui/label k) (ui/gap 4 0) (render-form v))) form)))
    
    (sequential? form)
    (ui/column
      (interpose (ui/gap 0 10)
        (map render-form form)))
    
    :else
    (ui/label form)))

(def ui
  (ui/dynamic _ [text (:text @*state)]
    (ui/padding 10 10
      (ui/column
        (ui/with-cursor :ibeam
          (ui/fill (paint/fill 0xFFFFFFFF)
            (ui/with-context
              {:hui.text-field/fill-cursor    (paint/fill 0xFF03BFFF)
               :hui.text-field/cursor-width   2
               :hui.text-field/padding-top    10
               :hui.text-field/padding-bottom 10
               :hui.text-field/padding-left   5
               :hui.text-field/padding-right  5
               }
              (ui/text-field *state))))
        (ui/gap 0 10)
        (ui/label (str "\"" text "\""))
        (ui/gap 0 10)
        [:stretch 1
         (ui/vscrollbar
           (ui/vscroll
             (ui/dynamic _ [state @*state]
               (render-form state))))]))))

; (reset! user/*example "text-field-debug")
