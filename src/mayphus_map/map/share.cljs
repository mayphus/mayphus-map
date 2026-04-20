(ns mayphus-map.map.share
  (:require [reagent.core :as r]))

(defonce share-state
  (r/atom {:status nil}))

(defn set-share-status! [message]
  (reset! share-state {:status message})
  (js/setTimeout #(swap! share-state assoc :status nil) 2200))

(defn export-map-data-url [runtime-state]
  (if-let [^js map-instance (:map @runtime-state)]
    (js/Promise.
     (fn [resolve reject]
       (letfn [(capture! []
                 (.triggerRepaint map-instance)
                 (js/requestAnimationFrame
                  (fn []
                    (js/requestAnimationFrame
                     (fn []
                       (if-let [canvas (.getCanvas map-instance)]
                         (try
                           (resolve (.toDataURL canvas "image/png"))
                           (catch :default _
                             (reject (js/Error. "Map capture failed"))))
                         (reject (js/Error. "Map canvas unavailable"))))))))]
         (if (.loaded map-instance)
           (capture!)
           (.once map-instance "idle" capture!)))))
    (js/Promise.reject (js/Error. "Map export unavailable"))))

(defn download-url! [url filename]
  (let [link (.createElement js/document "a")]
    (set! (.-href link) url)
    (set! (.-download link) filename)
    (.appendChild (.-body js/document) link)
    (.click link)
    (.remove link)))

(defn download-png! [runtime-state]
  (-> (export-map-data-url runtime-state)
      (.then (fn [data-url]
               (download-url! data-url "mayphus-map-hangzhou.png")
               (set-share-status! "PNG downloaded")))
      (.catch (fn []
                (set-share-status! "Map export unavailable")))))

(defn share-panel [runtime-state]
  (let [{:keys [status]} @share-state]
    [:div {:class "share-panel"}
     [:button {:on-click #(download-png! runtime-state)
               :title "Download PNG"
               :class "share-button"}
      "Export PNG"]
     [:p {:class "share-status"}
      (or status "")]]))
