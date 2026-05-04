function AlgorithmLog({ logs = [] }) {
  return (
    <div className="bg-gray-100 p-4 rounded h-40 overflow-y-auto">
      {logs.length === 0 ? (
        <p className="text-gray-600">No log messages yet.</p>
      ) : (
        <ul className="space-y-2">
          {logs.map((log, index) => (
            <li key={`log-${index}`} className="text-sm text-gray-800">
              {log}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default AlgorithmLog
