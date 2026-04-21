(ns mayphus-map.map.share
  (:require ["maplibre-gl" :as maplibre]
            [reagent.core :as r]))

(defonce share-state
  (r/atom {:exporting? false
           :status nil}))

(defonce status-timeout-id
  (atom nil))

(defn clear-status-timeout! []
  (when-let [timeout-id @status-timeout-id]
    (js/clearTimeout timeout-id)
    (reset! status-timeout-id nil)))

(defn set-share-status!
  ([message]
   (set-share-status! message true))
  ([message auto-clear?]
   (clear-status-timeout!)
   (swap! share-state assoc :status message)
   (when auto-clear?
     (reset! status-timeout-id
             (js/setTimeout
              (fn []
                (swap! share-state assoc :status nil)
                (reset! status-timeout-id nil))
              2200)))))

(defn current-view [^js map-instance]
  (let [center (.toArray (.getCenter map-instance))]
    {:center center
     :zoom (.getZoom map-instance)
     :bearing (.getBearing map-instance)
     :pitch (.getPitch map-instance)}))

(defn clone-map-style [^js map-instance]
  (js/JSON.parse (js/JSON.stringify (.getStyle map-instance))))

(defn create-export-container! [^js canvas]
  (let [container (.createElement js/document "div")
        width (max 1 (.-clientWidth canvas))
        height (max 1 (.-clientHeight canvas))
        style (.-style container)]
    (set! (.-position style) "fixed")
    (set! (.-left style) "-10000px")
    (set! (.-top style) "0")
    (set! (.-width style) (str width "px"))
    (set! (.-height style) (str height "px"))
    (set! (.-opacity style) "0")
    (set! (.-pointerEvents style) "none")
    (set! (.-zIndex style) "-1")
    (.appendChild (.-body js/document) container)
    container))

(defn remove-export-container! [^js container]
  (when-let [parent (.-parentNode container)]
    (.removeChild parent container)))

(defn canvas->png-blob [^js canvas]
  (js/Promise.
   (fn [resolve reject]
     (.toBlob canvas
              (fn [blob]
                (if blob
                  (resolve blob)
                  (reject (js/Error. "Map export failed"))))
              "image/png"))))

(defn export-map-blob-from-instance [^js map-instance]
  (if-let [source-canvas (.getCanvas map-instance)]
    (js/Promise.
     (fn [resolve reject]
       (let [container (create-export-container! source-canvas)
             export-map (atom nil)
             timeout-id (atom nil)
             finished? (atom false)
             finish! (fn [handler value]
                       (when-not @finished?
                         (reset! finished? true)
                         (when-let [pending-timeout @timeout-id]
                           (js/clearTimeout pending-timeout)
                           (reset! timeout-id nil))
                         (when-let [instance @export-map]
                           (.remove instance))
                         (remove-export-container! container)
                         (handler value)))
             export-options (clj->js
                             (merge {:container container
                                     :style (clone-map-style map-instance)
                                     :interactive false
                                     :attributionControl false
                                     :hash false
                                     :preserveDrawingBuffer true}
                                    (current-view map-instance)))]
         (try
           (reset! timeout-id
                   (js/setTimeout
                    #(finish! reject (js/Error. "Map export timed out"))
                    12000))
           (let [instance (new maplibre/Map export-options)]
             (reset! export-map instance)
             (.once instance "idle"
                    (fn [_]
                      (if-let [canvas (.getCanvas instance)]
                        (-> (canvas->png-blob canvas)
                            (.then #(finish! resolve %))
                            (.catch #(finish! reject %)))
                        (finish! reject (js/Error. "Map canvas unavailable"))))))
           (catch :default error
             (finish! reject error)))))
    (js/Promise.reject (js/Error. "Map canvas unavailable")))))

(defn export-map-blob [runtime-state]
  (if-let [^js map-instance (:map @runtime-state)]
    (js/Promise.
     (fn [resolve reject]
       (let [start-export! (fn [_]
                             (-> (export-map-blob-from-instance map-instance)
                                 (.then resolve)
                                 (.catch reject)))]
         (if (.loaded map-instance)
           (start-export! nil)
           (.once map-instance "idle" start-export!)))))
    (js/Promise.reject (js/Error. "Map export unavailable"))))

(defn download-url! [url filename]
  (let [link (.createElement js/document "a")]
    (set! (.-href link) url)
    (set! (.-download link) filename)
    (.appendChild (.-body js/document) link)
    (.click link)
    (.remove link)))

(defn download-blob! [blob filename]
  (let [url (.createObjectURL js/URL blob)]
    (download-url! url filename)
    (js/setTimeout #(.revokeObjectURL js/URL url) 1000)))

(defn download-png! [runtime-state]
  (swap! share-state assoc :exporting? true)
  (set-share-status! "Rendering PNG..." false)
  (-> (export-map-blob runtime-state)
      (.then (fn [blob]
               (download-blob! blob "mayphus-map-hangzhou.png")
               (set-share-status! "PNG downloaded")))
      (.catch (fn [_]
                (set-share-status! "Map export unavailable")))
      (.finally (fn []
                  (swap! share-state assoc :exporting? false)))))

(defn share-panel [runtime-state]
  (let [{:keys [exporting? status]} @share-state]
    [:div {:class "share-panel"}
     [:button {:on-click #(download-png! runtime-state)
               :disabled exporting?
               :title "Download PNG"
               :class "share-button"}
      (if exporting? "Rendering..." "Export PNG")]
     [:p {:class "share-status"}
      (or status "")]]))
