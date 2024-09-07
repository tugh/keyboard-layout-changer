(ns build
  (:require
   [clojure.tools.build.api :as b]))

(def class-dir "out/classes")
(def uber-file (format "out/keyboard-layout-changer.jar"))
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean
  [_]
  (b/delete {:path "out"}))

(defn uber
  [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src/" "resources/"]
               :target-dir class-dir})
  (b/compile-clj {:basis @basis
                  :ns-compile '[keyboard-layout-changer.core]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis @basis
           :main 'keyboard-layout-changer.core}))
