(ns mayphus-map.map.core
  (:require ["maplibre-gl" :as maplibre]
            [reagent.core :as r]
            [mayphus-map.map.base-layout :as base-layout]
            [mayphus-map.map.main-road-layout :as main-road-layout]
            [mayphus-map.map.share :as share]))

(defonce runtime-state
  (r/atom {:map nil}))

(def hangzhou-center [120.1551 30.2741])

(def map-style-url
  "https://tiles.openfreemap.org/styles/positron")

(defn map-options [container]
  {:container container
   :style map-style-url
   :center hangzhou-center
   :zoom 11
   :hash true
   :preserveDrawingBuffer true})

(defn remove-runtime! []
  (when-let [map-instance (:map @runtime-state)]
    (.remove map-instance))
  (reset! runtime-state {:map nil}))

(defn mount-map! [container]
  (remove-runtime!)
  (let [map-instance (new maplibre/Map (clj->js (map-options container)))]
    (.addControl map-instance (new maplibre/NavigationControl) "top-right")
    (.on map-instance "load"
         (fn []
           (let [layers (base-layout/style-layers map-instance)]
             (base-layout/apply-base-layout! map-instance layers)
             (main-road-layout/apply-main-road-layout! map-instance layers))))
    (reset! runtime-state {:map map-instance})))

(defn hero-panel []
  [:section {:class "hero-panel"}
   [:p {:class "eyebrow"} "Macro Roads"]
   [:h1 "mayphus-map"]
   [:p {:class "hero-copy"}
    "A stripped-down Hangzhou road map tuned for orientation, not GIS ceremony. "
    "Pan, zoom, keep the URL hash, and export a clean PNG when you want a snapshot."]])

(defn map-panel []
  (let [container-el (atom nil)]
    (r/create-class
     {:display-name "mayphus-map"
      :component-did-mount
      (fn []
        (when-let [container @container-el]
          (mount-map! container)))
      :component-will-unmount
      (fn []
        (remove-runtime!))
      :reagent-render
      (fn []
        [:div {:class "app-shell"}
         [hero-panel]
         [share/share-panel runtime-state]
         [:div {:ref #(reset! container-el %)
                :class "map-canvas"}]])})))

(defn app-root []
  [map-panel])
