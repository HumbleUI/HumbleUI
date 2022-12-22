(ns io.github.humbleui.ui.listeners
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]))

(core/deftype+ EventListener [event-type callback capture?]
  :extends core/AWrapper
  
  protocols/IComponent
  (-event [_ ctx event]
    (or
      (when (and capture?
              (= event-type (:event event)))
        (callback event ctx))
      (core/event-child child ctx event)
      (when (and (not capture?)
              (= event-type (:event event)))
        (callback event ctx)))))

(defn event-listener
  ([event-type callback child]
   (map->EventListener
     {:event-type event-type
      :callback   callback
      :capture?   false
      :child      child}))
  ([opts event-type callback child]
   (map->EventListener
     {:event-type event-type
      :callback   callback
      :capture?   (:capture? opts)
      :child      child})))

(defn on-key-focused [keymap child]
  (event-listener {:capture? true} :key
    (fn [e ctx]
      (when (and (:hui/focused? ctx) (:pressed? e))
        (when-some [callback (get keymap (:key e))]
          (callback)
          true)))
    child))

(core/deftype+ KeyListener [on-key-down on-key-up]
  :extends core/AWrapper
  
  protocols/IComponent
  (-event [_ ctx event]
    (or
      (core/event-child child ctx event)
      (when (= :key (:event event))
        (if (:pressed? event)
          (when on-key-down
            (on-key-down event))
          (when on-key-up
            (on-key-up event)))))))

(defn key-listener [{:keys [on-key-up on-key-down] :as opts} child]
  (map->KeyListener
    (assoc opts
      :child child)))

(core/deftype+ MouseListener [on-move
                              on-scroll
                              on-button
                              on-over
                              on-out
                              ^:mut over?]
  :extends core/AWrapper
  
  protocols/IComponent
  (-draw [_ ctx rect canvas]
    (set! child-rect rect)
    (set! over? (core/rect-contains? child-rect (:mouse-pos ctx)))
    (core/draw-child child ctx rect canvas))
  
  (-event [_ ctx event]
    (core/when-every [{:keys [x y]} event]
      (let [over?' (core/rect-contains? child-rect (core/ipoint x y))]
        (when (and (not over?) over?' on-over)
          (on-over event))
        (when (and over? (not over?') on-out)
          (on-out event))
        (set! over? over?')))
        
    (core/eager-or
      (when (and on-move
              over?
              (= :mouse-move (:event event)))
        (on-move event))
      (when (and on-scroll
              over?
              (= :mouse-scroll (:event event)))
        (on-scroll event))
      (when (and on-button
              over?
              (= :mouse-button (:event event)))
        (on-button event))
      (core/event-child child ctx event))))

(defn mouse-listener [{:keys [on-move on-scroll on-button on-over on-out] :as opts} child]
  (map->MouseListener
    (assoc opts
      :over? false
      :child child)))

(core/deftype+ TextListener [on-input]
  :extends core/AWrapper
  protocols/IComponent
  (-event [_ ctx event]
    (core/eager-or
      (when (= :text-input (:event event))
        (on-input (:text event)))
      (core/event-child child ctx event))))

(defn text-listener [{:keys [on-input]} child]
  (map->TextListener
    {:on-input on-input
     :child    child}))


