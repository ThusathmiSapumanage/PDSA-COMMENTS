"use client";

import React, { useState, useRef, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";

interface Option {
  value: string;
  label: string;
}

interface AlgoDropdownProps {
  label: string;
  options: Option[];
  value: string;
  onChange: (_v: string) => void; // eslint-disable-line no-unused-vars
  placeholder?: string;
  disabled?: boolean;
}

export default function AlgoDropdown({
  label,
  options,
  value,
  onChange,
  placeholder = "Select an option",
  disabled = false,
}: AlgoDropdownProps) {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const selectedOption = options.find((opt) => opt.value === value);

  // Close the menu if the dropdown becomes disabled while open (e.g. benchmark starts).
  useEffect(() => {
    if (disabled && isOpen) setIsOpen(false);
  }, [disabled, isOpen]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <div className="w-full mb-8 group" ref={dropdownRef}>
      <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40 mb-2 block group-focus-within:text-blue-500 transition-colors">
        {label}
      </label>

      <div className="relative">
        {/* Trigger Button */}
        <button
          onClick={() => !disabled && setIsOpen(!isOpen)}
          disabled={disabled}
          className={`w-full flex items-center justify-between py-4 bg-transparent border-b transition-all duration-500 text-left ${
            isOpen ? "border-blue-600" : "border-white/10"
          } disabled:opacity-40 disabled:cursor-not-allowed`}
        >
          <span className={`text-sm md:text-base font-medium tracking-wide ${value ? "text-white" : "text-white/20"}`}>
            {selectedOption ? selectedOption.label : placeholder}
          </span>

          <motion.svg
            animate={{ rotate: isOpen ? 180 : 0 }}
            className="w-4 h-4 text-white/40"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <polyline points="6 9 12 15 18 9"></polyline>
          </motion.svg>
        </button>

        {/* Dropdown Menu */}
        <AnimatePresence>
          {isOpen && (
            <motion.ul
              initial={{ opacity: 0, y: 10, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 10, scale: 0.95 }}
              className="absolute top-full left-0 w-full mt-2 bg-[#0a0a0a] border border-white/10 rounded-lg shadow-2xl z-[100] max-h-[300px] overflow-y-auto"
            >
              {options.map((option) => (
                <li
                  key={option.value}
                  onClick={() => {
                    onChange(option.value);
                    setIsOpen(false);
                  }}
                  className={`px-6 py-4 cursor-pointer text-sm md:text-base font-medium transition-all duration-300 flex items-center justify-between group-item ${
                    value === option.value ? "bg-blue-600 text-white" : "text-white/50 hover:bg-white/5 hover:text-white"
                  }`}
                >
                  {option.label}
                  {value === option.value && (
                    <div className="h-1.5 w-1.5 bg-white rounded-full" />
                  )}
                </li>
              ))}
            </motion.ul>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
