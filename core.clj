(ns core)

(defn entity-ref [entity]
  (first (filter (fn [[k _]] (= (name k) "id")) entity)))

(defn entity-ref? [x]
  (and (vector? x)
       (= (count x) 2)
       (let [[id-key id-value] x]
         (and (keyword? id-key)
              (= (name id-key) "id"))
         (int? id-value))))

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

;; API

(defn empty-db []
  {})

(defn db-with [db entities]
  (reduce
   (fn [db entity]
     (assoc db (entity-ref entity) entity)
   {}
   entities))



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
                  [foreign-key (q db (for [foreign-id foreign-ids]
                                       [foreign-id foreign-fields]))])
                :else
                [field (get entity field)]))))))

(comment
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
  ;; => (#:person{:age 52,
  ;;      :pets (#:animal{:name "Catso", :vet (#:person{:name "Jessica"})}
  ;;                     #:animal{:name "Doggy", :vet (#:person{:name "Jessica"})})})

  )

(defn transact! [db transaction]
  "Updates the database with data."
  (reduce
   (fn [db' [id operation-map]]
     (reduce
      (fn [db'' [operation args]]
        (case operation
          :add (update db'' id merge args)))
      db'
      operation-map))
   db
   transaction))

(comment
  (-> db
      (transact! {[:person/id 1] {:add {:person/name "Freddy"}}})
      (q {[:person/id 1] [:person/name]}))
  ;; => (#:person{:name "Freddy"})
  )
