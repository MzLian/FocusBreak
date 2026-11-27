package com.focusbreak.app.service

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/** Compose necesita un LifecycleOwner y un SavedStateRegistryOwner aunque se dibuje fuera de una Activity. */
class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    fun create() {
        controller.performAttach()
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
    }

    fun resume() { registry.currentState = Lifecycle.State.RESUMED }
    fun destroy() { registry.currentState = Lifecycle.State.DESTROYED }
}
