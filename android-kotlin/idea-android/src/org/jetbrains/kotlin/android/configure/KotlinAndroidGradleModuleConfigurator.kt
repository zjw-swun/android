/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.configure

import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.configuration.AndroidGradle
import org.jetbrains.kotlin.idea.configuration.KotlinWithGradleConfigurator
import org.jetbrains.kotlin.idea.configuration.getBuildSystemType
import org.jetbrains.kotlin.idea.util.projectStructure.version
import org.jetbrains.kotlin.idea.versions.MAVEN_STDLIB_ID_JDK7
import org.jetbrains.kotlin.idea.versions.hasJreSpecificRuntime
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

class KotlinAndroidGradleModuleConfigurator : KotlinWithGradleConfigurator() {

    override val name: String = NAME

    override val targetPlatform: TargetPlatform = JvmPlatform

    override val presentableText: String = "Android with Gradle"

    public override fun isApplicable(module: Module): Boolean = module.getBuildSystemType() == AndroidGradle

    override val kotlinPluginName: String = KOTLIN_ANDROID

    override fun getKotlinPluginExpression(forKotlinDsl: Boolean): String =
        if (forKotlinDsl) "kotlin(\"android\")" else "id 'org.jetbrains.kotlin.android' "

    fun getKotlinAndroidExtensionsExpression(forKotlinDsl: Boolean): String =
        if (forKotlinDsl) "kotlin(\"android.extensions\")" else "id 'org.jetbrains.kotlin.android.extensions' "

    override fun addElementsToFile(file: PsiFile, isTopLevelProjectFile: Boolean, version: String): Boolean {
        val manipulator = getManipulator(file, false)
        val sdk = ModuleUtil.findModuleForPsiElement(file)?.let { ModuleRootManager.getInstance(it).sdk }
        val jvmTarget = getJvmTarget(sdk, version)

        return if (isTopLevelProjectFile) {
            manipulator.configureProjectBuildScript(kotlinPluginName, version)
        }
        else {
            manipulator.configureModuleBuildScript(
                    kotlinPluginName,
                    getKotlinPluginExpression(file.isKtDsl()),
                    getStdlibArtifactName(sdk, version),
                    version,
                    jvmTarget
            )
            manipulator.configureModuleBuildScript(
              KOTLIN_ANDROID_EXTENSIONS,
              getKotlinAndroidExtensionsExpression(file.isKtDsl()),
              getStdlibArtifactName(sdk, version),
              version,
              null
            )
        }
    }

    override fun getStdlibArtifactName(sdk: Sdk?, version: String): String {
        if (sdk != null && hasJreSpecificRuntime(version)) {
            val sdkVersion = sdk.version
            if (sdkVersion != null && sdkVersion.isAtLeast(JavaSdkVersion.JDK_1_8)) {
                // Android dex can't convert our kotlin-stdlib-jre8 artifact, so use jre7 instead (KT-16530)
                return MAVEN_STDLIB_ID_JDK7
            }
        }

        return super.getStdlibArtifactName(sdk, version)
    }

    @JvmSuppressWildcards
    override fun configure(project: Project, excludeModules: Collection<Module>) {
        super.configure(project, excludeModules)
        // Sync after changing build scripts
        GradleSyncInvoker.getInstance().requestProjectSync(project, GradleSyncInvoker.Request.projectModified())
    }

    companion object {
        private val NAME = "android-gradle"

        private val KOTLIN_ANDROID = "kotlin-android"

        private val KOTLIN_ANDROID_EXTENSIONS = "kotlin-android-extensions"
    }
}
