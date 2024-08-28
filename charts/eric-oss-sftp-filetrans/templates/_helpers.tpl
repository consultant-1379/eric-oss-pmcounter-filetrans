{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-sftp-filetrans.name" }}
  {{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-oss-sftp-filetrans.version" }}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-sftp-filetrans.fullname" -}}
{{- if .Values.fullnameOverride -}}
  {{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
  {{- $name := default .Chart.Name .Values.nameOverride -}}
  {{- printf "%s" $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-oss-sftp-filetrans.chart" }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create image pull secrets for global (outside of scope)
*/}}
{{- define "eric-oss-sftp-filetrans.pullSecret.global" -}}
{{- $pullSecret := "" -}}
{{- if .Values.global -}}
  {{- if .Values.global.pullSecret -}}
    {{- $pullSecret = .Values.global.pullSecret -}}
  {{- end -}}
  {{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{/*
Create kafka service name , overriding from parent chart
*/}}
{{- define "eric-oss-sftp-filetrans.kafkaServiceName" -}}
{{- $kafkaServiceName := .Values.spring.kafka.bootstrap_servers -}}
{{- $serviceMesh := ( include "eric-oss-sftp-filetrans.service-mesh-enabled" . ) -}}
{{- $tls := ( include "eric-oss-sftp-filetrans.global-security-tls-enabled" . ) -}}
{{- if and (eq $serviceMesh "true") (eq $tls "true") -}}
    {{- $kafkaServiceName = .Values.spring.kafka.bootstrapServersTls -}}
{{ else }}
    {{- $kafkaServiceName = .Values.spring.kafka.bootstrap_servers -}}
{{ end }}
{{- if .Values.global -}}
  {{- if .Values.global.dependentServices -}}
    {{- if .Values.global.dependentServices.dmm -}}
        {{- if and (eq $serviceMesh "true") (eq $tls "true") -}}
           {{- if index .Values.global.dependentServices.dmm "kafka-bootstrapServersTls" -}}
               {{- $kafkaServiceName = index .Values.global.dependentServices.dmm "kafka-bootstrapServersTls" -}}
           {{- end -}}
        {{ else }}
           {{- if index .Values.global.dependentServices.dmm "kafka-bootstrap" -}}
               {{- $kafkaServiceName = index .Values.global.dependentServices.dmm "kafka-bootstrap" -}}
           {{- end -}}
        {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- print $kafkaServiceName -}}
{{- end -}}

{{/*
Create kafka service issuer reference , overriding from parent chart
*/}}
{{- define "eric-oss-sftp-filetrans.kafkaIssuerReference" -}}
{{- $kafkaIssuerReference := .Values.spring.kafka.issuerReference -}}
{{- if .Values.global -}}
  {{- if .Values.global.dependentServices -}}
    {{- if .Values.global.dependentServices.dmm -}}
       {{- if index .Values.global.dependentServices.dmm "kafka-issuerReference" -}}
            {{- $kafkaIssuerReference = index .Values.global.dependentServices.dmm "kafka-issuerReference" -}}
        {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- print $kafkaIssuerReference -}}
{{- end -}}

{{/*
Create image pull secret, service level parameter takes precedence
*/}}
{{- define "eric-oss-sftp-filetrans.pullSecret" -}}
{{- $pullSecret := (include "eric-oss-sftp-filetrans.pullSecret.global" . ) -}}
{{- if .Values.imageCredentials -}}
  {{- if .Values.imageCredentials.pullSecret -}}
    {{- $pullSecret = .Values.imageCredentials.pullSecret -}}
  {{- end -}}
{{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{/*
Configuration of image repository path (DR-D1121-106)
*/}}
{{- define "eric-oss-sftp-filetrans.mainImagePath" -}}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := (index $productInfo "images" "eric-oss-sftp-filetrans" "registry") -}}
    {{- $repoPath := (index $productInfo "images" "eric-oss-sftp-filetrans" "repoPath") -}}
    {{- $name := (index $productInfo "images" "eric-oss-sftp-filetrans" "name") -}}
    {{- $tag := (index $productInfo "images" "eric-oss-sftp-filetrans" "tag") -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.url -}}
                {{- $registryUrl = .Values.global.registry.url -}}
            {{- end -}}
            {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
                {{- $repoPath = .Values.global.registry.repoPath -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.repoPath -}}
        {{- end -}}
        {{- if (index .Values "imageCredentials" "eric-oss-sftp-filetrans") -}}
            {{- if (index .Values "imageCredentials" "eric-oss-sftp-filetrans" "registry") -}}
                {{- if (index .Values "imageCredentials" "eric-oss-sftp-filetrans" "registry" "url") -}}
                    {{- $registryUrl = (index .Values "imageCredentials" "eric-oss-sftp-filetrans" "registry" "url") -}}
                {{- end -}}
            {{- end -}}
            {{- if not (kindIs "invalid" (index .Values "imageCredentials" "eric-oss-sftp-filetrans" "repoPath")) -}}
                {{- $repoPath = (index .Values "imageCredentials" "eric-oss-sftp-filetrans" "repoPath") -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}

{{/*
Timezone variable
*/}}
{{- define "eric-oss-sftp-filetrans.timezone" }}
  {{- $timezone := "UTC" }}
  {{- if .Values.global }}
    {{- if .Values.global.timezone }}
      {{- $timezone = .Values.global.timezone }}
    {{- end }}
  {{- end }}
  {{- print $timezone | quote }}
{{- end -}}

{{/*
Return the fsgroup set via global parameter if it's set, otherwise 10000
*/}}
{{- define "eric-oss-sftp-filetrans.fsGroup.coordinated" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.fsGroup -}}
      {{- if .Values.global.fsGroup.manual -}}
        {{ .Values.global.fsGroup.manual }}
      {{- else -}}
        {{- if .Values.global.fsGroup.namespace -}}
          {{- if eq .Values.global.fsGroup.namespace true -}}
          # The 'default' defined in the Security Policy will be used.
          {{- else -}}
            10000
          {{- end -}}
        {{- else -}}
          10000
        {{- end -}}
      {{- end -}}
    {{- else -}}
      10000
    {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
{{- end -}}

{{/*
Define supplementalGroups (DR-D1123-135)
*/}}
{{- define "eric-oss-sftp-filetrans.supplementalGroups" -}}
  {{- $globalGroups := (list) -}}
  {{- if ( (((.Values).global).podSecurityContext).supplementalGroups) }}
    {{- $globalGroups = .Values.global.podSecurityContext.supplementalGroups -}}
  {{- end -}}
  {{- $localGroups := (list) -}}
  {{- if ( ((.Values).podSecurityContext).supplementalGroups) -}}
    {{- $localGroups = .Values.podSecurityContext.supplementalGroups -}}
  {{- end -}}
  {{- $mergedGroups := (list) -}}
  {{- if $globalGroups -}}
    {{- $mergedGroups = $globalGroups -}}
  {{- end -}}
  {{- if $localGroups -}}
    {{- $mergedGroups = concat $globalGroups $localGroups | uniq -}}
  {{- end -}}
  {{- if $mergedGroups -}}
    supplementalGroups: {{- toYaml $mergedGroups | nindent 8 -}}
  {{- end -}}
  {{- /*Do nothing if both global and local groups are not set */ -}}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "eric-oss-sftp-filetrans.selectorLabels" -}}
app.kubernetes.io/name: {{ include "eric-oss-sftp-filetrans.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "eric-oss-sftp-filetrans.serviceAccountName" -}}
  {{- if .Values.serviceAccount.create }}
    {{- default (include "eric-oss-sftp-filetrans.fullname" .) .Values.serviceAccount.name }}
  {{- else }}
    {{- default "default" .Values.serviceAccount.name }}
  {{- end }}
{{- end }}

{{/*
Create container level annotations
*/}}
{{- define "eric-oss-sftp-filetrans.container-annotations" }}
    {{- if .Values.appArmorProfile -}}
    {{- $appArmorValue := .Values.appArmorProfile.type -}}
        {{- if .Values.appArmorProfile.type -}}
            {{- if eq .Values.appArmorProfile.type "localhost" -}}
                {{- $appArmorValue = printf "%s/%s" .Values.appArmorProfile.type .Values.appArmorProfile.localhostProfile }}
            {{- end}}
container.apparmor.security.beta.kubernetes.io/eric-oss-sftp-filetrans: {{ $appArmorValue | quote }}
        {{- end}}
    {{- end}}
{{- end}}

{{/*
Seccomp profile section (DR-1123-128)
*/}}
{{- define "eric-oss-sftp-filetrans.seccomp-profile" }}
    {{- if .Values.seccompProfile }}
      {{- if .Values.seccompProfile.type }}
          {{- if eq .Values.seccompProfile.type "Localhost" }}
              {{- if .Values.seccompProfile.localhostProfile }}
seccompProfile:
  type: {{ .Values.seccompProfile.type }}
  localhostProfile: {{ .Values.seccompProfile.localhostProfile }}
            {{- end }}
          {{- else }}
seccompProfile:
  type: {{ .Values.seccompProfile.type }}
          {{- end }}
        {{- end }}
      {{- end }}
{{- end }}

{{/*
Merge eric-product-info, and user-defined annotations into a single set
of metadata annotations.
*/}}
{{- define "eric-oss-sftp-filetrans.annotations" -}}
  {{- $productInfo := include "eric-oss-sftp-filetrans.product-info" . | fromYaml -}}
  {{- $prometheus := include "eric-oss-sftp-filetrans.prometheus" . | fromYaml -}}
  {{- $config := include "eric-oss-sftp-filetrans.config-annotations" . | fromYaml -}}
  {{- include "eric-oss-sftp-filetrans.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $prometheus $config)) | trim }}
{{- end -}}

{{/*
Merge kubernetes-io-info, user-defined labels, and app and chart labels into a single set
of metadata labels.
*/}}
{{- define "eric-oss-sftp-filetrans.labels" -}}
  {{- $kubernetesIoInfo := include "eric-oss-sftp-filetrans.kubernetes-io-info" . | fromYaml -}}
  {{- $config := include "eric-oss-sftp-filetrans.config-labels" . | fromYaml -}}
  {{- include "eric-oss-sftp-filetrans.mergeLabels" (dict "location" .Template.Name "sources" (list $kubernetesIoInfo $config)) | trim }}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "eric-oss-sftp-filetrans.common-labels" }}
app: {{ .Chart.Name | quote }}
app.kubernetes.io/name: {{ .Chart.Name | quote }}
app.kubernetes.io/instance: {{ .Release.Name | quote }}
app.kubernetes.io/version: {{ .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" | quote }}
{{- end -}}

{{/*
Create Ericsson product app.kubernetes.io info
*/}}
{{- define "eric-oss-sftp-filetrans.kubernetes-io-info" -}}
helm.sh/chart: {{ include "eric-oss-sftp-filetrans.chart" . }}
{{- include "eric-oss-sftp-filetrans.common-labels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Create app and chart metadata labels
*/}}
{{- define "eric-oss-sftp-filetrans.app-and-chart-labels" -}}
app: {{ template "eric-oss-sftp-filetrans.name" . }}
chart: {{ template "eric-oss-sftp-filetrans.chart" . }}
{{- end -}}

{{/*
Create Ericsson Product Info
*/}}
{{- define "eric-oss-sftp-filetrans.product-info" -}}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end }}

{{/*
Create user-defined annotations
*/}}
{{ define "eric-oss-sftp-filetrans.config-annotations" }}
  {{- $global := (.Values.global).annotations -}}
  {{- $service := .Values.annotations -}}
  {{- include "eric-oss-sftp-filetrans.mergeAnnotations" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

Create log control configmap name.
*/}}
{{- define "eric-oss-sftp-filetrans.log-control-configmap.name" }}
  {{- include "eric-oss-sftp-filetrans.name" . | printf "%s-log-control-configmap" | quote }}
{{- end }}

{{/*
Create prometheus info
*/}}
{{- define "eric-oss-sftp-filetrans.prometheus" -}}
prometheus.io/path: {{ .Values.prometheus.path | quote }}
prometheus.io/port: {{ .Values.service.port | quote }}
prometheus.io/scrape: {{ .Values.prometheus.scrape | quote }}
{{- end -}}

{{/*
Create user-defined labels
*/}}
{{ define "eric-oss-sftp-filetrans.config-labels" }}
  {{- $global := (.Values.global).labels -}}
  {{- $service := .Values.labels -}}
  {{- include "eric-oss-sftp-filetrans.mergeLabels" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

{{/*
Define the role reference for security policy
*/}}
{{- define "eric-oss-sftp-filetrans.securityPolicy.reference" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.security -}}
      {{- if .Values.global.security.policyReferenceMap -}}
        {{ $mapped := index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" }}
        {{- if $mapped -}}
          {{ $mapped }}
        {{- else -}}
          default-restricted-security-policy
        {{- end -}}
      {{- else -}}
        default-restricted-security-policy
      {{- end -}}
    {{- else -}}
      default-restricted-security-policy
    {{- end -}}
  {{- else -}}
    default-restricted-security-policy
  {{- end -}}
{{- end -}}

{{/*
Define the annotations for security policy
*/}}
{{- define "eric-oss-sftp-filetrans.securityPolicy.annotations" -}}
# Automatically generated annotations for documentation purposes.
{{- end -}}

{{/*
Define Pod Disruption Budget value taking into account its type (int or string)
*/}}
{{- define "eric-oss-sftp-filetrans.pod-disruption-budget" -}}
  {{- if kindIs "string" .Values.podDisruptionBudget.minAvailable -}}
    {{- print .Values.podDisruptionBudget.minAvailable | quote -}}
  {{- else -}}
    {{- print .Values.podDisruptionBudget.minAvailable | atoi -}}
  {{- end -}}
{{- end -}}

{{/*
Define upper limit for TerminationGracePeriodSeconds
*/}}
{{- define "eric-oss-sftp-filetrans.terminationGracePeriodSeconds" -}}
{{- if .Values.terminationGracePeriodSeconds -}}
  {{- toYaml .Values.terminationGracePeriodSeconds -}}
{{- end -}}
{{- end -}}

{{/*
Create a merged set of nodeSelectors from global and service level.
*/}}
{{- define "eric-oss-sftp-filetrans.nodeSelector" -}}
{{- $globalValue := (dict) -}}
{{- if .Values.global -}}
    {{- if .Values.global.nodeSelector -}}
      {{- $globalValue = .Values.global.nodeSelector -}}
    {{- end -}}
{{- end -}}
{{- if .Values.nodeSelector -}}
  {{- range $key, $localValue := .Values.nodeSelector -}}
    {{- if hasKey $globalValue $key -}}
         {{- $Value := index $globalValue $key -}}
         {{- if ne $Value $localValue -}}
           {{- printf "nodeSelector \"%s\" is specified in both global (%s: %s) and service level (%s: %s) with differing values which is not allowed." $key $key $globalValue $key $localValue | fail -}}
         {{- end -}}
     {{- end -}}
    {{- end -}}
    {{- toYaml (merge $globalValue .Values.nodeSelector) | trim | nindent 2 -}}
{{- else -}}
  {{- if not ( empty $globalValue ) -}}
    {{- toYaml $globalValue | trim | nindent 2 -}}
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
    Define Image Pull Policy
*/}}
{{- define "eric-oss-sftp-filetrans.registryImagePullPolicy" -}}
    {{- $globalRegistryPullPolicy := "IfNotPresent" -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.imagePullPolicy -}}
                {{- $globalRegistryPullPolicy = .Values.global.registry.imagePullPolicy -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- print $globalRegistryPullPolicy -}}
{{- end -}}

{{- /*
Wrapper functions to set the contexts
*/ -}}
{{- define "eric-oss-sftp-filetrans.mergeAnnotations" -}}
  {{- include "eric-oss-sftp-filetrans.aggregatedMerge" (dict "context" "annotations" "location" .location "sources" .sources) }}
{{- end -}}
{{- define "eric-oss-sftp-filetrans.mergeLabels" -}}
  {{- include "eric-oss-sftp-filetrans.aggregatedMerge" (dict "context" "labels" "location" .location "sources" .sources) }}
{{- end -}}

{{- /*
Generic function for merging annotations and labels (version: 1.0.1)
{
    context: string
    sources: [[sourceData: {key => value}]]
}
This generic merge function is added to improve user experience
and help ADP services comply with the following design rules:
  - DR-D1121-060 (global labels and annotations)
  - DR-D1121-065 (annotations can be attached by application
                  developers, or by deployment engineers)
  - DR-D1121-068 (labels can be attached by application
                  developers, or by deployment engineers)
  - DR-D1121-160 (strings used as parameter value shall always
                  be quoted)
Installation or template generation of the Helm chart fails when:
  - same key is listed multiple times with different values
  - when the input is not string
IMPORTANT: This function is distributed between services verbatim.
Fixes and updates to this function will require services to reapply
this function to their codebase. Until usage of library charts is
supported in ADP, we will keep the function hardcoded here.
*/ -}}
{{- define "eric-oss-sftp-filetrans.aggregatedMerge" -}}
  {{- $merged := dict -}}
  {{- $context := .context -}}
  {{- $location := .location -}}
  {{- range $sourceData := .sources -}}
    {{- range $key, $value := $sourceData -}}
      {{- /* FAIL: when the input is not string. */ -}}
      {{- if not (kindIs "string" $value) -}}
        {{- $problem := printf "Failed to merge keys for \"%s\" in \"%s\": invalid type" $context $location -}}
        {{- $details := printf "in \"%s\": \"%s\"." $key $value -}}
        {{- $reason := printf "The merge function only accepts strings as input." -}}
        {{- $solution := "To proceed, please pass the value as a string and try again." -}}
        {{- printf "%s %s %s %s" $problem $details $reason $solution | fail -}}
      {{- end -}}
      {{- if hasKey $merged $key -}}
        {{- $mergedValue := index $merged $key -}}
        {{- /* FAIL: when there are different values for a key. */ -}}
        {{- if ne $mergedValue $value -}}
          {{- $problem := printf "Failed to merge keys for \"%s\" in \"%s\": key duplication in" $context $location -}}
          {{- $details := printf "\"%s\": (\"%s\", \"%s\")." $key $mergedValue $value -}}
          {{- $reason := printf "The same key cannot have different values." -}}
          {{- $solution := "To proceed, please resolve the conflict and try again." -}}
          {{- printf "%s %s %s %s" $problem $details $reason $solution | fail -}}
        {{- end -}}
      {{- end -}}
      {{- $_ := set $merged $key $value -}}
    {{- end -}}
  {{- end -}}
{{- /*
Strings used as parameter value shall always be quoted. (DR-D1121-160)
The below is a workaround to toYaml, which removes the quotes.
Instead we loop over and quote each value.
*/ -}}
{{- range $key, $value := $merged }}
{{ $key }}: {{ $value | quote }}
{{- end -}}
{{- end -}}

{{/*----------------------------------- Service mesh functions ----------------------------------*/}}

{{/*
DR-D470217-011 This helper defines the annotation which bring the service into the mesh.
*/}}
{{- define "eric-oss-sftp-filetrans.service-mesh-inject" }}
{{- if eq (include "eric-oss-sftp-filetrans.service-mesh-enabled" .) "true" }}
sidecar.istio.io/inject: "true"
{{- else -}}
sidecar.istio.io/inject: "false"
{{- end -}}
{{- end -}}

{{/*
DR-D470217-007-AD This helper defines whether this service enter the Service Mesh or not.
*/}}
{{- define "eric-oss-sftp-filetrans.service-mesh-enabled" }}
  {{- $globalMeshEnabled := "false" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
        {{- $globalMeshEnabled = .Values.global.serviceMesh.enabled -}}
    {{- end -}}
  {{- end -}}
  {{- $globalMeshEnabled -}}
{{- end -}}


{{- define "eric-oss-sftp-filetrans.istio-proxy-config-annotation" }}
{{- if eq (include "eric-oss-sftp-filetrans.service-mesh-enabled" .) "true" }}
proxy.istio.io/config: '{ "holdApplicationUntilProxyStarts": true }'
{{- end -}}
{{- end -}}


{{/*
GL-D470217-080-AD
This helper captures the service mesh version from the integration chart to
annotate the workloads so they are redeployed in case of service mesh upgrade.
*/}}
{{- define "eric-oss-sftp-filetrans.service-mesh-version" }}
{{- if eq (include "eric-oss-sftp-filetrans.service-mesh-enabled" .) "true" }}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
      {{- if .Values.global.serviceMesh.annotations -}}
        {{ .Values.global.serviceMesh.annotations | toYaml }}
      {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- end -}}

{/*
Define JVM heap size ((DR-D1126-010 | DR-D1126-011))
*/}}
{{- define "eric-oss-sftp-filetrans.jvmHeapSettings" -}}
    {{- $initRAM := "" -}}
    {{- $maxRAM := "" -}}
    {{/*
       ramLimit is set by default to 1.0, this is if the service is set to use anything less than M/Mi
       Rather than trying to cover each type of notation,
       if a user is using anything less than M/Mi then the assumption is its less than the cutoff of 1.3GB
       */}}
    {{- $ramLimit := 1.0 -}}
    {{- $ramComparison := 1.3 -}}

    {{- if not (index .Values "resources" "eric-oss-sftp-filetrans" "limits" "memory") -}}
        {{- fail "memory limit for eric-oss-sftp-filetrans is not specified" -}}
    {{- end -}}

    {{- if (hasSuffix "Gi" (index .Values "resources" "eric-oss-sftp-filetrans" "limits" "memory")) -}}
        {{- $ramLimit = trimSuffix "Gi" (index .Values "resources" "eric-oss-sftp-filetrans" "limits" "memory") | float64 -}}
    {{- else if (hasSuffix "G" (index .Values "resources" "eric-oss-sftp-filetrans" "limits" "memory")) -}}
        {{- $ramLimit = trimSuffix "G" (index .Values "resources" "eric-oss-sftp-filetrans" "limits" "memory") | float64 -}}
    {{- else if (hasSuffix "Mi" (index .Values "resources" "eric-oss-sftp-filetrans" "limits" "memory")) -}}
        {{- $ramLimit = (div (trimSuffix "Mi" (index .Values "resources" "eric-oss-sftp-filetrans" "limits" "memory") | float64) 1000) | float64  -}}
    {{- else if (hasSuffix "M" (index .Values "resources" "eric-oss-sftp-filetrans" "limits" "memory")) -}}
        {{- $ramLimit = (div (trimSuffix "M" (index .Values "resources" "eric-oss-sftp-filetrans" "limits" "memory")| float64) 1000) | float64  -}}
    {{- end -}}


    {{- if (index .Values "resources" "eric-oss-sftp-filetrans" "jvm") -}}
        {{- if (index .Values "resources" "eric-oss-sftp-filetrans" "jvm" "initialMemoryAllocationPercentage") -}}
            {{- $initRAM = index .Values "resources" "eric-oss-sftp-filetrans" "jvm" "initialMemoryAllocationPercentage" | float64 -}}
            {{- $initRAM = printf "-XX:InitialRAMPercentage=%f" $initRAM -}}
        {{- else -}}
            {{- fail "initialMemoryAllocationPercentage not set" -}}
        {{- end -}}
        {{- if and (index .Values "resources" "eric-oss-sftp-filetrans" "jvm" "smallMemoryAllocationMaxPercentage") (index .Values "resources" "eric-oss-sftp-filetrans" "jvm" "largeMemoryAllocationMaxPercentage") -}}
            {{- if lt $ramLimit $ramComparison -}}
                {{- $maxRAM = index .Values "resources" "eric-oss-sftp-filetrans" "jvm" "smallMemoryAllocationMaxPercentage" | float64 -}}
                {{- $maxRAM = printf "-XX:MaxRAMPercentage=%f" $maxRAM -}}
            {{- else -}}
                {{- $maxRAM = index .Values "resources" "eric-oss-sftp-filetrans" "jvm" "largeMemoryAllocationMaxPercentage" | float64 -}}
                {{- $maxRAM = printf "-XX:MaxRAMPercentage=%f" $maxRAM -}}
            {{- end -}}
        {{- else -}}
            {{- fail "smallMemoryAllocationMaxPercentage | largeMemoryAllocationMaxPercentage not set" -}}
        {{- end -}}
    {{- else -}}
        {{- fail "jvm heap percentages are not set" -}}
    {{- end -}}
{{- printf "%s %s" $initRAM $maxRAM -}}
{{- end -}}


{{/*
Define the log streaming method (DR-470222-010)
*/}}
{{- define "eric-oss-sftp-filetrans.streamingMethod" -}}
{{- $streamingMethod := "direct" -}}
{{- if .Values.global -}}
  {{- if .Values.global.log -}}
      {{- if .Values.global.log.streamingMethod -}}
        {{- $streamingMethod = .Values.global.log.streamingMethod }}
      {{- end -}}
  {{- end -}}
{{- end -}}
{{- if .Values.log -}}
  {{- if .Values.log.streamingMethod -}}
    {{- $streamingMethod = .Values.log.streamingMethod }}
  {{- end -}}
{{- end -}}
{{- print $streamingMethod -}}
{{- end -}}

{{/*
Define the label needed for reaching eric-log-transformer (DR-470222-010)
*/}}
{{- define "eric-oss-sftp-filetrans.directStreamingLabel" -}}
{{- $streamingMethod := (include "eric-oss-sftp-filetrans.streamingMethod" .) -}}
{{- if or (eq "direct" $streamingMethod) (eq "dual" $streamingMethod) }}
logger-communication-type: "direct"
{{- end -}}
{{- end -}}

{{/*
Define logging environment variables (DR-470222-010)
*/}}
{{ define "eric-oss-sftp-filetrans.loggingEnv" }}
{{- $streamingMethod := (include "eric-oss-sftp-filetrans.streamingMethod" .) -}}
{{- if .Values.log -}}
  {{- if .Values.log.streamingMethod }}
- name: LOG_CTRL_FILE
  value: "{{ .Values.log.logControlFile }}"
  {{- end -}}
  {{- if index .Values "log" "runtime-level-control" -}}
    {{- if index .Values "log" "runtime-level-control" "enabled" }}
- name: RUN_TIME_LEVEL_CONTROL
  value: "{{ index .Values "log" "runtime-level-control" "enabled" }}"
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- if or (eq "direct" $streamingMethod) (eq "dual" $streamingMethod) -}}
  {{- if eq "direct" $streamingMethod }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-http.xml"
  {{- end }}
  {{- if eq "dual" $streamingMethod }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-dual.xml"
  {{- end }}
- name: LOGSTASH_DESTINATION
  value: eric-log-transformer
- name: LOGSTASH_PORT
  value: "9080"
- name: POD_NAME
  valueFrom:
    fieldRef:
      fieldPath: metadata.name
- name: POD_UID
  valueFrom:
    fieldRef:
      fieldPath: metadata.uid
- name: CONTAINER_NAME
  value: eric-oss-sftp-filetrans
- name: NODE_NAME
  valueFrom:
    fieldRef:
      fieldPath: spec.nodeName
- name: NAMESPACE
  valueFrom:
    fieldRef:
      fieldPath: metadata.namespace
{{- else if eq $streamingMethod "indirect" }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-json.xml"
{{- else }}
  {{- fail ".log.streamingMethod unknown" }}
{{- end -}}
{{ end }}

{{/*
This helper defines the annotation for define service mesh volume.
*/}}
{{- define "eric-oss-sftp-filetrans.service-mesh-volume" -}}
{{- if and (eq (include "eric-oss-sftp-filetrans.service-mesh-enabled" .) "true") (eq (include "eric-oss-sftp-filetrans.global-security-tls-enabled" .) "true") (eq (include "eric-oss-sftp-filetrans.esoa-ism2osm-enabled" .) "false") }}
sidecar.istio.io/userVolume: '{"{{ include "eric-oss-sftp-filetrans.name" . }}-kafka-certs-tls":{"secret":{"secretName":"{{ include "eric-oss-sftp-filetrans.name" . }}-kafka-secret","optional":true}},"{{ include "eric-oss-sftp-filetrans.name" . }}-bdr-certs-tls":{"secret":{"secretName":"{{ include "eric-oss-sftp-filetrans.name" . }}-bdr-secret","optional":true}},"{{ include "eric-oss-sftp-filetrans.name" . }}-certs-ca-tls":{"secret":{"secretName":"eric-sec-sip-tls-trusted-root-cert"}}}'
sidecar.istio.io/userVolumeMount: '{"{{ include "eric-oss-sftp-filetrans.name" . }}-kafka-certs-tls":{"mountPath":"/etc/istio/tls/eric-oss-dmm-kf-op-sz-kafka-bootstrap/","readOnly":true},"{{ include "eric-oss-sftp-filetrans.name" . }}-bdr-certs-tls":{"mountPath":"/etc/istio/tls/eric-data-object-storage-mn/","readOnly":true},"{{ include "eric-oss-sftp-filetrans.name" . }}-certs-ca-tls":{"mountPath":"/etc/istio/tls-ca","readOnly":true}}'
{{ end }}
{{- end -}}

{{/*
This helper defines which out-mesh services will be reached by this one.
*/}}
{{- define "eric-oss-sftp-filetrans.service-mesh-ism2osm-labels" -}}
{{- if eq (include "eric-oss-sftp-filetrans.service-mesh-enabled" .) "true" }}
  {{- if eq (include "eric-oss-sftp-filetrans.global-security-tls-enabled" .) "true" }}
eric-oss-dmm-kf-op-sz-kafka-ism-access: "true"
eric-data-object-storage-mn-ism-access: "true"
  {{- end }}
{{- end }}
{{- end -}}

{{/*
This helper defines label for object-storage-mn-access.
*/}}
{{- define "eric-oss-sftp-filetrans.eric-data-object-storage-mn-access-label" -}}
eric-data-object-storage-mn-access: "true"
{{- end -}}

{{- define "eric-oss-sftp-filetrans.service-mesh-proxy-config" }}
{{- if eq (include "eric-oss-sftp-filetrans.service-mesh-enabled" .) "true" }}
proxy.istio.io/config: |
  holdApplicationUntilProxyStarts: true
{{- end -}}
{{- end -}}

{{/*
This helper defines the script for terminating the side car container by job container.
*/}}
{{- define "eric-oss-sftp-filetrans.service-mesh-sidecar-quit" }}
{{- if eq (include "eric-oss-sftp-filetrans.service-mesh-enabled" .) "true" }}
curl -X POST http://127.0.0.1:15020/quitquitquit
{{- end -}}
{{- end -}}

{{/*
check global.security.tls.enabled
*/}}
{{- define "eric-oss-sftp-filetrans.global-security-tls-enabled" -}}
{{- if  .Values.global -}}
  {{- if  .Values.global.security -}}
    {{- if  .Values.global.security.tls -}}
      {{- .Values.global.security.tls.enabled | toString -}}
    {{- else -}}
      {{- "false" -}}
    {{- end -}}
  {{- else -}}
    {{- "false" -}}
  {{- end -}}
{{- else -}}
  {{- "false" -}}
{{- end -}}
{{- end -}}

{{/*
Create Service Mesh Egress enabling option
*/}}
{{- define "eric-oss-sftp-filetrans.service-mesh-egress-enabled" }}
  {{- $globalMeshEgressEnabled := "false" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
      {{- if .Values.global.serviceMesh.egress -}}
        {{- $globalMeshEgressEnabled = .Values.global.serviceMesh.egress.enabled -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
  {{- $globalMeshEgressEnabled -}}
{{- end -}}

{{/*
This helper defines permissive network policy for external access
*/}}
{{- define "eric-oss-sftp-filetrans.service-mesh-egress-gateway-access-label" }}
{{- $serviceMesh := include "eric-oss-sftp-filetrans.service-mesh-enabled" . | trim -}}
{{- $serviceMeshEgress := include "eric-oss-sftp-filetrans.service-mesh-egress-enabled" . | trim -}}
{{- if and (eq $serviceMesh "true") (eq $serviceMeshEgress "true") -}}
service-mesh-egress-gateway-access: "true"
{{- end -}}
{{- end -}}

{{/*
Sftp service accounts secret key.
*/}}
{{- define "eric-oss-sftp-filetrans-bdrReadWriteSecretKey" -}}
{{- $serviceAccountSecret := printf "%s-%s" (include "eric-oss-sftp-filetrans.name" . ) "bdr-svc-account-secret" -}}
{{- if not (empty .Values.bdr.serviceAccountSecretName) }}
	{{- $serviceAccountSecret := .Values.bdr.serviceAccountSecretName -}}
{{- end -}}
{{- $secretObj := (lookup "v1" "Secret" .Release.Namespace $serviceAccountSecret) | default dict }}
{{- $secretData := (get $secretObj "data") | default dict }}
{{- $sftpBDRSecretKey := (get $secretData "sftp-bdr-secret-key") | default (randAlphaNum 14 | b64enc) }}
{{- print $sftpBDRSecretKey -}}
{{- end -}}

{{/*
Sftp service accounts access key.
*/}}
{{- define "eric-oss-sftp-filetrans-bdrReadWriteAccessKey" -}}
{{- $serviceAccountSecret := printf "%s-%s" (include "eric-oss-sftp-filetrans.name" . ) "bdr-svc-account-secret" -}}
{{- if not (empty .Values.bdr.serviceAccountSecretName) }}
	{{- $serviceAccountSecret := .Values.bdr.serviceAccountSecretName -}}
{{- end -}}
{{- $secretObj := (lookup "v1" "Secret" .Release.Namespace $serviceAccountSecret) | default dict }}
{{- $secretData := (get $secretObj "data") | default dict }}
{{- $sftpBDRAccessKey := (get $secretData "sftp-bdr-access-key") | default (randAlphaNum 14 | b64enc) }}
{{- print $sftpBDRAccessKey -}}
{{- end -}}

{{/*
Parser service accounts secret key.
*/}}
{{- define "eric-oss-sftp-filetrans-bdrReadOnlySecretKey" -}}
{{- $serviceAccountSecret := printf "%s-%s" (include "eric-oss-sftp-filetrans.name" . ) "bdr-svc-account-secret" -}}
{{- if not (empty .Values.bdr.serviceAccountSecretName) }}
	{{- $serviceAccountSecret := .Values.bdr.serviceAccountSecretName -}}
{{- end -}}
{{- $secretObj := (lookup "v1" "Secret" .Release.Namespace $serviceAccountSecret) | default dict }}
{{- $secretData := (get $secretObj "data") | default dict }}
{{- $parserBDRSecretKey := (get $secretData "parser-bdr-secret-key") | default (randAlphaNum 14 | b64enc) }}
{{- print $parserBDRSecretKey -}}
{{- end -}}

{{/*
Parser service accounts access key.
*/}}
{{- define "eric-oss-sftp-filetrans-bdrReadOnlyAccessKey" -}}
{{- $serviceAccountSecret := printf "%s-%s" (include "eric-oss-sftp-filetrans.name" . ) "bdr-svc-account-secret" -}}
{{- if not (empty .Values.bdr.serviceAccountSecretName) }}
	{{- $serviceAccountSecret := .Values.bdr.serviceAccountSecretName -}}
{{- end -}}
{{- $secretObj := (lookup "v1" "Secret" .Release.Namespace $serviceAccountSecret) | default dict }}
{{- $secretData := (get $secretObj "data") | default dict }}
{{- $parserBDRAccessKey := (get $secretData "parser-bdr-access-key") | default (randAlphaNum 14 | b64enc) }}
{{- print $parserBDRAccessKey -}}
{{- end -}}


{{/*
Define RoleBinding (DR-1123-134)
*/}}
{{- define "eric-oss-sftp-filetrans.securityPolicyRoleKind" -}}
  {{- $roleKind := "" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.securityPolicy -}}
      {{- if .Values.global.securityPolicy.rolekind -}}
        {{- if or (eq "Role" (.Values.global.securityPolicy).rolekind) (eq "ClusterRole" (.Values.global.securityPolicy).rolekind) -}}
          {{- $roleKind = .Values.global.securityPolicy.rolekind -}}
        {{- end -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
  {{- print $roleKind -}}
{{- end -}}

{{/*
Define RoleName to SecurityPolicy - DR-D1123-134
*/}}
{{- define "eric-oss-sftp-filetrans.securityPolicyRolename" -}}
{{- $rolename := (include "eric-oss-sftp-filetrans.name" .) -}}
{{- if .Values.securityPolicy -}}
    {{- if .Values.securityPolicy.rolename -}}
        {{- $rolename = .Values.securityPolicy.rolename -}}
    {{- end -}}
{{- end -}}
{{- $rolename -}}
{{- end -}}


{{/*
Define RolebindingName to SecurityPolicy - DR-D1123-134
*/}}
{{- define "eric-oss-sftp-filetrans.securityPolicy.rolebindingName" -}}
{{- $rolekind := "" -}}
{{- if .Values.global -}}
    {{- if .Values.global.securityPolicy -}}
        {{- if .Values.global.securityPolicy.rolekind -}}
            {{- $rolekind = .Values.global.securityPolicy.rolekind -}}
            {{- if (eq $rolekind "Role") -}}
               {{- print (include "eric-oss-sftp-filetrans.serviceAccountName" .) "-r-" (include "eric-oss-sftp-filetrans.securityPolicyRolename" .) "-sp" -}}
            {{- else if (eq $rolekind "ClusterRole") -}}
               {{- print (include "eric-oss-sftp-filetrans.serviceAccountName" .) "-c-" (include "eric-oss-sftp-filetrans.securityPolicyRolename" .) "-sp" -}}
            {{- end }}
        {{- end }}
    {{- end -}}
{{- end -}}
{{- end -}}

{{/*
This helper defines whether ism2osm to eric-esoa-subsystem-management enabled or not.
*/}}
{{- define "eric-oss-sftp-filetrans.esoa-ism2osm-enabled" }}
  {{- $esoaIsm2osmEnabled := "false" -}}
  {{- if .Values.ism2osm -}}
    {{- if .Values.ism2osm.outMeshService -}}
      {{- if .Values.ism2osm.outMeshService.ericEsoaSubsystemManagement -}}
        {{- if .Values.ism2osm.outMeshService.ericEsoaSubsystemManagement.enabled -}}
          {{- $esoaIsm2osmEnabled = .Values.ism2osm.outMeshService.ericEsoaSubsystemManagement.enabled -}}
        {{- end -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
  {{- $esoaIsm2osmEnabled -}}
{{- end -}}

{{/*
This helper adds label for esoa out-mesh service eric-esoa-subsystem-management that will be reached by this one.
*/}}
{{- define "eric-oss-sftp-filetrans.esoa-service-mesh-ism2osm-labels" }}
{{- if eq (include "eric-oss-sftp-filetrans.service-mesh-enabled" .) "true" }}
  {{- if eq (include "eric-oss-sftp-filetrans.global-security-tls-enabled" .) "true" }}
    {{- if eq (include "eric-oss-sftp-filetrans.esoa-ism2osm-enabled" .) "true" }}
eric-esoa-subsystem-management-ism-access: "true"
    {{- end }}
  {{- end }}
{{- end -}}
{{- end -}}

{{/*
This helper defines the annotation for define service mesh volume.
*/}}
{{- define "eric-oss-sftp-filetrans.esoa-service-mesh-volume" }}
{{- if and (eq (include "eric-oss-sftp-filetrans.service-mesh-enabled" .) "true") (eq (include "eric-oss-sftp-filetrans.global-security-tls-enabled" .) "true") (eq (include "eric-oss-sftp-filetrans.esoa-ism2osm-enabled" .) "true") }}
sidecar.istio.io/userVolume: '{"{{ include "eric-oss-sftp-filetrans.name" . }}-kafka-certs-tls":{"secret":{"secretName":"{{ include "eric-oss-sftp-filetrans.name" . }}-kafka-secret","optional":true}},"{{ include "eric-oss-sftp-filetrans.name" . }}-bdr-certs-tls":{"secret":{"secretName":"{{ include "eric-oss-sftp-filetrans.name" . }}-bdr-secret","optional":true}},"{{ include "eric-oss-sftp-filetrans.name" . }}-certs-ca-tls":{"secret":{"secretName":"eric-sec-sip-tls-trusted-root-cert"}},"{{ include "eric-oss-sftp-filetrans.name" . }}-ssm-certs-tls":{"secret":{"secretName":"{{ include "eric-oss-sftp-filetrans.name" . }}-ssm-secret","optional":true}}}'
sidecar.istio.io/userVolumeMount: '{"{{ include "eric-oss-sftp-filetrans.name" . }}-kafka-certs-tls":{"mountPath":"/etc/istio/tls/eric-oss-dmm-kf-op-sz-kafka-bootstrap/","readOnly":true},"{{ include "eric-oss-sftp-filetrans.name" . }}-bdr-certs-tls":{"mountPath":"/etc/istio/tls/eric-data-object-storage-mn/","readOnly":true},"{{ include "eric-oss-sftp-filetrans.name" . }}-certs-ca-tls":{"mountPath":"/etc/istio/tls-ca","readOnly":true},"{{ include "eric-oss-sftp-filetrans.name" . }}-ssm-certs-tls":{"mountPath":"/etc/istio/tls/eric-esoa-subsystem-management/","readOnly":true}}'
{{ end }}
{{- end -}}