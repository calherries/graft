(ns demo
  (:require [core :as g]))

;; Add some data to an empty db
(def db (g/transact {} [:create
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

;; Recursive query
(g/query db {[:person/id 1] [:person/name
                           {:person/pets [:animal/name
                                          {:animal/vet [:person/name]}]}]})
;; => [#:person{:name "Fred",
;;              :pets [#:animal{:name "Catso", :vet (#:person{:name "Rich Hickey"})}
;;                     #:animal{:name "Doggy", :vet (#:person{:name "Rich Hickey"})}]}]
;; Add new facts to the database
(-> db
    (g/transact [:merge [:person/id 1] {:person/name "Freddy"}])
    (g/query {[:person/id 1] [:person/name]}))
;; => [#:person{:name "Freddy"}]

;; Remove new facts from the database
(-> db
    (g/transact [:dissoc [:person/id 1] :person/name :person/pets])
    (g/query {[:person/id 1] [:person/name :person/pets]}))
;; => [#:person{:name nil, :pets nil}]

;; Delete entities
(-> db
    (g/transact [:delete [:person/id 1]])
    (g/query {[:person/id 1] [:person/name]}))
;; => [#:person{:name nil}]
