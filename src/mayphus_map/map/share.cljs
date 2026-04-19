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
    [:div {:style {:position "fixed"
                   :right "18px"
                   :bottom "18px"
                   :z-index 20
                   :display "grid"
                   :gap "8px"
                   :justify-items "end"}}
     [:button {:on-click #(download-png! runtime-state)
               :title "Download PNG"
               :style {:border "1px solid rgba(71, 85, 105, 0.18)"
                       :height "40px"
                       :padding "0 14px"
                       :border-radius "999px"
                       :background "rgba(251, 253, 255, 0.92)"
                       :backdrop-filter "blur(14px)"
                       :box-shadow "0 14px 32px rgba(15, 23, 42, 0.12)"
                       :color "#17304a"
                       :font-size "0.8rem"
                       :font-weight 700
                       :letter-spacing "0.08em"
                       :text-transform "uppercase"
                       :cursor "pointer"}}
      "Export PNG"]
     [:p {:style {:margin 0
                  :min-height "14px"
                  :max-width "160px"
                  :text-align "right"
                  :font-size "0.75rem"
                  :color "#52606d"}}
      (or status "")]]))
