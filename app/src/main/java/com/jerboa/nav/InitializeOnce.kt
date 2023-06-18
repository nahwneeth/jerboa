package com.jerboa.nav

interface Initializable {
    var initialized: Boolean
}

fun<T: Initializable> initializeOnce(initializable: T, initialize: T.() -> Unit) {
    if (!initializable.initialized) {
        initializable.initialize()
        initializable.initialized = true
    }
}
