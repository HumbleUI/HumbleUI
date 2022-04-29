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

(defn- edit' [s cmd arg]
  (serialize (edit (parse s) cmd arg)))

(deftest insert-test
  (are [s res] (= (edit' s :insert "x") res)
    "abc|def" "abcx|def"
    "|"       "x|"
    "|abc"    "x|abc"
    "abc|"    "abcx|")
  
  (are [s res] (= (edit' s :insert "xyz") res)
    "abc|def" "abcxyz|def"
    "|"       "xyz|"
    "|abc"    "xyz|abc"
    "abc|"    "abcxyz|"))

(deftest replace-test
  (are [s res] (= (edit' s :replace "x") res)
    "[ab]cdef" "x|cdef"
    "ab[cd]ef" "abx|ef"
    "abcd[ef]" "abcdx|"
    "[abcdef]" "x|"
        
    "]ab[cdef" "x|cdef"
    "ab]cd[ef" "abx|ef"
    "abcd]ef[" "abcdx|"
    "]abcdef[" "x|")

  (are [s res] (= (edit' s :replace "xyz") res)
    "[ab]cdef" "xyz|cdef"
    "ab[cd]ef" "abxyz|ef"
    "abcd[ef]" "abcdxyz|"
    "[abcdef]" "xyz|"

    "]ab[cdef" "xyz|cdef"
    "ab]cd[ef" "abxyz|ef"
    "abcd]ef[" "abcdxyz|"
    "]abcdef[" "xyz|"))

(deftest move-left-test
  (are [s res] (= (edit' s :move-left nil) res)
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
    "]abcdef[" "|abcdef"))

(deftest expand-left-test
  (are [s res] (= (edit' s :expand-left nil) res)
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
    "]abcdef[" "]abcdef["))

(deftest move-right-test
  (are [s res] (= (edit' s :move-right nil) res)
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
    "]abcdef[" "abcdef|"))

(deftest expand-right-test
  (are [s res] (= (edit' s :expand-right nil) res)
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
    "]abcdef[" "a]bcdef["))

(deftest move-beginning-test
  (are [s res] (= (edit' s :move-beginning nil) res)
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

(deftest expand-beginning-test
  (are [s res] (= (edit' s :expand-beginning nil) res)
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

(deftest move-end-test
  (are [s res] (= (edit' s :move-end nil) res)
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

(deftest expand-end-test
  (are [s res] (= (edit' s :expand-end nil) res)
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

(deftest delete-left-test
  (are [s res] (= (edit' s :delete-left nil) res)
    "|"    "|"
    "|abc" "|abc"
    "a|bc" "|bc"
    "ab|c" "a|c"
    "abc|" "ab|"))

(deftest delete-right-test
  (are [s res] (= (edit' s :delete-right nil) res)
    "|"    "|"
    "|abc" "|bc"
    "a|bc" "a|c"
    "ab|c" "ab|"
    "abc|" "abc|"))

(deftest kill-test
  (are [s res] (= (edit' s :kill nil) res)
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

(deftest select-all-test
  (are [s res] (= (edit' s :select-all nil) res)
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

(comment
  (test/test-ns *ns*))
