(ns examples.text-field
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(defn text-field [text & {:keys [from to placeholder cursor-blink-interval cursor-width padding-h padding-v padding-top padding-bottom border-radius]
                          :or {cursor-blink-interval 500, cursor-width 1, padding-h 0, padding-v 3, border-radius 4}
                          :as opts}]
  [ui/with-context
   {:hui.text-field/cursor-blink-interval cursor-blink-interval
    :hui.text-field/cursor-width          cursor-width
    :hui.text-field/border-radius         border-radius
    :hui.text-field/padding-top           (float (or padding-top padding-v))
    :hui.text-field/padding-bottom        (float (or padding-bottom padding-v))
    :hui.text-field/padding-left          (float padding-h)
    :hui.text-field/padding-right         (float padding-h)}
   [ui/text-field
    (assoc opts :*state
      (signal/signal
        {:text        text
         :placeholder placeholder
         :from        from
         :to          to}))]])

(defn ui []
  [ui/align {:y :center}
   [ui/vscrollbar
    [ui/align {:x :center}
     [ui/padding {:padding 20}
      [ui/focus-controller
       [ui/column {:gap 10}
        [ui/size {:width 300}
         [text-field "Change me ([{word1} word2] wo-rd3)  , word4 🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️ 🚵🏻‍♀️ more more more" :from 13 :to 18 :border-radius 0]]
        [ui/size {:width 300}
         [text-field "0123456890 AaBbCcDdEe FfGgHhIiJj KkLlMmNnOo PpQqRrSsTt UuVvWwXxYyZz" :focused (core/now) :padding-h 5 :padding-v 10 :cursor-width 2 :cursor-blink-interval 100 :border-radius 100]]
        [ui/size {:width 300}
         [text-field "" :placeholder "Type here" :padding-h 5 :padding-v 10]]
        [ui/size {:width 300}
         [text-field "0123456890 AaBbCcDdEe FfGgHhIiJj KkLlMmNnOo PpQqRrSsTt UuVvWwXxYyZz" :padding-h 5 :padding-top 20 :padding-bottom 5 :cursor-blink-interval 0]]
        [ui/size {:width 300}
         [ui/align {:x :left}
          [text-field "Content width" :from 13 :to 13 :padding-h 5 :padding-v 10]]]
        [ui/size {:width 300}
         [ui/align {:x :center}
          [text-field "Align center" :padding-h 5 :padding-v 10]]]
        [ui/size {:width 300}
         [ui/align {:x :right}
          [text-field "Align right" :padding-h 5 :padding-v 10]]]]]]]]])
