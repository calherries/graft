(ns core)

(defn ref [entity]
  (first (filter (fn [[k _]] (= (name k) "id")) entity)))

(defn shape [entity]
  (vec (for [[k v] entity]
         (cond
           (map? v)
           {k (shape v)}
           (vector? v)
           k))))

(comment
  (ref entity) ;; => [:person/id 1]

  (def entity {:person/id   1
               :person/name "hello"})
  (shape entity) ;; => [:person/id :person/name]

  (def entity {:person/id   1
               :person/pet {:pet/name "Dotty"}})
  (shape entity) ;; => [:person/id #:person{:pet [:pet/name]}]

  (def entity {:person/id   1
               :person/pets [{:pet/id   2
                              :pet/name "Dotty"}
                             {:pet/id 3
                              :pet/name "Hello"}]})
  )

;; API

(defn empty-db []
  {})

(defn db-with [db entities]
  (into {} (for [entity entities]
             [(ref entity) entity])))

(comment
  (def entity {:person/id   1
               :person/name "hello"})
  (def db (db-with (empty-db) [{:person/id   1
                                :person/name "Fred"
                                :person/age  52}
                               {:person/id   2
                                :person/name "Jessica"}])))

{[:customer/id 123] [:customer/name :customer/email]}

(defn q [db query]
  (for [[k v] query] ; assume every key is an id
    (let [entity (get db k)]
      (select-keys entity v))))

(comment
  (q db {[:person/id 1] [:person/name :person/age]})
  )