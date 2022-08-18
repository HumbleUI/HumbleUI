(ns io.github.humbleui.ui.text-field-test
  (:require
    [clojure.string :as str]
    [clojure.test :as test :refer [deftest is are testing]]
    [io.github.humbleui.ui.text-field :as text-field :refer [edit]]))

(defn- parse [s]
  {:text (str/replace s #"[\[\]\|]" "")
   :from (or
           (str/index-of s "|")
           (str/index-of (str/replace s "]" "") "["))
   :to   (or
           (str/index-of s "|")
           (str/index-of (str/replace s "[" "") "]"))})

(defn- serialize [{:keys [text from to]}]
  (assert (<= 0 from (count text)) (str "from: " from ", to: " to ", len: " (count text) ", text: \"" text "\""))
  (assert (<= 0 to (count text)) (str "from: " from ", to: " to ", len: " (count text) ", text: \"" text "\""))
  (if (= from to)
    (str (subs text 0 to) "|" (subs text to))
    (str
      (subs text 0 (min from to))
      (if (<= from to) "[" "]")
      (subs text (min from to) (max from to))
      (if (> to from) "]" "[")
      (subs text (max from to)))))

(defn edit' [s & cmds]
  (let [input  (parse s)
        cmds'  (map #(if (keyword? %) [% nil] %) cmds)
        output (reduce
                 (fn [state [cmd arg]]
                   (edit state cmd arg))
                 input cmds')]
    (serialize output)))

(deftest insert-test
  (are [s res] (= (edit' s [:insert "x"]) res)
    "abc|def" "abcx|def"
    "|"       "x|"
    "|abc"    "x|abc"
    "abc|"    "abcx|")
  
  (are [s res] (= (edit' s [:insert "xyz"]) res)
    "abc|def" "abcxyz|def"
    "|"       "xyz|"
    "|abc"    "xyz|abc"
    "abc|"    "abcxyz|"))

(deftest replace-test
  (are [s res] (= (edit' s :kill [:insert "x"]) res)
    "[ab]cdef" "x|cdef"
    "ab[cd]ef" "abx|ef"
    "abcd[ef]" "abcdx|"
    "[abcdef]" "x|"
        
    "]ab[cdef" "x|cdef"
    "ab]cd[ef" "abx|ef"
    "abcd]ef[" "abcdx|"
    "]abcdef[" "x|")

  (are [s res] (= (edit' s :kill [:insert "xyz"]) res)
    "[ab]cdef" "xyz|cdef"
    "ab[cd]ef" "abxyz|ef"
    "abcd[ef]" "abcdxyz|"
    "[abcdef]" "xyz|"

    "]ab[cdef" "xyz|cdef"
    "ab]cd[ef" "abxyz|ef"
    "abcd]ef[" "abcdxyz|"
    "]abcdef[" "xyz|"))

(deftest move-char-left-test
  (are [s res] (= (edit' s :move-char-left) res)
    "|"    "|"
    "|abc" "|abc"
    "a|bc" "|abc"
    "ab|c" "a|bc"
    "abc|" "ab|c"
    
    "[a]bcdef" "|abcdef"
    "ab[c]def" "ab|cdef" 
    "abcde[f]" "abcde|f"

    "[ab]cdef" "|abcdef"
    "ab[cd]ef" "ab|cdef" 
    "abcd[ef]" "abcd|ef"
    "[abcdef]" "|abcdef"
    
    "]a[bcdef" "|abcdef"
    "abc]d[ef" "abc|def" 
    "abcde]f[" "abcde|f"
    
    "]ab[cdef" "|abcdef"
    "ab]cd[ef" "ab|cdef" 
    "abcd]ef[" "abcd|ef"
    "]abcdef[" "|abcdef"
    
    "🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️|" "🚵🏻‍♀️🚵🏻‍♀️|🚵🏻‍♀️"
    "🚵🏻‍♀️🚵🏻‍♀️|🚵🏻‍♀️" "🚵🏻‍♀️|🚵🏻‍♀️🚵🏻‍♀️"
    "🚵🏻‍♀️|🚵🏻‍♀️🚵🏻‍♀️" "|🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️"
    "|🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️" "|🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️")
  
  (are [s res] (= (edit' s :move-char-left :move-char-left) res)
    "🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️|" "🚵🏻‍♀️|🚵🏻‍♀️🚵🏻‍♀️"
    "🚵🏻‍♀️🚵🏻‍♀️|🚵🏻‍♀️" "|🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️"
    "🚵🏻‍♀️|🚵🏻‍♀️🚵🏻‍♀️" "|🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️"
    "|🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️" "|🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️"))

(deftest move-char-right-test
  (are [s res] (= (edit' s :move-char-right) res)
    "|"    "|"
    "|abc" "a|bc"
    "a|bc" "ab|c"
    "ab|c" "abc|"
    "abc|" "abc|"
    
    "[a]bcdef" "a|bcdef"
    "ab[c]def" "abc|def" 
    "abcde[f]" "abcdef|"

    "[ab]cdef" "ab|cdef"
    "ab[cd]ef" "abcd|ef" 
    "abcd[ef]" "abcdef|"
    "[abcdef]" "abcdef|"
    
    "]a[bcdef" "a|bcdef"
    "abc]d[ef" "abcd|ef" 
    "abcde]f[" "abcdef|"
    
    "]ab[cdef" "ab|cdef"
    "ab]cd[ef" "abcd|ef" 
    "abcd]ef[" "abcdef|"
    "]abcdef[" "abcdef|"
    
    "🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️|" "🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️|"
    "🚵🏻‍♀️🚵🏻‍♀️|🚵🏻‍♀️" "🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️|"
    "🚵🏻‍♀️|🚵🏻‍♀️🚵🏻‍♀️" "🚵🏻‍♀️🚵🏻‍♀️|🚵🏻‍♀️"
    "|🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️" "🚵🏻‍♀️|🚵🏻‍♀️🚵🏻‍♀️")
  
  (are [s res] (= (edit' s :move-char-right :move-char-right) res)
    "🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️|" "🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️|"
    "🚵🏻‍♀️🚵🏻‍♀️|🚵🏻‍♀️" "🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️|"
    "🚵🏻‍♀️|🚵🏻‍♀️🚵🏻‍♀️" "🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️|"
    "|🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️" "🚵🏻‍♀️🚵🏻‍♀️|🚵🏻‍♀️"))

(deftest move-word-left-test
  (are [s res] (= (edit' s :move-word-left) res)
    "word (word, 1word), word1 wo2rd wo-rd  word?!|" "word (word, 1word), word1 wo2rd wo-rd  |word?!"
    "word (word, 1word), word1 wo2rd wo-rd  word?|!" "word (word, 1word), word1 wo2rd wo-rd  |word?!"
    "word (word, 1word), word1 wo2rd wo-rd  word|?!" "word (word, 1word), word1 wo2rd wo-rd  |word?!"
    "word (word, 1word), word1 wo2rd wo-rd  wor|d?!" "word (word, 1word), word1 wo2rd wo-rd  |word?!"
    "word (word, 1word), word1 wo2rd wo-rd  |word?!" "word (word, 1word), word1 wo2rd wo-|rd  word?!"
    "word (word, 1word), word1 wo2rd wo-rd | word?!" "word (word, 1word), word1 wo2rd wo-|rd  word?!"
    "word (word, 1word), word1 wo2rd wo-rd|  word?!" "word (word, 1word), word1 wo2rd wo-|rd  word?!"
    "word (word, 1word), word1 wo2rd wo-r|d  word?!" "word (word, 1word), word1 wo2rd wo-|rd  word?!"
    "word (word, 1word), word1 wo2rd wo-|rd  word?!" "word (word, 1word), word1 wo2rd |wo-rd  word?!"
    "word (word, 1word), word1 wo2rd wo|-rd  word?!" "word (word, 1word), word1 wo2rd |wo-rd  word?!"
    "word (word, 1word), word1 wo2rd w|o-rd  word?!" "word (word, 1word), word1 wo2rd |wo-rd  word?!"
    "word (word, 1word), word1 wo2rd |wo-rd  word?!" "word (word, 1word), word1 |wo2rd wo-rd  word?!"
    "word (word, 1word), word1 wo2rd| wo-rd  word?!" "word (word, 1word), word1 |wo2rd wo-rd  word?!"
    "word (word, 1word), |word1 wo2rd wo-rd  word?!" "word (word, |1word), word1 wo2rd wo-rd  word?!"
    "word (word, 1word),| word1 wo2rd wo-rd  word?!" "word (word, |1word), word1 wo2rd wo-rd  word?!"
    "word (word, 1word)|, word1 wo2rd wo-rd  word?!" "word (word, |1word), word1 wo2rd wo-rd  word?!"
    "word (word, 1word|), word1 wo2rd wo-rd  word?!" "word (word, |1word), word1 wo2rd wo-rd  word?!"
    "word (|word, 1word), word1 wo2rd wo-rd  word?!" "|word (word, 1word), word1 wo2rd wo-rd  word?!"
    "word |(word, 1word), word1 wo2rd wo-rd  word?!" "|word (word, 1word), word1 wo2rd wo-rd  word?!"
    "word| (word, 1word), word1 wo2rd wo-rd  word?!" "|word (word, 1word), word1 wo2rd wo-rd  word?!"
    "wor|d (word, 1word), word1 wo2rd wo-rd  word?!" "|word (word, 1word), word1 wo2rd wo-rd  word?!"
    "wo|rd (word, 1word), word1 wo2rd wo-rd  word?!" "|word (word, 1word), word1 wo2rd wo-rd  word?!"
    "w|ord (word, 1word), word1 wo2rd wo-rd  word?!" "|word (word, 1word), word1 wo2rd wo-rd  word?!"
    "|word (word, 1word), word1 wo2rd wo-rd  word?!" "|word (word, 1word), word1 wo2rd wo-rd  word?!"
    
    "  word  |" "  |word  "
    "  word|  " "  |word  "
    "  wo|rd  " "  |word  "
    "  |word  " "|  word  "
    "|  word  " "|  word  "
    
    "word 😀🚵🏻‍♀️🥸 word|" "word 😀🚵🏻‍♀️🥸 |word"
    "word 😀🚵🏻‍♀️🥸 |word" "|word 😀🚵🏻‍♀️🥸 word")
  
  (are [s res] (= (edit' s :move-word-left :move-word-left) res)
    "word word word word|" "word word |word word"
    "word word word| word" "word |word word word"
    "word word |word word" "|word word word word"
    "word| word word word" "|word word word word"
    "|word word word word" "|word word word word"))

(deftest move-word-right-test
  (are [s res] (= (edit' s :move-word-right) res)
    "word (word, 1word), word1 wo2rd wo-rd  word?!|" "word (word, 1word), word1 wo2rd wo-rd  word?!|"
    "word (word, 1word), word1 wo2rd wo-rd  word?|!" "word (word, 1word), word1 wo2rd wo-rd  word?!|"
    "word (word, 1word), word1 wo2rd wo-rd  word|?!" "word (word, 1word), word1 wo2rd wo-rd  word?!|"
    "word (word, 1word), word1 wo2rd wo-rd  wor|d?!" "word (word, 1word), word1 wo2rd wo-rd  word|?!"
    "word (word, 1word), word1 wo2rd wo-rd  |word?!" "word (word, 1word), word1 wo2rd wo-rd  word|?!"
    "word (word, 1word), word1 wo2rd wo-rd | word?!" "word (word, 1word), word1 wo2rd wo-rd  word|?!"
    "word (word, 1word), word1 wo2rd wo-rd|  word?!" "word (word, 1word), word1 wo2rd wo-rd  word|?!"
    "word (word, 1word), word1 wo2rd wo-r|d  word?!" "word (word, 1word), word1 wo2rd wo-rd|  word?!"
    "word (word, 1word), word1 wo2rd wo-|rd  word?!" "word (word, 1word), word1 wo2rd wo-rd|  word?!"
    "word (word, 1word), word1 wo2rd wo|-rd  word?!" "word (word, 1word), word1 wo2rd wo-rd|  word?!"
    "word (word, 1word), word1 wo2rd w|o-rd  word?!" "word (word, 1word), word1 wo2rd wo|-rd  word?!"
    "word (word, 1word), word1 wo2rd |wo-rd  word?!" "word (word, 1word), word1 wo2rd wo|-rd  word?!"
    "word (word, 1word), word1 wo2rd| wo-rd  word?!" "word (word, 1word), word1 wo2rd wo|-rd  word?!"
    "word (word, 1word), |word1 wo2rd wo-rd  word?!" "word (word, 1word), word1| wo2rd wo-rd  word?!"
    "word (word, 1word),| word1 wo2rd wo-rd  word?!" "word (word, 1word), word1| wo2rd wo-rd  word?!"
    "word (word, 1word)|, word1 wo2rd wo-rd  word?!" "word (word, 1word), word1| wo2rd wo-rd  word?!"
    "word (word, 1word|), word1 wo2rd wo-rd  word?!" "word (word, 1word), word1| wo2rd wo-rd  word?!"
    "word (|word, 1word), word1 wo2rd wo-rd  word?!" "word (word|, 1word), word1 wo2rd wo-rd  word?!"
    "word |(word, 1word), word1 wo2rd wo-rd  word?!" "word (word|, 1word), word1 wo2rd wo-rd  word?!"
    "word| (word, 1word), word1 wo2rd wo-rd  word?!" "word (word|, 1word), word1 wo2rd wo-rd  word?!"
    "wor|d (word, 1word), word1 wo2rd wo-rd  word?!" "word| (word, 1word), word1 wo2rd wo-rd  word?!"
    "wo|rd (word, 1word), word1 wo2rd wo-rd  word?!" "word| (word, 1word), word1 wo2rd wo-rd  word?!"
    "w|ord (word, 1word), word1 wo2rd wo-rd  word?!" "word| (word, 1word), word1 wo2rd wo-rd  word?!"
    "|word (word, 1word), word1 wo2rd wo-rd  word?!" "word| (word, 1word), word1 wo2rd wo-rd  word?!"

    "  word  |" "  word  |"
    "  word|  " "  word  |"
    "  wo|rd  " "  word|  "
    "  |word  " "  word|  "
    "|  word  " "  word|  "

    "|word 😀🚵🏻‍♀️🥸 word" "word| 😀🚵🏻‍♀️🥸 word"
    "word| 😀🚵🏻‍♀️🥸 word" "word 😀🚵🏻‍♀️🥸 word|")
  
  (are [s res] (= (edit' s :move-word-right :move-word-right) res)
    "word word word word|" "word word word word|"
    "word word word| word" "word word word word|"
    "word word |word word" "word word word word|"
    "word| word word word" "word word word| word"
    "|word word word word" "word word| word word"))

(deftest move-doc-start-test
  (are [s res] (= (edit' s :move-doc-start) res)
    "|"    "|"
    "|abc" "|abc"
    "a|bc" "|abc"
    "ab|c" "|abc"
    "abc|" "|abc"
    
    "[a]bcdef" "|abcdef"
    "ab[c]def" "|abcdef" 
    "abcde[f]" "|abcdef"

    "[ab]cdef" "|abcdef"
    "ab[cd]ef" "|abcdef" 
    "abcd[ef]" "|abcdef"
    "[abcdef]" "|abcdef"

    "]a[bcdef" "|abcdef"
    "abc]d[ef" "|abcdef" 
    "abcde]f[" "|abcdef"
    
    "]ab[cdef" "|abcdef"
    "ab]cd[ef" "|abcdef" 
    "abcd]ef[" "|abcdef"
    "]abcdef[" "|abcdef"))

(deftest move-doc-end-test
  (are [s res] (= (edit' s :move-doc-end) res)
    "|"    "|"
    "|abc" "abc|"
    "a|bc" "abc|"
    "ab|c" "abc|"
    "abc|" "abc|"
    
    "[a]bcdef" "abcdef|"
    "ab[c]def" "abcdef|" 
    "abcde[f]" "abcdef|"

    "[ab]cdef" "abcdef|"
    "ab[cd]ef" "abcdef|" 
    "abcd[ef]" "abcdef|"
    "[abcdef]" "abcdef|"

    "]a[bcdef" "abcdef|"
    "abc]d[ef" "abcdef|" 
    "abcde]f[" "abcdef|"
    
    "]ab[cdef" "abcdef|"
    "ab]cd[ef" "abcdef|" 
    "abcd]ef[" "abcdef|"
    "]abcdef[" "abcdef|"))

(deftest expand-char-left-test
  (are [s res] (= (edit' s :expand-char-left) res)
    "|"    "|"
    "|abc" "|abc"
    "a|bc" "]a[bc"
    "ab|c" "a]b[c"
    "abc|" "ab]c["
    
    "[a]bcdef" "|abcdef"
    "ab[c]def" "ab|cdef" 
    "abcde[f]" "abcde|f"

    "[ab]cdef" "[a]bcdef"
    "ab[cd]ef" "ab[c]def" 
    "abcd[ef]" "abcd[e]f"
    "[abcdef]" "[abcde]f"
    
    "]a[bcdef" "]a[bcdef"
    "abc]d[ef" "ab]cd[ef" 
    "abcde]f[" "abcd]ef["
    
    "]ab[cdef" "]ab[cdef"
    "ab]cd[ef" "a]bcd[ef" 
    "abcd]ef[" "abc]def["
    "]abcdef[" "]abcdef["
    
    "🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️|" "🚵🏻‍♀️🚵🏻‍♀️]🚵🏻‍♀️["
    "🚵🏻‍♀️🚵🏻‍♀️|🚵🏻‍♀️" "🚵🏻‍♀️]🚵🏻‍♀️[🚵🏻‍♀️"
    "🚵🏻‍♀️|🚵🏻‍♀️🚵🏻‍♀️" "]🚵🏻‍♀️[🚵🏻‍♀️🚵🏻‍♀️")
  
  (are [s res] (= (edit' s :expand-char-left :expand-char-left) res)
    "🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️|" "🚵🏻‍♀️]🚵🏻‍♀️🚵🏻‍♀️["
    "🚵🏻‍♀️🚵🏻‍♀️|🚵🏻‍♀️" "]🚵🏻‍♀️🚵🏻‍♀️[🚵🏻‍♀️"
    "🚵🏻‍♀️|🚵🏻‍♀️🚵🏻‍♀️" "]🚵🏻‍♀️[🚵🏻‍♀️🚵🏻‍♀️"
    "|🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️" "|🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️"))

(deftest expand-char-right-test
  (are [s res] (= (edit' s :expand-char-right) res)
    "|"    "|"
    "|abc" "[a]bc"
    "a|bc" "a[b]c"
    "ab|c" "ab[c]"
    "abc|" "abc|"
    
    "[a]bcdef" "[ab]cdef"
    "ab[c]def" "ab[cd]ef" 
    "abcde[f]" "abcde[f]"

    "[ab]cdef" "[abc]def"
    "ab[cd]ef" "ab[cde]f" 
    "abcd[ef]" "abcd[ef]"
    "[abcdef]" "[abcdef]"
    
    "]a[bcdef" "a|bcdef"
    "abc]d[ef" "abcd|ef" 
    "abcde]f[" "abcdef|"
    
    "]ab[cdef" "a]b[cdef"
    "ab]cd[ef" "abc]d[ef" 
    "abcd]ef[" "abcde]f["
    "]abcdef[" "a]bcdef["
    
    "🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️|" "🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️|"
    "🚵🏻‍♀️🚵🏻‍♀️|🚵🏻‍♀️" "🚵🏻‍♀️🚵🏻‍♀️[🚵🏻‍♀️]"
    "🚵🏻‍♀️|🚵🏻‍♀️🚵🏻‍♀️" "🚵🏻‍♀️[🚵🏻‍♀️]🚵🏻‍♀️"
    "|🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️" "[🚵🏻‍♀️]🚵🏻‍♀️🚵🏻‍♀️")
  
  (are [s res] (= (edit' s :expand-char-right :expand-char-right) res)
    "🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️|" "🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️|"
    "🚵🏻‍♀️🚵🏻‍♀️|🚵🏻‍♀️" "🚵🏻‍♀️🚵🏻‍♀️[🚵🏻‍♀️]"
    "🚵🏻‍♀️|🚵🏻‍♀️🚵🏻‍♀️" "🚵🏻‍♀️[🚵🏻‍♀️🚵🏻‍♀️]"
    "|🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️" "[🚵🏻‍♀️🚵🏻‍♀️]🚵🏻‍♀️"))

(deftest expand-word-left-test
  (are [s res] (= (edit' s :expand-word-left) res)
    "word word word|" "word word ]word["
    "word word wor|d" "word word ]wor[d"
    "word word wo|rd" "word word ]wo[rd"
    "word word w|ord" "word word ]w[ord"
    "word word |word" "word ]word [word"
    "wo|rd word word" "]wo[rd word word"
    "|word word word" "|word word word"
    
    "word word [word]" "word word |word"
    "word wo[rd word]" "word wo[rd ]word"
    "word word ]word[" "word ]word word["
    "word wo]rd word[" "word ]word word["))

(deftest expand-word-right-test
  (are [s res] (= (edit' s :expand-word-right) res)
    "|word word word" "[word] word word"
    "w|ord word word" "w[ord] word word"
    "wo|rd word word" "wo[rd] word word"
    "wor|d word word" "wor[d] word word"
    "word| word word" "word[ word] word"
    "word word wo|rd" "word word wo[rd]"
    "word word word|" "word word word|"
    
    "[word] word word" "[word word] word"
    "[word wo]rd word" "[word word] word"
    "]word[ word word" "word| word word"
    "]word wo[rd word" "word] wo[rd word"))

(deftest expand-doc-start-test
  (are [s res] (= (edit' s :expand-doc-start) res)
    "|"    "|"
    "|abc" "|abc"
    "a|bc" "]a[bc"
    "ab|c" "]ab[c"
    "abc|" "]abc["
    
    "[a]bcdef" "|abcdef"
    "ab[c]def" "]abc[def" 
    "abcde[f]" "]abcdef["

    "[ab]cdef" "|abcdef"
    "ab[cd]ef" "]abcd[ef" 
    "abcd[ef]" "]abcdef["
    "[abcdef]" "|abcdef"
    
    "]a[bcdef" "]a[bcdef"
    "abc]d[ef" "]abcd[ef" 
    "abcde]f[" "]abcdef["
    
    "]ab[cdef" "]ab[cdef"
    "ab]cd[ef" "]abcd[ef" 
    "abcd]ef[" "]abcdef["
    "]abcdef[" "]abcdef["))

(deftest expand-doc-end-test
  (are [s res] (= (edit' s :expand-doc-end) res)
    "|"    "|"
    "|abc" "[abc]"
    "a|bc" "a[bc]"
    "ab|c" "ab[c]"
    "abc|" "abc|"
    
    "[a]bcdef" "[abcdef]"
    "ab[c]def" "ab[cdef]" 
    "abcde[f]" "abcde[f]"

    "[ab]cdef" "[abcdef]"
    "ab[cd]ef" "ab[cdef]" 
    "abcd[ef]" "abcd[ef]"
    "[abcdef]" "[abcdef]"
    
    "]a[bcdef" "[abcdef]"
    "abc]d[ef" "abc[def]" 
    "abcde]f[" "abcdef|"
    
    "]ab[cdef" "[abcdef]"
    "ab]cd[ef" "ab[cdef]" 
    "abcd]ef[" "abcdef|"
    "]abcdef[" "abcdef|"))

(deftest delete-char-left-test
  (are [s res] (= (edit' s :delete-char-left) res)
    "|"    "|"
    "|abc" "|abc"
    "a|bc" "|bc"
    "ab|c" "a|c"
    "abc|" "ab|"
    
    "|🚵🏻‍♀️🤵🏽👨‍🏭🇦🇱" "|🚵🏻‍♀️🤵🏽👨‍🏭🇦🇱"
    "🚵🏻‍♀️|🤵🏽👨‍🏭🇦🇱" "|🤵🏽👨‍🏭🇦🇱"
    "🚵🏻‍♀️🤵🏽|👨‍🏭🇦🇱" "🚵🏻‍♀️|👨‍🏭🇦🇱"
    "🚵🏻‍♀️🤵🏽👨‍🏭|🇦🇱" "🚵🏻‍♀️🤵🏽|🇦🇱"
    "🚵🏻‍♀️🤵🏽👨‍🏭🇦🇱|" "🚵🏻‍♀️🤵🏽👨‍🏭|"))

(deftest delete-char-right-test
  (are [s res] (= (edit' s :delete-char-right) res)
    "|"    "|"
    "|abc" "|bc"
    "a|bc" "a|c"
    "ab|c" "ab|"
    "abc|" "abc|"
    
    "|🚵🏻‍♀️🤵🏽👨‍🏭🇦🇱" "|🤵🏽👨‍🏭🇦🇱"
    "🚵🏻‍♀️|🤵🏽👨‍🏭🇦🇱" "🚵🏻‍♀️|👨‍🏭🇦🇱"
    "🚵🏻‍♀️🤵🏽|👨‍🏭🇦🇱" "🚵🏻‍♀️🤵🏽|🇦🇱"
    "🚵🏻‍♀️🤵🏽👨‍🏭|🇦🇱" "🚵🏻‍♀️🤵🏽👨‍🏭|"
    "🚵🏻‍♀️🤵🏽👨‍🏭🇦🇱|" "🚵🏻‍♀️🤵🏽👨‍🏭🇦🇱|"))

(deftest delete-word-left-test
  (are [s res] (= (edit' s :delete-word-left) res)
    "word word|" "word |"
    "word wor|d" "word |d"
    "word wo|rd" "word |rd"
    "word w|ord" "word |ord"
    "word |word" "|word"
    "word| word" "| word"
    "wor|d word" "|d word"
    "wo|rd word" "|rd word"
    "w|ord word" "|ord word"
    "|word word" "|word word"))

(deftest delete-word-right-test
  (are [s res] (= (edit' s :delete-word-right) res)
    "word word|" "word word|"
    "word wor|d" "word wor|"
    "word wo|rd" "word wo|"
    "word w|ord" "word w|"
    "word |word" "word |"
    "word| word" "word|"
    "wor|d word" "wor| word"
    "wo|rd word" "wo| word"
    "w|ord word" "w| word"
    "|word word" "| word"))

(deftest delete-doc-start-test
  (are [s res] (= (edit' s :delete-doc-start) res)
    "|"    "|"
    "|abc" "|abc"
    "a|bc" "|bc"
    "ab|c" "|c"
    "abc|" "|"))

(deftest delete-doc-end-test
  (are [s res] (= (edit' s :delete-doc-end) res)
    "|"    "|"
    "|abc" "|"
    "a|bc" "a|"
    "ab|c" "ab|"
    "abc|" "abc|"))

(deftest kill-test
  (are [s res] (= (edit' s :kill) res)
    "[a]bcdef" "|bcdef"
    "ab[c]def" "ab|def" 
    "abcde[f]" "abcde|"

    "[ab]cdef" "|cdef"
    "ab[cd]ef" "ab|ef" 
    "abcd[ef]" "abcd|"
    "[abcdef]" "|"
    
    "]a[bcdef" "|bcdef"
    "abc]d[ef" "abc|ef" 
    "abcde]f[" "abcde|"
    
    "]ab[cdef" "|cdef"
    "ab]cd[ef" "ab|ef" 
    "abcd]ef[" "abcd|"
    "]abcdef[" "|"))

(deftest transpose-test
  (are [s res] (= (edit' s :transpose) res)
    "|"    "|"
    "|12345" "|12345"
    "1|2345" "21|345"
    "12|345" "132|45"
    "123|45" "1243|5"
    "1234|5" "12354|"
    "12345|" "12354|"
    
    "|🚵🏻‍♀️🤵🏽👨‍🏭🇦🇱" "|🚵🏻‍♀️🤵🏽👨‍🏭🇦🇱"
    "🚵🏻‍♀️|🤵🏽👨‍🏭🇦🇱" "🤵🏽🚵🏻‍♀️|👨‍🏭🇦🇱"
    "🚵🏻‍♀️🤵🏽|👨‍🏭🇦🇱" "🚵🏻‍♀️👨‍🏭🤵🏽|🇦🇱"
    "🚵🏻‍♀️🤵🏽👨‍🏭|🇦🇱" "🚵🏻‍♀️🤵🏽🇦🇱👨‍🏭|"
    "🚵🏻‍♀️🤵🏽👨‍🏭🇦🇱|" "🚵🏻‍♀️🤵🏽🇦🇱👨‍🏭|"))

(deftest move-to-position-test
  (are [s pos res] (= (edit' s [:move-to-position pos]) res)
    "|"    0 "|"
    "|abc" 0 "|abc"
    "|abc" 1 "a|bc"
    "|abc" 2 "ab|c"
    "|abc" 3 "abc|"
    
    "[a]bc" 0 "|abc"
    "[a]bc" 1 "a|bc"
    "[a]bc" 2 "ab|c"
    "[a]bc" 3 "abc|"
    
    "]a[bc" 0 "|abc"
    "]a[bc" 1 "a|bc"
    "]a[bc" 2 "ab|c"
    "]a[bc" 3 "abc|"))

(deftest select-word-test
  (are [s pos res] (= (edit' s [:select-word pos]) res)
    "|"    0 "|"
    "|abc" 0 "[abc]"
    "|abc" 1 "[abc]"
    "|abc" 2 "[abc]"
    "|abc" 3 "[abc]"
    
    "|abc d xy" 0 "[abc] d xy"
    "|abc d xy" 1 "[abc] d xy"
    "|abc d xy" 2 "[abc] d xy"
    "|abc d xy" 3 "abc[ ]d xy"
    "|abc d xy" 4 "abc [d] xy"
    "|abc d xy" 5 "abc d[ ]xy"
    "|abc d xy" 6 "abc d [xy]"
    "|abc d xy" 7 "abc d [xy]"
    "|abc d xy" 8 "abc d [xy]"
    
    "| abc,  def" 0 "[ ]abc,  def"
    "| abc,  def" 1 " [abc],  def"
    "| abc,  def" 2 " [abc],  def"
    "| abc,  def" 3 " [abc],  def"
    "| abc,  def" 4 " abc[,]  def"
    "| abc,  def" 5 " abc,[  ]def"
    "| abc,  def" 6 " abc,[  ]def"
    "| abc,  def" 7 " abc,  [def]"
    "| abc,  def" 8 " abc,  [def]"
    "| abc,  def" 9 " abc,  [def]"))

(deftest select-all-test
  (are [s res] (= (edit' s :select-all) res)
    "|"    "|"
    "|abc" "[abc]"
    "a|bc" "[abc]"
    "ab|c" "[abc]"
    "abc|" "[abc]"
    
    "[a]bcdef" "[abcdef]"
    "ab[c]def" "[abcdef]" 
    "abcde[f]" "[abcdef]"

    "[ab]cdef" "[abcdef]"
    "ab[cd]ef" "[abcdef]" 
    "abcd[ef]" "[abcdef]"
    "[abcdef]" "[abcdef]"
    
    "]a[bcdef" "[abcdef]"
    "abc]d[ef" "[abcdef]" 
    "abcde]f[" "[abcdef]"
    
    "]ab[cdef" "[abcdef]"
    "ab]cd[ef" "[abcdef]" 
    "abcd]ef[" "[abcdef]"
    "]abcdef[" "[abcdef]"))

(deftest undo-test
  (testing "edge cases"
    (are [s cmds res] (= (apply edit' s cmds) res)
      "|" [:undo] "|"
      "|" [:redo] "|"
      "|" [[:insert "x"] :undo :undo] "|"
      "|" [[:insert "x"] :undo :redo :redo] "x|"))

  (testing "insert"
    (are [s cmds res] (= (apply edit' s cmds) res)
      "|" [[:insert "x"] :undo] "|"
      "|" [[:insert "x"] [:insert "y"] :undo] "|"
      "|" [[:insert "x"] [:insert "y"] [:insert "z"] :undo] "|"
      "|" [[:insert "x"] :move-char-left [:insert "y"] :undo] "|x"
      "|" [[:insert "x"] :move-char-left [:insert "y"] :undo :undo] "|"
      "x|" [[:insert "y"] :move-char-left :move-char-right [:insert "z"] :undo] "x|"
      ))
  
  (testing "redo"
    (are [s cmds res] (= (apply edit' s cmds) res)
      "|" [[:insert "x"] :move-char-left [:insert "y"] :move-char-left [:insert "z"] :undo] "|yx"
      "|" [[:insert "x"] :move-char-left [:insert "y"] :move-char-left [:insert "z"] :undo :redo] "z|yx"
      "|" [[:insert "x"] :move-char-left [:insert "y"] :move-char-left [:insert "z"] :undo :undo] "|x"
      "|" [[:insert "x"] :move-char-left [:insert "y"] :move-char-left [:insert "z"] :undo :undo :redo] "|yx"
      "|" [[:insert "x"] :move-char-left [:insert "y"] :move-char-left [:insert "z"] :undo :undo :redo :redo] "z|yx"
      "|" [[:insert "x"] :move-char-left [:insert "y"] :move-char-left [:insert "z"] :undo :undo :undo] "|"
      "|" [[:insert "x"] :move-char-left [:insert "y"] :move-char-left [:insert "z"] :undo :undo :undo :redo] "|x"
      "|" [[:insert "x"] :move-char-left [:insert "y"] :move-char-left [:insert "z"] :undo :undo :undo :redo :redo] "|yx"
      "|" [[:insert "x"] :move-char-left [:insert "y"] :move-char-left [:insert "z"] :undo :undo :undo :redo :redo :redo] "z|yx"
      ))
  
  (testing "redo reset"
    (are [s cmds res] (= (apply edit' s cmds) res)
      "|" [[:insert "x"] :move-char-left [:insert "y"] :move-char-left [:insert "z"] :undo :undo :undo [:insert "a"] :redo :redo :redo] "a|"
      "|" [[:insert "x"] :move-char-left [:insert "y"] :move-char-left [:insert "z"] :undo :undo :delete-char-right :redo :redo] "|"
      "|" [[:insert "x"] :move-char-left [:insert "y"] :move-char-left [:insert "z"] :undo :undo :move-char-right :delete-char-left :redo :redo] "|"
      "|" [[:insert "x"] :move-char-left [:insert "y"] :move-char-left [:insert "z"] :undo :undo :move-char-right :redo :redo] "z|yx"
      ))
      
  (testing "delete-char-left"
    (are [s cmds res] (= (apply edit' s cmds) res)
      "xyz|" [:delete-char-left :undo] "xyz|"
      "xyz|" [:delete-char-left :delete-char-left :undo] "xyz|"
      "xyz|" [:delete-char-left :delete-char-left :delete-char-left :undo] "xyz|"
      "xyz|" [:delete-char-left :move-char-left :delete-char-left :undo] "x|y"
      "xyz|" [:delete-char-left :move-char-left :delete-char-left :undo :undo] "xyz|"
      ))

  (testing "delete-char-right"
    (are [s cmds res] (= (apply edit' s cmds) res)
      "|xyz" [:delete-char-right :undo] "|xyz"
      "|xyz" [:delete-char-right :delete-char-right :undo] "|xyz"
      "|xyz" [:delete-char-right :delete-char-right :delete-char-right :undo] "|xyz"
      "|xyz" [:delete-char-right :move-char-right :delete-char-right :undo] "y|z"
      "|xyz" [:delete-char-right :move-char-right :delete-char-right :undo :undo] "|xyz"
      ))
  
  (testing "all together"
    (are [s cmds res] (= (apply edit' s cmds) res)
      "|" [[:insert "x"] [:insert "y"] [:insert "z"] :delete-char-left :delete-char-left :delete-char-left :undo] "xyz|"
      "|" [[:insert "x"] [:insert "y"] [:insert "z"] :delete-char-left :delete-char-left :delete-char-left :undo :undo] "|"
      "|xyz" [[:insert "a"] [:insert "b"] [:insert "c"] :delete-char-right :delete-char-right :delete-char-right :undo] "abc|xyz"
      "|xyz" [[:insert "a"] [:insert "b"] [:insert "c"] :delete-char-right :delete-char-right :delete-char-right :undo :undo] "|xyz"
      ))
  )

(comment
  (test/test-ns *ns*))
