<?xml version="1.0"?>
<#import "root://activities/common/kotlin_macros.ftl" as kt>
<recipe>
	  
    <instantiate from="root/res/layout/fragment.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/layout/${escapeXmlAttribute(fragmentLayout)}.xml" />
    <open file="${escapeXmlAttribute(resOut)}/layout/${fragmentLayout}.xml" />

	<#if createActivity>
    <instantiate from="root/src/app_package/Activity.kt.ftl"
                   to="${escapeXmlAttribute(srcOut)}/${activityClass}.kt" />
	<open file="${escapeXmlAttribute(srcOut)}/${activityClass}.kt" />
	</#if>

    <instantiate from="root/src/app_package/Fragment.kt.ftl"
                   to="${escapeXmlAttribute(srcOut)}/${fragmentClass}.kt" />			   
    <open file="${escapeXmlAttribute(srcOut)}/${fragmentClass}.kt" />
	
    <instantiate from="root/src/app_package/ViewModel.kt.ftl"
                   to="${escapeXmlAttribute(srcOut)}/${viewModelClass}.kt" />
    <open file="${escapeXmlAttribute(srcOut)}/${viewModelClass}.kt" />
	
    <instantiate from="root/src/app_package/ViewState.kt.ftl"
                   to="${escapeXmlAttribute(srcOut)}/${viewStateClass}.kt" />
    <open file="${escapeXmlAttribute(srcOut)}/${viewStateClass}.kt" />
	
    <instantiate from="root/src/app_package/Action.kt.ftl"
                   to="${escapeXmlAttribute(srcOut)}/${actionClass}.kt" />
    <open file="${escapeXmlAttribute(srcOut)}/${actionClass}.kt" />

    <#if createViewEvents>
        <instantiate from="root/src/app_package/ViewEvents.kt.ftl"
                       to="${escapeXmlAttribute(srcOut)}/${viewEventsClass}.kt" />
    	<open file="${escapeXmlAttribute(srcOut)}/${viewEventsClass}.kt" />
    </#if>
	
</recipe>
