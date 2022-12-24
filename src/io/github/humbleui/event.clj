(ns io.github.humbleui.event
  (:require
    [io.github.humbleui.core :as core])
  (:import
    [io.github.humbleui.jwm
     EventFrame
     EventKey
     EventMouseButton
     EventMouseMove
     EventMouseScroll
     EventTextInput
     EventTextInputMarked
     EventWindowClose
     EventWindowCloseRequest
     EventWindowFocusIn
     EventWindowFocusOut
     EventWindowMaximize
     EventWindowMove
     EventWindowResize
     EventWindowRestore
     EventWindowScreenChange
     Key
     KeyLocation
     MouseButton]
    [io.github.humbleui.jwm.skija
     EventFrameSkija]))

(defn- mask->set [^long mask keys]
  (loop [acc  (transient #{})
         mask mask
         keys keys]
    (cond
      (empty? keys)
      (persistent! acc)
      
      (== 0 mask)
      (persistent! acc)
      
      (== 0 (bit-and mask 1))
      (recur acc (bit-shift-right mask 1) (next keys))
      
      :else
      (recur (conj! acc (first keys)) (bit-shift-right mask 1) (next keys)))))

(defn- modifiers->set [modifiers]
  (mask->set modifiers [:caps-lock :shift :control :alt :win-logo :linux-meta :linux-super :mac-command :mac-option :mac-fn]))

(def ^:private key->keyword
  {Key/UNDEFINED :undefined
   Key/CAPS_LOCK :caps-lock
   Key/SHIFT :shift
   Key/CONTROL :control
   Key/ALT :alt
   Key/WIN_LOGO :win-logo
   Key/LINUX_META :linux-meta
   Key/LINUX_SUPER :linux-super
   Key/MAC_COMMAND :mac-command
   Key/MAC_OPTION :mac-option
   Key/MAC_FN :mac-fn
   Key/ENTER :enter
   Key/BACKSPACE :backspace
   Key/TAB :tab
   Key/CANCEL :cancel
   Key/CLEAR :clear
   Key/PAUSE :pause
   Key/ESCAPE :escape
   Key/SPACE :space
   Key/PAGE_UP :page-up
   Key/PAGE_DOWN :page-down
   Key/END :end
   Key/HOME :home
   Key/LEFT :left
   Key/UP :up
   Key/RIGHT :right
   Key/DOWN :down
   Key/COMMA :comma
   Key/MINUS :minus
   Key/PERIOD :period
   Key/SLASH :slash
   Key/DIGIT0 :digit0
   Key/DIGIT1 :digit1
   Key/DIGIT2 :digit2
   Key/DIGIT3 :digit3
   Key/DIGIT4 :digit4
   Key/DIGIT5 :digit5
   Key/DIGIT6 :digit6
   Key/DIGIT7 :digit7
   Key/DIGIT8 :digit8
   Key/DIGIT9 :digit9
   Key/SEMICOLON :semicolon
   Key/EQUALS :equals
   Key/A :a
   Key/B :b
   Key/C :c
   Key/D :d
   Key/E :e
   Key/F :f
   Key/G :g
   Key/H :h
   Key/I :i
   Key/J :j
   Key/K :k
   Key/L :l
   Key/M :m
   Key/N :n
   Key/O :o
   Key/P :p
   Key/Q :q
   Key/R :r
   Key/S :s
   Key/T :t
   Key/U :u
   Key/V :v
   Key/W :w
   Key/X :x
   Key/Y :y
   Key/Z :z
   Key/OPEN_BRACKET :open-bracket
   Key/BACK_SLASH :back-slash
   Key/CLOSE_BRACKET :close-bracket
   Key/MULTIPLY :multiply
   Key/ADD :add
   Key/SEPARATOR :separator
   Key/DELETE :delete
   Key/NUM_LOCK :num-lock
   Key/SCROLL_LOCK :scroll-lock
   Key/F1 :f1
   Key/F2 :f2
   Key/F3 :f3
   Key/F4 :f4
   Key/F5 :f5
   Key/F6 :f6
   Key/F7 :f7
   Key/F8 :f8
   Key/F9 :f9
   Key/F10 :f10
   Key/F11 :f11
   Key/F12 :f12
   Key/F13 :f13
   Key/F14 :f14
   Key/F15 :f15
   Key/F16 :f16
   Key/F17 :f17
   Key/F18 :f18
   Key/F19 :f19
   Key/F20 :f20
   Key/F21 :f21
   Key/F22 :f22
   Key/F23 :f23
   Key/F24 :f24
   Key/PRINTSCREEN :printscreen
   Key/INSERT :insert
   Key/HELP :help
   Key/BACK_QUOTE :back_quote
   Key/QUOTE :quote
   Key/MENU :menu
   Key/KANA :kana
   Key/VOLUME_UP :volume-up
   Key/VOLUME_DOWN :volume-down
   Key/MUTE :mute})

(defn event->map [e]
  (core/case-instance e
    EventFrame
    {:event :frame}
    
    EventFrameSkija
    {:event   :frame-skija
     :surface (.getSurface e)}
    
    EventKey
    {:event     :key
     :key       (key->keyword (.getKey e))
     :key-name  (.getName (.getKey e))
     :key-types (mask->set (.-_mask (.getKey e)) [:function :navigation :arrow :modifier :letter :digit :whitespace :media])
     :pressed?  (.isPressed e)
     :modifiers (modifiers->set (.-_modifiers e))
     :location  (condp identical? (.getLocation e)
                  KeyLocation/DEFAULT :default
                  KeyLocation/RIGHT   :right
                  KeyLocation/KEYPAD  :keypad)}

    EventMouseButton
    {:event     :mouse-button
     :button    (condp identical? (.getButton e)
                  MouseButton/PRIMARY   :primary
                  MouseButton/SECONDARY :secondary
                  MouseButton/MIDDLE    :middle
                  MouseButton/BACK      :back
                  MouseButton/FORWARD   :forward)
     :pressed?  (.isPressed e)
     :x         (.getX e)
     :y         (.getY e)
     :modifiers (modifiers->set (.-_modifiers e))}
    
    EventMouseMove
    {:event     :mouse-move
     :x         (.getX e)
     :y         (.getY e)
     :buttons   (mask->set (.-_buttons e) [:primary :secondary :middle :back :forward])
     :modifiers (modifiers->set (.-_modifiers e))}
    
    EventMouseScroll
    {:event       :mouse-scroll
     :delta-x     (.getDeltaX e)
     :delta-y     (.getDeltaY e)
     :delta-chars (.getDeltaChars e)
     :delta-lines (.getDeltaLines e)
     :delta-pages (.getDeltaPages e)
     :x           (.getX e)
     :y           (.getY e)
     :modifiers   (modifiers->set (.-_modifiers e))}

    EventTextInput
    {:event :text-input
     :text  (.getText e)}
    
    EventTextInputMarked
    {:event           :text-input-marked
     :text            (.getText e)
     :selection-start (.getSelectionStart e)
     :selection-end   (.getSelectionEnd e)}
    
    EventWindowClose
    {:event :window-close}
    
    EventWindowCloseRequest
    {:event :window-close-request}
    
    EventWindowFocusIn
    {:event :window-focus-in}
    
    EventWindowFocusOut
    {:event :window-focus-out}
    
    EventWindowMaximize
    {:event :window-maximize}
    
    EventWindowFocusIn
    {:event :window-minimize}
    
    EventWindowMove
    {:event       :window-move
     :window-left (.getWindowLeft e)
     :window-top  (.getWindowTop e)}
    
    EventWindowResize
    {:event          :window-resize
     :window-width   (.getWindowWidth e)
     :window-height  (.getWindowHeight e)
     :content-width  (.getContentWidth e)
     :content-height (.getContentHeight e)}
    
    EventWindowRestore
    {:event :window-restore}
    
    EventWindowScreenChange
    {:event :window-screen-change}
    
    (do
      ; (println "Unrecognized event" e)
      nil)))
