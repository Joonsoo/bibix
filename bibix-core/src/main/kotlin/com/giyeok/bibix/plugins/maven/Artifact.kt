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
import java.io.PrintStream
import java.nio.file.Path
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Artifact {
  fun build(context: BuildContext): BuildRuleReturn {
    val mavenRepos = context.getSharedDirectory("com.giyeok.bibix.plugins.maven")
    return BuildRuleReturn.withDirectoryLock(mavenRepos) {
      // TODO resolve 결과 캐시해놨다 그대로 사용
      // TODO -> bibix value proto로 저장/로드할 때 CName sourceId 복구 문제
      // TODO -> 프로젝트 전체 디렉토리를 옮겼을 때 문제가 없을지 확인
      val groupId = (context.arguments.getValue("group") as StringValue).value
      val artifactId = (context.arguments.getValue("artifact") as StringValue).value
      val extension = (context.arguments.getValue("extension") as StringValue).value
      val version = (context.arguments["version"] as? StringValue)?.value
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

//    val repositories = listOf(
//      RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build()
//    )

      val system: RepositorySystem = newRepositorySystem()
      val session: RepositorySystemSession = newRepositorySystemSession(mavenRepos, system)

      val artifact = DefaultArtifact(groupId, artifactId, "", extension, version)
      val repositories = newRepositories(system, session)

      val artifactRequest = ArtifactRequest()
      artifactRequest.artifact = artifact
      artifactRequest.repositories = repositories

      val collectRequest = CollectRequest()
      collectRequest.root = Dependency(artifact, javaScope)
      collectRequest.repositories = repositories
      val classpathFilter = DependencyFilterUtils.classpathFilter(javaScope)
      val dependencyRequest = DependencyRequest(collectRequest, classpathFilter)
      val dependencyResult = system.resolveDependencies(session, dependencyRequest)
//    println("dependencies: $dependencyResult")
//    println(dependencyResult.artifactResults)

      val artifactsMap = dependencyResult.artifactResults.associateBy { it.artifact }

      fun traverse(node: DependencyNode): ClassPkg {
        val artifactResult = artifactsMap.getValue(node.artifact)
        return ClassPkg(
          mavenDep("central", artifactResult.artifact),
          JarInfo(artifactResult.artifact.file.toPath(), null),
          node.children.filter { artifactsMap.containsKey(it.artifact) }.map { traverse(it) },
        )
      }

      val dep = traverse(dependencyResult.root)
      BuildRuleReturn.value(dep.toBibix())
    }
  }

  private fun mavenDep(repo: String, artifact: Artifact): MavenDep =
    MavenDep(repo, artifact.groupId, artifact.artifactId, artifact.version)

  private fun newRepositorySystem(): RepositorySystem {
    // from https://github.com/snyk/aether-demo/blob/master/aether-demo-snippets/src/main/java/org/eclipse/aether/examples/manual/ManualRepositorySystemFactory.java
    val locator: DefaultServiceLocator = MavenRepositorySystemUtils.newServiceLocator()
    locator.addService(
      RepositoryConnectorFactory::class.java,
      BasicRepositoryConnectorFactory::class.java
    )
    locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
    locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
    locator.setErrorHandler(object : DefaultServiceLocator.ErrorHandler() {
      override fun serviceCreationFailed(type: Class<*>?, impl: Class<*>?, exception: Throwable) {
        exception.printStackTrace()
      }
    })
    return locator.getService(RepositorySystem::class.java)
  }

  fun newRepositorySystemSession(
    localRepoBaseDirectory: Path,
    system: RepositorySystem
  ): DefaultRepositorySystemSession {
    val session = MavenRepositorySystemUtils.newSession()
    // TODO repo 위치 수정 - build rule 사이에 공유 가능한 위치가 필요하네..
    val localRepo = LocalRepository(localRepoBaseDirectory.toFile())
    session.localRepositoryManager = system.newLocalRepositoryManager(session, localRepo)
    session.transferListener = ConsoleTransferListener()
    session.repositoryListener = ConsoleRepositoryListener()
    session.updatePolicy = RepositoryPolicy.UPDATE_POLICY_DAILY

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

  class ConsoleTransferListener @JvmOverloads constructor(out: PrintStream? = null) :
    AbstractTransferListener() {
    private val out: PrintStream
    private val downloads: MutableMap<TransferResource, Long> =
      ConcurrentHashMap<TransferResource, Long>()
    private var lastLength = 0

    init {
      this.out = out ?: System.out
    }

    override fun transferInitiated(event: TransferEvent) {
      val message =
        if (event.requestType === TransferEvent.RequestType.PUT) "Uploading" else "Downloading"
      out.println(
        message + ": " + event.resource.repositoryUrl + event.resource.resourceName
      )
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
      out.print(buffer)
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
        out.println(
          type + ": " + resource.repositoryUrl + resource.resourceName + " (" + len
            + throughput + ")"
        )
      }
    }

    override fun transferFailed(event: TransferEvent) {
      transferCompleted(event)
      if (event.exception !is MetadataNotFoundException) {
        event.exception.printStackTrace(out)
      }
    }

    private fun transferCompleted(event: TransferEvent) {
      downloads.remove(event.resource)
      val buffer = StringBuilder(64)
      pad(buffer, lastLength)
      buffer.append('\r')
      out.print(buffer)
    }

    override fun transferCorrupted(event: TransferEvent) {
      event.exception.printStackTrace(out)
    }

    private fun toKB(bytes: Long): Long {
      return (bytes + 1023) / 1024
    }
  }

  class ConsoleRepositoryListener @JvmOverloads constructor(out: PrintStream? = null) :
    AbstractRepositoryListener() {
    private val out: PrintStream

    init {
      this.out = out ?: System.out
    }

    override fun artifactDeployed(event: RepositoryEvent) {
      out.println("Deployed " + event.artifact.toString() + " to " + event.repository)
    }

    override fun artifactDeploying(event: RepositoryEvent) {
      out.println("Deploying " + event.artifact.toString() + " to " + event.repository)
    }

    override fun artifactDescriptorInvalid(event: RepositoryEvent) {
      out.println(
        "Invalid artifact descriptor for " + event.artifact.toString() + ": "
          + event.exception.message
      )
    }

    override fun artifactDescriptorMissing(event: RepositoryEvent) {
      out.println("Missing artifact descriptor for " + event.artifact)
    }

    override fun artifactInstalled(event: RepositoryEvent) {
      out.println("Installed " + event.artifact.toString() + " to " + event.file)
    }

    override fun artifactInstalling(event: RepositoryEvent) {
      out.println("Installing " + event.artifact.toString() + " to " + event.file)
    }

    override fun artifactResolved(event: RepositoryEvent) {
      out.println(
        "Resolved artifact " + event.artifact.toString() + " from " + event.repository
      )
    }

    override fun artifactDownloading(event: RepositoryEvent) {
      out.println(
        "Downloading artifact " + event.artifact.toString() + " from " + event.repository
      )
    }

    override fun artifactDownloaded(event: RepositoryEvent) {
      out.println(
        "Downloaded artifact " + event.artifact.toString() + " from " + event.repository
      )
    }

    override fun artifactResolving(event: RepositoryEvent) {
      out.println("Resolving artifact " + event.artifact)
    }

    override fun metadataDeployed(event: RepositoryEvent) {
      out.println("Deployed " + event.metadata.toString() + " to " + event.repository)
    }

    override fun metadataDeploying(event: RepositoryEvent) {
      out.println("Deploying " + event.metadata.toString() + " to " + event.repository)
    }

    override fun metadataInstalled(event: RepositoryEvent) {
      out.println("Installed " + event.metadata.toString() + " to " + event.file)
    }

    override fun metadataInstalling(event: RepositoryEvent) {
      out.println("Installing " + event.metadata.toString() + " to " + event.file)
    }

    override fun metadataInvalid(event: RepositoryEvent) {
      out.println("Invalid metadata " + event.metadata)
    }

    override fun metadataResolved(event: RepositoryEvent) {
      out.println(
        "Resolved metadata ${event.metadata} from ${event.repository}"
      )
    }

    override fun metadataResolving(event: RepositoryEvent) {
      out.println(
        "Resolving metadata ${event.metadata} from ${event.repository}"
      )
    }
  }
}
