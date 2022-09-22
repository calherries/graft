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
                      [c (get-in db [r c])]))))))                         ; return the value from the entity

(defn transact
  "Updates the database with commands. Commands are s-exp vectors with :create, :merge, :dissoc, or :delete as the first element"
  [db & cs]
  (let [create (fn [d & es]
                 (reduce (fn [d' e]
                           (let [[idk id] (first (filter #(= (name (key %)) "id") e))]
                             (assoc d' [idk id] (dissoc e idk))))
                         d es))]
    (reduce (fn [d [o & xs]]
              (case o
                :create (apply create d xs)
                :merge  (apply update d (first xs) merge (rest xs))
                :dissoc (apply update d (first xs) dissoc (rest xs))
                :delete (dissoc d (first xs))))
            db cs)))
