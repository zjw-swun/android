/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.gradle.structure.model.helpers

import com.android.tools.idea.gradle.structure.model.meta.ValueDescriptor
import com.google.common.util.concurrent.Futures.immediateFuture
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.pom.java.LanguageLevel

fun booleanValues(): ListenableFuture<List<ValueDescriptor<Boolean>>> =
  immediateFuture(listOf(ValueDescriptor(value = false), ValueDescriptor(value = true)))

fun installedSdksAsStrings(): ListenableFuture<List<ValueDescriptor<String>>> =
  immediateFuture(installedEnvironments().androidSdks.map { ValueDescriptor(it.value.toString(), it.description) })

fun installedSdksAsInts(): ListenableFuture<List<ValueDescriptor<Int>>> = immediateFuture(installedEnvironments().androidSdks)
fun installedBuildTools(): ListenableFuture<List<ValueDescriptor<String>>> = immediateFuture(installedEnvironments().buildTools)
fun installedCompiledApis(): ListenableFuture<List<ValueDescriptor<String>>> = immediateFuture(installedEnvironments().compiledApis)

fun languageLevels(): ListenableFuture<List<ValueDescriptor<LanguageLevel>>> = immediateFuture(listOf(
    ValueDescriptor(value = LanguageLevel.JDK_1_6, description = "1.6"),
    ValueDescriptor(value = LanguageLevel.JDK_1_7, description = "1.7"),
    ValueDescriptor(value = LanguageLevel.JDK_1_8, description = "1.8")
))

