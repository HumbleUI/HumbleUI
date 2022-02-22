<p align="center">
  <img src="./extras/logo.png" height="400">
</p>

Humble UI is a desktop UI framework for Clojure.

## Status

Work in progress. No docs, and everything changes every day.

## Resources

Slack:

- #humbleui on Clojurians Slack ([invite here](http://clojurians.net/))

Posts:

- [Thoughts on Clojure UI framework](https://tonsky.me/blog/clojure-ui/)
- [Humble Chronicles: Decomposition](https://tonsky.me/blog/humble-decomposition/)

Videos:

- [Wordle in Clojure with Humble UI](https://www.youtube.com/watch?v=qSswvHrVnvo)

## Development

Run nREPL server:

```
./script/run.py
```

See `(comment)` forms in [user.clj](https://github.com/HumbleUI/HumbleUI/tree/main/dev/user.clj).

## Examples

|![](extras/screenshot_button.png)|![](extras/screenshot_container.png)|
|---|---|
|![](extras/screenshot_calculator.png)|![](extras/screenshot_wordle.png)|

```clj
(ns examples.label
  (:require
    [io.github.humbleui.ui :as ui]))

(def ui
  (ui/valign 0.5
    (ui/halign 0.5
      (ui/dynamic ctx [{:keys [font-ui fill-text]} ctx]
        (ui/label "Hello from Humble UI! 👋" font-ui fill-text)))))
```
