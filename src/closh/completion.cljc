(ns closh.completion
  (:require [clojure.string]
            [lumo.repl]
            [closh.builtin :refer [getenv]]))

(def ^:no-doc child-process (js/require "child_process"))

(defn- stream-output
  "Helper function to get output from a node stream as a string."
  [stream cb]
  (let [out #js[]]
    (doto stream
      (.on "data" #(.push out %))
      (.on "end" #(cb nil (.join out "")))
      (.on "error" #(cb % "")))))

(defn get-completions-spawn
  "Get completions by spawning a command."
  [cmd args]
  (js/Promise.
    (fn [resolve reject]
      (let [proc (child-process.spawn cmd args #js{:encoding "utf-8"})]
        (stream-output (.-stdout proc)
          (fn [_ stdout]
            (let [completions (if (clojure.string/blank? stdout)
                                #js[]
                                (apply array (clojure.string/split (clojure.string/trim stdout) #"\n")))]
              (resolve completions))))))))

(defn complete-fish
  "Get completions from a fish shell. Spawns a process."
  [line]
  (-> (get-completions-spawn (str (getenv "CLOSH_SOURCES_PATH") "/scripts/completion/completion.fish") #js[line])
      (.then (fn [completions] (.map completions #(first (clojure.string/split % #"\t"))))))) ; discard the tab-separated description

(defn complete-bash
  "Get completions from bash. Spawns a process."
  [line]
  (get-completions-spawn (str (getenv "CLOSH_SOURCES_PATH") "/scripts/completion/completion.bash") #js[line]))

(defn complete-zsh
  "Get completions from zsh. Spawns a process."
  [line]
  (get-completions-spawn (str (getenv "CLOSH_SOURCES_PATH") "/scripts/completion/completion.zsh") #js[line]))

(defn complete-lumo
  "Get completions from Lumo."
  [line]
  (js/Promise.
    (fn [resolve reject]
      (try
        (lumo.repl/get-completions line resolve)
        (catch :default e (reject e))))))

(defn append-completion
  "Appends completion to a line, discards the common part from in between."
  [line completion]
  (let [line-lower (clojure.string/lower-case line)
        completion-lower (clojure.string/lower-case completion)]
    (loop [i (count completion-lower)]
      (if (zero? i)
        (str line completion)
        (let [sub (subs completion-lower 0 i)]
          (if (clojure.string/ends-with? line-lower sub)
            (str (subs line 0 (- (count line) i)) completion)
            (recur (dec i))))))))

(defn process-completions
  "Processes completions for a given line, cleans up results by removing duplicates."
  [line completions]
  (->> completions
    (map #(append-completion line %))
    (filter #(not= line %))
    (distinct))) ; bash seems to return duplicates sometimes

(defn complete
  "Gets completions for a given line. Delegates to existing shells and Lumo. Callback style compatible with node's builtin readline completer function."
  [line cb]
  (-> (js/Promise.all
       #js[(when (re-find #"\([^)]*$" line) ; only send exprs with unmatched paren to lumo
             (complete-lumo line))
           (-> (complete-fish line)
               (.then #(if (seq %) % (complete-bash line)))
               (.then #(if (seq %) % (complete-zsh line))))])
    (.then (fn [completions]
             (->> completions
               (map #(process-completions line %))
               (interpose [""])
               (apply concat)
               (apply array))))
    (.then #(cb nil #js[% line]))
    (.catch #(cb %))))
