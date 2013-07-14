(ns hesokuri.test-hesokuri.util
  (:use clojure.test
        hesokuri.util))

(deftest test-sh-print-when
  (let [result (atom {})

        -sh-print-when
        (fn [print-when & args]
          (swap! result #(dissoc % :printed :return))
          (let [return (apply sh-print-when print-when args)]
            (swap! result #(assoc % :return return))))]
    (binding [*sh* (fn [x y] (if (< x y)
                               {:exit 1 :err "err" :out "out"}
                               {:exit 0
                                :err (format "err: %d" (- x y))
                                :out (format "out: %d" (- x y))}))

              *print-for-sh*
              (fn [args stderr stdout]
                (swap! result
                       #(assoc % :printed
                               {:args args :stderr stderr :stdout stdout})))]
      (are [args exp-return exp-stderr exp-stdout]
           (= (apply -sh-print-when args)
              {:return exp-return
               :printed {:args (rest args)
                         :stderr exp-stderr
                         :stdout exp-stdout}})
           [(constantly true) 5 4] 0 "err: 1" "out: 1"))))
