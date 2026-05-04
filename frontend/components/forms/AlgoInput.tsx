"use client";

import React, { useState, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

interface AlgoInputProps {
    label: string;
    type: 'text' | 'email' | 'tel' | 'number';
    placeholder?: string;
    value: string;
    onChange: (_v: string) => void; // eslint-disable-line no-unused-vars
    required?: boolean;
    nameType?: 'name' | 'phone' | 'email' | 'none';
}

export default function AlgoInput({ 
    label, 
    type, 
    placeholder, 
    value, 
    onChange, 
    required = false,
    nameType = 'none' 
}: AlgoInputProps) {
    const [isFocused, setIsFocused] = useState(false);

    // Dynamic validation using useMemo for performance and lint compliance
    const error = useMemo(() => {
        if (required && !value) return `${label} is required`;

        if (nameType === 'name' && value) {
            if (/\d/.test(value)) return "Names cannot contain numbers";
        }

        if (nameType === 'phone' && value) {
            const slPhoneRegex = /^(?:0|(?:\+94))?[1-9]\d{8}$|^[1-9]\d{8}$/;
            if (!slPhoneRegex.test(value.replace(/\s/g, ''))) {
                return "Invalid Sri Lankan phone number format";
            }
        }

        if (nameType === 'email' && value) {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(value)) {
                return "Please enter a valid email address";
            }
        }

        return null;
    }, [value, label, required, nameType]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        onChange(e.target.value);
    };

    return (
        <div className="w-full mb-8 group">
            <div className="flex justify-between items-end mb-2">
                <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40 group-focus-within:text-blue-500 transition-colors">
                    {label} {required && <span className="text-blue-600">*</span>}
                </label>
                <AnimatePresence>
                    {error && (
                        <motion.span 
                            initial={{ opacity: 0, y: 5 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: 5 }}
                            className="text-[9px] font-bold text-red-500 uppercase tracking-widest"
                        >
                            {error}
                        </motion.span>
                    )}
                </AnimatePresence>
            </div>

            <div className={`relative transition-all duration-500 border-b ${error ? 'border-red-500' : isFocused ? 'border-blue-600' : 'border-white/10'}`}>
                <input
                    type={type}
                    value={value}
                    onChange={handleChange}
                    onFocus={() => setIsFocused(true)}
                    onBlur={() => setIsFocused(false)}
                    placeholder={placeholder}
                    className="w-full bg-transparent py-4 text-white placeholder:text-white/10 focus:outline-none text-sm md:text-base tracking-wide font-medium"
                />
                
                {/* Underline Animation */}
                <motion.div 
                    initial={false}
                    animate={{ width: isFocused ? '100%' : '0%' }}
                    className="absolute bottom-[-1px] left-0 h-[1px] bg-blue-500"
                />
            </div>
        </div>
    );
}
