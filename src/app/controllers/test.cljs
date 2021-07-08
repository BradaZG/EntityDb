(ns app.controllers.test
  (:require [keechma.next.controller :as ctrl]
            [keechma.next.controllers.pipelines :as pipelines]
            [keechma.next.controllers.entitydb :as edb]
            [keechma.next.toolbox.logging :as l]
            [promesa.core :as p]
            [keechma.entitydb.query :as q]
            [datascript.core :as d]
            [datascript.db :as db]
            [autonormal.core :as a]
            [keechma.pipelines.core :as pp :refer-macros [pipeline!]]))

(derive :test ::pipelines/controller)

;; ENTITY DB examples
(def print-entity-state
  (pipeline! [value {:keys [deps-state* state*] :as ctrl}]
             (let [entitydb (:entitydb @deps-state*)]
               (l/pp entitydb)
               (l/pp "AAA" (edb/get-named entitydb :user/current))
               (l/pp "BBB" (edb/get-entity entitydb :user 1))
               (l/pp "CCC" (edb/get-collection entitydb :programming-language/mobile))
               (l/pp "DDD" (edb/get-collection entitydb :user/list))
               (l/pp "FFF" (edb/get-ident-for-named entitydb :user/current))
               (l/pp "GGG" (edb/get-idents-for-collection entitydb :user/list))
               (l/pp "PPP" (edb/get-entity entitydb :programming-language 1 [(edb/reverse-include :user)]))
               (l/pp "RRR" (edb/get-named entitydb :user/current [(edb/include :programming-languages)]))
               (l/pp "SSS" (edb/get-entity-from-ident entitydb {:type :user, :id 1}))
               (l/pp "TTT1" (edb/get-entities-from-idents entitydb [{:type :user, :id 1} {:type :user, :id 3}]))
               (l/pp "TTT2" (edb/get-entities-from-idents entitydb (edb/get-idents-for-collection entitydb :user/list))))))

(def insert-named
  (pipeline! [value {:keys [deps-state* state*] :as ctrl}]
             (println "----------------> INSERT NAMED")
             (edb/insert-named! ctrl :entitydb :user :user/current {:id 1 :name "Domagoj" :lastname "Marusic"})
             (edb/insert-named! ctrl :entitydb :user :user/active {:id 2 :name "Marko" :lastname "Ivic"})
             (edb/insert-entity! ctrl :entitydb :user {:id 2 :nickname "Max" :age "35"})
               ;(edb/insert-named! ctrl :entitydb :user :user/active {:id 2 :name "Tin Two Active" :lastname "Levacic"})
             (ctrl/dispatch ctrl :test :print-entity-state)))

(def insert-collection
  (pipeline! [value {:keys [deps-state* state*] :as ctrl}]
             (println "----------------> INSERT COLLECTION")
             (edb/insert-collection! ctrl :entitydb :user :user/list [{:id 1 :name "John" :lastname "Smith"}
                                                                      {:id 3 :name "Bob" :lastname "Smith"}])
             (edb/insert-collection! ctrl :entitydb :programming-language :programming-language/web [{:id 1 :name :javascript}
                                                                                                     {:id 2 :name :clojurescript}])
             (edb/insert-collection! ctrl :entitydb :programming-language :programming-language/mobile [{:id 3 :name :swift}])
             (edb/insert-collection! ctrl :entitydb :programming-language :programming-language/machine-learning [{:id 4 :name :python}])
             (edb/insert-collection! ctrl :entitydb :user :user/friends [{:id 1 :username "Brada"} {:id 2 :username "MaxI"}])
             (ctrl/dispatch ctrl :test :print-entity-state)))

(def remove-named
  (pipeline! [value {:keys [deps-state* state*] :as ctrl}]
             (println "----------------> REMOVE NAMED")
             (edb/remove-named! ctrl :entitydb :user/current)
             (p/delay 3000)
             (ctrl/dispatch ctrl :test :print-entity-state)))

(def remove-entity
  (pipeline! [value {:keys [deps-state* state*] :as ctrl}]
             (println "----------------> REMOVE ENTITY")
             (edb/remove-entity! ctrl :entitydb :user 1)
             (p/delay 3000)
             (ctrl/dispatch ctrl :test :print-entity-state)))

(def schema-test
  (pipeline! [value {:keys [deps-state* state*] :as ctrl}]
             (println "----------------> SCHEMA TEST")
             (edb/insert-named! ctrl :entitydb :user :user/current {:id 1 :name "Domagoj" :lastname "Marusic"
                                                                         ;:languages [{:id 1 :language :clojure}
                                                                         ;            {:id 2 :language :javascript}]
                                                                    :programming-language [{:id 1} {:id 3}]})
             (ctrl/dispatch ctrl :test :print-entity-state)))

;; DATASCRIPT EXAMPLES
(def batman
  (d/db-with
   (db/empty-db {:name    {:db/unique      :db.unique/identity}
                 :alias   {:db/unique      :db.unique/value
                           :db/cardinality :db.cardinality/many}
                 :powers  {:db/cardinality :db.cardinality/many}
                 :weapons {:db/cardinality :db.cardinality/many}
                 :nemesis {:db/valueType   :db.type/ref
                           :db/cardinality :db.cardinality/many}})
   [{:name      "Bruce Wayne"
     :age       32
     :gender    "M"
     :alias     "Batman"
     :powers    ["Rich"]
     :weapons   ["Belt" "Kryptonite Spear"]
     :alignment "Chaotic Good"
     :nemesis   [{:name "Joker"} {:name "Penguin"}]}]))


(def parker-family
  (d/db-with
   (db/empty-db {:name   {:db/unique :db.unique/identity}
                 :alias  {:db/unique      :db.unique/value
                          :db/cardinality :db.cardinality/many}
                 :spouse {:db/cardinality :db.cardinality/one
                          :db/valueType   :db.type/ref}
                 :child  {:db/cardinality :db.cardinality/many
                          :db/valueType   :db.type/ref}})
   [{:name "Peter Parker" :gender "M" :alias ["Spider-Man" "Spidey"]}
    {:name "Richard Parker" :gender "M" :spouse {:name "Mary Parker"}}
    {:name "Mary Parker" :gender "F" :spouse {:name "Richard Parker"}}
    {:name "Ben Parker" :gender "M" :spouse {:name "May Parker"}}
    {:name "May Parker" :gender "F" :spouse {:name "Ben Parker"}}
    {:name "Richard Parker" :child {:name "Peter Parker"}}
    {:name "Mary Parker" :child {:name "Peter Parker"}}
    {:child  [{:name "Ben Parker"}
              {:name "Richard Parker"}]
     :gender "M"}
    {:child  [{:name "Ben Parker"}
              {:name "Richard Parker"}]
     :gender "F"}]))


(def schema {:car/maker {:db/type :db.type/ref}
             :car/colors {:db/cardinality :db.cardinality/many}})

(def conn (d/create-conn schema))

(d/transact! conn [{:maker/name "Honda"
                    :maker/country "Japan"}])

(d/transact! conn [{:db/id -1
                    :maker/name "BMW"
                    :maker/country "Germany"}
                   {:car/maker -1
                    :car/name "i525"
                    :car/colors ["red" "green" "blue"]}])


(def schema-two {:maker/email {:db/unique :db.unique/identity}
                 :car/model {:db/unique :db.unique/identity}
                 :car/maker {:db/type :db.type/ref}
                 :car/colors {:db/cardinality :db.cardinality/many}})

(def conn-two (d/create-conn schema-two))

(d/transact! conn-two [{:maker/email "ceo@bmw.com"
                        :maker/name "BMW"}
                       {:car/model "E39530i"
                        :car/maker [:maker/email "ceo@bmw.com"]
                        :car/name "2003 530i"}])

(d/transact! conn-two [{:car/model "E39520i"
                        :car/maker [:maker/email "ceo@bmw.com"]
                        :car/name "2003 520i"}])

(d/transact! conn-two [{:maker/email "ceo@bmw.com"
                        :maker/name "BMW Motors"}])


(def schema-three {:user/id {:db.unique :db.unique/identity}
                   :user/name {}
                   :user/age {}
                   :user/parent {:db.valueType :db.type/ref
                                 :db.cardinality :db.cardinality/many}})

(def conn-three (d/create-conn schema-three))

(d/transact! conn-three
             [{:user/id "1"
               :user/name "alice"
               :user/age 27}
              {:user/id "2"
               :user/name "bob"
               :user/age 29}
              {:user/id "3"
               :user/name "kim"
               :user/age 2
               :user/parent [[:user/id "1"]
                             [:user/id "2"]]}
              {:user/id "4"
               :user/name "aaron"
               :user/age 61}
              {:user/id "5"
               :user/name "john"
               :user/age 39
               :user/parent [[:user/id "4"]]}
              {:user/id "6"
               :user/name "mark"
               :user/age 34}
              {:user/id "7"
               :user/name "kris"
               :user/age 8
               :user/parent [[:user/id "4"]
                             [:user/id "5"]]}])

;; AUTONORMAL EXAMPLES
(def data
  {:person/id 0 :person/name "Rachel"
   :friend/list [{:person/id 1 :person/name "Marco"}
                 {:person/id 2 :person/name "Cassie"}
                 {:person/id 3 :person/name "Jake"}
                 {:person/id 4 :person/name "Tobias"}
                 {:person/id 5 :person/name "Ax"}]})

;; you can pass in multiple entities to instantiate a db, so `a/db` gets a vector
(def an-one (a/db [data]))

;; Marco and Jake are each others best friend
(def an-two
  (a/add an-one {:person/id 1
                 :friend/best {:person/id 3
                               :friend/best {:person/id 1}}}))

(def an-three
  (a/add an-two {:species {:andalites [{:person/id 5
                                        :person/species "andalite"}]}}))

(def query '[{[:person/id 0] [:person/id
                              :person/name
                              {:friend/list ...}]}])


(def start
  (pipeline! [value {:keys [deps-state* state*] :as ctrl}]
             (let [entitydb (:entitydb @deps-state*)]
               (pipeline! [value ctrl]

                          ;; ENTITYDB
                          (ctrl/dispatch ctrl :test :insert-named)
                          (p/delay 3000)
                          (ctrl/dispatch ctrl :test :insert-collection)
                          (p/delay 3000)
                          ;(ctrl/dispatch ctrl :test :remove-named)
                          ;(ctrl/dispatch ctrl :test :remove-entity)
                          (p/delay 500)
                          (ctrl/dispatch ctrl :test :schema-test)

                          ;; DATASCRIPT
                          (l/pp batman)
                          (l/pp parker-family)

                          ; get all the names associated with all the entities in the database
                          (l/pp (d/q
                                 '[:find ?name
                                   :in $
                                   :where
                                   [?e :name ?name]]
                                 parker-family))

                          ; who is Peter Parker uncle
                          (l/pp (d/q
                                 '[:find [?uncle-name ...]
                                   :in $ ?person
                                   :where
                                   [?parent :child ?person]
                                   [?grandparent :child ?parent]
                                   [?grandparent :child ?uncle]
                                   [?uncle :gender "M"]
                                   [(not= ?parent ?uncle)]
                                   [?uncle :name ?uncle-name]]
                                 parker-family [:name "Peter Parker"]))

                          ; who are Spideys aunt and uncle
                          (l/pp (d/q
                                 '[:find (pull ?parent-sibling [:name {:spouse [:name]}])
                                   :in $ ?person
                                   :where
                                   [?parent :child ?person]
                                   [?grandparent :child ?parent]
                                   [?grandparent :child ?parent-sibling]
                                   [(not= ?parent ?parent-sibling)]]
                                 parker-family [:alias "Spidey"]))

                          ; everything about entity 4
                          (l/pp (d/pull parker-family '[*] 4))
                          ; everything about Ben Parker
                          (l/pp (d/pull parker-family '[*] [:name "Ben Parker"]))
                          ; everything about Richard Parker
                          (l/pp (d/pull parker-family '[*] [:name "Richard Parker"]))
                          ; everything about Peter Parker
                          (l/pp (d/pull parker-family '[*] [:name "Peter Parker"]))
                          ; who are Peter's parents
                          ; an attribute preceded by an _ is a "back reference"
                          (l/pp (d/pull parker-family '[{:_child [*]}] [:name "Peter Parker"]))

                          (l/pp (d/pull parker-family '[{:_child
                                                         [{:_child
                                                           [{:child [:name]}]}]}]
                                        [:name "Peter Parker"]))
                          (l/pp @conn)
                          (l/pp @conn-two)
                          (l/pp @conn-three)

                          (l/pp (d/q '[:find ?name
                                       :where
                                       [?e :maker/name "BMW"]
                                       [?c :car/maker ?e]
                                       [?c :car/name ?name]]
                                     @conn))

                          (l/pp (d/entity @conn-two [:car/model "E39530i"]))
                          (l/pp (d/entity @conn-two [:maker/email "ceo@bmw.com"]))
                          (l/pp (:maker/name (d/entity @conn-two [:maker/email "ceo@bmw.com"])))
                          (l/pp (d/q '[:find [?name ...]
                                       :where
                                       [?c :car/maker [:maker/email "ceo@bmw.com"]]
                                       [?c :car/name ?name]]
                                     @conn-two))

                          (l/pp (d/q '[:find ?e
                                       :where [?e :user/id]]
                                     @conn-three))
                          (l/pp (d/q '[:find ?e ?n
                                       :where
                                       [?e :user/id]
                                       [?e :user/name ?n]]
                                     @conn-three))

                          (l/pp (d/q '[:find [?e ...]
                                       :where
                                       [?e :user/id]]
                                     @conn-three))
                          (l/pp (d/q '[:find [?n ...]
                                       :where
                                       [?e :user/id]
                                       [?e :user/name ?n]]
                                     @conn-three))

                          (l/pp (d/q '[:find ?n .
                                       :where
                                       [?e :user/id]
                                       [?e :user/name ?n]]
                                     @conn-three))

                          ;;AUTONORMAL
                          (l/pp an-one)
                          (l/pp (get-in an-one [:person/id 1]))
                          (l/pp an-two)
                          (l/pp an-three)
                          (l/pp (a/pull an-three [[:person/id 1]]))
                          (l/pp (a/pull an-three [{[:person/id 1] [:person/name
                                                                   {:friend/best [:person/name]}]}]))
                          (l/pp (a/pull an-three [{:species [{:andalites [:person/name]}]}]))
                          (l/pp (= (-> (a/pull an-three query)
                                       (get [:person/id 0]))
                                   data))))))

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
