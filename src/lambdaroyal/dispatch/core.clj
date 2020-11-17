(ns lambdaroyal.dispatch.core)

(defprotocol PDispatcher
  (dispatch [queue es lambda] "dispatches a job for execution. queue is the name of the queues to dispatch the job to, es is the execution time (ET) as unix epoch, lambda λ is a parameterless function that gets called when the job to be executed")
  (pause [queue] "pauses the execution of jobs for the queue with name queue")
  (resume [queue] "resumes executing jobs in a given queue with name queue"))
