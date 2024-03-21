/*
 * Copyright (c) HashiCorp, Inc.
 * SPDX-License-Identifier: MPL-2.0
 */

// This file is maintained in the GoogleCloudPlatform/magic-modules repository and copied into the downstream provider repositories. Any changes to this file in the downstream will be overwritten.

package tests

import ServiceSweeperName
import jetbrains.buildServer.configs.kotlin.BuildType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import jetbrains.buildServer.configs.kotlin.Project
import org.junit.Assert
import projects.googleCloudRootProject

class SweeperTests {
    @Test
    fun projectSweeperDoesNotSkipProjectSweep() {
        val root = googleCloudRootProject(testContextParameters())

        // Find Project sweeper project
        val projectSweeperProject = getSubProject(root, projectSweeperProjectName)

        // For the project sweeper to be skipped, SKIP_PROJECT_SWEEPER needs a value
        // See https://github.com/GoogleCloudPlatform/magic-modules/blob/501429790939717ca6dce76dbf4b1b82aef4e9d9/mmv1/third_party/terraform/services/resourcemanager/resource_google_project_sweeper.go#L18-L26

        projectSweeperProject.buildTypes.forEach{bt ->
            val value = bt.params.findRawParam("env.SKIP_PROJECT_SWEEPER")!!.value
            assertTrue("env.SKIP_PROJECT_SWEEPER should be set to an empty value, so project sweepers are NOT skipped in the ${projectSweeperProject.name} project. Value = `${value}` ", value == "")
        }
    }

    @Test
    fun serviceSweepersSkipProjectSweeper() {
        val root = googleCloudRootProject(testContextParameters())

        // Find GA nightly test project
        val gaNightlyTestProject = getNestedProjectFromRoot(root, gaProjectName, nightlyTestsProjectName)
        // Find GA MM Upstream project
        val gaMmUpstreamProject = getNestedProjectFromRoot(root, gaProjectName, mmUpstreamProjectName)

        // Find Beta nightly test project
        val betaNightlyTestProject = getNestedProjectFromRoot(root, betaProjectName, nightlyTestsProjectName)
        // Find Beta MM Upstream project
        val betaMmUpstreamProject = getNestedProjectFromRoot(root, betaProjectName, mmUpstreamProjectName)

        val allProjects: ArrayList<Project> = arrayListOf(gaNightlyTestProject, gaMmUpstreamProject, betaNightlyTestProject, betaMmUpstreamProject)
        allProjects.forEach{ project ->
            // Find sweeper inside
            val sweeper = getBuildFromProject(project, ServiceSweeperName)

            // For the project sweeper to be skipped, SKIP_PROJECT_SWEEPER needs a value
            // See https://github.com/GoogleCloudPlatform/magic-modules/blob/501429790939717ca6dce76dbf4b1b82aef4e9d9/mmv1/third_party/terraform/services/resourcemanager/resource_google_project_sweeper.go#L18-L26

            val value = sweeper.params.findRawParam("env.SKIP_PROJECT_SWEEPER")!!.value
            assertTrue("env.SKIP_PROJECT_SWEEPER should be set to a non-empty string so project sweepers are skipped in the ${project.name} project. Value = `${value}` ", value != "")
        }
    }

    @Test
    fun gaNightlyProjectServiceSweeperRunsInGoogle() {
        val root = googleCloudRootProject(testContextParameters())

        // Find GA nightly test project
        val gaNightlyTestProject = getNestedProjectFromRoot(root, gaProjectName, nightlyTestsProjectName)

        // Find sweeper inside
        val sweeper = getBuildFromProject(gaNightlyTestProject, ServiceSweeperName)

        // Check PACKAGE_PATH is in google (not google-beta)
        val value = sweeper.params.findRawParam("PACKAGE_PATH")!!.value
        assertEquals("./google/sweeper", value)
    }

    @Test
    fun betaNightlyProjectServiceSweeperRunsInGoogleBeta() {
        val root = googleCloudRootProject(testContextParameters())

        // Find Beta nightly test project
        val betaNightlyTestProject = getNestedProjectFromRoot(root, betaProjectName, nightlyTestsProjectName)

        // Find sweeper inside
        val sweeper: BuildType = getBuildFromProject(betaNightlyTestProject, ServiceSweeperName)

        // Check PACKAGE_PATH is in google-beta
        val value = sweeper.params.findRawParam("PACKAGE_PATH")!!.value
        assertEquals("./google-beta/sweeper", value)
    }
}
