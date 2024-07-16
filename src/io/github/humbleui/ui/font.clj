(in-ns 'io.github.humbleui.ui)

(util/import-vars typeface/load-typeface)
(util/import-vars typeface/load-typefaces)
(util/import-vars typeface/typeface)

(defn- font-resolve-aliases [font-family aliases]
  (loop [res      []
         families (str/split font-family #"\s*,\s*")]
    (util/cond+
      (empty? families)
      res
      
      :let [family   (first families)
            resolved (aliases family)]
      
      resolved
      (recur res (concat (str/split resolved #"\s*,\s*") (next families)))
      
      :else
      (recur (conj res family) (next families)))))
         

(defn get-font
  "Get cached instance of a font. All options are optional:
   
   :font-family     <string> - Font families, comma-separated
   :font-size       <number> - Font size in dip
   :font-cap-height <number> - Cap height in dip
   :font-weight     <number>  - 0...1000, default 400
   :font-width      <keyword> - :ultra-condensed | :extra-condensed | :condensed | :semi-condensed | :normal | :semi-expanded | :expanded | :extra-expanded | :ultra-expanded
   :font-slant      <keyword> - :upright | :italic | :oblique"
  (^Font []
    (get-font {}))
  (^Font [opts]
    (let [opts (util/merge-some
                 {:font-size       (:font-size *ctx*)
                  :font-cap-height (:font-cap-height *ctx*)
                  :font-weight     (:font-weight *ctx*)
                  :font-width      (:font-width *ctx*)
                  :font-slant      (:font-slant *ctx*)}
                 opts)
          families (-> (:font-family opts)
                     (or (:font-family *ctx*))
                     (font-resolve-aliases (:font-family-aliases *ctx*)))
          opts (-> opts
                 (dissoc :font-family)
                 (assoc  :font-families families)
                 (update :font-size scaled)
                 (update :font-cap-height scaled))]
      (font/get-font opts))))

(defn with-font-family-aliases
  "Create font-family aliases, e.g.
   
   {\"sans-serif\" \"Inter\"
    \"face-ui\"    \"Segoe UI, SF Pro Text, sans-serif\"}
   
   Can be nested and contain multiple fonts"
  [m child]
  [with-context
   {:font-family-aliases
    (merge (:font-family-aliases *ctx*) m)}
   child])
