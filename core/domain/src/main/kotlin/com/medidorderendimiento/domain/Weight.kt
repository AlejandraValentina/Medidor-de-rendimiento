package com.medidorderendimiento.domain

import java.time.Instant

enum class ManualSource { MANUAL }

data class WeightMeasurement(
    val id: LocalId,
    val mass: BodyMass,
    val recordedAt: Instant,
    val civilDay: CivilDay,
    val source: ManualSource = ManualSource.MANUAL,
    val revision: Long = 1,
) {
    init { require(revision > 0) { "Revision must be greater than zero" } }
}
