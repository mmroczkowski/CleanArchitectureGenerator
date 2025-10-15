package com.mitteloupe.cag.cleanarchitecturegenerator.model

import com.mitteloupe.cag.core.option.DependencyInjection as CoreDependencyInjection

enum class DependencyInjection(
    val coreValue: CoreDependencyInjection
) {
    Hilt(CoreDependencyInjection.Hilt),
    Koin(CoreDependencyInjection.Koin),
    None(CoreDependencyInjection.None);

    companion object {
        fun fromString(value: String?): DependencyInjection =
            entries
                .find { it.name.equals(value, ignoreCase = true) } ?: Hilt
    }
}
