package com.giyeok.bibix.plugins.maven

import com.giyeok.bibix.base.*
import com.giyeok.bibix.plugins.jvm.ClassPkg
import com.giyeok.bibix.plugins.jvm.JarInfo
import com.giyeok.bibix.plugins.jvm.MavenDep
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.*
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.collection.DependencyCollectionContext
import org.eclipse.aether.collection.DependencySelector
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transfer.AbstractTransferListener
import org.eclipse.aether.transfer.MetadataNotFoundException
import org.eclipse.aether.transfer.TransferEvent
import org.eclipse.aether.transfer.TransferResource
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.filter.DependencyFilterUtils
import java.nio.file.Path
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Artifact {
  data class MavenArtifactName(val group: String, val artifact: String, val version: String?)

  fun build(context: BuildContext): BuildRuleReturn {
    if (!context.hashChanged &&
      context.prevBuildTime != null &&
      context.prevResult != null &&
      Duration.between(
        context.prevBuildTime,
        Instant.now()
      ) <= Duration.ofHours(6)
    ) {
      return BuildRuleReturn.value(context.prevResult!!)
    }
    val mavenReposDir = context.getSharedDirectory(sharedRepoName)
    return BuildRuleReturn.withDirectoryLock(mavenReposDir) {
      // TODO resolve 결과 캐시해놨다 그대로 사용
      // TODO -> bibix value proto로 저장/로드할 때 CName sourceId 복구 문제
      // TODO -> 프로젝트 전체 디렉토리를 옮겼을 때 문제가 없을지 확인
      val groupId = (context.arguments.getValue("group") as StringValue).value
      val artifactId = (context.arguments.getValue("artifact") as StringValue).value
      val extension = (context.arguments.getValue("extension") as StringValue).value
      val version = (context.arguments["version"] as? StringValue)?.value
      val classifier = (context.arguments["classifier"] as? StringValue)?.value
      val scope = (context.arguments.getValue("scope") as EnumValue).value
      val javaScope = when (scope) {
        "compile" -> JavaScopes.COMPILE
        "test" -> JavaScopes.TEST
        else -> TODO()
      }
      val repos = (context.arguments.getValue("repos") as ListValue).values.map { resolver ->
        0
        // TODO
//      val tuple = ((resolver as DataClassInstanceValue).value as NamedTupleValue)
//      Pair(
//        (tuple.pairs[0].second as EnumValue).value,
//        (tuple.pairs[1].second as StringValue).value
//      )
      }
      val excludes = (context.arguments.getValue("excludes") as SetValue).values.map { value ->
        value as ClassInstanceValue
        MavenArtifactName(
          group = value.getStringField("group"),
          artifact = value.getStringField("artifact"),
          version = value.getNullableStringField("version"),
        )
      }.toSet()

//    val repositories = listOf(
//      RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build()
//    )

      val dep = resolveArtifact(
        context.buildEnv,
        context.progressLogger,
        mavenReposDir,
        groupId,
        artifactId,
        extension,
        version,
        classifier,
        scope,
        javaScope,
        repos,
        excludes,
      )
      BuildRuleReturn.value(dep.toBibix())
    }
  }

  companion object {
    const val sharedRepoName = "com.giyeok.bibix.plugins.maven"
    fun resolveArtifact(
      buildEnv: BuildEnv,
      logger: ProgressLogger,
      mavenReposDir: Path,
      groupId: String,
      artifactId: String,
      extension: String,
      version: String?,
      classifier: String?,
      scope: String,
      javaScope: String,
      repos: List<Int>,
      excludes: Set<MavenArtifactName>,
    ): ClassPkg {
      val system: RepositorySystem = newRepositorySystem()
      val session: RepositorySystemSession =
        newRepositorySystemSession(mavenReposDir, system, buildEnv, logger)

      val artifact = DefaultArtifact(groupId, artifactId, classifier ?: "", extension, version)
      val repositories = newRepositories(system, session)

      val artifactRequest = ArtifactRequest()
      artifactRequest.artifact = artifact
      artifactRequest.repositories = repositories

      val resolved = system.resolveArtifact(session, artifactRequest)
      logger.logInfo("$resolved")

      val collectRequest = CollectRequest()
      collectRequest.root = Dependency(artifact, javaScope)
      collectRequest.repositories = repositories
      val classpathFilter = DependencyFilterUtils.classpathFilter(javaScope)
      val dependencyRequest = DependencyRequest(collectRequest, classpathFilter)
      dependencyRequest.setFilter { node, parents ->
        val name1 =
          MavenArtifactName(node.artifact.groupId, node.artifact.artifactId, node.artifact.version)
        val name2 = MavenArtifactName(node.artifact.groupId, node.artifact.artifactId, null)
        !excludes.contains(name1) && !excludes.contains(name2)
      }
      val dependencyResult = system.resolveDependencies(session, dependencyRequest)
//    println("dependencies: $dependencyResult")
//    println(dependencyResult.artifactResults)

      val artifactsMap = dependencyResult.artifactResults.associateBy { it.artifact }

      fun traverse(node: DependencyNode): ClassPkg {
        val artifactResult = artifactsMap.getValue(node.artifact)
        // TODO provided, system은 어떻게 하는게 맞지? 일단 나는 안 쓰고 있어서..
        val availableChildren = node.children.filter { artifactsMap.containsKey(it.artifact) }
        val compileDeps = availableChildren.filter { it.dependency.scope == "compile" }
        val runtimeDeps = availableChildren.filter { it.dependency.scope == "runtime" }
        return ClassPkg(
          mavenDep("central", artifactResult.artifact),
          JarInfo(artifactResult.artifact.file.toPath(), null),
          compileDeps.map { traverse(it) },
          runtimeDeps.map { traverse(it) },
          listOf()
        )
      }

      return traverse(dependencyResult.root)
    }

    private fun mavenDep(repo: String, artifact: Artifact): MavenDep =
      MavenDep(repo, artifact.groupId, artifact.artifactId, artifact.version, artifact.classifier)

    private fun newRepositorySystem(): RepositorySystem {
      // from https://github.com/snyk/aether-demo/blob/master/aether-demo-snippets/src/main/java/org/eclipse/aether/examples/manual/ManualRepositorySystemFactory.java
      val locator: DefaultServiceLocator = MavenRepositorySystemUtils.newServiceLocator()
      locator.addService(
        RepositoryConnectorFactory::class.java,
        BasicRepositoryConnectorFactory::class.java
      )
      locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
      locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
      locator.setErrorHandler(object: DefaultServiceLocator.ErrorHandler() {
        override fun serviceCreationFailed(type: Class<*>?, impl: Class<*>?, exception: Throwable) {
          exception.printStackTrace()
        }
      })
      return locator.getService(RepositorySystem::class.java)
    }

    fun newRepositorySystemSession(
      localRepoBaseDirectory: Path,
      system: RepositorySystem,
      buildEnv: BuildEnv,
      logger: ProgressLogger
    ): DefaultRepositorySystemSession {
      val session = MavenRepositorySystemUtils.newSession()

      val localRepo = LocalRepository(localRepoBaseDirectory.toFile())
      session.localRepositoryManager = system.newLocalRepositoryManager(session, localRepo)
      session.transferListener = ConsoleTransferListener(logger)
      session.repositoryListener = ConsoleRepositoryListener(logger)
      session.updatePolicy = RepositoryPolicy.UPDATE_POLICY_DAILY

      // -Dos.detected.name=linux -Dos.detected.arch=x86_64 -Dos.detected.classifier=linux-x86_64
      val osName = when (buildEnv.os) {
        is OS.Linux -> "linux"
        is OS.MacOSX -> "macosx"
        is OS.Windows -> "windows"
        OS.Unknown -> "???"
      }
      session.setSystemProperty("os.detected.name", osName)
      val archName = when (buildEnv.arch) {
        Architecture.X86_64 -> "x86_64"
        Architecture.X86 -> "x86"
        Architecture.Aarch_64 -> "aarch_64"
        Architecture.Unknown -> "???"
      }
      session.setSystemProperty("os.detected.arch", archName)
      session.setSystemProperty("os.detected.classifier", "$osName-$archName")

      session.dependencySelector = object: DependencySelector {
        override fun selectDependency(dependency: Dependency): Boolean {
          // TODO 그 외 scope은? 일단 test는 bibix에서는 필요 없을 것 같긴 한데
          return dependency.scope == "compile" || dependency.scope == "runtime"
        }

        override fun deriveChildSelector(context: DependencyCollectionContext): DependencySelector {
          return this
        }
      }

      // uncomment to generate dirty trees
      // session.setDependencyGraphTransformer( null );
      return session
    }

    fun newRepositories(
      system: RepositorySystem,
      session: RepositorySystemSession
    ): List<RemoteRepository> {
      return ArrayList(
        listOf(
          RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/")
            .build()
        )
      )
    }

    class ConsoleTransferListener(private val logger: ProgressLogger): AbstractTransferListener() {
      private val downloads: MutableMap<TransferResource, Long> =
        ConcurrentHashMap<TransferResource, Long>()
      private var lastLength = 0

      override fun transferInitiated(event: TransferEvent) {
        val message =
          if (event.requestType === TransferEvent.RequestType.PUT) "Uploading" else "Downloading"
        logger.logInfo("$message: ${event.resource.repositoryUrl}${event.resource.resourceName}")
      }

      override fun transferProgressed(event: TransferEvent) {
        val resource: TransferResource = event.resource
        downloads[resource] = java.lang.Long.valueOf(event.transferredBytes)
        val buffer = StringBuilder(64)
        for ((key, complete) in downloads) {
          val total: Long = key.contentLength
          buffer.append(getStatus(complete, total)).append("  ")
        }
        val pad = lastLength - buffer.length
        lastLength = buffer.length
        pad(buffer, pad)
        buffer.append('\r')
        logger.logVerbose(buffer.toString())
      }

      private fun getStatus(complete: Long, total: Long): String {
        return if (total >= 1024) {
          toKB(complete).toString() + "/" + toKB(total) + " KB "
        } else if (total >= 0) {
          "$complete/$total B "
        } else if (complete >= 1024) {
          toKB(complete).toString() + " KB "
        } else {
          "$complete B "
        }
      }

      private fun pad(buffer: StringBuilder, spaces: Int) {
        var spaces = spaces
        val block = "                                        "
        while (spaces > 0) {
          val n = Math.min(spaces, block.length)
          buffer.append(block, 0, n)
          spaces -= n
        }
      }

      override fun transferSucceeded(event: TransferEvent) {
        transferCompleted(event)
        val resource: TransferResource = event.resource
        val contentLength: Long = event.transferredBytes
        if (contentLength >= 0) {
          val type =
            if (event.requestType === TransferEvent.RequestType.PUT) "Uploaded" else "Downloaded"
          val len =
            if (contentLength >= 1024) toKB(contentLength).toString() + " KB" else "$contentLength B"
          var throughput = ""
          val duration: Long = System.currentTimeMillis() - resource.transferStartTime
          if (duration > 0) {
            val bytes: Long = contentLength - resource.resumeOffset
            val format = DecimalFormat("0.0", DecimalFormatSymbols(Locale.ENGLISH))
            val kbPerSec = bytes / 1024.0 / (duration / 1000.0)
            throughput = " at " + format.format(kbPerSec) + " KB/sec"
          }
          logger.logInfo("$type: ${resource.repositoryUrl}${resource.resourceName} ($len$throughput)")
        }
      }

      override fun transferFailed(event: TransferEvent) {
        transferCompleted(event)
        if (event.exception !is MetadataNotFoundException) {
          logger.logException(event.exception)
        }
      }

      private fun transferCompleted(event: TransferEvent) {
        downloads.remove(event.resource)
        val buffer = StringBuilder(64)
        pad(buffer, lastLength)
        buffer.append('\r')
        logger.logVerbose(buffer.toString())
      }

      override fun transferCorrupted(event: TransferEvent) {
        logger.logException(event.exception)
      }

      private fun toKB(bytes: Long): Long {
        return (bytes + 1023) / 1024
      }
    }

    class ConsoleRepositoryListener(
      private val logger: ProgressLogger
    ): AbstractRepositoryListener() {
      override fun artifactDeployed(event: RepositoryEvent) {
        logger.logInfo("Deployed ${event.artifact} to ${event.repository}")
      }

      override fun artifactDeploying(event: RepositoryEvent) {
        logger.logInfo("Deploying ${event.artifact} to ${event.repository}")
      }

      override fun artifactDescriptorInvalid(event: RepositoryEvent) {
        logger.logInfo("Invalid artifact descriptor for ${event.artifact}: ${event.exception.message}")
      }

      override fun artifactDescriptorMissing(event: RepositoryEvent) {
        logger.logInfo("Missing artifact descriptor for ${event.artifact}")
      }

      override fun artifactInstalled(event: RepositoryEvent) {
        logger.logInfo("Installed ${event.artifact} to ${event.file}")
      }

      override fun artifactInstalling(event: RepositoryEvent) {
        logger.logInfo("Installing ${event.artifact} to ${event.file}")
      }

      override fun artifactResolved(event: RepositoryEvent) {
        logger.logInfo("Resolved artifact ${event.artifact} from ${event.repository}")
      }

      override fun artifactDownloading(event: RepositoryEvent) {
        logger.logInfo("Downloading artifact ${event.artifact} from ${event.repository}")
      }

      override fun artifactDownloaded(event: RepositoryEvent) {
        logger.logInfo("Downloaded artifact ${event.artifact} from ${event.repository}")
      }

      override fun artifactResolving(event: RepositoryEvent) {
        logger.logInfo("Resolving artifact ${event.artifact}")
      }

      override fun metadataDeployed(event: RepositoryEvent) {
        logger.logInfo("Deployed ${event.metadata} to ${event.repository}")
      }

      override fun metadataDeploying(event: RepositoryEvent) {
        logger.logInfo("Deploying ${event.metadata} to ${event.repository}")
      }

      override fun metadataInstalled(event: RepositoryEvent) {
        logger.logInfo("Installed ${event.metadata} to ${event.file}")
      }

      override fun metadataInstalling(event: RepositoryEvent) {
        logger.logInfo("Installing ${event.metadata} to ${event.file}")
      }

      override fun metadataInvalid(event: RepositoryEvent) {
        logger.logInfo("Invalid metadata ${event.metadata}")
      }

      override fun metadataResolved(event: RepositoryEvent) {
        logger.logInfo("Resolved metadata ${event.metadata} from ${event.repository}")
      }

      override fun metadataResolving(event: RepositoryEvent) {
        logger.logInfo("Resolving metadata ${event.metadata} from ${event.repository}")
      }
    }
  }
}