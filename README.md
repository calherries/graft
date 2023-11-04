# Graft

A 30 LOC in-memory graph database implementation. Supports EQL queries.

Usage:
```clojure
;; Add some data to an empty db
(def db (transact {} [:create
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
                               :vet  [[:person/id 2]]}]))

;; Query with joins
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

```
