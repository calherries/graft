(ns core)

(defn -entity-ref [e]
  (let [id? (fn [k] (= (name k) "id"))]
    (first (filter (comp id? key) e))))

(comment
  (-entity-ref #:person{:id   1
                        :name "Fred"
                        :pets [[:animal/id 3]
                               [:animal/id 4]]})
  ;; => [:person/id 1]
  )

;; API

(defn db-with
  "Adds entities to the database."
  [db entities]
  (reduce
   (fn [db e]
     (assoc db (-entity-ref e) e))
   db
   entities))

(defn q
  "Queries the database recursively."
  [db query]
  (for [[r cs] query]                           ; every top-level key is an entity ref
    (let [e (get db r)]                         ; get the entity
      (into {} (for [c cs]                      ; for each connection
                 (cond
                   (map? c)                      ; if c is a join
                   (let [[c' jcs] (first c)      ; c' is a field and jcs is a list of join cs
                         jrs      (get e c')     ; joined entity refs
                         jquery   (into {} (map #(vector %1 jcs) jrs))
                         jres     (q db jquery)] ; get the results of the nested query
                     [c' jres])
                   (keyword? c)                 ; if c is a field
                   [c (get e c)]))))))          ; the field value from the entity

(defn transact
  "Updates the database with a transaction t"
  [db t]
  (reduce
   (fn [db' [o r & xs]]
     (case o
       :merge  (apply update db' r merge xs)
       :dissoc (apply update db' r dissoc xs)
       :delete (dissoc db' r)))
   db
   t))

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
  (= (q db {[:person/id 1] [:person/name
                            {:person/pets [:animal/name]}]})
     '(#:person{:name "Fred", :pets (#:animal{:name "Catso"} #:animal{:name "Doggy"})}))

  ;; recursive query
  (q db {[:person/id 1] [:person/name
                         {:person/pets [:animal/name
                                        {:animal/vet [:person/name]}]}]})

  ;; => (#:person{:name "Fred",
  ;;      :pets (#:animal{:name "Catso", :vet (#:person{:name "Rich Hickey"})}
  ;;                     #:animal{:name "Doggy",
  ;;                      :vet (#:person{:name "Rich Hickey"})})})

  (def db empty-db)

  (-> db
      (transact [[:merge [:person/id 1] {:person/name "Freddy"}]])
      (q {[:person/id 1] [:person/name]}))
  ;; => (#:person{:name "Freddy"})

  (-> db
      (transact [[:dissoc [:person/id 1] :person/name :person/pets]])
      (q {[:person/id 1] [:person/name :person/pets]}))
  ;; => (#:person{:name nil})

  (-> db
      (transact [[:delete [:person/id 1]]])
      (q {[:person/id 1] [:person/name]}))
  ;; => (#:person{:name nil})

  )
