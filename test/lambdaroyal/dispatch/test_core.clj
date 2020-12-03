(ns lambdaroyal.dispatch.test-core
  (:require [midje.sweet :refer :all]
            [lambdaroyal.dispatch.core :refer :all]))

(facts "test correct behaviour without jobs throwing errors"
       (let [c 10000
             threads 100
             max-job-postpone 100 ;; jobs are pending max 100 ms from now
             poll-frequency 50
             jobs (atom [])
             d (dispatcher :verbose false 
                           :frequency poll-frequency
                           :on-done (fn [job] (swap! jobs conj job)))
             res (atom [])]
         (doseq [x (range c)] 
           (dispatch d (rand-int threads) 
                     (+ (System/currentTimeMillis) (rand-int max-job-postpone))
                     #(swap! res conj x)))
         (Thread/sleep 1000)
         (stop d)
         (fact "all jobs must have been executed" (count @res) => c)
         (fact "each job is allowed to be executed just once" (-> @res distinct count) => c)
         (fact "no duplicates are allowed" (filter (fn [[k xs]] (> (count xs) 1)) (group-by identity @res)) => '())))


(facts "test correct behaviour with jobs throwing errors"
       (let [c 100000
             threads 10
             max-job-postpone 1000 ;; jobs are pending max 100 ms from now
             poll-frequency 100
             jobs (atom [])
             jobs-failed (atom [])
             d (dispatcher :verbose false 
                           :frequency poll-frequency
                           :on-failed (fn [job] (swap! jobs-failed conj job))
                           :on-done (fn [job] (swap! jobs conj job)))
             res (atom [])]
         (doseq [x (range c)] 
           (dispatch d (rand-int threads) 
                     (+ (System/currentTimeMillis) (rand-int max-job-postpone))
                     ;; every second job fails
                     (fn [] 
                       (io!
                        (if (= (mod x 2) 1)
                          (throw (Exception. "foo"))
                          (swap! res conj x))))))
         (Thread/sleep 2000)
         (stop d)
         (fact "all non-errornous jobs must have been executed" (count @res) => (/ c 2))
         (fact "each non-errournous job is allowed to be executed just once" (-> @res distinct count) => (/ c 2))
         (fact "no duplicates are allowed" (filter (fn [[k xs]] (> (count xs) 1)) (group-by identity @res)) => '())
         (fact "all errornous jobs must have been failed" (count @jobs-failed) => (/ c 2))
         (fact "each errournous job is allowed to fail just once" (-> @jobs-failed distinct count) => (/ c 2))
         (fact "no duplicates are allowed on errornous jobs" (filter (fn [[k xs]] (> (count xs) 1)) (group-by identity @jobs-failed)) => '())))

(dispatch d
          "Datasink" ;; Queuename
          (+ (System/currentTimeMillis) 1000) ;; execute in one minute
                     ;; every second job fails
                     (fn [] 
                       (io!
                        (if (= (mod x 2) 1)
                          (throw (Exception. "foo"))
                          (swap! res conj x)))))
