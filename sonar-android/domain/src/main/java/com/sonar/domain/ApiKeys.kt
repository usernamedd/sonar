package com.sonar.domain

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WhisperApiKey

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GeminiApiKey

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SearchApiKey
