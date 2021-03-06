(ns closh.core
  (:require [clojure.string]
            [closh.parser]
            [closh.compiler]))

(defmacro sh
  "Expands tokens in command mode to executable code."
  [& tokens]
  (closh.compiler/compile-interactive (closh.parser/parse tokens)))

(defmacro sh-value
  "Expands tokens in command mode to executable code."
  [& tokens]
  `(-> ~(closh.compiler/compile-batch (closh.parser/parse tokens))
       (process-value)))

(defmacro sh-str
  "Expands command mode returning process output as string."
  [& tokens]
  `(-> ~(closh.compiler/compile-batch (closh.parser/parse tokens))
       (process-output)
       (clojure.string/trim)))

(defmacro sh-seq
  "Expands command mode collecting process output returning it as a sequence of strings split by whitespace."
  [& tokens]
  `(-> ~(closh.compiler/compile-batch (closh.parser/parse tokens))
       (process-output)
       (clojure.string/trim)
       (clojure.string/split  #"\s+")))

(defmacro sh-lines
  "Expands command mode collecting process output returning it as a sequence of lines."
  [& tokens]
  `(-> ~(closh.compiler/compile-batch (closh.parser/parse tokens))
       (process-output)
       (clojure.string/trim)
       (clojure.string/split  #"\n")))

(defmacro sh-code
  "Expands command mode returning process exit code."
  [& tokens]
  `(-> ~(closh.compiler/compile-interactive (closh.parser/parse tokens))
       (wait-for-process)
       (.-exitCode)))

(defmacro sh-ok
  "Expands command mode returning true if process completed with non-zero exit code."
  [& tokens]
  `(-> ~(closh.compiler/compile-interactive (closh.parser/parse tokens))
       (wait-for-process)
       (.-exitCode)
       (zero?)))

(defmacro defalias [name value]
  `(set! closh.core/*closh-aliases* (assoc closh.core/*closh-aliases* (str (quote ~name)) ~value)))

(defmacro defabbr [name value]
  `(set! closh.core/*closh-abbreviations* (assoc closh.core/*closh-abbreviations* (str (quote ~name)) ~value)))

(defmacro defcmd
  ([name fn]
   `(do (set! closh.core/*closh-commands* (assoc closh.core/*closh-commands* (quote ~name) ~fn))
        nil))
  ([name & body]
   `(do (defn ~name ~@body)
        (defcmd ~name ~name))))
