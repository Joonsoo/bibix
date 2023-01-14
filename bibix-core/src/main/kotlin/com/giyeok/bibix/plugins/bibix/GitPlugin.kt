package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.*
import com.giyeok.bibix.interpreter.BibixProject
import com.giyeok.bibix.repo.sha1Hash
import com.giyeok.bibix.utils.toHexString
import com.google.protobuf.kotlin.toByteStringUtf8
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import java.io.IOException

class GitPlugin {
  fun refSpec(args: Map<String, BibixValue>): String {
    val ref = (args["ref"] as? StringValue)?.value
    val branch = (args["branch"] as? StringValue)?.value
    val tag = (args["tag"] as? StringValue)?.value

    return when {
      ref != null -> {
        check(branch == null && tag == null)
        ref
      }

      branch != null -> {
        check(tag == null)
        "refs/heads/$branch"
      }

      tag != null -> {
        "refs/tags/$tag"
      }

      else -> "refs/heads/main"
    }
  }

  // TODO buildscript name 지정할 수 있도록?
  fun build(context: BuildContext): BibixValue {
    // TODO root directory lock
    val gitRepos = context.getSharedDirectory("com.giyeok.bibix.plugins.bibix.git")

    val url = (context.arguments.getValue("url") as StringValue).value
    val path = (context.arguments.getValue("path") as StringValue).value

    val gitDirectory = gitRepos.resolve(sha1Hash(url.toByteStringUtf8()).toHexString())

    val existingRepo = try {
      Git.open(gitDirectory.toFile())
    } catch (e: IOException) {
      null
    }

    val refSpec = refSpec(context.arguments)

    val credentialsProvider = CredentialsProvider.getDefault()

    val pulledRepo = if (existingRepo == null) null else {
      context.progressLogger.logInfo("Pulling from the existing repository $url @ $refSpec")
      val remotes = existingRepo.remoteList().call()
      val remoteName = remotes.find { it.urIs.contains(URIish(url)) }
      if (remoteName == null) null else {
        existingRepo.pull()
          .setRemote(remoteName.name)
          .setRemoteBranchName(refSpec)
          .setCredentialsProvider(credentialsProvider)
          .call()
        existingRepo.checkout()
          .setName(refSpec)
          .call()
        existingRepo
      }
    }

    val repo = if (pulledRepo != null) pulledRepo else {
      context.progressLogger.logInfo("Cloning git repository from $url @ $refSpec")
      Git.cloneRepository()
        .setURI(url)
        .setDirectory(gitDirectory.toFile())
        .setBranch(refSpec)
        .setCredentialsProvider(credentialsProvider)
        .call()
    }

    repo.checkout().setName(refSpec).call()

    val projectRoot = gitDirectory.resolve(path)
    val scriptName = context.arguments["scriptName"] as? StringValue

    return ClassInstanceValue(
      "com.giyeok.bibix.plugins.bibix",
      "BibixProject",
      listOfNotNull(
        "projectRoot" to PathValue(projectRoot),
        scriptName?.let { "scriptName" to it }
      ).toMap()
    )
  }
}
