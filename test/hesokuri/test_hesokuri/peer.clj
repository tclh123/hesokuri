; Copyright (C) 2013 Google Inc.
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;    http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns hesokuri.test-hesokuri.peer
  (:use [clojure.java.io :only [file]]
        clojure.test
        hesokuri.test-hesokuri.mock
        hesokuri.util)
  (:require [hesokuri.peer :as peer]))

(def ^:dynamic peer-repo (->PeerRepo "repohost" "/repopath"))

(def ^:dynamic local-repo {:dir (file "/local-path") :bare false :init true})

(defn push [peer]
  (peer/push
   peer
   local-repo
   peer-repo
   "branch-name"
   "hash"
   [["push-branch" :push-arg]]))

(defn accessible-args []
  [(:host peer-repo) (peer/default :timeout-for-ping)])

(defn push-but-fail-ping [peer]
  (with-redefs [peer/accessible (mock {(accessible-args) [false]})
                current-time-millis (mock {[] [42 43 44 45]})]
    (let [peer (-> peer push push push push)]
      (is (= 42 (peer :last-fail-ping-time)))
      peer)))

(deftest retrying-unresponsive-peer
  (let [peer (push-but-fail-ping peer/default)]
    (with-redefs [peer/accessible (mock {(accessible-args) [true]})

                  current-time-millis
                  (mock {[] [(+ 46 (peer/default :minimum-retry-interval))]})

                  sh-print (constantly 0)]
      (let [peer (push peer)]
        (is (= "hash" (get-in peer [:pushed [local-repo "branch-name"]])))
        (is (nil? (peer :last-fail-ping-time)))))))

(deftest clear-fail-ping-even-when-failing-push
  (let [peer (push-but-fail-ping peer/default)]
    (with-redefs [hesokuri.peer/accessible (mock {(accessible-args) [true]})

                  current-time-millis
                  (mock {[] [(+ 46 (peer/default :minimum-retry-interval))]})

                  sh-print (constantly 1)]
      (let [peer (push peer)]
        (is (= {} (peer :pushed)))
        (is (nil? (peer :last-fail-ping-time)))))))
