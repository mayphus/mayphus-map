(ns roadsigil.map.base-layout)

(defn style-layers [^js map-instance]
  (js->clj (.-layers (.getStyle map-instance)) :keywordize-keys true))

(defn set-paint-safe! [^js map-instance layer-id prop value]
  (try
    (.setPaintProperty map-instance layer-id prop value)
    (catch :default _ nil)))

(defn mute-basemap! [^js map-instance layers]
  (doseq [{:keys [id type]} layers
          :when id]
    (let [layer-id (name id)]
      (case type
        "fill"
        (set-paint-safe! map-instance layer-id "fill-opacity" 0.5)

        "line"
        (set-paint-safe! map-instance layer-id "line-opacity" 0.3)

        "symbol"
        (do
          (set-paint-safe! map-instance layer-id "text-opacity" 0.42)
          (set-paint-safe! map-instance layer-id "icon-opacity" 0.4))

        "circle"
        (do
          (set-paint-safe! map-instance layer-id "circle-opacity" 0.4)
          (set-paint-safe! map-instance layer-id "circle-stroke-opacity" 0.4))

        "fill-extrusion"
        (set-paint-safe! map-instance layer-id "fill-extrusion-opacity" 0.35)

        "raster"
        (set-paint-safe! map-instance layer-id "raster-opacity" 0.5)

        "hillshade"
        (set-paint-safe! map-instance layer-id "hillshade-exaggeration" 0.4)

        nil))))

(defn apply-base-layout! [^js map-instance layers]
  (mute-basemap! map-instance layers))
