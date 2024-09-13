(in-ns 'io.github.humbleui.ui)

(defn- app-node [theme app]
  (cond
    (nil? app)
    nil
    
    (instance? clojure.lang.IDeref app)
    (recur theme @app)
    
    (fn? app)
    (make
      [default-theme theme
       [focus-controller
        [overlay-root
         app]]])

    :else
    app))

(defn window
  (^Window [app]
    (window {} app))
  (^Window [opts app]
    (let [{:keys [exit-on-close? title mac-icon screen width height x y bg-color on-paint on-event]
           :or {exit-on-close? true
                title    "Humble 🐝 UI"
                width    800
                height   600
                x        :center
                y        :center
                bg-color 0xFFF6F6F6}} opts
          *mouse-pos (volatile! (util/ipoint 0 0))
          *scale     (volatile! 0)
          ref?       (instance? clojure.lang.IRef app)
          *app-node  (atom (app-node (:theme opts) app))
          ctx-fn     (fn [window]
                       (when-not (window/closed? window)
                         {:window    window
                          :scale     (window/scale window)
                          :mouse-pos @*mouse-pos}))
          paint-fn   (fn [window canvas]
                       (locking window
                         (canvas/clear canvas bg-color)
                         (let [content-rect (window/content-rect window)
                               bounds       (util/irect-xywh 0 0 (:width content-rect) (:height content-rect))]
                           (when on-paint
                             (on-paint window canvas))
                           (when-some [app @*app-node]
                             (protocols/-draw app (ctx-fn window) bounds (util/ipoint (:width content-rect) (:height content-rect)) bounds canvas)))))
          event-fn   (fn event-fn [window event]
                       (locking window
                         (when-not (window/closed? window)
                           (let [scale (window/scale window)]
                             (when (not= @*scale scale)
                               (vreset! *scale scale)
                               (event-fn window {:event :window-scale-change, :scale scale}))))
                         (util/when-some+ [{:keys [x y]} event]
                           (vreset! *mouse-pos (util/ipoint x y)))
                         (when on-event
                           (on-event window event))
                         (when-some [app @*app-node]
                           (when-let [result (protocols/-event app (ctx-fn window) event)]
                             (window/request-frame window)
                             result))))
          window     (window/make
                       {:on-close (when (or ref? exit-on-close?)
                                    #(do
                                       (when ref?
                                         (remove-watch app ::redraw))
                                       (when exit-on-close?
                                         (System/exit 0))))
                        :on-paint paint-fn
                        :on-event event-fn})
          screen     (if screen
                       (util/find-by :id screen (app/screens))
                       (app/primary-screen))
          {:keys [scale work-area]} screen
          x          (cond
                       (= :left x)   0
                       (= :center x) (-> (:width work-area) (- (* width scale)) (quot 2))
                       (= :right x)  (-> (:width work-area) (- (* width scale)))
                       :else         (* scale x))
          y          (cond
                       (= :top y)    0
                       (= :center y) (-> (:height work-area) (- (* height scale)) (quot 2))
                       (= :bottom y) (-> (:height work-area) (- (* height scale)))
                       :else         (* scale y))]
      (window/set-window-position window (+ (:x work-area) x) (+ (:y work-area) y))
      (window/set-window-size window (* scale width) (* scale height))
      (window/set-title window title)
      (when (and (= :macos app/platform) mac-icon
        (window/set-icon window mac-icon)))
      (doto window
        (window/set-visible true)
        (window/bring-to-front))
      (when ref?
        (add-watch app ::redraw
          (fn [_ _ old new]
            (when-not (identical? old new)
              (some-> @*app-node protocols/-unmount)
              (reset! *app-node (app-node (:theme opts) app))
              (window/request-frame window)))))
      window)))
