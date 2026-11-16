#!/usr/bin/env clojure

(ns session-store.core)

;; ============================================================================
;; 1. SHARED CONCURRENT STATE (ATOM) & INITIAL DUMMY DATA
;; ============================================================================
;; An Atom holds an immutable Clojure map of {session-id -> session-data}.
(def session-store
  (atom {}
        :validator (fn [state]
                     (and (map? state)
                          (every? #(>= (:requests % 0) 0) (vals state))))))

(defn load-dummy-data!
  "Initializes the atom with pre-loaded dummy sessions representing pre-existing users."
  []
  (let [now (System/currentTimeMillis)
        dummy-sessions
        {"sess-001" {:user-id "alice_guest" :status :active :requests 5  :last-active (- now 120000)}
         "sess-002" {:user-id "bob_admin"   :status :active :requests 24 :last-active (- now 60000)}
         "sess-003" {:user-id "charlie_dev" :status :expired :requests 45 :last-active (- now 300000)}
         "sess-004" {:user-id "dave_support":status :active :requests 10 :last-active (- now 40000)}}]
    (reset! session-store dummy-sessions)
    (println "[INIT] Loaded pre-existing dummy sessions into store:")
    (doseq [[sid data] (sort dummy-sessions)]
      (println (format "  %s -> %s" sid data)))
    (println "")))

;; ============================================================================
;; 2. PURE FUNCTIONS & REFERENTIAL TRANSPARENCY
;; ============================================================================
(defn create-session
  [sessions session-id user-id timestamp]
  (assoc sessions session-id {:user-id user-id
                              :status :active
                              :requests 1
                              :last-active timestamp}))

(defn record-activity-safe
  [sessions session-id timestamp]
  (if-let [session (get sessions session-id)]
    (if (= :active (:status session))
      (assoc sessions session-id (-> session
                                     (update :requests inc)
                                     (assoc :last-active timestamp)))
      sessions)
    sessions))

(defn expire-session
  [sessions session-id]
  (if (contains? sessions session-id)
    (assoc-in sessions [session-id :status] :expired)
    sessions))

;; ============================================================================
;; 3. HIGHER-ORDER FUNCTIONS & REPORTING (STATELESS COMPUTATION)
;; ============================================================================
(defn active-sessions-count
  [snapshot]
  (->> (vals snapshot)
       (filter #(= :active (:status %)))
       count))

;; ============================================================================
;; 4. CONCURRENT WORKERS & READER SIMULATION
;; ============================================================================
(defn simulate-user-traffic
  [session-id user-id iterations]
  
  (let [sid session-id
        uid user-id
        iters iterations]
    (future
      ;; 1. Create new session via swap! and log operation
      (swap! session-store create-session sid uid (System/currentTimeMillis))
      (println (format "   [OPERATION] CREATE -> Session '%s' created for user '%s'" sid uid))
      
      ;; 2. Simulate active usage loop with operation logging
      (dotimes [i iters]
        (Thread/sleep (+ 80 (rand-int 100)))
        (swap! session-store record-activity-safe sid (System/currentTimeMillis))
        (println (format "   [OPERATION] UPDATE -> Activity recorded for session '%s' (Iteration %d/%d)" 
                         sid (inc i) iters)))
      
      ;; 3. Expire session and log operation
      (Thread/sleep 200)
      (swap! session-store expire-session sid)
      (println (format "   [OPERATION] EXPIRE -> Session '%s' marked as expired" sid)))))

(defn start-consistent-reader
  [stop-flag]
  (future
    (while (not @stop-flag)
      (Thread/sleep 1000)
      (let [snapshot @session-store
            total-sessions (count snapshot)
            active-count (active-sessions-count snapshot)
            total-reqs (reduce + 0 (map #(get % :requests 0) (vals snapshot)))]
        (println (format "\n[READER SNAPSHOT] Total Sessions: %d | Active: %d | Total Requests Processed: %d\n"
                         total-sessions active-count total-reqs))))))

;; ============================================================================
;; 5. MAIN ENTRY POINT & INTERACTIVE TERMINATION
;; ============================================================================
(let []
  (println "============================================================")
  (println " Starting Concurrent Session Store Simulation               ")
  (println "============================================================")

  ;; 1. Initialize Dummy Data
  (load-dummy-data!)

  ;; 2. Start the background reader thread
  (let [stop-reader (atom false)
        reader-task (start-consistent-reader stop-reader)
        
        ;; Define active traffic simulation threads
        active-users [["sess-101" "diana_user" 5]
                      ["sess-102" "evan_user" 6]
                      ["sess-103" "fiona_user" 4]
                      ["sess-104" "george_user" 7]
                      ["sess-105" "hannah_user" 5]]
        
        writer-tasks (mapv (fn [[sid uid iters]]
                             (simulate-user-traffic sid uid iters))
                           active-users)]

    (println "\nSimulation running with concurrent background threads.")

    ;; 3. Wait for all writer threads to finish executing their operations
    (run! deref writer-tasks)
    (println "\n[INFO] All simulation operations have successfully completed.")

    ;; 4. Stop the background reader thread
    (reset! stop-reader true)
    @reader-task

    ;; 5. Prompt user to press Enter before displaying final state
    (println "\nPress [ENTER] to print final consistent state and exit...")
    (flush)
    (read-line)

    ;; 6. Display final consistent state and shut down
    (println "------------------------------------------------------------")
    (println "Final Consistent State (@session-store):")
    (doseq [[sid data] (sort @session-store)]
      (println (format "  %s -> %s" sid data)))

    (println "============================================================")
    (println " Application successfully terminated.                       ")
    (println "============================================================")
    
    (shutdown-agents)))
