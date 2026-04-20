(ns mayphus-map.map.main-road-layout)

(def custom-layer-ids
  ["major-road-highlight-casing"
   "major-road-highlight"])

(def preset-themes
  {:macro-roads
   {:filter ["all"
             ["==", "$type", "LineString"]
             ["in", "class", "motorway", "trunk", "primary"]]
    :casing-color "#324d67"
    :fill-color "#9bb7d1"
    :label-color "#4b6177"
    :label-halo "#f8fbff"
    :base-line-opacity 0.12}
   :ring-roads
   {:filter ["all"
             ["==", "$type", "LineString"]
             ["in", "class", "motorway", "trunk"]]
    :casing-color "#203348"
    :fill-color "#f4b76e"
    :label-color "#61472d"
    :label-halo "#fff5e7"
    :base-line-opacity 0.08}
   :river-crossings
   {:filter ["all"
             ["==", "$type", "LineString"]
             ["in", "class", "motorway", "trunk", "primary"]]
    :casing-color "#2d4560"
    :fill-color "#72b8d8"
    :label-color "#34516a"
    :label-halo "#eefbff"
    :base-line-opacity 0.1}
   :grid-study
   {:filter ["all"
             ["==", "$type", "LineString"]
             ["in", "class", "primary", "secondary", "tertiary"]]
    :casing-color "#31473c"
    :fill-color "#8dc7a4"
    :label-color "#446353"
    :label-halo "#f4fbf4"
    :base-line-opacity 0.06}})

(def default-preset :macro-roads)

(defn current-theme [preset-id]
  (get preset-themes preset-id (get preset-themes default-preset)))

(defn style-layers [^js map-instance]
  (js->clj (.-layers (.getStyle map-instance)) :keywordize-keys true))

(defn road-source-spec [layers]
  (or
   (some (fn [{:keys [source source-layer]}]
           (when (= source-layer "transportation")
             {:source source
              :source-layer source-layer}))
         layers)
   (some (fn [{:keys [id source source-layer]}]
           (when (and source source-layer
                      (re-find #"highway|motorway|road" (name id)))
             {:source source
              :source-layer source-layer}))
         layers)))

(defn road-label-anchor [layers]
  (or
   (some (fn [{:keys [id source-layer type]}]
           (when (and (= type "symbol")
                      (= source-layer "transportation_name"))
             (name id)))
         layers)
   (some (fn [{:keys [id type]}]
           (when (and (= type "symbol")
                      (re-find #"highway|road" (name id)))
             (name id)))
         layers)))

(defn road-label-layer? [{:keys [id type source-layer]}]
  (and (= type "symbol")
       (or (= source-layer "transportation_name")
           (re-find #"highway|road" (name id)))))

(defn enhance-road-labels! [^js map-instance layers {:keys [label-color label-halo]}]
  (doseq [{:keys [id]} (filter road-label-layer? layers)]
    (.setPaintProperty map-instance (name id) "text-color" label-color)
    (.setPaintProperty map-instance (name id) "text-halo-color" label-halo)
    (.setPaintProperty map-instance (name id) "text-halo-width" 2.4)
    (.setPaintProperty map-instance (name id) "text-halo-blur" 0.6)))

(defn base-major-road-layer? [{:keys [id type source-layer]}]
  (and (= type "line")
       (= source-layer "transportation")
       (re-find #"highway_(motorway|major)" (name id))))

(defn mute-base-major-roads! [^js map-instance layers base-line-opacity]
  (doseq [{:keys [id]} (filter base-major-road-layer? layers)]
    (.setPaintProperty map-instance (name id) "line-opacity" base-line-opacity)))

(defn major-road-highlight-layers [{:keys [source source-layer]}
                                   {:keys [filter casing-color fill-color]}]
  [{:id "major-road-highlight-casing"
    :type "line"
    :source source
    :source-layer source-layer
    :minzoom 6
    :filter filter
    :layout {:line-cap "round"
             :line-join "round"}
    :paint {:line-color casing-color
            :line-opacity 0.98
            :line-width
            [:interpolate [:linear] [:zoom]
             6 1.3
             10 2.8
             14 5.6
             18 11]}}
   {:id "major-road-highlight"
    :type "line"
    :source source
    :source-layer source-layer
    :minzoom 6
    :filter filter
    :layout {:line-cap "round"
             :line-join "round"}
    :paint {:line-color fill-color
            :line-opacity 0.98
            :line-width
            [:interpolate [:linear] [:zoom]
             6 0.35
             10 0.9
             14 2.2
             18 5.4]}}])

(defn remove-custom-layers! [^js map-instance]
  (doseq [layer-id custom-layer-ids]
    (when (.getLayer map-instance layer-id)
      (.removeLayer map-instance layer-id))))

(defn apply-preset! [^js map-instance preset-id]
  (let [layers (style-layers map-instance)
        theme (current-theme preset-id)
        source-spec (road-source-spec layers)
        before-id (road-label-anchor layers)]
    (when source-spec
      (remove-custom-layers! map-instance)
      (mute-base-major-roads! map-instance layers (:base-line-opacity theme))
      (enhance-road-labels! map-instance layers theme)
      (doseq [layer (major-road-highlight-layers source-spec theme)]
        (if before-id
          (.addLayer map-instance
                     (clj->js layer)
                     before-id)
          (.addLayer map-instance
                     (clj->js layer)))))))
