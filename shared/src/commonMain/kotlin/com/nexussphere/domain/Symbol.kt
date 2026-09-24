package com.nexussphere.domain

import kotlinx.serialization.Serializable

/**
 * Strongly-typed asset symbol to prevent bare-string misuse.
 * Exchange suffix (.TO) distinguishes Canadian vs US listings.
 */
@Serializable
@JvmInline
value class Symbol(val value: String) {
    init {
        require(value.isNotBlank()) { "Symbol must not be blank" }
        require(value == value.uppercase()) { "Symbol must be uppercase: $value" }
    }

    val isCanadian: Boolean get() = value.endsWith(".TO")
    val baseTicker: String get() = value.removeSuffix(".TO")

    override fun toString(): String = value

    companion object {
        /** TFSA portfolio holdings. */
        val TFSA_HOLDINGS: Set<Symbol> = setOf(
            Symbol("AMD"),
            Symbol("BB"),
            Symbol("BB.TO"),
            Symbol("ENB"),
            Symbol("ORCL"),
            Symbol("PANW"),
            Symbol("PLTR"),
            Symbol("TSM"),
        )
    }
}
