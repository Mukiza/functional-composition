;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Functional Composition                       ;;
;;                                              ;;
;; Chris Ford (ThoughtWorks)                    ;;
;; http://github.com/ctford/goldberg            ;;
;; http://github.com/overtone/overtone          ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns goldberg.variations.canone-alla-quarta
  (:use
    [clojure.repl]
    [overtone.live :only
      [midi->hz stop at now definst sin-osc env-gen asr pluck line FREE perc]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sound                                        ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(definst tone# [frequency 440] (sin-osc frequency))
(definst beep# [frequency 440 duration 1000]
  (let [envelope (env-gen (line 1.0 0 (/ duration 1000) FREE))]
          (* envelope (sin-osc frequency))))

(definst bell# [frequency 440 duration 4.0]
  (let [harmonic-decay [1.0 0.5 0.25 0.125]] 
         (map-indexed
           (fn [harmonic proportion]
             (let [envelope (env-gen (perc 0.01 (* duration proportion)))
                   overtone (* (inc harmonic) frequency)]
               (* envelope proportion (sin-osc overtone))))
          harmonic-decay)))

;(tone#)
;(beep#)
;(bell#)
;(stop)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Melody                                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn play# [notes] 
  (let [play-at# (fn [[ms midi]] (at ms (bell# (midi->hz midi))))]
    (->> notes (map play-at#) dorun)
    notes))

(defn even-melody# [pitches]
  (let [times (reductions + (cons (now) (repeat 400)))
        notes (map vector times pitches)]
    (play# notes)))

;(piano# 55)
;(even-melody# (range 60 67))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scale                                        ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sum-n [series n] (reduce + (take n series)))

(defn scale [intervals]
  #(if-not (neg? %)
     (sum-n (cycle intervals) %)
     ((comp - (scale (reverse intervals)) -) %)))

(def major (scale [2 2 1 2 2 2 1]))
(def minor (scale [2 1 2 2 1 2 2]))

(defn start-from [base] (partial + base))
(def G (start-from 67))
(def D (start-from 74))
(def G-minor (comp G minor))

;(even-melody#
;  (let [_ -100]
;    (map (comp D major) [0 1 2 0, 0 1 2 0, 2 3 4 _, 2 3 4 _]))
;)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Abstractions                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn bpm [beats] (fn [beat] (-> beat (/ beats) (* 60) (* 1000))))
;((bpm 120) 3)

(defn run [[from & tos]]
  (if-let [to (first tos)]
    (let [up-or-down (if (<= from to)
                       (range from to)
                       (reverse (range (inc to) (inc from))))]
      (concat up-or-down (run tos)))
    [from]))

;(even-melody# (map (comp G major)
;            (run [0 4 -1 0 1 0])
;            ))

(defn accumulate [series] (map (partial sum-n series) (range (count series))))
(def repeats (partial mapcat #(apply repeat %)))
(def runs (partial mapcat run))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Melody                                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def melody 
  (let [call
          [(repeats [[2 1/4] [1 1/2] [14 1/4] [1 3/2]])
          (runs [[0 -1 3 0] [4] [1 8]])]
        response
          [(repeats [[10 1/4] [1 1/2] [2 1/4] [1 9/4]])
          (runs [[7 -1 0] [0 -3]])]
        development
          [(repeats [[1 3/4] [12 1/4] [1 1/2] [1 1] [1 1/2] [12 1/4] [1 3]])
          (runs [[4] [4] [2 -3] [-1 -2] [0] [3 5] [1] [1] [1 2] [-1 1 -1] [5 0]])]
        [durations pitches] (map concat call response development)
        timings (map (partial + 1/2) (accumulate durations))]
    (map vector timings pitches)))

(def bass
  (let [triples (partial mapcat #(repeat 3 %))]
    (map vector
       (accumulate (repeats [[21 1] [13 1/4]]))
       (concat (triples (runs [[-7 -10] [-12 -10]])) (run [5 -7])))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Canone alla quarta - Johann Sebastian Bach   ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn canon [f] (fn [notes] (concat notes (f notes))))

(def timing 0)
(def pitch 1)
(defn skew [k f] (fn [points] (map #(update-in % [k] f) points))) 
(defn shift [point] (fn [points] (map #(->> % (map + point) vec) points)))

(defn simple [wait] (shift [wait 0]))
(defn interval [interval] (shift [0 interval]))
(def mirror (skew pitch -))
(def crab (skew timing -))
(def table (comp mirror crab))

(def canone-alla-quarta (canon (comp (interval -3) mirror (simple 3))))

(defn canon# [start tempo scale]
  (let [in-time (comp (shift [start 0]) (skew timing tempo))
        in-key (skew pitch scale)
        play-now# (comp play# in-key in-time)]

    (-> bass play-now#)
    (-> melody canone-alla-quarta play-now#)))

;(canon# (now) (bpm 90) (comp D major))
