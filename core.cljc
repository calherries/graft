(ns core)

(defn query
  "Queries the database recursively."
  [db q]
  (vec (for [[r cs] q]                                                    ; every top-level key is an entity ref
         (into {} (for [c cs]                                             ; for each connection
                    (cond
                      (map? c)                                            ; if c is a join
                      (let [[c' jcs] (first c)                            ; destructure into join field and join cs
                            jrs      (get-in db [r c'])                   ; get joined entity refs
                            jq       (into {} (map #(vector % jcs) jrs))] ; query for each joined entity
                        [c' (query db jq)])                               ; recursively query
                      (keyword? c)                                        ; if c is a field
                      [c (get-in db [r c])]))))))                         ; the field value from the entity

(defn transact
  "Updates the database with commands. Commands are s-exp vectors with :create, :merge, :dissoc, or :delete as the first element"
  [db & cs]
  (let [create (fn [db & es]
                 (reduce (fn [d e]
                           (let [[idk id] (first (filter #(= (name (key %)) "id") e))]
                             (assoc d [idk id] (dissoc e idk))))
                         db es))]
    (reduce (fn [d [o & xs]]
              (case o
                :create (apply create d xs)
                :merge  (apply update d (first xs) merge (rest xs))
                :dissoc (apply update d (first xs) dissoc (rest xs))
                :delete (dissoc d (first xs))))
            db cs)))

(comment
  ;; Add some data to an empty db
  (def db (-> {}
              (transact [:create
                         #:person{:id   1
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
                                  :vet  [[:person/id 2]]}])))

  ;; Recursive query
  (query db {[:person/id 1] [:person/name
                             {:person/pets [:animal/name
                                            {:animal/vet [:person/name]}]}]})
  ;; => [#:person{:name "Fred",
  ;;              :pets [#:animal{:name "Catso", :vet (#:person{:name "Rich Hickey"})}
  ;;                     #:animal{:name "Doggy", :vet (#:person{:name "Rich Hickey"})}]}]

  ;; Add new facts to the database
  (-> db
      (transact [:merge [:person/id 1] {:person/name "Freddy"}])
      (query {[:person/id 1] [:person/name]}))
  ;; => [#:person{:name "Freddy"}]

  ;; Remove new facts from the database
  (-> db
      (transact [:dissoc [:person/id 1] :person/name :person/pets])
      (query {[:person/id 1] [:person/name :person/pets]}))
  ;; => [#:person{:name nil, :pets nil}]

  ;; Delete entities
  (-> db
      (transact [:delete [:person/id 1]])
      (query {[:person/id 1] [:person/name]}))
  ;; => [#:person{:name nil}]

  )
