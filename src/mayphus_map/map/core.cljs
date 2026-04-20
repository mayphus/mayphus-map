(ns mayphus-map.map.core
  (:require ["maplibre-gl" :as maplibre]
            [reagent.core :as r]
            [mayphus-map.map.base-layout :as base-layout]
            [mayphus-map.map.main-road-layout :as main-road-layout]
            [mayphus-map.map.share :as share]))

(defonce runtime-state
  (r/atom {:map nil}))

(defonce ui-state
  (r/atom {:active-preset :macro-roads}))

(def hangzhou-center [120.1551 30.2741])

(def map-style-url
  "https://tiles.openfreemap.org/styles/positron")

(def presets
  [{:id :macro-roads
    :eyebrow "Macro Roads"
    :title "Read Hangzhou by its arterial spine."
    :description "The broad city frame: expressways, trunks, and the routes that hold the whole urban field together."
    :chip "Citywide structure"
    :view {:center hangzhou-center
           :zoom 10.95
           :pitch 0
           :bearing 0}}
   {:id :ring-roads
    :eyebrow "Ring Roads"
    :title "Focus on the loops that organize motion."
    :description "A tighter read of the orbital system. This view makes the ring logic feel deliberate instead of incidental."
    :chip "Orbital hierarchy"
    :view {:center [120.1551 30.2741]
           :zoom 11.4
           :pitch 0
           :bearing 0}}
   {:id :river-crossings
    :eyebrow "River Crossings"
    :title "See the city through its bridge corridors."
    :description "Shift south toward the Qiantang and treat the crossings as the real hinges between districts."
    :chip "Bridge corridors"
    :view {:center [120.237 30.255]
           :zoom 11.8
           :pitch 0
           :bearing 0}}
   {:id :grid-study
    :eyebrow "Grid Study"
    :title "Drop from expressways into the city grain."
    :description "The secondary lattice matters too. This mode pulls the urban grid forward so the street fabric stops hiding behind the big roads."
    :chip "Street grain"
    :view {:center [120.163 30.285]
           :zoom 12.55
           :pitch 0
           :bearing 0}}])

(def presets-by-id
  (into {} (map (juxt :id identity) presets)))

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

(defn current-preset []
  (get presets-by-id (:active-preset @ui-state) (first presets)))

(defn move-to-preset! [^js map-instance {:keys [view]}]
  (.easeTo map-instance
           (clj->js
            (merge {:duration 1400
                    :essential true}
                   view))))

(defn apply-preset! [^js map-instance preset]
  (main-road-layout/apply-preset! map-instance (:id preset))
  (move-to-preset! map-instance preset))

(defn mount-map! [container]
  (remove-runtime!)
  (let [map-instance (new maplibre/Map (clj->js (map-options container)))]
    (.addControl map-instance (new maplibre/NavigationControl) "top-right")
    (.on map-instance "load"
         (fn []
           (let [layers (base-layout/style-layers map-instance)]
             (base-layout/apply-base-layout! map-instance layers)
             (apply-preset! map-instance (current-preset)))))
    (reset! runtime-state {:map map-instance})))

(defn activate-preset! [preset]
  (swap! ui-state assoc :active-preset (:id preset))
  (when-let [map-instance (:map @runtime-state)]
    (apply-preset! map-instance preset)))

(defn preset-button [preset active?]
  [:button {:type "button"
            :class (str "preset-button" (when active? " is-active"))
            :on-click #(activate-preset! preset)}
   [:span {:class "preset-label"} (:eyebrow preset)]
   [:strong {:class "preset-title"} (:title preset)]
   [:span {:class "preset-description"} (:description preset)]])

(defn hero-panel []
  (let [{:keys [id eyebrow title description chip]} (current-preset)]
    [:section {:class "hero-panel"}
     [:div {:class "hero-topline"}
      [:p {:class "eyebrow"} eyebrow]
      [:span {:class "hero-chip"} chip]]
     [:h1 "mayphus-map"]
     [:p {:class "hero-copy"}
      "An editorial road atlas for Hangzhou. Pick a reading mode, jump to a stored viewpoint, and export the scene once it says something precise."]
     [:div {:class "story-card"}
      [:p {:class "story-kicker"} "Current lens"]
      [:h2 {:class "story-title"} title]
      [:p {:class "story-copy"} description]]
     [:div {:class "preset-list"}
      (for [preset presets]
        ^{:key (:id preset)}
        [preset-button preset (= id (:id preset))])]
     [:p {:class "panel-note"}
      "URL hash stays live while you pan. Use the presets to reset the narrative instead of manually hunting for a view."]]))

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
