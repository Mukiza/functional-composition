;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Canon Fodder - Chris Ford                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns overtunes.songs.canone-alla-quarta
  (:use
    [overtone.live :only [at now]]
    [overtone.inst.sampled-piano :only [sampled-piano] :rename {sampled-piano piano#}]))

(defn => [val & fs] (reduce #(apply %2 [%1]) val fs))

(defn play# [notes] 
  (let [play-at# (fn [[ms midi]] (at ms (piano# midi)))]
    (->> notes (map play-at#) dorun)))

(defn demo# [pitches]
  (let [start (now)
        note-length 300
        end (+ start (* note-length (count pitches)))
        notes (map vector (range start end note-length) pitches)]
    (play# notes)))

;(demo# (range 60 73))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scale                                        ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sum-n [series n] (reduce + (take n series)))
(defn scale [intervals]
  #(if (not (neg? %))
     (sum-n (cycle intervals) %)
     (=> % - (scale (reverse intervals)) -)))

(def major (scale [2 2 1 2 2 2 1]))
(def minor (scale [2 1 2 2 1 2 2]))
(def g-major (comp (partial + 74) major)) 

;(major 2)
;(minor 2)
;(demo# (let [key (comp (partial + 67) major), rest -100]
;         (map key [0 1 2 0 0 1 2 0 2 3 4 rest 2 3 4 rest])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Melody                                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn run [[a & bs]] 
  (let [up-or-down
          #(if (<= %1 %2)
            (range %1 %2)
            (reverse (range (inc %2) (inc %1))))]
    (if bs
      (concat
        (up-or-down a (first bs))
        (run bs))
      [a])))

(defn sums [series] (map (partial sum-n series) (range (count series))))
(def repeats (partial mapcat (partial apply repeat)))
(def runs (partial mapcat run))

;(demo# (map g-major
;            (run [0 3 1 3 -1 0])
;            ))

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
        line
          (map concat call response development)]
    (map vector (sums (nth line 0)) (nth line 1))))

;melody

(def bassline
  (let [triples (partial mapcat (partial repeat 3))]
    (map vector
       (sums (repeats [[21 1] [12 1/4]]))
       (concat (triples (runs [[0 -3] [-5 -3]])) (run [12 0])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Canone alla quarta - Johann Sebastian Bach   ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn bpm [per-minute] #(-> % (/ per-minute) (* 60) (* 1000)))

(defn transform [k f]
 (let [map-in (fn [m k f] (map #(update-in % [k] f) m))] 
   #(map-in % k f)))

(defn shift [point]
  (let [offset (fn [point1 point2] (vec (map + point1 point2)))]
        (partial map (partial offset point))))

(defn canone-alla-quarta# []
  (let [[timing pitch] [0 1]
        in-time (comp (shift [(now) 0]) (transform timing (bpm 90)))
        in-key (transform pitch g-major)
        play-now# (comp play# in-key in-time)]

    (=> bassline (shift [0 -7]) play-now#)
    (=> melody (shift [1/2 0]) play-now#)
    (=> melody (transform pitch -) (shift [7/2 -3]) play-now#)))

(canone-alla-quarta#)
