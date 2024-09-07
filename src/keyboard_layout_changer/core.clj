(ns keyboard-layout-changer.core
  (:require
   [clojure.core.async :as async]
   [clojure.java.shell :as shell]
   [clojure.java.io :as io])
  (:import
   [com.github.kwhat.jnativehook GlobalScreen]
   [com.github.kwhat.jnativehook.keyboard NativeKeyListener NativeKeyEvent]
   [com.sun.javafx.application PlatformImpl]
   [javafx.scene.media Media MediaPlayer]
   [java.time Instant])
  (:gen-class))

(def debug? (atom true))

(defn log
  [& args]
  (when @debug?
    (apply println args)))

(defn play-sound-effect
  []
  (PlatformImpl/runLater
   #(-> "sound-effect.wav"
        io/resource
        .toURI
        .toString
        Media.
        MediaPlayer.
        .play)))

(defn current-keyboard-layout
  []
  (->> (shell/sh "setxkbmap" "-query")
       :out
       (re-find #"layout:\s+(\w+)")
       second))

(def timeout-ms 1000)

(defonce cancel-chan (async/chan 1))

(defn flush-cancel-chan
  []
  (async/go-loop []
    (if-not (nil? (async/poll! cancel-chan))
      (do
        (log "Received non nil value from cancel channel, this means layout has changed already")
        (recur))
      (log "Successfully flushed cancel channel"))))

(defn change-layout
  []
  (let [layout (current-keyboard-layout)
        new-layout (if (= layout "us") "tr" "us")]
    (shell/sh "setxkbmap" new-layout)
    (play-sound-effect)))

(defn plan-change-layout
  []
  (async/go
    (let [[val _] (async/alts! [(async/timeout timeout-ms) cancel-chan])]
      (if (not= :cancel val)
        (do
          (change-layout)
          (log "Changed layout to" (current-keyboard-layout)))
        (log "Cancelled layout change")))))

(defn on-key-down
  [e]
  (when (= "Meta"
           (NativeKeyEvent/getKeyText (.getKeyCode e)))
    (log "Meta key pressed at" (.toString (Instant/now)))
    (plan-change-layout)))

(defn on-key-up
  [e]
  (when (= "Meta"
           (NativeKeyEvent/getKeyText (.getKeyCode e)))
    (log "Meta key released at" (.toString (Instant/now)))
    (async/go
      (log "Sending :cancel signal...")
      (async/>! cancel-chan :cancel)
      (log "Initiating timeout")
      (async/<! (async/timeout 100))
      (log "Attempting flush on cancel channel")
      (flush-cancel-chan))))

(def key-listener
  (reify NativeKeyListener
    (nativeKeyPressed [_ e] (on-key-down e))
    (nativeKeyReleased [_ e] (on-key-up e))))

(defn -main [& args]
  (PlatformImpl/startup (fn []))
  (GlobalScreen/registerNativeHook)
  (GlobalScreen/addNativeKeyListener key-listener))
