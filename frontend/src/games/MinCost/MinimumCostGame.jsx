import { useState, useEffect } from "react";
import GameLayoutShell from "./GameLayoutShell";
import "./MinCost.css";

function MinimumCostGame() {
  // Game Data
  const [matrix, setMatrix] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [blockedRows, setBlockedRows] = useState([]);
  const [blockedCols, setBlockedCols] = useState([]);
  const [logs, setLogs] = useState([]);
  const [totalCost, setTotalCost] = useState(0);
  const [greedyTotalCost, setGreedyTotalCost] = useState(0);
  const [hungarianTotalCost, setHungarianTotalCost] = useState(0);
  const [greedyTimeMs, setGreedyTimeMs] = useState(0);
  const [hungarianTimeMs, setHungarianTimeMs] = useState(0);

  // Form States
  const [formData, setFormData] = useState({
    playerName: localStorage.getItem("mincost_player") || "",
    email: localStorage.getItem("mincost_email") || "",
    phone: localStorage.getItem("mincost_phone") || "",
    n: 50,
    minCost: 20,
    maxCost: 200,
  });

  const [errors, setErrors] = useState({});
  const [toast, setToast] = useState(null);

  // Persistence
  useEffect(() => {
    localStorage.setItem("mincost_player", formData.playerName);
    localStorage.setItem("mincost_email", formData.email);
    localStorage.setItem("mincost_phone", formData.phone);
  }, [formData.playerName, formData.email, formData.phone]);

  const showToast = (message, type = "info") => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 4000);
  };

  const validate = (field, value) => {
    let errs = { ...errors };

    if (field === "playerName") {
      if (/[0-9]/.test(value)) errs.playerName = "Name cannot contain numbers";
      else delete errs.playerName;
    }

    if (field === "email") {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (value && !emailRegex.test(value)) errs.email = "Invalid email format";
      else delete errs.email;
    }

    if (field === "phone") {
        // Sri Lankan Phone Rules: 0 + 9 digits, or +94 + 9 digits, or 9 digits
        const phoneRegex = /^(?:\+94|0)?\d{9}$/;
        if (value && !phoneRegex.test(value)) errs.phone = "Invalid Sri Lankan phone number";
        else delete errs.phone;
    }

    if (field === "n") {
        if (value < 50 || value > 100) errs.n = "N must be between 50 and 100";
        else delete errs.n;
    }

    if (field === "minCost" || field === "maxCost") {
        const min = field === "minCost" ? value : formData.minCost;
        const max = field === "maxCost" ? value : formData.maxCost;
        if (min >= max) errs.range = "Min cost must be less than Max cost";
        else delete errs.range;
        
        if (min < 20 || max > 200) errs.bounds = "Costs must be between $20 and $200";
        else delete errs.bounds;
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    const finalValue = (name === "n" || name === "minCost" || name === "maxCost") 
        ? parseInt(value) || 0 
        : value;
    
    setFormData(prev => ({ ...prev, [name]: finalValue }));
    validate(name, finalValue);
  };

  const handleStartGame = () => {
    // Final validation check
    if (Object.keys(errors).length > 0 || !formData.playerName) {
        showToast("Please fix the validation errors before starting", "error");
        return;
    }

    showToast("Calculating optimal assignments...", "process");

    const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL;
    if (!API_BASE) {
      showToast("NEXT_PUBLIC_API_BASE_URL not set in environment", "error");
      return;
    }
    fetch(`${API_BASE}/api/mincost/start`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        n: formData.n,
        minCost: formData.minCost,
        maxCost: formData.maxCost,
        playerId: 1 // In a real app, this would be from a session
      })
    })
      .then((res) => {
          if (!res.ok) return res.json().then(err => { throw new Error(err.message) });
          return res.json();
      })
      .then((data) => {
        const nextMatrix = Array.isArray(data.matrix) ? data.matrix : [];
        const greedyAssignments = Array.isArray(data.greedyAssignments)
          ? data.greedyAssignments
          : [];
        const hungarianAssignments = Array.isArray(data.hungarianAssignments)
          ? data.hungarianAssignments
          : [];
        
        setMatrix(nextMatrix);
        setAssignments(greedyAssignments);
        setBlockedRows(greedyAssignments.map((item) => item.row));
        setBlockedCols(greedyAssignments.map((item) => item.col));
        setLogs(data.greedyLogs || ["Matrix generated"]);

        setGreedyTotalCost(data.greedyTotalCost ?? 0);
        setHungarianTotalCost(data.hungarianTotalCost ?? 0);
        setGreedyTimeMs(data.greedyTimeMs ?? 0);
        setHungarianTimeMs(data.hungarianTimeMs ?? 0);
        setTotalCost(data.greedyTotalCost ?? 0);

        showToast("Optimization Complete!", "success");
      })
      .catch((err) => {
        showToast(err.message || "Connection to backend failed", "error");
        setLogs((prevLogs) => [
          ...prevLogs,
          "Error: " + (err.message || "Failed to fetch results"),
        ]);
      });
  };

  const handleNextStep = () => {
    showToast("Interactive steps coming soon in v2.0", "info");
  };

  return (
    <div className="min-cost-container">
      {toast && (
        <div className={`professional-alert ${toast.type}`}>
          <span className="alert-icon">
            {toast.type === "error" ? "❌" : toast.type === "success" ? "✅" : "🔔"}
          </span>
          <span className="alert-message">{toast.message}</span>
        </div>
      )}

      <GameLayoutShell
        formData={formData}
        handleInputChange={handleInputChange}
        errors={errors}
        matrix={matrix}
        assignments={assignments}
        blockedRows={blockedRows}
        blockedCols={blockedCols}
        logs={logs}
        totalCost={totalCost}
        greedyTotalCost={greedyTotalCost}
        hungarianTotalCost={hungarianTotalCost}
        greedyTimeMs={greedyTimeMs}
        hungarianTimeMs={hungarianTimeMs}
        onStartGame={handleStartGame}
        onNextStep={handleNextStep}
      />
    </div>
  );
}

export default MinimumCostGame;