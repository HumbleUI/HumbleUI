(ns io.github.humbleui.ui.hoverable
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]))

(core/deftype+ Hoverable [on-hover on-out ^:mut hovered?]
  :extends core/AWrapper
  
  protocols/IContext
  (-context [_ ctx]
    (cond-> ctx
      hovered? (assoc :hui/hovered? true)))
  
  protocols/IComponent
  (-draw [this ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (when-some [ctx' (protocols/-context this ctx)]
      (core/draw-child child ctx' child-rect canvas)))

  (-event [this ctx event]
    (core/eager-or
      (when-some [ctx' (protocols/-context this ctx)]
        (core/event-child child ctx' event))
      (core/when-every [{:keys [x y]} event]
        (let [hovered?' (core/rect-contains? child-rect (core/ipoint x y))]
          (when (not= hovered? hovered?')
            (set! hovered? hovered?')
            (if hovered?'
              (when on-hover
                (on-hover))
              (when on-out
                (on-out)))
            true))))))

(defn hoverable
  "Enable the child element to respond to mouse hover events.

  If no callback, the event can still effect rendering through use of dynamic
  context as follows:

    (ui/dynamic ctx [hovered? (:hui/hovered? ctx)]
       # here we know the hover state of the object
       ...)

  You can also respond to hover events by providing optional :on-hover and/or
  :on-out callbacks in an options map as the first argument. The callback
  functions take no arguments and ignore their return value."
  ([child]
   (map->Hoverable
     {:child child
      :hovered? false}))
  ([{:keys [on-hover on-out]} child]
   (map->Hoverable
     {:on-hover on-hover
      :on-out   on-out
      :child    child
      :hovered? false})))
