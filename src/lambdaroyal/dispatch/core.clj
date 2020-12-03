(ns lambdaroyal.dispatch.core)

(defprotocol PDispatcher
  (dispatch [this queue es lambda] "dispatches a job for execution. queue is the name of the queues to dispatch the job to, es is the execution time (ET) as unix epoch, lambda λ is a parameterless function that gets called when the job to be executed")
  (pause [this queue] "pauses the execution of jobs for the queue with name queue")
  (resume [this queue] "resumes executing jobs in a given queue with name queue")
  (stop [this] "stop the dispatch from accepting new jobs, all currently running queue workers will eventually terminate. returns a promise (TODO: that gets realized once all jobs queue workers finished"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn uid
  "returns a random key of length 9 containing [1..9 a..z A..Z] except for 0,I,O,l"
  ([]
   (let [xs (map char (concat (range 49 58) (range 65 73) (range 74 79) (range 80 91) (range 97 108) (range 109 123)))]
     (apply str (repeatedly 12 #(rand-nth xs))))))


(defmacro when-let*
          [bindings & body]
          `(let ~bindings
                (if (and ~@(take-nth 2 bindings))
                  (do ~@body)
                  )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; factory
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dispatcher 
  "Creates a dispatcher. Optional params are:
  :on-done :: job -> nil
  :on-failed :: {:keys [started-at stopped-at :error-message :id] :as job} -> nil
  :on-start :: job -> nil
  :frequency (default 1000) using this frequency we poll for jobs to execute
  :verbose (default false)"
  [& params]
  (let [{:keys [frequency verbose] 
         :or {frequency 1000 verbose false} :as params} (if params (apply hash-map params) {})
        {:keys [on-done on-failed on-start] 
         :or {on-done (fn [job] (if verbose (println (format "[Dispatcher] job [%s] done. execution (ms) [%s]" (:id job) (- (:stopped-at job) (:started-at job))))))
              on-failed (fn [job] (if verbose (println (format "[Dispatcher] job [%s] failed. execution (ms) [%s] error message [%s]" (:id job) (- (:stopped-at job) (:started-at job)) (:error-message job)))))
              on-start (fn [job] (if verbose (println (format "[Dispatcher] job [%s] started" (:id job)))))}} params
        stopped (atom false)
        queues (ref {})

        ;;executes a job within a bucket
        execute-pending-jobs (fn [queue jobs]                      
                               (future
                                 (try
                                   (locking queue
                                     (doseq [job jobs]
                                       (let [job (assoc job :started-at (System/currentTimeMillis))]
                                         (on-start job)
                                         (try
                                           ((:lambda job))
                                           (on-done (assoc job :stopped-at (System/currentTimeMillis)))
                                           (catch Exception e 
                                             (on-failed (assoc job :error-msg (.getMessage e) :stopped-at (System/currentTimeMillis))))))))
                                   (catch Exception e (do (if verbose (.printStackTrace e)) (throw e))))))

        check-for-pending-jobs (fn [queue]
                                   (try
                                     (loop []
                                       (let [pending-jobs (map last
                                                               (dosync
                                                                (let [pending-seq (subseq @queue < [(System/currentTimeMillis) nil nil])]
                                                                  (doseq [[key job] pending-seq]
                                                                    (alter queue dissoc key))
                                                                  pending-seq)))]
                                         (if-not (empty? pending-jobs)
                                           (do
                                             (if verbose (println (format "[Dispatcher/check-for-pending-jobs] %s jobs are now pending to execute" (count pending-jobs))))
                                             (execute-pending-jobs queue pending-jobs)
                                             (recur)))))                                     
                                     (catch Throwable t 
                                       (do
                                         (if verbose (.printStackTrace t))
                                         (throw t)))))

        ;;adds a ref watch to a queue, which is a ref to a sorted-map
        unique-watchers (atom #{})
        queue-ref-watcher (fn  
                            [queue-name queue]
                            (add-watch queue :queue-watch (fn [key _ old-state new-state]
                                                            (if (empty? old-state)
                                                              (future
                                                                (if (contains? @unique-watchers queue-name) (throw (IllegalStateException. "Cannot register second ref-watcher on queue [%a]" queue-name)))
                                                                (swap! unique-watchers conj queue-name)
                                                                (if verbose (println (format "[Dispatcher] started worker for queue [%s]" queue-name)))
                                                                (loop []
                                                                  (if (-> stopped deref false?)
                                                                    (do
                                                                      (check-for-pending-jobs queue)
                                                                      (Thread/sleep frequency)
                                                                      (recur)))))))))

        orderable-unique-job-queue (fn [es {id :id sort :sort :or {sort 0}}]
                                     [es sort id])

        add-to-queue (fn [queue-name es job]
                       (let [job (assoc job :queue queue-name)
                             add-job (fn [queue]
                                       (alter queue assoc (orderable-unique-job-queue es job) job))]
                         (if-let [queue' (get @queues queue-name)]
                           (add-job queue')
                           ;; no queue for queue-name in queues, create a new one
                           ;; that actually is sorted-map (sorted by execution time, values are buckes for jobs)
                           (let [queue' (ref (sorted-map))
                                 _ (queue-ref-watcher queue-name queue')
                                 _ (add-job queue')]
                             (alter queues assoc queue-name queue'))))
                       job)]
    (reify PDispatcher
      (dispatch [this queue es lambda]
        (if (-> stopped deref true?) (throw (IllegalStateException. "Failed to dispatch job on stopped dispatcher")))
        (let [job {:id (uid) :es es :lambda lambda}]
          (dosync
           (add-to-queue queue es job))))

      (pause [this queue] nil)
      (resume [this queue] nil)
      (stop [this] (reset! stopped true)))))
