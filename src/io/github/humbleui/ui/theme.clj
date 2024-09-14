(in-ns 'io.github.humbleui.ui)

(defn default-theme
  ([child]
   (default-theme {} child))
  ([opts child]
   (load-typeface (io/resource "io/github/humbleui/fonts/Inter-Regular.ttf"))
   (load-typeface (io/resource "io/github/humbleui/fonts/Inter-Bold.ttf"))
   (load-typeface (io/resource "io/github/humbleui/fonts/Inter-Italic.ttf"))
   (load-typeface (io/resource "io/github/humbleui/fonts/FiraCode-Regular.ttf"))
   (load-typeface (io/resource "io/github/humbleui/fonts/FiraCode-Bold.ttf"))
   (let [ctx-fn (util/memo-fn [opts {:keys [scale]}]
                  (let [cap-height (* scale 9) ; (:cap-height (font/metrics font-ui))
                        paint      (or (:paint opts) {:fill 0xFF000000})
                        fill-gray  (or (:fill-gray opts) {:fill 0xFF808080})
                        theme      {:font-family     "Inter"
                                    :font-cap-height 9
                                    :font-family-aliases
                                    {"sans-serif" "Inter"
                                     "monospace"  "Fira Code"}
                       
                                    :paint     paint
                                    :fill-gray fill-gray
                         
                                    :hui.scroll/fill-track     {:fill 0x10000000}
                                    :hui.scroll/fill-thumb     {:fill 0x60000000}
                                                  
                                    :hui.slider/thumb-size           (* 16 scale)
                                    :hui.slider/track-height         (* 2 scale)
                                    :hui.slider/fill-track-active    {:fill 0xFF0080FF}
                                    :hui.slider/fill-track-inactive  {:fill 0xFFD9D9D9}
                                    :hui.slider/fill-thumb           {:fill 0xFFFFFFFF}
                                    :hui.slider/stroke-thumb         {:stroke 0xFF0080FF, :width 2}
                                    :hui.slider/fill-thumb-active    {:fill 0xFFE0E0E0}
                                    :hui.slider/stroke-thumb-active  {:stroke 0xFF0060E0, :width 2}
                         
                                    ; :hui.text-field/font                    nil
                                    ; :hui.text-field/font-placeholder        nil
                                    ; :hui.text-field/font-features           []
                                    :hui.text-field/cursor-blink-interval   500
                                    :hui.text-field/paint                   paint
                                    :hui.text-field/fill-placeholder        fill-gray
                                    :hui.text-field/fill-cursor             paint
                                    :hui.text-field/fill-selection-active   {:fill 0xFFB1D7FF}
                                    :hui.text-field/fill-selection-inactive {:fill 0xFFDDDDDD}
                                    :hui.text-field/fill-bg-active          {:fill 0xFFFFFFFF}
                                    :hui.text-field/fill-bg-inactive        {:fill 0xFFF8F8F8}
                                    :hui.text-field/border-radius           4
                                    :hui.text-field/border-active           {:stroke 0xFF749EE4}
                                    :hui.text-field/border-inactive         {:stroke 0xFFCCCCCC}
                                    :hui.text-field/cursor-width            (float 1)
                                    :hui.text-field/padding-top             (-> cap-height math/round (/ scale) float)
                                    :hui.text-field/padding-bottom          (-> cap-height math/round (/ scale) float)
                                    :hui.text-field/padding-left            (-> cap-height (/ 2) math/round (/ scale) float)
                                    :hui.text-field/padding-right           (-> cap-height (/ 2) math/round (/ scale) float)}]
                    (util/merge-some theme opts)))]
     (fn
       ([child]
        [ui/with-context (ctx-fn opts *ctx*) child])
       ([opts child]
        [ui/with-context (ctx-fn opts *ctx*) child])))))
