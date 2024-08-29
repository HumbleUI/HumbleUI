(in-ns 'io.github.humbleui.ui)

(util/deftype+ WithContext []
  :extends AWrapperNode

  (-context [_ ctx]
    (let [[_ overrides _ ] (parse-element element)]
      (merge ctx overrides))))

(defn with-context [overrides child]
  (map->WithContext {}))
