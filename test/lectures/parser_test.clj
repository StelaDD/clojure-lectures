(ns lectures.parser-test
  (:use clojure.test
        lectures.parser)
  (:require [clojure.string :as str]))

(defmacro expect
  [parser & body]
  `(are [input# output#] (= (parse ~parser input#) output#)
        ~@body))

(defn- with-margin
  "Removes all leading whitespace followed by a pipe. This is useful to create
   multiline-strings with the Clojure literals that are don't start in the beginning
   of their source file."
  [string]
  (->> string
       str/split-lines
       (map #(str/replace % #"^\s*\|" ""))
       (str/join "\n")))

(deftest parser-test
  (testing "Inline code"
    (expect inline-code
            "`foo`"   [:code "foo"]
            "`x * y`" [:code "x * y"]
            "`x\ny`"  nil))
  (testing "Bold text"
    (expect bold
            "*foo*"      [:bold "foo"]
            "*foo\nbar*" nil))
  (testing "Line of text"
    (expect text-line
            "Text"                  ["Text"]
            "*Bold* text"           [[:bold "Bold"] " text"]
            "Unmatched *asterix"    ["Unmatched *asterix"]
            "Text with `code`"      ["Text with " [:code "code"]]
            "Unmatched `backtick"   ["Unmatched `backtick"]
            "Ending with newline\n" ["Ending with newline"]))
  (testing "Bullet list"
    (expect bullet-list
            (with-margin "|* First
                          |* Second")
            [:bullet-list
             [:incremental ["First"]]
             [:incremental ["Second"]]]

            (with-margin "|+ First
                          |+ Second")
            [:bullet-list
             [:static ["First"]]
             [:static ["Second"]]]

            (with-margin "|+ First
                          |* Second")
            [:bullet-list
             [:static ["First"]]
             [:incremental ["Second"]]]))
  (testing "Slide"
    (expect slide
            (with-margin "|= Title
                          |
                          |First line
                          |Second line")
            [:slide ["Title"]
             ["First line"]
             ["Second line"]]

            (with-margin "|= Slide with a list
                          |
                          |This is a list:
                          |* First item
                          |* Second item")
            [:slide ["Slide with a list"]
             ["This is a list:"]
             [:bullet-list
              [:incremental ["First item"]]
              [:incremental ["Second item"]]]]))
  (testing "Presentation"
    (expect presentation
            (with-margin "|= First slide
                          |
                          |First line
                          |
                          |= Second slide
                          |
                          |Second line")
            [:presentation
             [:slide ["First slide"]
              ["First line"]]
             [:slide ["Second slide"]
              ["Second line"]]])))