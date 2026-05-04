import MatrixGrid from './MatrixGrid'
import AlgorithmLog from './AlgorithmLog'
import ResultsPanel from './ResultsPanel'

function GameLayoutShell({
  formData,
  handleInputChange,
  errors,
  matrix,
  assignments,
  blockedRows,
  blockedCols,
  logs,
  totalCost,
  greedyTotalCost,
  hungarianTotalCost,
  greedyTimeMs,
  hungarianTimeMs,
  onStartGame,
  onNextStep,
}) {
  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 space-y-8">
      {/* Header Section */}
      <div className="text-left space-y-2">
        <h1 className="text-4xl sm:text-5xl font-bold text-gray-900 tracking-tight">
          Minimum Cost <span className="text-purple-600">Optimizer</span>
        </h1>
        <p className="text-gray-500 text-lg">Solve complex task assignments using Greedy & Hungarian algorithms.</p>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">
        {/* Left Column: Settings Form */}
        <div className="xl:col-span-1 space-y-6">
          <div className="config-card space-y-6">
            <h3 className="text-xl font-semibold border-b pb-3">Worker Profile</h3>
            
            <div className="space-y-4">
              <div className="input-group">
                <label className="input-label">Manager / Player Name</label>
                <input
                  name="playerName"
                  value={formData.playerName}
                  onChange={handleInputChange}
                  className={`custom-input ${errors.playerName ? 'invalid' : ''}`}
                  placeholder="Enter your name"
                />
                <div className="error-hint">{errors.playerName}</div>
              </div>

              <div className="input-group">
                <label className="input-label">Business Email</label>
                <input
                  name="email"
                  value={formData.email}
                  onChange={handleInputChange}
                  className={`custom-input ${errors.email ? 'invalid' : ''}`}
                  placeholder="manager@company.com"
                />
                <div className="error-hint">{errors.email}</div>
              </div>

              <div className="input-group">
                <label className="input-label">Contact Phone (SL)</label>
                <input
                  name="phone"
                  value={formData.phone}
                  onChange={handleInputChange}
                  className={`custom-input ${errors.phone ? 'invalid' : ''}`}
                  placeholder="0771234567"
                />
                <div className="error-hint">{errors.phone}</div>
              </div>
            </div>

            <h3 className="text-xl font-semibold border-b pb-3 pt-4">Game Parameters</h3>
            
            <div className="grid grid-cols-2 gap-4">
                <div className="input-group">
                    <label className="input-label">Matrix Size (N)</label>
                    <input
                        type="number"
                        name="n"
                        value={formData.n}
                        onChange={handleInputChange}
                        className={`custom-input ${errors.n ? 'invalid' : ''}`}
                    />
                    <div className="error-hint">{errors.n}</div>
                </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
                <div className="input-group">
                    <label className="input-label">Min Cost ($)</label>
                    <input
                        type="number"
                        name="minCost"
                        value={formData.minCost}
                        onChange={handleInputChange}
                        className={`custom-input ${errors.range || errors.bounds ? 'invalid' : ''}`}
                    />
                </div>
                <div className="input-group">
                    <label className="input-label">Max Cost ($)</label>
                    <input
                        type="number"
                        name="maxCost"
                        value={formData.maxCost}
                        onChange={handleInputChange}
                        className={`custom-input ${errors.range || errors.bounds ? 'invalid' : ''}`}
                    />
                </div>
            </div>
            {(errors.range || errors.bounds) && (
                <div className="error-hint text-center">{errors.range || errors.bounds}</div>
            )}

            <div className="pt-6 flex flex-col gap-3">
              <button
                type="button"
                onClick={onStartGame}
                className="btn-primary w-full"
              >
                <span>🚀</span> Generate & Solve
              </button>

              <button
                type="button"
                onClick={onNextStep}
                className="px-4 py-3 rounded-lg border-2 border-emerald-500 text-emerald-600 font-semibold hover:bg-emerald-50 transition-all text-center"
              >
                Learn Step-by-Step
              </button>
            </div>
          </div>
        </div>

        {/* Right Column: Matricies & Results */}
        <div className="xl:col-span-2 space-y-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-3">
              <h2 className="text-lg font-bold flex items-center gap-2">
                <span className="bg-blue-100 text-blue-600 px-2 py-1 rounded text-sm">G</span>
                Greedy Assignment
              </h2>
              <div className="matrix-wrapper">
                <MatrixGrid
                  matrix={matrix}
                  assignments={assignments}
                  blockedRows={blockedRows}
                  blockedCols={blockedCols}
                />
              </div>
            </div>

            <div className="space-y-3">
              <h2 className="text-lg font-bold flex items-center gap-2">
                <span className="bg-purple-100 text-purple-600 px-2 py-1 rounded text-sm">H</span>
                Hungarian Optimal
              </h2>
              <div className="matrix-wrapper">
                <MatrixGrid matrix={matrix} assignments={assignments} />
              </div>
            </div>
          </div>

          <ResultsPanel
            greedyTotalCost={greedyTotalCost}
            hungarianTotalCost={hungarianTotalCost}
            greedyTimeMs={greedyTimeMs}
            hungarianTimeMs={hungarianTimeMs}
          />

          <div className="space-y-3">
            <h2 className="text-xl font-bold flex items-center gap-2">
              <span>📝</span> Execution Logs
            </h2>
            <AlgorithmLog logs={logs} />
          </div>
        </div>
      </div>
    </div>
  )
}

export default GameLayoutShell