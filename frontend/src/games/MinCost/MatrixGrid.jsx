function MatrixGrid({ matrix = [], assignments = [], blockedRows = [], blockedCols = [] }) {
  const hasRows = Array.isArray(matrix) && matrix.length > 0
  const columnCount = hasRows ? matrix[0].length : 0
  const isAssigned = (row, col) => assignments.some((item) => item.row === row && item.col === col)

  return (
    <div className="matrix-scroll scrollbar-thin">
      <table className="w-full border-separate border-spacing-0">
        <thead className="sticky top-0 z-10">
          <tr>
            <th className="border-b border-r text-center p-2 bg-gray-100 text-xs font-bold sticky left-0 top-0 z-20 min-w-[50px]">
              T\E
            </th>
            {Array.from({ length: columnCount }, (_, columnIndex) => (
              <th
                key={`employee-${columnIndex}`}
                className={`border-b border-r text-center p-2 bg-gray-100 text-xs font-bold min-w-[50px] ${blockedCols.includes(columnIndex) ? 'opacity-40' : ''}`}
              >
                E{columnIndex + 1}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {hasRows ? (
            matrix.map((row, rowIndex) => (
              <tr key={`task-row-${rowIndex}`}>
                <th
                  className={`border-b border-r text-center p-2 bg-gray-100 text-xs font-bold sticky left-0 z-10 min-w-[50px] ${blockedRows.includes(rowIndex) ? 'opacity-40' : ''}`}
                >
                  T{rowIndex + 1}
                </th>
                {row.map((value, columnIndex) => {
                  const cellIsAssigned = isAssigned(rowIndex, columnIndex)
                  const rowBlocked = blockedRows.includes(rowIndex)
                  const colBlocked = blockedCols.includes(columnIndex)
                  const dimmedClass = rowBlocked || colBlocked ? 'opacity-40' : ''
                  const assignedClass = cellIsAssigned ? 'bg-purple-200 text-purple-900 font-bold opacity-100 scale-105' : ''

                  return (
                    <td
                      key={`cell-${rowIndex}-${columnIndex}`}
                      className={`border-b border-r text-center p-2 text-sm transition-all hover:bg-purple-50 ${dimmedClass} ${assignedClass}`}
                    >
                      {value}
                    </td>
                  )
                })}
              </tr>
            ))
          ) : (
            <tr>
              <td className="border text-center p-4 text-gray-400 italic" colSpan={columnCount + 1}>
                No assignment data to display
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}

export default MatrixGrid