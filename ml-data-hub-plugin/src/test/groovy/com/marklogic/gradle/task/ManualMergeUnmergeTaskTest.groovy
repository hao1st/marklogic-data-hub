package com.marklogic.gradle.task

import com.marklogic.hub.HubConfig
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildSuccess

import java.nio.file.Paths

class ManualMergeUnmergeTaskTest extends BaseTest{

    String URIs = "/person-1.json,/person-1-1.json,/person-1-2.json"
    String query = "cts:uris((),(),cts:and-query((cts:collection-query('sm-person-merged'),cts:collection-query('sm-person-mastered'))))"

    def setupSpec() {
        createGradleFiles()
        runTask('hubInit')
        copyResourceToFile("master-test/person.entity.json", Paths.get(testProjectDir.root.toString(),"entities", "person.entity.json").toFile())
        copyResourceToFile("master-test/myNewFlow.flow.json", Paths.get(testProjectDir.root.toString(),"flows","myNewFlow.flow.json").toFile())
        copyResourceToFile("master-test/person-1.json", Paths.get(testProjectDir.root.toString(),"input","person-1.json").toFile())
        copyResourceToFile("master-test/person-1-1.json", Paths.get(testProjectDir.root.toString(),"input","person-1-1.json").toFile())
        copyResourceToFile("master-test/person-1-2.json", Paths.get(testProjectDir.root.toString(),"input","person-1-2.json").toFile())
        copyResourceToFile("master-test/person-1.mapping.json", Paths.get(testProjectDir.root.toString(),"mappings","person","person-1.mapping.json").toFile())
        clearDatabases(HubConfig.DEFAULT_STAGING_NAME, HubConfig.DEFAULT_FINAL_NAME, HubConfig.DEFAULT_JOB_NAME);
        runTask('mlLoadModules')
    }

    def setup() {

    }

    def cleanup() {
        String query = "cts:uris((),(),cts:or-query((cts:collection-query('master'), cts:collection-query('http://marklogic.com/semantics#default-graph')))) ! xdmp:document-delete(.)"
        runInDatabase(query, HubConfig.DEFAULT_FINAL_NAME)
    }

    def "Verify manual merge with no flowName param"() {
        when:
        def result = runFailTask('hubMergeEntities', '-PmergeURIs=testURI')

        then:
        notThrown(UnexpectedBuildSuccess)
        result.output.contains("flowName is a required parameter")

    }

    def "Verify manual merge with no mergeURIs param"() {
        when:
        def result = runFailTask('hubMergeEntities', '-PflowName=testFlow')

        then:
        notThrown(UnexpectedBuildSuccess)
        result.output.contains("mergeURIs is a required parameter")
    }

    def "Verify manual unmerge with no mergeURI param"() {
        when:
        def result = runFailTask('hubUnmergeEntities')

        then:
        notThrown(UnexpectedBuildSuccess)
        result.output.contains("mergeURI is a required parameter")
    }

    def manualMerge() {
        //given:
        runTask('hubRunFlow', '-PflowName=myNewFlow', '-Psteps=1,2')

        //when:
        def result = runTask('hubMergeEntities', '-PmergeURIs='+URIs, '-PflowName=myNewFlow', '-PstepNumber=3')

        //then:
        result.task(':hubMergeEntities').outcome == TaskOutcome.SUCCESS
        getDocCount(HubConfig.DEFAULT_FINAL_NAME, "sm-person-archived") == 3
        getDocCount(HubConfig.DEFAULT_FINAL_NAME, "sm-person-auditing") == 1
        getDocCount(HubConfig.DEFAULT_FINAL_NAME, "sm-person-merged") == 1
    }

    def manualMergeUnmerge() {
        //given:
        manualMerge()
        String mergedANDMastered = "fn:count("+query+")"
        String mergeURI = runInDatabase(query, HubConfig.DEFAULT_FINAL_NAME).next().getString()

        //when:
        def result = runTask('hubUnmergeEntities', "-PmergeURI="+mergeURI, "-PretainAuditTrail=true", "-PblockFutureMerges=true")

        //then:
        result.task(':hubUnmergeEntities').outcome == TaskOutcome.SUCCESS
        getDocCount(HubConfig.DEFAULT_FINAL_NAME, "sm-person-archived") == 1
        getDocCount(HubConfig.DEFAULT_FINAL_NAME, "sm-person-auditing") == 1 //should be 2. DHFPROD-4055
        runInDatabase(mergedANDMastered, HubConfig.DEFAULT_FINAL_NAME).next().getNumber() == 3
    }

    def "Verify blockFutureMerges doesnt apply to manual merge"() {
        //A manual merged document, if unmerged with blockFutureMerges=true, future attempts of
        // manual merge on those documents should be blocked
        given:
        manualMergeUnmerge()
        String mergedANDMastered = "fn:count("+query+")"


        when:
        def result = runTask('hubMergeEntities', '-PmergeURIs='+URIs, '-PflowName=myNewFlow', '-PstepNumber=3')

        then:
        result.task(':hubMergeEntities').outcome == TaskOutcome.SUCCESS
        getDocCount(HubConfig.DEFAULT_FINAL_NAME, "sm-person-archived") == 3
        runInDatabase(mergedANDMastered, HubConfig.DEFAULT_FINAL_NAME).next().getNumber() == 1

    }

    def "Verify blockFutureMerges for regular mastering"() {
        given:
        manualMergeUnmerge()
        String mergedANDMastered = "fn:count("+query+")"

        when:
        def result = runTask('hubRunFlow', '-PflowName=myNewFlow', '-Psteps=3')

        then:
        result.task(':hubRunFlow').outcome == TaskOutcome.SUCCESS
        getDocCount(HubConfig.DEFAULT_FINAL_NAME, "sm-person-archived") == 1
        getDocCount(HubConfig.DEFAULT_FINAL_NAME, "sm-person-auditing") == 2 //should be 3. DHFPROD-4055
        runInDatabase(mergedANDMastered, HubConfig.DEFAULT_FINAL_NAME).next().getNumber() == 3 //count remains at 3 since merge should be blocked.  DHFPROD-4055
    }

    def "Verify manual merge with preview"() {
        //Should return merged doc as response and not save in DB

    }

    def "Verify manual merge with step option override"() {
        //Should override step options
    }

}
