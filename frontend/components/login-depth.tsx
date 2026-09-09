"use client"

import React from "react"

/*
 * Teal (#0F766E → #14B8A6) = raw structured data, precision, trust
 * Lime (#84CC16 → #BEF264) = resolved signal, clarity, confidence
 * The gradient direction: data → insight → decision
 */
export function LoginDepth() {
  return (
    <div
      className="relative hidden md:flex flex-col justify-between overflow-hidden bg-[#060D10] p-8 text-white select-none rounded-r-[inherit]"
      aria-hidden="true"
    >
      {/* Ambient depth glows */}
      <div className="pointer-events-none absolute -top-32 -right-32 h-[500px] w-[500px] rounded-full bg-[#0F766E]/15 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-32 -left-32 h-[400px] w-[400px] rounded-full bg-[#84CC16]/10 blur-3xl" />
      <div className="pointer-events-none absolute top-1/2 left-1/2 h-72 w-72 -translate-x-1/2 -translate-y-1/2 rounded-full bg-[#0D9488]/8 blur-2xl" />

      {/* ─── Central Brand Copy ─── */}
      <div className="relative z-10 my-auto py-8">
        {/* Eyebrow */}
        <p className="mb-3 text-[11px] font-semibold uppercase tracking-[0.18em] text-[#84CC16]/80">
          Financial Compliance Intelligence
        </p>

        {/* Headline — FinComp brand voice */}
        <h2 className="text-2xl font-bold leading-snug tracking-tight text-white/95 lg:text-[1.75rem]">
          Risk, under control.
        </h2>

        {/* Sub-copy — product description, not law text */}
        <p className="mt-3 max-w-[280px] text-sm leading-relaxed text-white/50">
          From raw transactions to clear decisions. FinComp monitors, scores, and resolves financial risk — in one place.
        </p>
      </div>

      {/* ─── Bottom Product Card ─── */}
      {/* Represents the three core capabilities */}
      <div className="relative z-10">
        <div className="grid grid-cols-3 gap-3">
          {[
            { value: "Monitor", label: "Transactions" },
            { value: "Score", label: "Client risk" },
            { value: "Decide", label: "With clarity" },
          ].map((item) => (
            <div key={item.label} className="text-center">
              <p className="text-sm font-semibold text-[#84CC16]">{item.value}</p>
              <p className="mt-0.5 text-[10px] text-white/40">{item.label}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
