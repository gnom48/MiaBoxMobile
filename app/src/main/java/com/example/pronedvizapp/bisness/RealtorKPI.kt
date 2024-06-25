package com.example.pronedvizapp.bisness

class RealtorKPI {

    enum class RealtorLevel {
        INTERN, SPECIALIST, EXPERT, ROP
    }

    fun calculateLevelAndPercentage(
        deals: Int,
        coldCalls: Int,
        meetings: Int,
        ads: Int,
        showings: Int,
        exclusiveContracts: Int,
        nonExclusiveContracts: Int,
        successfulDeals: Int,
        leadsInCRM: Int
    ): Pair<RealtorLevel, Double> {
        val basePercentage = when {
            deals <= 3 -> 0.4
            deals <= 20 -> 0.43
            else -> {
                if (false/* condition for ROP */) {
                    0.5
                } else {
                    0.45
                }
            }
        }

        val coldCallsPercentage = when {
            coldCalls > 200 -> 0.02
            coldCalls > 90 -> 0.02
            else -> 0.02
        }

        val meetingsPercentage = when {
            // Add your conditions here for meetings percentage
            else -> 0.0
        }

        val adsPercentage = when {
            // Add your conditions here for ads percentage
            else -> 0.0
        }

        val totalPercentage = basePercentage + coldCallsPercentage + meetingsPercentage + adsPercentage

        val level = when {
            deals <= 3 -> RealtorLevel.INTERN
            deals <= 20 -> RealtorLevel.SPECIALIST
            deals > 20 /* && condition for ROP */ -> RealtorLevel.ROP
            else -> RealtorLevel.EXPERT
        }

        return Pair(level, totalPercentage)
    }
}