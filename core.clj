(ns core)

(defn entity-ref [entity]
  (first (filter (fn [[k _]] (= (name k) "id")) entity)))

(defn shape [entity]
  (if (map? entity)
    (vec (for [[k v] entity]
           (cond
             (map? v)
             {k (shape v)}
             (vector? v)
             {k (shape v)}
             :else
             k)))
    ;; else vector?
    (shape (first entity))))

(comment
  (def entity {:person/id   1
               :person/name "hello"})
  (entity-ref entity) ;; => [:person/id 1]
  (shape entity) ;; => [:person/id :person/name]

  (def entity {:person/id   1
               :person/pet {:pet/name "Dotty"}})
  (shape entity) ;; => [:person/id #:person{:pet [:pet/name]}]

  (def entity {:person/id   1
               :person/pets [{:pet/id   2
                              :pet/name "Dotty"}
                             {:pet/id 3
                              :pet/name "Hello"}]})
  (shape entity) ;; => [:person/id #:person{:pets [:pet/id :pet/name]}]
  )

;; API

(defn empty-db []
  {})

(defn db-with [db entities]
  (into {} (for [entity entities]
             [(entity-ref entity) entity])))

(comment
  (def entity {:person/id   1
               :person/name "hello"})
  (def db (db-with (empty-db) [{:person/id   1
                                :person/name "Fred"
                                :person/age  52
                                :person/pets [[:animal/id 3]
                                              [:animal/id 4]]}
                               {:person/id   2
                                :person/name "Dr. Rich"
                                :person/age  25}
                               {:person/id   2
                                :person/name "Jessica"}
                               {:animal/id   3
                                :animal/name "Catso"
                                :animal/vet  [[:person/id 2]]}
                               {:animal/id   4
                                :animal/name "Doggy"
                                :animal/vet  [[:person/id 2]]}]))

  {[:customer/id 123] [:customer/name :customer/email]}
  )

(defn p [x] (do (prn x) x))

(defn q
  "Queries the database. Supports multiple joins"
  [db query]
  (for [[id query-fields] query] ; assume every key is an id
    (let [entity (get db id)]
      (into {}
            (for [field query-fields]
              (cond
                (map? field)
                ; join
                (let [[foreign-key foreign-fields] (first field)
                      foreign-ids                  (get entity foreign-key)]
                  [foreign-key (for [foreign-id foreign-ids]
                                 (let [foreign-entity (get db foreign-id)]
                                   (into {}
                                         (for [foreign-field foreign-fields]
                                           [foreign-field (get foreign-entity foreign-field)]))))])
                :else
                [field (get entity field)]))))))

(comment
  (q db {[:person/id 1] [:person/name :person/age]})
  (q db {[:animal/id 3] [:animal/name]})
  (q db {[:person/id 1] [:person/age
                         {:person/pets [:animal/name]}]})
  (= (q db {[:person/id 1] [:person/age
                            {:person/pets [:animal/name]}]})
     '(#:person{:age 52, :pets (#:animal{:name "Catso"} #:animal{:name "Doggy"})}))

  ;; recursive query
  (q db {[:person/id 1] [:person/age
                         {:person/pets [:animal/name
                                        {:animal/vet [:person/name]}]}]})
  )