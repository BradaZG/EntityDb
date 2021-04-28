(ns app.controllers.test
  (:require [keechma.next.controller :as ctrl]
            [keechma.next.controllers.pipelines :as pipelines]
            [keechma.next.controllers.entitydb :as edb]
            [keechma.next.toolbox.logging :as l]
            [promesa.core :as p]
            [keechma.entitydb.query :as q]
            [keechma.pipelines.core :as pp :refer-macros [pipeline!]]))

(derive :test ::pipelines/controller)

(def print-entity-state
  (pipeline! [value {:keys [deps-state* state*] :as ctrl}]
             (let [entitydb (:entitydb @deps-state*)]
               (l/pp "AAA " (edb/get-named entitydb :user/current [(q/include :grades)]))
               (l/pp entitydb))))

(def insert-named
  (pipeline! [value {:keys [deps-state* state*] :as ctrl}]
             (let [entitydb (:entitydb @deps-state*)]
               (println "----------------> INSERT NAMED")
               (edb/insert-named! ctrl :entitydb :user :user/current {:id 1 :name "Tin" :lastname "Levacic"})
               #_(edb/insert-named! ctrl :entitydb :user :user/active {:id 1 :name "Tin Active" :lastname "Levacic"})
               (ctrl/dispatch ctrl :test :print-entity-state))))

(def insert-collection
  (pipeline! [value {:keys [deps-state* state*] :as ctrl}]
             (let [entitydb (:entitydb @deps-state*)]
               (println "----------------> INSERT COLLECTION")
               (edb/insert-collection! ctrl :entitydb :user :user/list [{:id 1 :name "John" :lastname "Smith"}
                                                                        {:id 2 :name "Bob" :lastname "Smith"}])
               (ctrl/dispatch ctrl :test :print-entity-state))))

(def remove-named
  (pipeline! [value {:keys [deps-state* state*] :as ctrl}]
             (let [entitydb (:entitydb @deps-state*)]
               (println "----------------> REMOVE NAMED")
               (edb/remove-named! ctrl :entitydb :user/current)
               (p/delay 3000)
               (ctrl/dispatch ctrl :test :print-entity-state))))

(def remove-entity
  (pipeline! [value {:keys [deps-state* state*] :as ctrl}]
             (let [entitydb (:entitydb @deps-state*)]
               (println "----------------> REMOVE ENTITY")
               (edb/remove-entity! ctrl :entitydb :user 1)
               (p/delay 3000)
               (ctrl/dispatch ctrl :test :print-entity-state))))

(def schema-test
  (pipeline! [value {:keys [deps-state* state*] :as ctrl}]
             (let [entitydb (:entitydb @deps-state*)]
               (println "----------------> SCHEMA TEST")
               (edb/insert-collection! ctrl :entitydb :user :user/list [{:id 1 :name "Tin" :lastname "Levacic" :language [{:id 1 :language :clojure}
                                                                                                                          {:id 2 :language :javascript}]}])
               (ctrl/dispatch ctrl :test :print-entity-state))))

(def start
  (pipeline! [value {:keys [deps-state* state*] :as ctrl}]
             (let [entitydb (:entitydb @deps-state*)]
               (pipeline! [value ctrl]
                          (ctrl/dispatch ctrl :test :insert-named)
                          (p/delay 3000)
                          (ctrl/dispatch ctrl :test :insert-collection)
                          (p/delay 3000)
                          #_(ctrl/dispatch ctrl :test :remove-named)
                          #_(ctrl/dispatch ctrl :test :remove-entity)
                          (ctrl/dispatch ctrl :test :schema-test)
                          (p/delay 500)))))



(def pipelines
  {:keechma.on/start start
   :insert-named insert-named
   :insert-collection insert-collection
   :remove-named remove-named
   :remove-entity remove-entity
   :schema-test schema-test
   :print-entity-state print-entity-state})

(defmethod ctrl/prep :test [ctrl]
  (pipelines/register ctrl pipelines))

(defmethod ctrl/derive-state :test [_ state _]
  state)