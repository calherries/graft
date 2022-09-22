# Graft

A proof-of-concept, <100 LOC, EQL in-memory graph database.

To show how simple graphs can be.

Usage:
```clojure
(require '[graft.core :as g])

(def db (g/db-with g/empty-db
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

;; Recursive queries
(g/q db {[:person/id 1] [:person/name
                         {:person/pets [:animal/name {:animal/vet [:person/name]}]}]})
;; => (#:person{:name "Fred",
;;              :pets (#:animal{:name "Catso", :vet (#:person{:name "Rich Hickey"})}
;;                     #:animal{:name "Doggy", :vet (#:person{:name "Rich Hickey"})})})

;; Adding data
(-> db
    (g/transact [[:merge [:person/id 1] {:person/name "Freddy"}]])
    (g/q {[:person/id 1] [:person/name]}))
;; => (#:person{:name "Freddy"})

;; Removing data
(-> db
    (g/transact [[:dissoc [:person/id 1] :person/name]])
    (g/q {[:person/id 1] [:person/name]}))
;; => (#:person{:name nil})
```
