function ResultsPanel({
  greedyTotalCost = 0,
  hungarianTotalCost = 0,
  greedyTimeMs = 0,
  hungarianTimeMs = 0,
}) {
  const formatMs = (value) => Number(value ?? 0).toFixed(3)

  const optimalAlgorithm =
    hungarianTotalCost <= greedyTotalCost ? "Hungarian" : "Greedy"

  return (
    <section className="bg-gray-900 border border-gray-700 rounded-xl p-4 text-sm text-gray-100 space-y-4">
      <h2 className="text-base font-semibold uppercase tracking-wide">
        Execution Results
      </h2>

      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <span>Greedy Total:</span>
          <span className="font-medium">{greedyTotalCost}</span>
        </div>

        <div className="flex items-center justify-between">
          <span>Hungarian Total:</span>
          <span className="font-semibold text-green-400">
            {hungarianTotalCost}
          </span>
        </div>

        <div className="flex items-center justify-between pt-2 border-t border-gray-700">
          <span className="text-gray-400">Optimal Algorithm:</span>
          <span className="text-green-400 font-semibold">
            {optimalAlgorithm}
          </span>
        </div>
      </div>

      <div className="border-t border-gray-700 pt-3 space-y-2">
        <h3 className="font-semibold">Execution Time</h3>

        <div className="grid grid-cols-2 gap-4">
          <div className="rounded-md border border-gray-700 p-3">
            <p className="text-gray-300">Greedy</p>
            <p className="font-medium">{formatMs(greedyTimeMs)} ms</p>
          </div>

          <div className="rounded-md border border-gray-700 p-3">
            <p className="text-gray-300">Hungarian</p>
            <p className="font-medium text-green-400">
              {formatMs(hungarianTimeMs)} ms
            </p>
          </div>
        </div>
      </div>
    </section>
  )
}

export default ResultsPanel