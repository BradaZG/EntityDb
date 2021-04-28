(ns app.app
  (:require [keechma.next.controllers.router]
            [keechma.next.controllers.dataloader]
            [keechma.next.controllers.subscription]
            [app.controllers.test]
            ["react-dom" :as rdom]))

(defn page-eq? [page] (fn [{:keys [router]}] (= page (:page router))))

(defn role-eq? [role] (fn [deps] (= role (:role deps))))

(def homepage? (page-eq? "home"))

(defn slug [{:keys [router]}] (:slug router))

(def app
  {:keechma.subscriptions/batcher rdom/unstable_batchedUpdates,
   :keechma/controllers
   {:router       {:keechma.controller/params true
                   :keechma.controller/type   :keechma/router
                   :keechma/routes            [["" {:page "offers"}] ":page" ":page/:subpage"]}

    :entitydb #:keechma.controller {:params true
                                    :type :keechma/entitydb
                                    :keechma.entitydb/schema   {:user {:entitydb/relations {:languages {:entitydb.relation/path [:languages :*]
                                                                                                        :entitydb.relation/type :language}}}}}
    :test #:keechma.controller {:params true
                                :deps [:entitydb]}}})