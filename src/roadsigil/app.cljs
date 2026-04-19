(ns roadsigil.app
  (:require [reagent.core :as r]
            ["react-dom/client" :as react-dom-client]
            [roadsigil.map.core :as map]))

(defn root []
  [map/app-root])

(defn init []
  (when-let [container (.getElementById js/document "app")]
    (.render (react-dom-client/createRoot container)
             (r/as-element [root]))))
