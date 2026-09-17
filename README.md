# jenkins-shared-library

Jenkins Shared Library con utilidades reutilizables para pipelines de CI/CD.

## Funciones disponibles (`vars/`)

### Publicación de informes de calidad

| Función | Parámetros | Descripción |
|---|---|---|
| `publishCheckstyleReport` | `checkstyleFile` | Publica el informe de Checkstyle usando el plugin Warnings NG. |
| `publishCloverReport` | `cloverReportDir`, `cloverReportFileName` | Publica el informe de cobertura de código generado por Clover. |
| `publishCpdReport` | `cpdFile` | Publica el informe de duplicación de código (CPD) usando el plugin Warnings NG. |
| `publishDeptracGraph` | `deptracDir`, `deptracFile` | Publica el grafo de dependencias generado por Deptrac como informe HTML en Jenkins. |
| `publishJunitReport` | `junitFile` | Publica los resultados de tests JUnit. |

### Notificaciones

| Función | Parámetros | Descripción |
|---|---|---|
| `slackSendMessage` | `buildStatus`, `time` | Envía un mensaje a Slack con el resultado del build (SUCCESS / UNSTABLE / FAILURE) y el tiempo transcurrido. |
| `notifyCompassDeployment` | `compassCloudId`, `componentId`, `state`, `environment`, `startedAt`, `completedAt`, `pipelineName` | Notifica un evento de despliegue al componente correspondiente en Atlassian Compass. Mapea automáticamente el entorno a `DEVELOPMENT`, `STAGING` o `PRODUCTION`. |
| `notifyJiraDeployment` | `cloudId`, `componentId`, `state`, `environment`, `startedAt`, `completedAt`, `pipelineName` | Notifica un evento de despliegue a Jira Deployments API. Mapea el entorno a `development`, `staging` o `production`. |
| `notifySentryDeployment` | `orgSlug`, `projectSlug`, `version`, `environment`, `startedAt`, `completedAt`, `pipelineName`, `commitSha` (opcional), `repoSlug` (opcional) | Crea (si no existe) el release en Sentry y registra un deploy asociado con entorno, fecha y enlace al build de Jenkins. `version` debe coincidir exactamente con el valor que envía el SDK como tag `release` (p. ej. `api_version` en el proyecto `api`); si se pasa como una rama tipo `release/1.30`, se usa el segmento tras la última `/` (Sentry no permite `/` en el nombre de versión). Si se pasa `commitSha`, el release se asocia a ese commit (`refs`) para que Sentry resuelva los commits y el suspect commit en los issues — requiere tener la integración de Bitbucket instalada en Sentry. `repoSlug` por defecto es igual a `projectSlug`; indícalo si el repo de Bitbucket tiene otro nombre (p. ej. proyecto Sentry `raplus` con repo `horus`). |
| `resolveBitbucketCommit` | `workspace`, `repoSlug`, `branch` | Resuelve el SHA del HEAD de una rama en un repo de Bitbucket Cloud vía la API REST. Pensado para obtener el `commitSha` que espera `notifySentryDeployment` antes de desplegar. Devuelve cadena vacía (sin fallar el build) si no puede resolverlo. |

### Utilidades de Docker

| Función | Parámetros | Descripción |
|---|---|---|
| `checkContainerStatus` | `container`, `step` | Inspecciona el exit code de un contenedor Docker y pone el build en `FAILURE` si es distinto de 0. Almacena el resultado en `env.<step>_result`. |

## Credenciales requeridas

Las funciones de notificación a Atlassian requieren las siguientes credenciales configuradas en Jenkins:

- **`COMPASS_BASIC_AUTH`** — usuario y token de Atlassian (Basic Auth). Usado por `notifyCompassDeployment` y `notifyJiraDeployment`.
- **`SENTRY_AUTH_TOKEN`** — Secret text con un token de Sentry (org `logalty`) con scope `project:releases`. Usado por `notifySentryDeployment`.
- **`bitbucket-check-version`** — Credencial Username/Password de Bitbucket (workspace `firmapro`), la misma que ya usa el job `check-versions` con `check_versions_report.py`. Usada por `resolveBitbucketCommit`. (No usar `bitbucket_token_read`: su username está emparejado con un token del esquema legacy y da 401.)

## Uso en Jenkinsfile

```groovy
@Library('jenkins-shared-library') _

pipeline {
    agent any
    stages {
        stage('Test') {
            steps {
                sh 'mvn test'
                publishJunitReport('target/surefire-reports/*.xml')
                publishCheckstyleReport('target/checkstyle-result.xml')
                publishCloverReport('target/site/clover', 'clover.xml')
            }
        }
        stage('Deploy') {
            steps {
                // ... deploy logic ...
                notifyCompassDeployment(
                    'your-cloud-id',
                    'ari:cloud:compass:...',
                    'SUCCESSFUL',
                    'production',
                    '2026-01-01T10:00:00Z',
                    '2026-01-01T10:05:00Z',
                    env.JOB_NAME
                )
                def commitSha = resolveBitbucketCommit('firmapro', 'api', 'release/1.30')
                notifySentryDeployment(
                    'logalty',
                    'api',
                    env.API_VERSION,
                    'production',
                    '2026-01-01T10:00:00Z',
                    '2026-01-01T10:05:00Z',
                    env.JOB_NAME,
                    commitSha
                )
            }
        }
    }
    post {
        always {
            slackSendMessage(currentBuild.result, currentBuild.durationString)
        }
    }
}
```
