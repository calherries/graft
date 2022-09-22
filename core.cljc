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
  (cond
    (map? entity)
    (vec (for [[k v] entity]
           (cond
             (map? v)
             {k (shape v)}
             (vector? v)
             {k (shape v)}
             :else
             k)))
    (vector? entity)
    (shape (first entity))))

;; API

(def empty-db {})

(defn db-with
  "Adds entities to the database."
  [db entities]
  (reduce
   (fn [db entity]
     (assoc db (entity-ref entity) entity))
   db
   entities))

(defn q
  "Queries the database. Supports multiple joins"
  [db query]
  (for [[id query-fields] query] ; assume every top-level key is an id
    (let [entity (get db id)]
      (into {}
            (for [field query-fields]
              (cond
                (map? field) ; join
                (let [[foreign-key
                       foreign-fields] (first field)
                      foreign-ids      (get entity foreign-key)
                      nested-query     (for [foreign-id foreign-ids]
                                         [foreign-id foreign-fields])]
                  [foreign-key (q db nested-query)])
                :else
                [field (get entity field)]))))))

(defn transact!
  "Updates the database."
  [db transaction]
  (reduce
   (fn [db' [operation id & args]]
     (case operation
       :merge   (update db' id merge (first args))
       :dissocs (apply update db' id dissoc (first args))
       :delete  (dissoc db' id)))
   db
   transaction))

(comment
  (def db (db-with empty-db
                   [#:person{:id   1
                             :name "Fred"
                             :pets [[:animal/id 3]
                                    [:animal/id 4]]}
                    #:person{:id   2
                             :name "Rich Hickey"}
                    #:animal{:id   3
                             :name "Catso"
                             :vet  [[:person/id 2]]}
                    #:animal{:id   4
                             :name "Doggy"
                             :vet  [[:person/id 2]]}]))

  (q db {[:person/id 1] [:person/name]})
  (q db {[:animal/id 3] [:animal/name]})
  (q db {[:animal/id 3] [:animal/field-that-doesnt-exist]})
  (q db {[:person/id 1] [:person/age
                         {:person/pets [:animal/name]}]})
  (= (q db {[:person/id 1] [:person/age
                            {:person/pets [:animal/name]}]})
     '(#:person{:age 52, :pets (#:animal{:name "Catso"} #:animal{:name "Doggy"})}))

  ;; recursive query
  (q db {[:person/id 1] [:person/name
                         {:person/pets [:animal/name
                                        {:animal/vet [:person/name]}]}]})
  ;; => (#:person{:name "Fred",
  ;;      :pets (#:animal{:name "Catso", :vet (#:person{:name "Jessica"})}
  ;;                     #:animal{:name "Doggy", :vet (#:person{:name "Jessica"})})})

  (def db empty-db)

  (-> db
      (transact! [[:merge [:person/id 1] {:person/name "Freddy"}]])
      (q {[:person/id 1] [:person/name]}))
  ;; => (#:person{:name "Freddy"})

  (-> db
      (transact! [[:dissocs [:person/id 1] [:person/name]]])
      (q {[:person/id 1] [:person/name]}))
  ;; => (#:person{:name nil})

  (-> db
      (transact! [[:delete [:person/id 1]]])
      (q {[:person/id 1] [:person/name]}))
  ;; => (#:person{:name nil})

  )
