package com.giyeok.bibix.plugins.prelude

import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.BuildContext
import com.giyeok.bibix.base.BuildRuleReturn
import com.giyeok.bibix.base.StringValue
import com.giyeok.bibix.graph.runner.argsMapFrom
import com.giyeok.bibix.interpreter.BibixProject
import com.giyeok.bibix.repo.hashString
import com.giyeok.bibix.utils.toHexString
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import java.io.IOException
import org.eclipse.jgit.api.Git as JGit

class Git {
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
  fun build(context: BuildContext): BuildRuleReturn {
    val gitRepos = context.getSharedDirectory("com.giyeok.bibix.plugins.bibix.git")

    return BuildRuleReturn.withDirectoryLock(gitRepos) {
      // 이 target의 targetId 대신 arguments들의 해시를 ID로 사용
      val repoId = argsMapFrom(context.arguments).hashString().toHexString()
      val gitDirectory = gitRepos.resolve(repoId)

      val existingRepo = try {
        JGit.open(gitDirectory.toFile())
      } catch (e: IOException) {
        null
      }

      val url = (context.arguments.getValue("url") as StringValue).value
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
        JGit.cloneRepository()
          .setURI(url)
          .setDirectory(gitDirectory.toFile())
          .setBranch(refSpec)
          .setCredentialsProvider(credentialsProvider)
          .call()
      }

      repo.checkout().setName(refSpec).call()

      val path = (context.arguments.getValue("path") as StringValue).value
      val projectRoot = gitDirectory.resolve(path)
      val scriptName = context.arguments["scriptName"] as? StringValue

      BuildRuleReturn.value(BibixProject(projectRoot, scriptName?.value).toBibixValue())
    }
  }
}
