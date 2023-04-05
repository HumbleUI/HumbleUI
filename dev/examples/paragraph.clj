(ns examples.paragraph
  (:require
    [clojure.string :as str]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(def text
  "An interface is humane if it is responsive to human needs and considerate of human frailties. If you want to create a humane interface, you must have an understanding of the relevant information on how both humans and machines operate. In addition, you must cultivate in yourself a sensitivity to the difficulties that people experience. That is not necessarily a simple undertaking. We become accustomed to the ways that products work to the extent that we come to accept their methods as a given, even when their interfaces are unnecessarily complex, confusing, wasteful, and provocative of human error.

Many of us become annoyed, for example, at the amount of time that it takes a computer to start up, or to boot. An advertisement for a computer-based car radio in 1999 assures us that “unlike a home computer, there are no long boot-up times to worry about.” A scan through six respected books on the subject of interface design—books that have been written over most of the span of time during which interface design has been a recognized field—finds the topic of booting unmentioned (Shneiderman 1987; Norman 1988; Laurel 1990; Tognazzini 1992; Mayhew 1992; Cooper 1995). I am sure that each of these authors would wholeheartedly agree that reducing or eliminating this delay would improve usability; I have never met a user to whom the delay is not an annoyance. Yet the boot delay is so much a familiar and accepted part of using a computer that it is seldom questioned in the interface-design literature. There has never been any technical necessity for a computer to take more than a few seconds to begin operation when it is turned on. We have slow-booting computers only because many designers and implementers did not assign a high priority to making the interface humane in this regard. In addition, some people believe that the sale of millions of slow-booting computers “proves” that the way these machines now work is just fine.

The annoyance of having to wait for a machine to start up has not been ignored in other product areas. The defunct Apple Newton, the Palm Pilot, and other handheld computers manage to start instantly, and the introduction of a sleep mode—a state in which the computer is using less power than when it is on but from which it can be brought to operational status quickly—on certain computers is a step in the right direction.

Engineering has successfully tackled some more difficult problems. In early television sets, for example, the wait due to the time it took to heat the cathode of the picture tube was nearly a minute. In certain television sets, engineers added a circuit to keep the cathode warm, reducing the wait to reach operating temperature. (Keeping the cathode fully heated would have been wasteful of electricity and would have shortened the life of the tube.) Other engineers designed picture tubes that had cathodes that heated in a few seconds. Either way, the user's needs were satisfied. In the early twentieth century, the failure of the Stanley Steamer, a steam-powered automobile that apparently was a superior vehicle in other aspects, may have been a result of the 20-minute delay between firing it up and its having sufficient boiler pressure to drive.

That a user should not be kept waiting unnecessarily is an obvious and humane design principle. It is also humane not to hurry a user; the more general principle is: Users should set the pace of an interaction.

It does not take much technical knowledge to see, for example, that higher-bandwidth communication channels can hasten downloading of web pages. Other relationships are not so evident. It is useful for designers of a human-machine interface to understand the innards of the technology; otherwise, they have no way to judge the validity of assertions—by, say, programmers or hardware designers—that a given interface detail is not feasible.

— Jef Raskin. The Humane Interface")

(defn paragraph
  ([text]
   (paragraph {} text))
  ([opts text]
   (ui/halign 0
     (ui/dynamic ctx [{:keys [scale]} ctx]
       (ui/rect (paint/fill 0xFFEEEEEE)
         (let [opts' (merge {:features ["cv01" "cv09" "ss03"]} opts)]
           (ui/paragraph opts' text)))))))

(def gap
  (ui/gap 0 20))

(def ui
  (ui/vscrollbar
    (ui/valign 0
      (ui/padding 30
        (ui/shadow {:dy 2 :blur 4 :color 0x33000000}
          (ui/shadow {:dy 5 :blur 20 :color 0x20000000}
            (ui/rect (paint/fill 0xFFFFFFFF)
              (ui/padding 40 60
                (ui/rect (paint/stroke 0xFFEEEEEE 2)
                  (ui/column
                    (ui/dynamic ctx [{:keys [font-ui scale]} ctx]
                      (paragraph
                        {:font (font/make-with-cap-height
                                 (font/typeface font-ui)
                                 (* 20 scale))
                         :line-height 30}
                        "Definition of a Humane Interface"))
                    gap
                    (->> (str/split text #"\n\n")
                      (map paragraph)
                      (interpose gap))))))))))))
