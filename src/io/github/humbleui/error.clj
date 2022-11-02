(ns io.github.humbleui.error
  (:require
    [clojure.java.io :as io]
    [clojure.stacktrace :as stacktrace]
    [clojure.string :as str]))

(defn- with-message [^Throwable t msg]
  (doto (proxy [Throwable] [msg nil false true])
    (.setStackTrace (.getStackTrace t))))

;; CompilerException has location info, but its cause RuntimeException has the message ¯\_(ツ)_/¯
(defn- root-cause [^Throwable t]
  (loop [t t
         data nil]
    (if (and (nil? data) (instance? clojure.lang.Compiler$CompilerException t))
      (recur t (ex-data t))
      (if-some [cause (some-> t .getCause)]
        (recur cause data)
        (if-some [{:clojure.error/keys [line column source]} data]
          (with-message t (format "%s: %s (%s:%d:%d)" (.getName (class t)) (.getMessage t) source line column))
          t)))))

(defn print-root-trace [^Throwable t]
  (stacktrace/print-stack-trace (root-cause t)))

(defn- duplicate? [^StackTraceElement prev-el ^StackTraceElement el]
  (and
    (= (.getClassName prev-el) (.getClassName el))
    (= (.getFileName prev-el) (.getFileName el))
    (= "invokeStatic" (.getMethodName prev-el))
    (#{"invoke" "doInvoke"} (.getMethodName el))))

(defn- clear-duplicates [els]
  (for [[prev-el el] (map vector (cons nil els) els)
        :when (or (nil? prev-el) (not (duplicate? prev-el el)))]
    el))

(defn- exists? [path]
  (when (.exists (io/file path))
    path))

(defn- path [^StackTraceElement el]
  (let [path (-> (.getClassName el)
               (str/split #"\$")
               (first)
               (str/split #"\."))]
    (some identity
      (for [base [(str/join "/" path)
                  (str/join "/" (butlast path))]
            dir  ["src" "dev"]
            ext  ["clj" "cljc"]]
        (exists? (str dir "/" base "." ext))))))

(defn- trace-element [^StackTraceElement el]
  (let [file     (.getFileName el)
        clojure? (and file
                   (or (.endsWith file ".clj")
                     (.endsWith file ".cljc")
                     (= file "NO_SOURCE_FILE")))]
    {:method
     (if clojure?
       (if (#{"invoke" "doInvoke" "invokeStatic"} (.getMethodName el))
         (clojure.lang.Compiler/demunge (.getClassName el))
         (str (clojure.lang.Compiler/demunge (.getClassName el)) "/" (clojure.lang.Compiler/demunge (.getMethodName el))))
       (str (.getClassName el) "::" (.getMethodName el)))
     
     :file
     (or 
       (when clojure?
         (path el))
       (.getFileName el))
     
     :line
     (.getLineNumber el)}))

(defn as-table [table]
  (let [[method file] (for [col [:method :file]]
                        (->> table
                          (map #(get % col))
                          (map str)
                          (map count)
                          (reduce max 0)))
        format-str (str "\t%-" method "s\t%-" file "s\t:%d")]
    (->> table
      (map #(format format-str (:method %) (:file %) (:line %)))
      (str/join "\n"))))

(defn- trace-str [^Throwable t]
  (str
    "\n"
    (->> (.getStackTrace t)
      (take-while #(not= "clojure.lang.Compiler" (.getClassName ^StackTraceElement %)))
      (remove #(#{"clojure.lang.RestFn" "clojure.lang.AFn"} (.getClassName ^StackTraceElement %)))
      (clear-duplicates)
      (map trace-element)
      (reverse)
      (as-table))
    "\n"
    (let [cls (.getSimpleName (class t))]
      (when-not (re-matches #"Throwable\$[0-9a-f]+" cls)
        (str cls ": ")))
    (.getMessage t)
    (when (instance? clojure.lang.Compiler$CompilerException t)
      (let [t      ^clojure.lang.Compiler$CompilerException t
            line   (or (.-line t) (:clojure.error/line (ex-data t)))
            column (:clojure.error/column (ex-data t))
            source (or (.-source t) (:clojure.error/source (ex-data t)))]
        (str " (" source ":" line ":" column ")")))
    (when-some [data (ex-data t)]
      (str " " (pr-str data)))))

(defn log-error [^Throwable throwable]
  (binding [*out* *err*]
    (println (trace-str (root-cause throwable)))))