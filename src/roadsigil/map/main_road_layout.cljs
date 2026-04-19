(ns roadsigil.map.main-road-layout)

(def major-road-filter
  ["all"
   ["==", "$type", "LineString"]
   ["in", "class", "motorway", "trunk", "primary"]])

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

(defn enhance-road-labels! [^js map-instance layers]
  (doseq [{:keys [id]} (filter road-label-layer? layers)]
    (.setPaintProperty map-instance (name id) "text-color" "#516274")
    (.setPaintProperty map-instance (name id) "text-halo-color" "#ffffff")
    (.setPaintProperty map-instance (name id) "text-halo-width" 2.4)
    (.setPaintProperty map-instance (name id) "text-halo-blur" 0.6)))

(defn base-major-road-layer? [{:keys [id type source-layer]}]
  (and (= type "line")
       (= source-layer "transportation")
       (re-find #"highway_(motorway|major)" (name id))))

(defn mute-base-major-roads! [^js map-instance layers]
  (doseq [{:keys [id]} (filter base-major-road-layer? layers)]
    (.setPaintProperty map-instance (name id) "line-opacity" 0.12)))

(defn major-road-highlight-layers [{:keys [source source-layer]}]
  [{:id "major-road-highlight-casing"
    :type "line"
    :source source
    :source-layer source-layer
    :minzoom 6
    :filter major-road-filter
    :layout {:line-cap "round"
             :line-join "round"}
    :paint {:line-color "#7a8faa"
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
    :filter major-road-filter
    :layout {:line-cap "round"
             :line-join "round"}
    :paint {:line-color "#7a8faa"
            :line-opacity 0.98
            :line-width
            [:interpolate [:linear] [:zoom]
             6 0.35
             10 0.9
             14 2.2
             18 5.4]}}])

(defn apply-main-road-layout! [^js map-instance layers]
  (let [source-spec (road-source-spec layers)
        before-id (road-label-anchor layers)]
    (when source-spec
      (mute-base-major-roads! map-instance layers)
      (enhance-road-labels! map-instance layers)
      (doseq [layer (major-road-highlight-layers source-spec)]
        (if before-id
          (.addLayer map-instance
                     (clj->js layer)
                     before-id)
          (.addLayer map-instance
                     (clj->js layer)))))))
