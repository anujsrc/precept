(ns todomvc.util-test
    (:require [clojure.test :refer [deftest testing is run-tests]]
              [todomvc.tuplerules :refer [def-tuple-session]]
              [clara.tools.inspect :as inspect]
              [clara.tools.tracing :as trace]
              [todomvc.util :refer :all]
              [clara.rules :refer [query defquery]]
              [clara.tools.tracing :as trace]))

(defquery find-all []
  [:all [[e a v]] (= ?e e) (= ?a a) (= ?v v)])

(defn todo-tx [id title done]
  (merge
    {:db/id      id
     :todo/title title}
    (when-not (nil? done)
      {:todo/done done})))

(defn is-tuple? [x]
  (and (vector x)
    (= (count x) 3)
    (keyword? (second x))))

(deftest map->tuples-test
  (testing "Converting an entity map to a vector of tuples"
    (let [entity-map (todo-tx (java.util.UUID/randomUUID) "Hi" :tag)
          entity-vec (map->tuples entity-map)]
      (is (map? entity-map)
        "Should receive a hashmap as input")
      (is (vector? entity-vec)
        "Should return a vector")
      (is (every? is-tuple? entity-vec)
        "Every entry in main vector should pass the tuple test")
      (is (every? #(= (first %) (:db/id entity-map)) entity-vec)
        "Every entry should use the :db/id from the map for its eid"))))

(deftest insertable-test
  (let [m-fact  (todo-tx (java.util.UUID/randomUUID) "Hi" :tag)
        m-facts (into [] (repeat 5 m-fact))]
    (is (every? is-tuple? (insertable m-fact)))
    (is (every? is-tuple? (insertable m-facts)))
    (is (every? is-tuple? (insertable (insertable m-fact))))
    (is (every? is-tuple? (insertable (insertable m-facts))))))

(deftest insert-test
  (let [session @(def-tuple-session mysess)
        numfacts 5
        m-fact  (todo-tx (java.util.UUID/randomUUID) "Hi" :tag)
        m-facts (into [] (repeat numfacts m-fact))
        trace (trace/get-trace (-> session
                                (trace/with-tracing)
                                (insert m-facts)))]
    (is (= :add-facts (:type (first trace))))
    (is (= (count (:facts (first trace)))
           (* numfacts (dec (count (keys m-fact))))))))

(run-tests)
