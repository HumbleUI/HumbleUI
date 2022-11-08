(ns io.github.humbleui.clipboard
  (:refer-clojure :exclude [get set])
  (:import
    [io.github.humbleui.jwm Clipboard ClipboardFormat ClipboardEntry]))

(defn- format->clj [^ClipboardFormat format]
  (keyword (.-_formatId format)))

(defn- format->java ^ClipboardFormat [format]
  (let [id (if-some [ns (namespace format)]
             (str ns "/" (name format))
             (name format))]
    (or
      (.get Clipboard/_formats id)
      (throw (ex-info (str "Unregistered ClipboardFormat " format) {:format format})))))

(defn- entry->clj [^ClipboardEntry entry]
  (when entry
    (let [format (format->clj (.getFormat entry))]
      (cond-> {:format format
               :data   (.getData entry)}
        (#{:text/plain :text/rtf :text/html :text/url} format)
        (assoc :text (.getString entry))))))

(defn- entry->java ^ClipboardEntry [{:keys [format data text] :as entry}]
  (when entry
    (if text
      (ClipboardEntry/makeString (format->java format) ^String text)
      (ClipboardEntry/make (format->java format) ^bytes data))))

(defn get [format & formats]
  (->>
    (cons format formats)
    (map format->java)
    (into-array ClipboardFormat)
    (Clipboard/get)
    (entry->clj)))

(defn get-text []
  (get :text/plain))

(defn set [entry & entries]
  (->>
    (cons entry entries)
    (map entry->java)
    (into-array ClipboardEntry)
    (Clipboard/set)))

(defn set-text [text]
  (set {:format :text/plain :text text}))