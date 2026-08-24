package com.medidorderendimiento.domain

@JvmInline
value class LocalId(val value: String) {
    init {
        require(value.isNotBlank()) { "A local identifier must not be blank" }
    }
}
