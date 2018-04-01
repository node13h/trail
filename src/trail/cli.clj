(ns trail.cli
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]))

(def cli-options
  [[nil "--backwards" "Migrate backwards"
    :default false]
   ["-v" nil "Verbosity level; may be specified multiple times to increase value"
    :id :verbosity
    :default 0
    :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["IP lease history API"
        ""
        "Usage: trail [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  start      Start REST API server"
        "  migrate    Run database migrations"
        ""]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn parse-args
  [args]
  (when (= 1 (count args))
    (let [action (first args)]
      (cond
        (#{"start" "migrate"} action) {:action action}))))

(defn validate-args
  "Validate command line arguments"
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)
        {:keys [action]} (parse-args arguments)]
    (cond
      (:help options) {:exit-message (usage summary) :ok? true}
      errors {:exit-message (error-msg errors)}
      (some? action) {:action action :options options}
      :else {:exit-message (usage summary)})))
